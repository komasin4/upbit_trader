package komasin4.finance.upbit.scheduler;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import komasin4.finance.upbit.mapper.CandleMapper;
import komasin4.finance.upbit.model.DayCandleModel;
import komasin4.finance.upbit.model.MinuteCandleModel;
import komasin4.finance.upbit.model.OrderModel;
import komasin4.finance.upbit.model.SignalModel;
import komasin4.finance.upbit.service.CandleService;
import komasin4.finance.upbit.service.OrderService;
import komasin4.finance.upbit.service.SendMessageService;

@Service
public class MonitorScheduler {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired 
	private TaskScheduler taskScheduler;
	
	@Autowired
	CandleService candleService;

	@Autowired
	CandleMapper candleMapper;
	
	@Autowired
	OrderService orderService;
	
	@Autowired
	SendMessageService sendMessageService;
	
	@Value("${spring.profiles.active}")
	private String activeProfile;

	DecimalFormat currency = new DecimalFormat("###,###");
	DecimalFormat round = new DecimalFormat("#.##");
	DecimalFormat vol = new DecimalFormat("#.########");

	private DayCandleModel todayCandle;

	private MinuteCandleModel snapShotCandle;

	private String finalSignalMinTime;
	private double finalSignalMinValue = 0;
	private String finalSignalMaxTime;
	private double finalSignalMaxValue = 0;
	private int checkCount = 0;
	
	private boolean b20SellSig = true;		//********** 20선 매도 시그널 발생 여부
	private boolean bBBSellSig = true;
	private boolean bMAXSellSig  = true;
	private boolean bBBBuySig  = true;
	private boolean b20BuySig  = true;
	private boolean bMINBuySig  = true;
	
	private boolean bSignal20Sell = true;
	
	private double buyMinPrice = 0;

	private double signalBuyValueBB = 0;
	private double signalBuyValueMIN = 0;

	private double price_unit = 1000;   //호가단위 1,000원
	private double volume_unit = 15000; //**********매수단위 15,000원
	private double incomeLimitPercent = 0.002; //**********매수 가격보다 incomeLimit 만큼 비싸게 팔아야 수수료 빼고 수익
	
	private int multi_BB = 2;	//********** BB 하단 도달시 매수 비율 (volume_unit * multi_BB)
	private int multi_MIN = 6;  //********** 최저가 도달시 매수 비율 (volume_unit * multi_MIN)
	
	private int iMinBaseUnit = 120;	//**********최저가 기준 봉 갯수
	
	ScheduledFuture<?> task;
	private final int initFixedRate = 200;
	
	public void remove() {
		task.cancel(true);
		if(task.isCancelled())	{
			logger.info("monitor stopped!!!");
		} else {
			logger.info("monitor stop failed!!!");
		}
	}

	public void start(int iFixedRate) {
		if(task == null || task.isCancelled())	{
			task = taskScheduler.scheduleAtFixedRate(()->startMonitor(), iFixedRate);
			logger.debug("started!!!");
		} else	{
			logger.debug("already started!!!");
		}
	}

	@PostConstruct
	public void start()	{
//		if("real".equals(activeProfile))	{
//			if(task == null)
//				task = taskScheduler.scheduleAtFixedRate(()->startMonitor(), initFixedRate);
//		}
	}
	
