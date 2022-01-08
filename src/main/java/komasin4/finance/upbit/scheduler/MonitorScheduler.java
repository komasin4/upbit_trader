package komasin4.finance.upbit.scheduler;

import java.text.DecimalFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import komasin4.finance.upbit.model.DayCandleModel;
import komasin4.finance.upbit.model.MinuteCandleModel;
import komasin4.finance.upbit.service.CandleService;
import komasin4.finance.upbit.util.DateUtil;

@Service
@Profile("local")
public class MonitorScheduler {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	CandleService candleService;

	DecimalFormat currency = new DecimalFormat("###,###");
	DecimalFormat round = new DecimalFormat("#.##");

	private String prevCandleTime; 
	private int prevCandleLocation = 0;
	private Double prevPrice = 0.0;
	
	//private boolean isNewCandle = false; //new캔들일 경우 oldLoction 갱신
	
	
	private DayCandleModel todayCandle;
	private int location = 0;  //4.BB상단이상, 3.BB상단~20선이상, 2.20선~BB하단이하, 1.BB하단
	private int signalType = 0; //1.매수 2.매도
	private boolean bSignal = false;
	
	
	private int signalCount = 0;
	private int signalLimit = 0;
	private double finalSignalValue = 0;
	
	//@Scheduled(cron = "*/1 * * * * ?")
	//@Scheduled(initialDelay = 1000, fixedRate = 100)
	@Scheduled(initialDelay = 1000, fixedRate = 10000)
	public void startMonitor()	{
		
		List<MinuteCandleModel> candles = candleService.getCandlesFromDB(null, 2);

		String candle_time = candles.get(0).getCandle_time();

		if(todayCandle == null)	{	//최초 실생시 일봉 데이터 가져옴
			todayCandle = candleService.getDayCandle().get(0);
		} else {
			String todayCandleDate = todayCandle.getCandle_date_time_utc().substring(0, 10);
			String currCandleDate=  candles.get(0).getCandle_date_time_utc().substring(0, 10);
			//logger.info("UTC Time:" + todayCandle.getCandle_date_time_utc() + ":" + candles.get(0).getCandle_date_time_utc());
			//logger.info("UTC Time:" + todayCandleDate + ":" + currCandleDate);
			if(!todayCandleDate.equals(currCandleDate))	//날짜가 바뀌면 일봉 데이터 갱신
				todayCandle = candleService.getDayCandle().get(0);
		}

		//logger.debug("get data : " + candle_time + ":" + candles.get(0).getCandle_date_time_utc());
		
		candles.get(0).getAvg_20();
		candles.get(0).getBb_lower();
		candles.get(0).getBb_upper();
		candles.get(0).getHigh_price();
		candles.get(0).getLow_price();
		candles.get(0).getOpening_price();
		double currVal = candles.get(0).getTrade_price();

		candles.get(1).getAvg_20();
		candles.get(1).getBb_lower();
		candles.get(1).getBb_upper();
		candles.get(1).getHigh_price();
		candles.get(1).getLow_price();
		candles.get(1).getOpening_price();
		double prevEndVal = candles.get(1).getTrade_price();

		double diff = currVal - prevEndVal;
		double daydiff = currVal - todayCandle.getPrev_closing_price();


		//		logger.info("현재가:" + currency.format(currVal));

		StringBuffer strBuff = new StringBuffer();

		strBuff.append(currency.format(currVal))
		.append(" ")
		.append(daydiff>0?"+":"")
		.append(round.format(daydiff/todayCandle.getPrev_closing_price() * 100))
		.append(" ")
		.append(currency.format(daydiff))
		.append(" 분봉 ")
		;

		//logger.info("전일대비:" + (daydiff>0?"+":"") + round.format(daydiff/todayCandle.getPrev_closing_price() * 100) + ":" + daydiff);

		if(diff > 0)	{
			//logger.info("+" + currency.format(diff) + ":" + currency.format(currVal));
			strBuff.append("+");
		}

		strBuff.append(currency.format(diff))
		.append(" ")
		.append(currency.format(candles.get(0).getBb_upper()))
		.append(" ")
		.append(currency.format(candles.get(0).getAvg_20()))
		.append(" ")
		.append(currency.format(candles.get(0).getBb_lower()))
		.append(" locastion:")
		.append(location);
		;

		//logger.info(strBuff.toString());

		if(currVal >= candles.get(0).getBb_upper())	{
			//if( prevEndVal < candles.get(1).getBb_upper())	{
				if(location < 4 && location > 0 && signalType == 1)	{
					//logger.info("매도시그널(BB)!! " + currency.format(currVal) + ":" + currency.format(candles.get(0).getBb_upper()));
					signalType = 2;
					signalCount = 0;
				}
			//}
			location = 4;
		} else if(currVal >= candles.get(0).getAvg_20())	{
			//if( prevEndVal < candles.get(1).getAvg_20())	{
				if(location < 3 && location > 0 && signalType == 1)	{
					//logger.info("매도시그널(20)!! " + currency.format(currVal) + ":" + currency.format(candles.get(0).getAvg_20()));
					signalType = 2;
					signalCount = 0;
				}
			//}
			location = 3;
		} else if (currVal <= candles.get(0).getAvg_20()){
			//if( prevEndVal > candles.get(1).getAvg_20())	{
				if(location > 2 && location > 0)	{
					if(signalCount < 10 || (finalSignalValue - currVal)/finalSignalValue > (0.5/100) )	{
						//logger.info("매수시그널(20)!! " + currency.format(currVal) + ":" + currency.format(candles.get(0).getAvg_20()) + ":" + signalCount);
						signalType = 1;
						finalSignalValue = currVal;
						signalCount++;
					}
				}
			//}
			location = 2;
		} else if (currVal <= candles.get(0).getBb_lower()){
			//if( prevEndVal > candles.get(1).getBb_lower())	{
				if(location > 1 && location > 0)	{
					if(signalCount < 10 || (finalSignalValue - currVal)/finalSignalValue > (0.5/100) )	{
						//logger.info("매수시그널(BB)!! " + currency.format(currVal) + ":" + currency.format(candles.get(0).getBb_lower()) + ":" + signalCount);
						signalType = 1;
						finalSignalValue = currVal;
						signalCount++;
					}
				}
			//}
			location = 1;
		}

		
		//분봉이 FIX 되고 새로운 분봉이 시작하게 되면 oldPostion기록
		
		logger.debug(candle_time + ":" + prevCandleTime);
		
		if(!candle_time.equals(prevCandleTime))	{
			prevCandleTime = candle_time;
			prevCandleLocation = location;
			logger.debug("새 분봉 시작. 이전 캔들타임/위치 갱신\t" + DateUtil.convertToDateString(prevCandleTime) + "\t" + currency.format(prevPrice) + " -> " + currency.format(currVal) + "\t" + prevCandleLocation);
			prevPrice = currVal;
		}

		
		//logger.info(location + " --------------------");

		//logger.info(currency.format(prevEndVal) + "->" + currency.format(currVal) + ":" + currency.format(diff));
		//logger.info(currency.format(todayCandle.getPrev_closing_price()) + ":" + daydiff + ":" + daydiff/todayCandle.getPrev_closing_price()*100 + "%");
	}
}
