package komasin4.finance.upbit.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import komasin4.finance.upbit.mapper.CandleMapper;
import komasin4.finance.upbit.model.MinuteCandleModel;
import komasin4.finance.upbit.scheduler.NewMonitorScheduler;

@Controller
public class NewMonitorController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	NewMonitorScheduler nScheduler;
	
	@Autowired
	CandleMapper candleMapper;
	
	@GetMapping("/nstart")
	public @ResponseBody String start(@RequestParam(value = "rate", defaultValue="300") int iFixedRate)	{
		return nScheduler.scheduleStart(iFixedRate) + " rate(" + iFixedRate + ")";
	}
	
	@GetMapping("/nstop")
	public @ResponseBody String stop()	{
		return nScheduler.scheduleStop();
	}
	
	@GetMapping("/nstatus")
	public @ResponseBody String status()	{
		return nScheduler.status();
	}
	
	@GetMapping("/nset")
	public @ResponseBody String setValue(
							@RequestParam(value="fall", required=false) Double fall,
							@RequestParam(value="raise", required=false) Double raise,
							@RequestParam(value="volume", required=false) Double volume,
							@RequestParam(value="income", required=false) Double income,
							@RequestParam(value="minbase", required=false) Integer minbase,
							@RequestParam(value="mbb", required=false) Integer mbb,
							@RequestParam(value="mmin", required=false) Integer mmin,
							@RequestParam(value="trade", required=false) Boolean trade
						  )	{
		
		//String rtnVal = "";
		
		if(fall != null)
			nScheduler.setFallRate(fall);
		if(raise != null)
			nScheduler.setRaiseRate(raise);
		if(volume != null)
			nScheduler.setVolume_unit(volume);
		if(income != null)
			nScheduler.setIncomeLimitPercent(income);
		if(minbase != null)
			nScheduler.setiMinBaseUnit(minbase);
		if(mbb != null)
			nScheduler.setMulti_BB(mbb);
		if(mmin != null)
			nScheduler.setMulti_MIN(mmin);
		if(trade != null)
			nScheduler.setbTrade(trade);
		
		return nScheduler.toString();
		
	}
	
	@GetMapping("/ntest")
	public String nTest()	{
		try {
			List<MinuteCandleModel> preCandles = candleMapper.selectMinuteCandles(null, 3);
			
			logger.debug("ntest...");
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "ntest";
	}
}
