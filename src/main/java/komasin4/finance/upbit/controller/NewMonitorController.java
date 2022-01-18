package komasin4.finance.upbit.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import komasin4.finance.upbit.scheduler.NewMonitorScheduler;

@Controller
public class NewMonitorController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	NewMonitorScheduler nScheduler;
	
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
	public String setValue(
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
			nScheduler.setFallRate(raise);
		if(volume != null)
			nScheduler.setFallRate(volume);
		if(income != null)
			nScheduler.setFallRate(income);
		if(minbase != null)
			nScheduler.setFallRate(minbase);
		if(mbb != null)
			nScheduler.setFallRate(mbb);
		if(mmin != null)
			nScheduler.setFallRate(mmin);
		if(trade != null)
			nScheduler.setbTrade(trade);
		
		return nScheduler.toString();
		
	}
}