	//@Scheduled(initialDelay = 1000, fixedRate = 200)
	public void startMonitor()	{

		try {

			List<MinuteCandleModel> candles = candleMapper.selectMinuteCandles(null, 2);

			MinuteCandleModel currCandle = candles.get(0);	
			MinuteCandleModel lastCandle = candles.get(1);


			if(todayCandle == null)	{	//최초 실생시 일봉 데이터 가져옴
				todayCandle = candleService.getDayCandle().get(0);
				logger.info("초기데이터 저장 (일봉) : " + todayCandle.toString());
			} else {
				String todayCandleDate = todayCandle.getCandle_date_time_utc().substring(0, 10);
				String currCandleDate =  currCandle.getCandle_date_time_utc().substring(0, 10);
				if(!todayCandleDate.equals(currCandleDate))	{	//날짜가 바뀌면 일봉 데이터 갱신
					todayCandle = candleService.getDayCandle().get(0);
					logger.info("일봉데이터 변경 : " + todayCandle.toString());
				}
			}

			if(snapShotCandle == null)	{	//최초 실행시 snapshot candle 초기화
				snapShotCandle = currCandle;
				logger.info("초기데이터 저장 (분봉) : " + currCandle.toString());
			}

			//BB 활용 매수/매도 타이밍 -- from
			int location = getLocation(currCandle);
			int snapshotLocation = getLocation(snapShotCandle);
			int lastCandleLocation = getLocation(lastCandle);

			int diff = location - lastCandleLocation;
			int signalType = getSignalType(location, diff);
			
			
			Map<String, Double> valueMap = candleMapper.selectMaxMinValue(currCandle.getCandle_time(), iMinBaseUnit);
			
			if(finalSignalMaxValue == 0)
				finalSignalMaxValue = valueMap.get("maxPrice");
			
			if(finalSignalMinValue == 0)
				finalSignalMinValue = valueMap.get("minPrice");
			
			if(currCandle.getTrade_price() > valueMap.get("maxPrice") && currCandle.getTrade_price() > finalSignalMaxValue)	{	//신고가
				signalType = 100;
			} else if(currCandle.getTrade_price() < valueMap.get("minPrice") && currCandle.getTrade_price() < finalSignalMinValue) {	//신저가
				signalType = -100;
			}

			double order_price = 0;
			double volume = 0;;
			String side = "";

			SignalModel signal = new SignalModel(currCandle.getCandle_date_time_utc(), currCandle.getCandle_date_time_kst(), signalType, currCandle.getTrade_price(), volume, "N");
			
			if(signalType == 2 && bBBSellSig)	{			//상승하여 BB상단 터치(매도)
				logger.info("BB상단 터치 (매도)\t");
				bBBSellSig = false;
				b20SellSig = false;
				bMAXSellSig  = true;
				bBBBuySig	= true;
				b20BuySig	= true;
				bMINBuySig  = true;
				side = "ask";
				order_price = currCandle.getTrade_price() - price_unit;
				List<SignalModel> sellList = candleMapper.selectTradeQueue(order_price - order_price * incomeLimitPercent);
				logger.info("sellList size:" + sellList.size());
				for(SignalModel sell : sellList)	{
					logger.info(sell.getTime_kst() + ":" + sell.getSignal_type() + ":" + sell.getSignal_price());
					OrderModel order = new OrderModel(side, order_price, sell.getVolume());
					boolean bExcuteSell = orderService.order(order);
					logger.debug("bExcuteSell:" + bExcuteSell);
					if(bExcuteSell)	{
						sell.setUp_signal_price(order_price);
						sell.setUp_signal_type(signalType);
						logger.info("update row:" + sell.getTrade_no() + ":" + candleMapper.updateTradeQueue(sell));
						
						StringBuffer sb = new StringBuffer()
											.append("BB상단 터치 (매도)\n") 
											.append("매도가  : ").append(currency.format(order_price)).append("\n") 
											.append("매도수량 : ").append(vol.format(sell.getVolume())).append("\n")
											.append("매도금액 : ").append(currency.format((order_price*sell.getVolume()))).append("\n");
											
						sendMessageService.send(sb.toString());
					}
				}
			} else if(signalType == 1 && b20SellSig && bSignal20Sell)	{	//상승하여 20선 터치(매도)
//				bBBSellSig = true;
//				b20SellSig = false;
//				bMAXSellSig  = true;
//				bBBBuySig	= true;
//				b20BuySig	= true;
//				bMINBuySig	= true;
				side = "ask";
				order_price = currCandle.getTrade_price() - price_unit;
				List<SignalModel> sellList = candleMapper.selectTradeQueue(order_price - order_price * incomeLimitPercent);
				//logger.info("sellList size:" + sellList.size());
				int sellCnt = 0;
				for(SignalModel sell : sellList)	{
					logger.info(sell.getTime_kst() + ":" + sell.getSignal_type() + ":" + sell.getSignal_price());
					OrderModel order = new OrderModel(side, order_price, sell.getVolume());
					boolean bExcuteSell = orderService.order(order);
					logger.debug("bExcuteSell:" + bExcuteSell);
					if(bExcuteSell)	{
						sell.setUp_signal_price(order_price);
						sell.setUp_signal_type(signalType);
						logger.info("update row:" + sell.getTrade_no() + ":" + candleMapper.updateTradeQueue(sell));
						sellCnt++;
						StringBuffer sb = new StringBuffer()
								.append("20선 터치 (매도)\n") 
								.append("매도가  : ").append(currency.format(order_price)).append("\n") 
								.append("매도수량 : ").append(vol.format(sell.getVolume())).append("\n")
								.append("매도금액 : ").append(currency.format((order_price*sell.getVolume()))).append("\n");
						sendMessageService.send(sb.toString());
						//sendMessageService.send("20선 터치 (매도):" + currency.format(order_price) + ":" +  vol.format(sell.getVolume()) + ":" + currency.format((order_price*sell.getVolume())));
					}
				}
				if(sellCnt > 0)	{
					logger.info("20선 터치 (매도)\t");
					bBBSellSig = true;
					b20SellSig = false;
					bMAXSellSig  = true;
					bBBBuySig	= true;
					b20BuySig	= true;
					bMINBuySig	= true;
				} else {
					
				}
			} else if(signalType == 100 && bMAXSellSig){	//신고가(매도)
				logger.info("신고가(매도)\t");
				bBBSellSig = false;
				b20SellSig = false;
				bMAXSellSig  = false;
				bBBBuySig	= true;
				b20BuySig	= true;
				bMINBuySig	= true;
				side = "ask";
				order_price = currCandle.getTrade_price() - price_unit;
				List<SignalModel> sellList = candleMapper.selectTradeQueue(order_price - order_price * incomeLimitPercent);
				logger.info("sellList size:" + sellList.size());
				for(SignalModel sell : sellList)	{
					logger.info(sell.getTime_kst() + ":" + sell.getSignal_type() + ":" + sell.getSignal_price());
					OrderModel order = new OrderModel(side, order_price, sell.getVolume());
					boolean bExcuteSell = orderService.order(order);
					logger.debug("bExcuteSell:" + bExcuteSell);
					if(bExcuteSell)	{
						sell.setUp_signal_price(order_price);
						sell.setUp_signal_type(signalType);
						logger.info("update row:" + sell.getTrade_no() + ":" + candleMapper.updateTradeQueue(sell));
						
						StringBuffer sb = new StringBuffer()
								.append("신고가(매도)\n") 
								.append("매도가  : ").append(currency.format(order_price)).append("\n") 
								.append("매도수량 : ").append(vol.format(sell.getVolume())).append("\n")
								.append("매도금액 : ").append(currency.format((order_price*sell.getVolume()))).append("\n");

						sendMessageService.send(sb.toString());

						//sendMessageService.send("신고가(매도):" + currency.format(order_price) + ":" +  vol.format(sell.getVolume()) + ":" + currency.format((order_price*sell.getVolume())));
					}
				}
			} else if(signalType == -1 && b20BuySig)	{	//하락하여 20선 터치(매수)
				logger.info("20선 터치 (매수)\t");
				bBBSellSig = true;
				b20SellSig = false;
				bMAXSellSig  = true;
				bBBBuySig	= true;
				b20BuySig	= false;
				bMINBuySig	= true;
				side = "bid";
				order_price = currCandle.getTrade_price() + price_unit;
				volume = volume_unit/order_price;
				signal.setVolume(volume);
				OrderModel order = new OrderModel(side, order_price, signal.getVolume());
				boolean bExcuteSell = orderService.order(order);
				if(bExcuteSell)	{
					candleMapper.insertTradeQueue(signal);
					StringBuffer sb = new StringBuffer()
							.append("20선 터치 (매수)\n") 
							.append("매수가  : ").append(currency.format(order_price)).append("\n") 
							.append("매수수량 : ").append(vol.format(volume)).append("\n")
							.append("매수금액 : ").append(currency.format((order_price*volume))).append("\n");
					sendMessageService.send(sb.toString());
					//sendMessageService.send("20선 터치 (매수):" + currency.format(order_price) + ":" +  vol.format(volume) + ":" + currency.format((order_price*volume)));
				}
				//candleMapper.updateTradeQueue(signal.getTrade_no());
			} else if(signalType == -2 && (bBBBuySig || (signalBuyValueBB-order_price)/signalBuyValueBB < (0.3/100)))	{	//하락하여 BB하단 터치(매수)
				logger.info("BB하단 터치(매수)\t");
				bBBSellSig = true;
				b20SellSig = true;
				bMAXSellSig  = true;
				bBBBuySig	= false;
				b20BuySig	= false;
				bMINBuySig	= true;
				side = "bid";
				order_price = currCandle.getTrade_price() + price_unit;
				volume = volume_unit*multi_BB/order_price;
				signal.setVolume(volume);
				OrderModel order = new OrderModel(side, order_price, signal.getVolume());
				boolean bExcuteSell = orderService.order(order);
				if(bExcuteSell)	{
					candleMapper.insertTradeQueue(signal);
					StringBuffer sb = new StringBuffer()
							.append("BB하단 터치(매수)\n") 
							.append("매수가  : ").append(currency.format(order_price)).append("\n") 
							.append("매수수량 : ").append(vol.format(volume)).append("\n")
							.append("매수금액 : ").append(currency.format((order_price*volume))).append("\n");
					sendMessageService.send(sb.toString());
					//sendMessageService.send("BB하단 터치(매수):" + currency.format(order_price) + ":" +  vol.format(volume) + ":" + currency.format((order_price*volume)));
				}
				signalBuyValueBB = order_price;
				//candleMapper.updateTradeQueue(signal.getTrade_no());
			} else if(signalType == -100 && (bMINBuySig || (signalBuyValueMIN-order_price)/signalBuyValueMIN < (0.3/100))){	//신저가(매수)
				logger.info("신저가(매수)\t");
				bBBSellSig = true;
				b20SellSig = true;
				bMAXSellSig  = true;
				bBBBuySig	= false;
				b20BuySig	= false;
				bMINBuySig	= false;
				buyMinPrice = order_price;
				side = "bid";
				order_price = currCandle.getTrade_price() + price_unit;
				volume = volume_unit*multi_MIN/order_price;
				signal.setVolume(volume);
				OrderModel order = new OrderModel(side, order_price, signal.getVolume());
				boolean bExcuteSell = orderService.order(order);
				if(bExcuteSell)	{
					candleMapper.insertTradeQueue(signal);
					
					StringBuffer sb = new StringBuffer()
							.append("신저가(매수)\n") 
							.append("매수가  : ").append(currency.format(order_price)).append("\n") 
							.append("매수수량 : ").append(vol.format(volume)).append("\n")
							.append("매수금액 : ").append(currency.format((order_price*volume))).append("\n");
					sendMessageService.send(sb.toString());
					//sendMessageService.send("신저가(매수):" + currency.format(order_price) + ":" +  vol.format(volume) + ":" + currency.format((order_price*volume)));
				}
				signalBuyValueMIN = order_price;
				//candleMapper.updateTradeQueue(signal.getTrade_no());
			}
			
			if("bid".equals(side))	{			//매수
				
			} else if ("ask".equals(side))	{	//매도
				
			}
				
			
			String strSignals = signalType + "\t" 
					+ (bBBSellSig?"T":"F") + ":" + (b20SellSig?"T":"F") + ":" + (bMAXSellSig?"T":"F") + "/" 
					+ (bBBBuySig?"T":"F") + ":" + (b20BuySig?"T":"F") + ":" + (bMINBuySig?"T":"F")
					+ "\t" + side + "\t" + currency.format(order_price) + "\t" + vol.format(volume);
			
			if (currCandle.getTrade_price() != snapShotCandle.getTrade_price()) {
				logger.info(checkCount + "\t" + strSignals + "\t" + currCandle.getCandle_time() + "\t" + currency.format(currCandle.getTrade_price()) + "\t" + currency.format((currCandle.getTrade_price() - todayCandle.getPrev_closing_price())) + "\t\t" + location + "\t" + currency.format(currCandle.getBb_upper()) + "\t"  + currency.format(currCandle.getAvg_20()) + "\t" + currency.format(currCandle.getBb_lower()) + "\t" + currency.format(signalBuyValueBB) + "\t" + currency.format(signalBuyValueMIN) + "\t" + currency.format(finalSignalMaxValue) + "\t" + currency.format(finalSignalMinValue));
			} else if (checkCount%100 == 0) {
				logger.info(checkCount + "\t" + strSignals + "\t" + currCandle.getCandle_time() + "\t" + currency.format(currCandle.getTrade_price()) + "\t" + currency.format((currCandle.getTrade_price() - todayCandle.getPrev_closing_price())) + "\t\t" + location + "\t" + currency.format(currCandle.getBb_upper()) + "\t"  + currency.format(currCandle.getAvg_20()) + "\t" + currency.format(currCandle.getBb_lower()) + "\t" + currency.format(signalBuyValueBB) + "\t" + currency.format(signalBuyValueMIN) + "\t" + currency.format(finalSignalMaxValue) + "\t" + currency.format(finalSignalMinValue));
			}


			//이전 봉에서 신저가 발생시 신저가를 종가로 update 해즘.
			if(lastCandle.getCandle_time().equals(finalSignalMinTime))
				finalSignalMinValue = lastCandle.getTrade_price();

			if(lastCandle.getCandle_time().equals(finalSignalMaxTime))
				finalSignalMaxValue = lastCandle.getTrade_price();

			
			//분봉이 결정되면 매도 signal reset
			if(!currCandle.getCandle_time().equals(snapShotCandle.getCandle_time()))	{
				bBBSellSig = true;
				b20SellSig = true;
				bMAXSellSig  = true;
			}

			checkCount++;
			snapShotCandle = currCandle;	//다음 스케쥴에서 활용하기 위해 현재 candle 저장
		} catch (Exception e)	{
			logger.error("message", e);
		}
	}

