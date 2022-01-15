package komasin4.finance.upbit.scheduler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

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

	//20선 도달시 매수
	//매수 이후 1분 이내 다시 매수 신호 발생시는 패스
	//매수 신호 이후 1분 초과 했으나 가격차이가 XX% 미만이면 패스

	//BB하단 도달시 매수
	//매수 이후 1분 이내 다시 매수 신호 발생시는 패스
	//매수 신호 이후 1분 초과 했으나 가격차이가 XX% 미만이면 패스

	//신저가 발생시 매수
	//매수 신호 이후 1분 초과 했으나 가격차이가 XX% 미만이면 패스
	//신저가 매수 이후 XX% 이상 하락 하면 추가 매수.

	private DayCandleModel todayCandle;
	double maxPrice = 0;
	double minPrice = 0;
	
	private MinuteCandleModel snapShotCandle;	//바로직전분봉
	
	int iMinBaseUnit = 120;

	private double lastBuySignalPrice;			//최근 매수 신호 발생 가격
	private double fallRate = 0.002;	//매수기준 하락 폭 - 가장 최근 매수 가격보다 이 % 이상 떨어져야 매수 
	private int checkCount = 0;

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
			
			//고가,저가 정보 가져오기
			Map<String, Double> valueMap = candleMapper.selectMaxMinValue(currCandle.getCandle_time(), iMinBaseUnit);
			maxPrice = valueMap.get("maxPrice");
			minPrice = valueMap.get("minPrice");

			//일봉 정보 가져오기
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

			if(checkCount % 200 == 0) {
			logger.info("{} {} ({}) Location : {} -> {}    curr:{} sig:{} %:{} min:{} %:{} fall%:{}"
					, checkCount
					, (iSignal>0?"매도":iSignal<0?"매수":"관망")
					, iSignal
					, lastLocation
					, currLocation
					, F.cf.format(currCandle.getTrade_price())
					, F.cf.format(lastBuySignalPrice)
					, (lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice
					, F.cf.format(minPrice)
					, (minPrice - currCandle.getTrade_price())/minPrice
					, fallRate
					);
			}
			
			boolean bBuy = false;
			boolean bSell = false;
			
			if(iSignal > 0)		{		//매도신호 처리
				//매도 신호 이후 매수 신호 발생시는 매수 처리.
				bSell = false;
			} else if(iSignal < 0)	{   //매수신호 처리

				if(lastBuySignalPrice == 0 || (lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice > fallRate)	{
					//fallRate 이상 하락 했다면 매수
					logger.warn("(*) {} {} 이평선(매수): {} : {} : {} : {}", checkCount, iSignal, F.cf.format(currCandle.getTrade_price()), F.cf.format(lastBuySignalPrice), (lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice, fallRate);
					
					lastBuySignalPrice = currCandle.getTrade_price();
					
					String sSend = "(*) 이평선 도달 매수 / " + iSignal + " / " +  F.cf.format(currCandle.getTrade_price()) + " / " + F.cf.format(lastBuySignalPrice) + " / " + (lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice + " / " + fallRate;
					sendMessageService.send(sSend);
					
							
					bBuy = true;
					
				} else {
					logger.debug("이평선({} 패스): {} : {} : {} : {}", iSignal, F.cf.format(currCandle.getTrade_price()), F.cf.format(lastBuySignalPrice), (lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice, fallRate);
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
					logger.warn("(*) 신저가(매수): {} : {} : {} : {}", F.cf.format(currCandle.getTrade_price()), F.cf.format(lastBuySignalPrice), (lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice, fallRate);
					
					lastBuySignalPrice = currCandle.getTrade_price();
							
					bBuy = true;

					String sSend = "(*) 신저가(매수): " + F.cf.format(currCandle.getTrade_price()) + " " + F.cf.format(lastBuySignalPrice) + " " + (lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice + " " + fallRate;
					sendMessageService.send(sSend);
					
				} else {
					logger.debug("신저가(패스): {} : {} : {} : {}", F.cf.format(currCandle.getTrade_price()), F.cf.format(lastBuySignalPrice), (lastBuySignalPrice - currCandle.getTrade_price())/lastBuySignalPrice, fallRate);
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
}