package komasin4.finance.upbit.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import komasin4.finance.upbit.scheduler.MonitorScheduler;

@Controller
public class MainController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	
	@Value("${spring.datasource.url}")
	private String dUrl;
	
	@Autowired
	MonitorScheduler monitor;
	
	@GetMapping("/test")
	public String test()	{
		return "test";
	}
	
	@GetMapping("/test1")
	public @ResponseBody String test1()	{
		return "test1";
	}

	@GetMapping("/stop")
	public @ResponseBody String stop()	{
		
		monitor.remove();
		
		return "cancel";
	}

	@GetMapping("/start")
	public @ResponseBody String start(@RequestParam(value = "rate", defaultValue="200") int iFixedRate)	{
		
		monitor.start(iFixedRate);
		
		return "cancel";
	}
}
