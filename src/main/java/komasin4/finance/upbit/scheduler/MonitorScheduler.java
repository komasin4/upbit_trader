package komasin4.finance.upbit.scheduler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import komasin4.finance.upbit.mapper.CandleMapper;
import komasin4.finance.upbit.model.DayCandleModel;
import komasin4.finance.upbit.model.MinuteCandleModel;
import komasin4.finance.upbit.service.CandleService;

@Service
//@Profile({"real", "local", "office"})
public class MonitorScheduler {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	CandleMapper candleMapper;
	
	@Autowired
	CandleService candleService;

//	DecimalFormat currency = new DecimalFormat("###,###");
//	DecimalFormat round = new DecimalFormat("#.##");
//	DecimalFormat vol = new DecimalFormat("#.########");
	
	private DayCandleModel todayCandle;
	private MinuteCandleModel snapShotCandle;	//바로직전분봉
	
	private int checkCount = 0;

//	@Scheduled(initialDelay = 1000, fixedRate = 200)
	@Scheduled(initialDelay = 1000, fixedRate = 2000)
	public void startMonitor()	{
		
		//logger.info("startMonitor!");
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
			
			//현재 가격의 위치를 가져옴.
			// 3 : BB상한의 위
			// 2 : BB상한
			// 1 : 20선 ~ BB상한 사이
			// 0 : 20선
			//-1 : BB하단 ~ 20선 사이
			//-2 : BB하단
			//-3 : BB하단의 아래
			int currLocation = getLocation(currCandle);
			
			//이전봉 종가의 위치를 가져옴.
			int lastLocation = getLocation(lastCandle);
			
			int diff = currLocation - lastLocation;

			//이전분봉기준으로 신호 발생 여부 체크
			int iSignal = getSignalType(currLocation, diff);
			
			logger.info("(%d) Location:%d->%d", iSignal, lastLocation, currLocation);
			
			
			checkCount++;
			snapShotCandle = currCandle;	//다음 스케쥴에서 활용하기 위해 현재 candle 저장

			logger.debug("---------------------------------------");
		} catch (Exception e)	{
			logger.error("message", e);
		}
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
	
	private int getSignalType(int iLocation, int iDiff)	{
		
		int rtnSignal = 0;

		switch(iLocation)	{
		case(3):				//BB상단 위
			if(iDiff > 0)		//상승하여 BB 상단 터치 - 매도
				rtnSignal = 2;
			break;
		case(2):				//BB상단
			if(iDiff > 0)		//상승하여 BB상단 터치 - 매도
				rtnSignal = 2;
			break;
		case(1):				//20선 위
			if(iDiff > 0)		//상승하여 20선 터치 - 매도
				rtnSignal = 1;
			break;
		case(0):				//20선
			if(iDiff > 0)		//상승 하여 20선 터치 - 매도
				rtnSignal = 1;		
			else if(iDiff < 0)	//하락하여 20선 터치 - 매수
				rtnSignal = -1;
			break;
		case(-1):				//20선 아래
			if(iDiff < 0)		//하락하여 20선 터치 - 매수
				rtnSignal = -1;
			break;
		case(-2):				//BB하단
			if(iDiff < 0)		//하락하여 BB하단 - 매수
				rtnSignal = -2;
			break;
		case(-3):				//BB하단 아래
			if(iDiff < 0)		//하락하여 BB하단 - 매수
				rtnSignal = -2;
			break;
		}

		return rtnSignal;
	}
}
