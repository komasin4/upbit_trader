package komasin4.finance.upbit.scheduler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import komasin4.finance.upbit.mapper.CandleMapper;
import komasin4.finance.upbit.model.DayCandleModel;
import komasin4.finance.upbit.model.MinuteCandleModel;
import komasin4.finance.upbit.model.OrderModel;
import komasin4.finance.upbit.model.SignalModel;
import komasin4.finance.upbit.service.CandleService;
import komasin4.finance.upbit.service.OrderService;
import komasin4.finance.upbit.service.SendMessageService;
import komasin4.finance.upbit.util.MonitorUtil;
import komasin4.finance.upbit.base.F;


//20선 도달시 매수
//매수 이후 1분 이내 다시 매수 신호 발생시는 패스
//매수 신호 이후 1분 초과 했으나 가격차이가 XX% 미만이면 패스

//BB하단 도달시 매수
//매수 이후 1분 이내 다시 매수 신호 발생시는 패스
//매수 신호 이후 1분 초과 했으나 가격차이가 XX% 미만이면 패스


//신저가 발생시 매수
//매수 신호 이후 1분 초과 했으나 가격차이가 XX% 미만이면 패스
//신저가 매수 이후 XX% 이상 하락 하면 추가 매수.

@Service
public class NewMonitorScheduler extends BaseScheduler {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	CandleService candleService;

	@Autowired
	CandleMapper candleMapper;
	
	@Autowired
	SendMessageService sendMessageService;
	
	@Autowired
	OrderService orderService;
	
	@Autowired 
	private TaskScheduler taskScheduler;
	
	@Value("${spring.profiles.active}")
	private String activeProfile;
	
	private final int initFixedRate = 300;

	//20선 도달시 매수
	//매수 이후 1분 이내 다시 매수 신호 발생시는 패스
	//매수 신호 이후 1분 초과 했으나 가격차이가 XX% 미만이면 패스

	//BB하단 도달시 매수
	//매수 이후 1분 이내 다시 매수 신호 발생시는 패스
	//매수 신호 이후 1분 초과 했으나 가격차이가 XX% 미만이면 패스

	//신저가 발생시 매수
	//매수 신호 이후 1분 초과 했으나 가격차이가 XX% 미만이면 패스
	//신저가 매수 이후 XX% 이상 하락 하면 추가 매수.
	
	
	boolean bTrade = false;
	

	private DayCandleModel todayCandle;
	double maxPrice = 0;
	double minPrice = 0;
	
	private MinuteCandleModel snapShotCandle;	//바로직전분봉
	
	int iMinBaseUnit = 120;

	private double lastSellSignalPrice = 1000;  //최근 매도 신호 발생 가격 
	private double lastBuySignalPrice  = 0;		//최근 매수 신호 발생 가격
	private int checkCount = 0;
	
	private double price_unit = 1000;   		//호가단위 1,000원

	private double fallRate = 0.003;			//매수기준 하락 폭 - 가장 최근 매수 가격보다 이 % 이상 떨어져야 매수 
	private double raiseRate = 0.003;			//매도기준 상승 폭 - 가장 최근 매도 가격보다 이 % 이상 상승하면 매도 
	private double volume_unit = 10000; 		//**********매수단위 15,000원
	private double incomeLimitPercent = 0.002;	//**********매수 가격보다 incomeLimit 만큼 비싸게 팔아야 수수료 빼고 수익
	private int multi_BB = 3;					//********** BB 하단 도달시 매수 비율 (volume_unit * multi_BB)
	private int multi_MIN = 10;  				//********** 최저가 도달시 매수 비율 (volume_unit * multi_MIN)
	
	private String sellSignalTime;

	@PostConstruct
	public void startAuto()	{
		
//		if("office".equals(activeProfile) || "local".equals(activeProfile))	{
//			if(task == null) {
//				task = taskScheduler.scheduleAtFixedRate(()->start(), initFixedRate);
//				//task = taskScheduler.scheduleAtFixedRate(()->start(), 2000);
//			}
//		}
	}
	
