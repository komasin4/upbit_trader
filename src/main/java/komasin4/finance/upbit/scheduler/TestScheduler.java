package komasin4.finance.upbit.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TestScheduler {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	//@Scheduled(initialDelay = 1000, fixedDelay = 1000)
	//@Async
	public void testD() throws Exception	{
		logger.debug("D");
		Thread.sleep(1000);
	}
	

	//@Scheduled(initialDelay = 1000, fixedRate = 2000)
	//@Async
	public void testR() throws Exception	{
		logger.debug("R");
		Thread.sleep(3000);
	}	
}
