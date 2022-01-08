package komasin4.finance.upbit.scheduler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import komasin4.finance.upbit.model.MinuteCandleModel;
import komasin4.finance.upbit.service.CandleService;
import komasin4.finance.upbit.util.DateUtil;

@Service
@Profile("real")
public class GetCandleScheduler {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Value("${spring.datasource.url}")
	private String dUrl;
	
	@Autowired
	CandleService candleService;
	
	//@Scheduled(cron = "*/1 * * * * ?")
	//@Scheduled(cron = "*/1 * * * * ?")
	@Scheduled(initialDelay = 1000, fixedRate = 250)
	public void OneMinuteCandle()	{
		//logger.info("1분봉 처리:" + DateUtil.getCurrentTime() + ":" + DateUtil.getCurrentTime("yyyy-MM-dd HH:mm", "Asia/Seoul"));
		
		
		//getCandle(int unit, OffsetDateTime to, int count)
		
		int unit = 1;
		int count = 2;
		
		//OffsetDateTime to = DateUtil.getKSTTime(DateUtil.getCurrentTime("yyyy-MM-dd HH:mm"));
		
		List<MinuteCandleModel> candles = candleService.getMinuteCandle(unit, DateUtil.getCurrentTime("yyyy-MM-dd HH:mm:ss", "Asia/Seoul"), count);
		
		int index = 0;
		
		
		for(int i = candles.size() - 1 ; i >= 0 ; i--)	{
//		for(MinuteCandleModel candle : candles)	{
			if(index == (candles.size() - 1))	{
				logger.debug("------------------------------");
			}
			
			MinuteCandleModel candle = candles.get(i);
			
			MinuteCandleModel oldCandle = candleService.getCandleFromDB(candle.getCandle_time());
			if(oldCandle != null)	{
				oldCandle.toString();
				logger.debug("[" + index + "]:" + "db:" + oldCandle.toString());
				//데이터 비교
				if(isCandleChanged(candle, oldCandle)) { // 캔들업데이트 필요
					logger.debug("[" + index + "]:" + "api(update):" + candle.toString());
					logger.debug("[" + index + "]:" + "candle updated!!!");
					candleService.updateCandle(candle);
				}
			}
			else	{ 
				logger.info("[" + index + "]:" + "api(insert):" + candle.toString());
				candleService.insertCandle(candle, false);
			}
			index++;
		}
		
	}
	
	private boolean isCandleChanged(MinuteCandleModel newCandle, MinuteCandleModel oldCandle)	{
		boolean rtn = true;
		
		if((newCandle.getCandle_acc_trade_price() == oldCandle.getCandle_acc_trade_price()) && 
			(newCandle.getCandle_acc_trade_volume() == oldCandle.getCandle_acc_trade_volume()) &&
			(newCandle.getTimestamp() == oldCandle.getTimestamp()))
			rtn = false;
		else {
			logger.debug("diff:getCandle_acc_trade_price:" + newCandle.getCandle_acc_trade_price() + " vs " + oldCandle.getCandle_acc_trade_price());
			logger.debug("diff:getCandle_acc_trade_volume:" + newCandle.getCandle_acc_trade_volume() + " vs " + oldCandle.getCandle_acc_trade_volume());
			logger.debug("diff:getTimestamp:" + newCandle.getTimestamp() + " vs " + oldCandle.getTimestamp());
		}
		return rtn;
	}

	//@Scheduled(cron = "5/* * * * * ?")
//	@Scheduled(fixedRate = 2000)
//	@Async
//	public void test1()	{
//		logger.debug("\t1:" + DateUtil.getCurrentTime());
//	}
//
//	//@Scheduled(cron = "5/* * * * * ?")
//	@Scheduled(fixedRate = 5000)
//	@Async
//	public void test2()	{
//		logger.debug("\t\t2:" + DateUtil.getCurrentTime());
//	}
}
