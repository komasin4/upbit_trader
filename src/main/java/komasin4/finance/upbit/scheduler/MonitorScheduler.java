package komasin4.finance.upbit.scheduler;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import komasin4.finance.upbit.mapper.CandleMapper;
import komasin4.finance.upbit.model.DayCandleModel;
import komasin4.finance.upbit.model.MinuteCandleModel;
import komasin4.finance.upbit.service.CandleService;
import komasin4.finance.upbit.service.SendMessageService;

@Service
//@Profile({"real", "local", "office"})
public class MonitorScheduler {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired 
	private TaskScheduler taskScheduler;
	
	@Autowired
	SendMessageService sendMessageService;

	@Autowired
	CandleMapper candleMapper;
	
	@Autowired
	CandleService candleService;

	DecimalFormat cFormat = new DecimalFormat("###,###");
	DecimalFormat rFormat = new DecimalFormat("#.##");
	DecimalFormat vFormat = new DecimalFormat("#.########");
	
	private final int initFixedRate = 200;
	
	private DayCandleModel todayCandle;
	private MinuteCandleModel snapShotCandle;	//바로직전분봉
	
	//private String lastBuySignalTime;			//최근 매수 신호 발생 시간
	private double lastBuySignalPrice;			//최근 매수 신호 발생 가격
	private double fallRate = 0.2 * (1/100);	//매수기준 하락 폭 - 가장 최근 매수 가격보다 이 % 이상 떨어져야 매수 
	
	private int iMinBaseUnit = 120;				//신고가,신저가 기준 봉 갯수
	
	private double lastMinPrice = 0;
	private double lastMaxPrice = 0;
	
	private int checkCount = 0;
	
	ScheduledFuture<?> task;
	
	public void remove() {
		logger.debug("stoped!!!");
		task.cancel(true);
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
		if(task == null)
			task = taskScheduler.scheduleAtFixedRate(()->startMonitor(), initFixedRate);
	}
	
	//@Scheduled(initialDelay = 1000, fixedRate = 200)
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
			
			
			//이동평균선/볼린저밴드 활용한 매수/매도 신호 체크 -- from
			
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
			
			if(checkCount % 10 == 0) {
			logger.info("{} {} ({}) Location : {} -> {}"
					, checkCount
					, (iSignal>0?"매도":iSignal<0?"매수":"관망")
					, iSignal
					, lastLocation
					, currLocation);
			}
			
			boolean bBuy = false;
			boolean bSell = false;
			
			if(iSignal > 0)		{		//매도신호 처리
				//매도 신호 이후 매수 신호 발생시는 매수 처리.
				bSell = false;
			} else if(iSignal < 0)	{   //매수신호 처리

				if(lastBuySignalPrice == 0 || (lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice > fallRate)	{
					//fallRate 이상 하락 했다면 매수
					logger.debug("이평선({} 매수): {} : {} : {}", iSignal, cFormat.format(currCandle.getTrade_price()), cFormat.format(lastBuySignalPrice), (lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice * 100);
					
					lastBuySignalPrice = currCandle.getTrade_price();
					
					String sSend = "이평선 도달 매수 / " + iSignal + " / " +  cFormat.format(currCandle.getTrade_price() + " / " + cFormat.format(lastBuySignalPrice) + " / " + (lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice * 100 + " / " + fallRate);
					sendMessageService.send(sSend);
					
							
					bBuy = true;
					
				} else {
					logger.debug("이평선({} 패스): {} : {} : {}", iSignal, cFormat.format(currCandle.getTrade_price()), cFormat.format(lastBuySignalPrice), (lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice * 100);
				}
			}
			//이동평균선/볼린저밴드 활용한 매수/매도 신호 체크 -- to
			
			
			//신저가/신고가 활용 매수/매도 신호 체크 -- from
			
			//분봉이 결정되거나 신저가.신고가가 없다면 신저가.신고가 업데이트
			if(!currCandle.getCandle_time().equals(snapShotCandle.getCandle_time()) || lastMaxPrice == 0 || lastMinPrice == 0)	{
//				bBBSellSig = true;
//				b20SellSig = true;
//				bMAXSellSig  = true;
				setMinMaxPrice(currCandle);
			}
			
			if(checkCount % 100 == 0)
				logger.info("신고가:신저가:" + cFormat.format(lastMaxPrice) + ":" + cFormat.format(lastMinPrice));
			
			
			//1.신저가 발생시 매수 신호 발생
			//직전 매수 신호 보다 ?? % 이하 일 경우만 매수 처리. 아니면 패스 
			if(currCandle.getTrade_price() < lastMinPrice)	{
				if(lastBuySignalPrice == 0 || (lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice > fallRate)	{
					//fallRate 이상 하락 했다면 매수
					logger.debug("신저가(매수): {} : {} : {}", cFormat.format(currCandle.getTrade_price()), cFormat.format(lastBuySignalPrice), (lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice * 100);
					
					lastBuySignalPrice = currCandle.getTrade_price();
							
					bBuy = true;

					String sSend = "신저가 도달 매수 " + iSignal + " " +  cFormat.format(currCandle.getTrade_price());
					sendMessageService.send(sSend);
					
				} else {
					logger.debug("신저가(패스): {} : {} : {}", cFormat.format(currCandle.getTrade_price()), cFormat.format(lastBuySignalPrice), (lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice * 100);
				}
			}
			
			//신저가/신고가 활용 매수/매도 신호 체크 -- to
			
			
			//매도시 이전 매수 신호 무조건 리셋
			if(bSell)	{
				//lastBuySignalTime = null;
				lastBuySignalPrice = 0;
			}
			
			//누적 매수 금액이 XX 일 경우 매수 보류 ?
			
			
			checkCount++;
			snapShotCandle = currCandle;	//다음 스케쥴에서 활용하기 위해 현재 candle 저장
		} catch (Exception e)	{
			logger.error("message", e);
		}
	}
	
	private void setMinMaxPrice(MinuteCandleModel candle) throws Exception	{
		Map<String, Double> valueMap = candleMapper.selectMaxMinValue(candle.getCandle_time(), iMinBaseUnit);
		lastMaxPrice = valueMap.get("maxPrice");
		lastMinPrice = valueMap.get("minPrice");
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
