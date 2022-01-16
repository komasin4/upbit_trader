package komasin4.finance.upbit.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import komasin4.finance.upbit.mapper.CandleMapper;
import komasin4.finance.upbit.model.MinuteCandleModel;
import komasin4.finance.upbit.model.SignalModel;
import komasin4.finance.upbit.scheduler.MonitorScheduler;
import komasin4.finance.upbit.service.CandleService;
import komasin4.finance.upbit.util.DateUtil;

import com.google.gson.Gson;

import komasin4.finance.upbit.service.SendMessageService;


@RestController
public class MainController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	
	@Value("${spring.datasource.url}")
	private String dUrl;
	
	@Autowired CandleService candleService;
	
	@Autowired
	CandleMapper candleMapper;
	
	@Autowired
	MonitorScheduler monitor;
	
	@Autowired
	SendMessageService sendMessageService;
	
//	@Scheduled(initialDelay = 1000, fixedRate = 200)
//	public void startMonitor()	{
//		monitor.startMonitor();
//	}

	@GetMapping("apitest")
	public String getCandleFromAPI( 
									@RequestParam(value = "dBase", required = true) String dBase
							      , @RequestParam(value = "dTill", required = true) String dTill
							      , @RequestParam(value = "unit", defaultValue = "1") int unit 
							      , @RequestParam(value = "count", defaultValue = "1") int count 

							      								)	{
		
//		int unit = 1;
//		int count = 120;
		
		logger.debug("period:" + dBase + " ~ " + dTill);
		logger.debug("unit:" + unit + ":count:" + count);
		
		List<MinuteCandleModel> candleList = candleService.getCandle(dBase, dTill, unit, count, true);
		
		return "test:" + DateUtil.getCurrentTime();
	}

	@GetMapping("dbtest")
	public String getCandleFromDB()	{
		
		//List<MinuteCandleModel> candleList = candleService.getCandlesFromDB(null, 1);
		List<MinuteCandleModel> candleList;
		try {
			candleList = candleMapper.selectMinuteCandles(null, 1);
			for(MinuteCandleModel candle : candleList)	{
				logger.debug("candle:" + candle.toString());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "test";
	}
	
	@GetMapping("getenv")
	public String getEnv()	{
		double price = 51642000D;
		try {
			List<SignalModel> sellList = candleMapper.selectTradeQueue(price);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dUrl;
	}
	
	@GetMapping("setValue")
	public String setValue(@RequestParam(value="20sig", required=false) Boolean bSet
								, @RequestParam(value="vol", required=false) Double vol
								, @RequestParam(value="income", required=false) Double incom
								, @RequestParam(value="minbase", required=false) Integer minbase
								, @RequestParam(value="mbb", required=false) Integer mbb
								, @RequestParam(value="mmin", required=false) Integer mmin)	{
		
		//
		
		
		if(bSet != null)
			monitor.setSignal20Sell(bSet);
		
		if(vol != null)
			monitor.setVolumeUnit(vol);
		
		if(mbb != null)
			monitor.setMultiBB(mbb);
			
		if(mmin != null)
			monitor.setMultiMIN(mmin);

		if(incom != null)
			monitor.setIncomeLimitPercent(incom);

		if(minbase != null)
			monitor.setMinBaseUnit(minbase);

		return "set=" + bSet + ", vol="+vol + ", mbb=" + mbb + ", mmin=" + mmin + ", incom=" + incom + ", minbase=" + minbase;
		
	}

	@GetMapping("getValue")
	public String getValue()	{
		return "set=" + monitor.getSignal20Sell() + ", vol=" + monitor.getVolumeUnit() + ", mbb=" + monitor.getMultiBB() + ", mmin=" + monitor.getMultiMIN() + ",incom=" + monitor.getIcomeLimitPercent() + ", minbase=" + monitor.getMinBaseUnit();
	}
	
	@GetMapping("/stop")
	public @ResponseBody String stop()	{
		
		monitor.remove();
		
		return "stop";
	}
	
	@GetMapping("/start")
	public @ResponseBody String start(@RequestParam(value = "rate", defaultValue="200") int iFixedRate)	{
		
		monitor.start(iFixedRate);
		
		return "start:rate(" + iFixedRate + ")";
	}
	
	@GetMapping("/send")
	public @ResponseBody String sendMessage(@RequestParam(value="txt") String sMessage)		{
		return new Gson().toJson(sendMessageService.send(sMessage));
	}
	
	@GetMapping("/status")
	public String status()	{
		MinuteCandleModel snapShotCandle = monitor.getSnapShotCandle();
		
		StringBuffer rtnSB = new StringBuffer()
								.append("currentCandleDate : " + snapShotCandle.getCandle_date_time_kst());
		return rtnSB.toString();
	}
}