	private int getSignalType(int location, int diff)	{
		int signal = 0;

		switch(location)	{
		case(3):				//BB상단 위
			if(diff > 0)		//상승하여 BB 상단 터치 - 매도
				signal = 2;
			break;
		case(2):				//BB상단
			if(diff > 0)		//상승하여 BB상단 터치 - 매도
				signal = 2;
			break;
		case(1):				//20선 위
			if(diff > 0)		//상승하여 20선 터치 - 매도
				signal = 1;
			break;
		case(0):				//20선
			if(diff > 0)		//상승 하여 20선 터치 - 매도
				signal = 1;		
			else if(diff < 0)	//하락하여 20선 터치 - 매수
				signal = -1;
			break;
		case(-1):				//20선 아래
			if(diff < 0)		//하락하여 20선 터치 - 매수
				signal = -1;
			break;
		case(-2):				//BB하단
			if(diff < 0)		//하락하여 BB하단 - 매수
				signal = -2;
			break;
		case(-3):				//BB하단 아래
			if(diff < 0)		//하락하여 BB하단 - 매수
				signal = -2;
			break;
		}
		return signal;
	}

	private int getLocation(MinuteCandleModel candle)	{
		int rtnLocation = 0;
		
		if(candle.getTrade_price() > candle.getBb_upper())
			rtnLocation = 3;
		else if(candle.getTrade_price() == candle.getBb_upper())
			rtnLocation = 2;
		else if(candle.getTrade_price() > candle.getAvg_20())
			rtnLocation = 1;
		else if(candle.getTrade_price() == candle.getAvg_20())
			rtnLocation = 0;
		else if(candle.getTrade_price() < candle.getBb_lower())
			rtnLocation = -3;
		else if(candle.getTrade_price() == candle.getBb_lower())
			rtnLocation = -2;
		else if(candle.getTrade_price() < candle.getAvg_20())
			rtnLocation = -1;

		return rtnLocation;
	}
	