	@Override
	public void start() {
		// TODO Auto-generated method stub
		super.start();
		//logger.debug("BuyScheduler...");

		try {
			//분봉 정보 가져오기
			List<MinuteCandleModel> candles = candleMapper.selectMinuteCandles(null, 2);
			MinuteCandleModel currCandle = candles.get(0);	
			MinuteCandleModel lastCandle = candles.get(1);
			
			if(snapShotCandle != null && lastCandle.getCandle_time().equals(snapShotCandle.getCandle_time()))	{
				//이전 캔들이 BB 상단의 아래에서 위로 뚫고 올라 갔다면 매도 여부와 상관 없이 다음 매수 신호 발생시는 매수
				logger.debug("candle_time 변경!!! {} -> {}", lastCandle.getCandle_time(), currCandle.getCandle_time());

				List<MinuteCandleModel> preCandles = candleMapper.selectMinuteCandles(null, 3);
				
				int location_1 = MonitorUtil.getLocation(preCandles.get(1));
				int location_2 = MonitorUtil.getLocation(preCandles.get(2));
				
				logger.debug("check location {} -> {}", location_2, location_1);
				
				if(location_1 >= 3 && location_2 < 3)	{
					logger.info("lastBuySignalPrice reset by BB up : {} -> 0", lastBuySignalPrice);
					lastBuySignalPrice = 0;
				}
			}
			
			
			//고가,저가 정보 가져오기
			Map<String, Double> valueMap = candleMapper.selectMaxMinValue(currCandle.getCandle_time(), iMinBaseUnit);
			maxPrice = valueMap.get("maxPrice");
			minPrice = valueMap.get("minPrice");

			//일봉 정보 가져오기
			try	{
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
			}} catch (Exception e)	{
				todayCandle = new DayCandleModel();
			}
			
			//현재 가격의 위치를 가져옴.
			// 3 : BB상한의 위
			// 2 : BB상한
			// 1 : 20선 ~ BB상한 사이
			// 0 : 20선
			//-1 : BB하단 ~ 20선 사이
			//-2 : BB하단
			//-3 : BB하단의 아래
			int currLocation = MonitorUtil.getLocation(currCandle);
			
			//이전봉 종가의 위치를 가져옴.
			int lastLocation = MonitorUtil.getLocation(lastCandle);
			int diff = currLocation - lastLocation;

			//이전분봉기준으로 신호 발생 여부 체크
			int iSignal = MonitorUtil.getSignalType(currLocation, diff);

			if(checkCount % 50 == 0) {
			logger.info("{} {} ({}) Location : {} -> {}    curr:{} sig:{} %:{} min:{} %:{} fall%:{}"
					, checkCount
					, (iSignal>0?"매도":iSignal<0?"매수":"관망")
					, iSignal
					, lastLocation
					, currLocation
					, F.cf.format(currCandle.getTrade_price())
					, F.cf.format(lastBuySignalPrice)
					, F.vf.format((lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice)
					, F.cf.format(minPrice)
					, F.vf.format((minPrice - currCandle.getTrade_price())/minPrice)
					, fallRate
					);
			}
			
			boolean bBuy = false;
			boolean bSell = false;
			
			//매도
			//1. 신호 발생시
			//2. 이전 매도 가격 보다 X % 이상 올라갔을때
			//3. 매수 신호 발생시는 이전 매도 가격 reset
			
			
			if((currCandle.getTrade_price()-lastSellSignalPrice)/lastSellSignalPrice  > raiseRate  || (iSignal > 0 && !currCandle.getCandle_time().equals(sellSignalTime)))	{
//			if((currCandle.getTrade_price()-lastSellSignalPrice)/lastSellSignalPrice  > raiseRate  || (iSignal > 0 && currCandle.getTrade_price() > snapShotCandle.getTrade_price()))	{
				
				int signalType = 1;
				String tmpStr = "시그널";
				
				if((currCandle.getTrade_price()-lastSellSignalPrice)/lastSellSignalPrice  > raiseRate)	{
					tmpStr = "상승폭";
					signalType = 2;
				}
				
			
				logger.warn("(*) {} {} {} (매도): {} : {} : {} : {}", 
						checkCount, iSignal, tmpStr, 
						F.cf.format(currCandle.getTrade_price()), 
						F.cf.format(lastSellSignalPrice), 
						F.vf.format((lastSellSignalPrice - currCandle.getTrade_price())/lastSellSignalPrice), 
						raiseRate);
				
				
				String side = "ask";
				double order_price = currCandle.getTrade_price() - price_unit;
				List<SignalModel> sellList = candleMapper.selectTradeQueue(order_price - order_price * incomeLimitPercent);
				logger.info("sellList size:" + sellList.size());
				for(SignalModel sell : sellList)	{
					logger.info(sell.getTime_kst() + ":" + sell.getSignal_type() + ":" + sell.getSignal_price());
					OrderModel order = new OrderModel(side, order_price, sell.getVolume());

					StringBuffer sb = new StringBuffer();

					if(bTrade)	{
						boolean bExcuteSell = orderService.order(order);
						logger.debug("bExcuteSell:" + bExcuteSell);
						
						if(bExcuteSell)	{
							sell.setUp_signal_price(order_price);
							sell.setUp_signal_type(signalType==1?iSignal:200);	//상승폭 매도일 경우 signal type = 200 으로 세팅
							logger.info("update row:" + sell.getTrade_no() + ":" + candleMapper.updateTradeQueue(sell));
							//bSell = true;
						} else {
							sb.append("주문실패\n");
						}
					}
					
					sb.append("(*)")
					  .append(tmpStr)
					  .append(" 매도\n")
					  .append("매도가     : ").append(F.cf.format(order_price)).append("\n") 
					  .append("매도수량   : ").append(F.vf.format(sell.getVolume())).append("\n")
					  .append("매수금액   : ").append(F.cf.format((order_price*sell.getVolume()))).append("\n")
					  .append("Signal     : ").append(iSignal).append("\n")
					  .append("currPrice  : ").append(F.cf.format(currCandle.getTrade_price())).append("\n")
					  .append("lastPrice  : ").append(F.cf.format(lastSellSignalPrice)).append("\n")
					  .append("diff     % : ").append(F.vf.format((lastSellSignalPrice - currCandle.getTrade_price())/lastSellSignalPrice)).append("\n")
					  .append("raiseRate% : ").append(raiseRate);

					sendMessageService.send(sb.toString());
					
					if(iSignal > 1 || signalType > 1)
						bSell = true;
				}
								
				lastSellSignalPrice = currCandle.getTrade_price();
				//lastBuySignalPrice = 0;
				sellSignalTime = currCandle.getCandle_time();

				//매도 신호 이후 매수 신호 발생시는 매수 처리.
			}				

			if(iSignal < 0)	{   //매수신호 처리

				if(lastBuySignalPrice == 0 || (lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice > fallRate)	{
					//fallRate 이상 하락 했다면 매수
					logger.warn("(*) {} {} 이평선(매수): {} : {} : {} : {}", 
							checkCount, iSignal, 
							F.cf.format(currCandle.getTrade_price()), 
							F.cf.format(lastBuySignalPrice), 
							F.vf.format((lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice), 
							fallRate);
					
					double order_price = currCandle.getTrade_price() + price_unit;
					double volume = volume_unit/order_price;
					
					//BB매도시는 multi_BB 적용
					if(iSignal < -1)
						volume = volume_unit*multi_BB/order_price;
					
					String side = "bid";

					SignalModel signal = new SignalModel(currCandle.getCandle_date_time_utc(), currCandle.getCandle_date_time_kst(), iSignal, currCandle.getTrade_price(), volume, "N");
					signal.setVolume(volume);
					
					StringBuffer sb = new StringBuffer();

					if(bTrade)	{
						OrderModel order = new OrderModel(side, order_price, signal.getVolume());
						boolean bExcuteSell = orderService.order(order);
						
						if(bExcuteSell)	{
							candleMapper.insertTradeQueue(signal);
							//sendMessageService.send("20선 터치 (매수):" + currency.format(order_price) + ":" +  vol.format(volume) + ":" + currency.format((order_price*volume)));
							//bBuy = true;
						} else {
							sb.append("주문실패\n");
						}
					}

					if(iSignal < -1)
						sb.append("(*)BB 도달 매수\n");
					else
						sb.append("(*)20선 도달 매수\n");
					
					sb.append("매수가     : ").append(F.cf.format(order_price)).append("\n") 
					  .append("매수수량   : ").append(F.vf.format(volume)).append("\n")
					  .append("매수금액   : ").append(F.cf.format((order_price*volume))).append("\n")
					  .append("Signal     : ").append(iSignal).append("\n")
					  .append("currPrice  : ").append(F.cf.format(currCandle.getTrade_price())).append("\n")
					  .append("lastPrice  : ").append(F.cf.format(lastBuySignalPrice)).append("\n")
					  .append("diff     % : ").append(F.vf.format((lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice)).append("\n")
					  .append("baseRate % : ").append(fallRate);

					sendMessageService.send(sb.toString());
					
					lastSellSignalPrice = 0;
					lastBuySignalPrice = currCandle.getTrade_price();
					sellSignalTime = currCandle.getCandle_time();
					bBuy = true;
				} else {
					logger.trace("이평선({} 패스): {} : {} : {} : {}", iSignal, 
							F.cf.format(currCandle.getTrade_price()), 
							F.cf.format(lastBuySignalPrice), 
							F.vf.format((lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice), 
							fallRate);
				}
			}
			//이동평균선/볼린저밴드 활용한 매수/매도 신호 체크 -- to
			
			
			//신저가/신고가 활용 매수/매도 신호 체크 -- from
			
			//분봉이 결정되거나 신저가.신고가가 없다면 신저가.신고가 업데이트
//			if(!currCandle.getCandle_time().equals(snapShotCandle.getCandle_time()) || lastMaxPrice == 0 || lastMinPrice == 0)	{
//				setMinMaxPrice(currCandle);
//			}
			
//			if(checkCount % 100 == 0)
//				logger.info("신고가:신저가:" + F.cf.format(lastMaxPrice) + ":" + F.cf.format(lastMinPrice));
			
			
			//1.신저가 발생시 매수 신호 발생
			//직전 매수 신호 보다 ?? % 이하 일 경우만 매수 처리. 아니면 패스 
			if(currCandle.getTrade_price() < minPrice)	{
				if(lastBuySignalPrice == 0 || (lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice > fallRate)	{
					//fallRate 이상 하락 했다면 매수
					logger.warn("(*) 신저가(매수): {} : {} : {} : {}", 
							F.cf.format(currCandle.getTrade_price()), 
							F.cf.format(lastBuySignalPrice), 
							F.vf.format((lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice), 
							fallRate);
//					StringBuffer sb = new StringBuffer()
//							.append("(*)이평선 도달 매수\n")
//							.append("Signal     : ").append(iSignal).append("\n")
//							.append("currPrice  : ").append(F.cf.format(currCandle.getTrade_price())).append("\n")
//							.append("lastPrice  : ").append(F.cf.format(lastBuySignalPrice)).append("\n")
//							.append("diff     % : ").append((lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice).append("\n")
//							.append("baseRate % : ").append(fallRate);
//
					
					double order_price = currCandle.getTrade_price() + price_unit;;
					double volume = volume_unit*multi_MIN/order_price;	//신저가의 경우 multi_MIN 적용
					String side = "bid";

					SignalModel signal = new SignalModel(currCandle.getCandle_date_time_utc(), currCandle.getCandle_date_time_kst(), iSignal, currCandle.getTrade_price(), volume, "N");
					signal.setVolume(volume);
					
					StringBuffer sb = new StringBuffer();
					
					if(bTrade)	{
						OrderModel order = new OrderModel(side, order_price, signal.getVolume());
						boolean bExcuteSell = orderService.order(order);
						
						if(bExcuteSell)	{
							candleMapper.insertTradeQueue(signal);
							//sendMessageService.send("20선 터치 (매수):" + currency.format(order_price) + ":" +  vol.format(volume) + ":" + currency.format((order_price*volume)));
						} else {
							sb.append("주문실패\n");
						}
					}

					sb.append("(*)신저가 도달 매수\n")
					  .append("매수가     : ").append(F.cf.format(order_price)).append("\n") 
					  .append("매수수량   : ").append(F.vf.format(volume)).append("\n")
					  .append("매수금액   : ").append(F.cf.format((order_price*volume))).append("\n")
					  .append("Signal     : ").append(iSignal).append("\n")
					  .append("currPrice  : ").append(F.cf.format(currCandle.getTrade_price())).append("\n")
					  .append("lastPrice  : ").append(F.cf.format(lastBuySignalPrice)).append("\n")
					  .append("diff     % : ").append(F.vf.format((lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice)).append("\n")
					  .append("baseRate % : ").append(fallRate);
					sendMessageService.send(sb.toString());

					lastSellSignalPrice = 0;
					lastBuySignalPrice = currCandle.getTrade_price();
					sellSignalTime = currCandle.getCandle_time();
					bBuy = true;
//					sendMessageService.send(sb.toString());
					
				} else {
					logger.trace("신저가(패스): {} : {} : {} : {}", 
							F.cf.format(currCandle.getTrade_price()), 
							F.cf.format(lastBuySignalPrice), 
							F.vf.format((lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice), 
							fallRate);
				}
			}
			
			//신저가/신고가 활용 매수/매도 신호 체크 -- to
			
			
			//매도시 이전 매수 신호 무조건 리셋
			if(bSell)	{
				//lastBuySignalTime = null;
				logger.info("lastBuySignalPrice reset by bSell : {} -> 0", lastBuySignalPrice);
				lastBuySignalPrice = 0;
			}
			
			//누적 매수 금액이 XX 일 경우 매수 보류 ?
			
			
			checkCount++;
			snapShotCandle = currCandle;	//다음 스케쥴에서 활용하기 위해 현재 candle 저장
			
		} catch (Exception e)	{
			logger.error("message", e);
		}
	}
	
	public String status()	{
		
		/*
	private DayCandleModel todayCandle;
	double maxPrice = 0;
	double minPrice = 0;
	
	private MinuteCandleModel snapShotCandle;	//바로직전분봉
	
	int iMinBaseUnit = 120;

	private double lastBuySignalPrice;			//최근 매수 신호 발생 가격
	private double fallRate = 0.002;	//매수기준 하락 폭 - 가장 최근 매수 가격보다 이 % 이상 떨어져야 매수 
	private int checkCount = 0; 
		 */
		return "checkCount:" + checkCount + " ,date:" + snapShotCandle.getCandle_date_time_kst() + ", tradePrice:" + F.cf.format(snapShotCandle.getTrade_price()) + ", lastBuySignalPrice:" + F.cf.format(lastBuySignalPrice) + ", maxPrice:" + F.cf.format(maxPrice) + ", minPrice:" + F.cf.format(minPrice);
	}
	

/*	
	boolean bTrade = false;
	private double fallRate = 0.002;			//매수기준 하락 폭 - 가장 최근 매수 가격보다 이 % 이상 떨어져야 매수 
	private double raiseRate = 0.002;			//매도기준 상승 폭 - 가장 최근 매도 가격보다 이 % 이상 상승하면 매도 
	private double volume_unit = 15000; 		//**********매수단위 15,000원
	private double incomeLimitPercent = 0.002;	//**********매수 가격보다 incomeLimit 만큼 비싸게 팔아야 수수료 빼고 수익
	private int multi_BB = 2;					//********** BB 하단 도달시 매수 비율 (volume_unit * multi_BB)
	private int multi_MIN = 6;  				//********** 최저가 도달시 매수 비율 (volume_unit * multi_MIN)
 */
	
	public boolean isbTrade() {
		return bTrade;
	}

	public void setbTrade(boolean bTrade) {
		this.bTrade = bTrade;
	}

	public double getFallRate() {
		return fallRate;
	}

	public void setFallRate(double fallRate) {
		this.fallRate = fallRate;
	}

	public double getRaiseRate() {
		return raiseRate;
	}

	public void setRaiseRate(double raiseRate) {
		this.raiseRate = raiseRate;
	}

	public double getVolume_unit() {
		return volume_unit;
	}

	public void setVolume_unit(double volume_unit) {
		this.volume_unit = volume_unit;
	}

	public double getIncomeLimitPercent() {
		return incomeLimitPercent;
	}

	public void setIncomeLimitPercent(double incomeLimitPercent) {
		this.incomeLimitPercent = incomeLimitPercent;
	}

	public int getMulti_BB() {
		return multi_BB;
	}

	public void setMulti_BB(int multi_BB) {
		this.multi_BB = multi_BB;
	}

	public int getMulti_MIN() {
		return multi_MIN;
	}

	public void setMulti_MIN(int multi_MIN) {
		this.multi_MIN = multi_MIN;
	}

	public int getiMinBaseUnit() {
		return iMinBaseUnit;
	}

	public void setiMinBaseUnit(int iMinBaseUnit) {
		this.iMinBaseUnit = iMinBaseUnit;
	}

	@Override
	public String toString() {
		return "NewMonitorScheduler [bTrade=" + bTrade + ", maxPrice=" + maxPrice + ", minPrice=" + minPrice
				+ ", iMinBaseUnit=" + iMinBaseUnit + ", fallRate=" + fallRate + ", raiseRate=" + raiseRate
				+ ", volume_unit=" + volume_unit + ", incomeLimitPercent=" + incomeLimitPercent + ", multi_BB="
				+ multi_BB + ", multi_MIN=" + multi_MIN + "]";
	}
}
