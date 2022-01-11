package komasin4.finance.upbit.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	
	@Value("${spring.datasource.url}")
	private String dUrl;
	
	@GetMapping("test")
	public String test()	{
		return "test!";
	}
	
}