	public double getMinBaseUnit()	{
		return iMinBaseUnit;
	}

	public double getIcomeLimitPercent()	{
		return incomeLimitPercent;
	}

	public boolean getSignal20Sell()	{
		return bSignal20Sell;
	}
	
	public double getVolumeUnit()	{
		return volume_unit;
	}

	public double getMultiBB()	{
		return multi_BB;
	}

	public double getMultiMIN()	{
		return multi_MIN;
	}

	public void setMinBaseUnit(int value){
		iMinBaseUnit = value;
		try {
			updateMaxMinValue();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.info("set iMinBaseUnit = " + iMinBaseUnit);
	}

	public void setMultiBB(int value)	{
		multi_BB = value;
		logger.info("set multi_BB = " + multi_BB);
	}

	public void setMultiMIN(int value)	{
		multi_MIN = value;
		logger.info("set multi_MIN = " + multi_MIN);
	}

	public void setSignal20Sell(boolean bSet)	{
		bSignal20Sell = bSet;
		logger.info("set bSignal20Sell = " + bSignal20Sell);
	}

	public void setVolumeUnit(double value)	{
		volume_unit = value;
		logger.info("set volume_unit = " + volume_unit);
	}

	public void setIncomeLimitPercent(double value)	{
		incomeLimitPercent = value;
		logger.info("set incomeLimitPercent = " + incomeLimitPercent);
	}
	
	private void updateMaxMinValue() throws Exception	{
		Map<String, Double> valueMap = candleMapper.selectMaxMinValue(snapShotCandle.getCandle_time(), iMinBaseUnit);
		
		finalSignalMaxValue = valueMap.get("maxPrice");
		finalSignalMinValue = valueMap.get("minPrice");

		logger.info("set finalSignalMaxValue = " + finalSignalMaxValue + ", finalSignalMinValue = " + finalSignalMinValue);
	}
	
	public MinuteCandleModel getSnapShotCandle()	{
		return snapShotCandle;
	}
}
