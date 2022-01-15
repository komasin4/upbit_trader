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
}
