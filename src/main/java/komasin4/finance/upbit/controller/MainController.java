package komasin4.finance.upbit.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import komasin4.finance.upbit.model.MinuteCandleModel;
import komasin4.finance.upbit.service.CandleService;
import komasin4.finance.upbit.util.DateUtil;

@RestController
public class MainController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	
	@Value("${spring.datasource.url}")
	private String dUrl;
	
	@Autowired CandleService candleService;

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
		
		List<MinuteCandleModel> candleList = candleService.getCandlesFromDB(null, 1);
		for(MinuteCandleModel candle : candleList)	{
			logger.debug("candle:" + candle.toString());
		}
		return "test";
	}
	
	@GetMapping("getenv")
	public String getEnv()	{
		return dUrl;
	}
}
