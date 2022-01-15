package komasin4.finance.upbit.scheduler;

import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;

public class BaseScheduler {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired 
	private TaskScheduler taskScheduler;
	
	ScheduledFuture<?> task;
	private final int initFixedRate = 300;
	
	public String scheduleStart(int iFixedRate) {
		String rtnString = "";
		if(task == null || task.isCancelled())	{
			task = taskScheduler.scheduleAtFixedRate(()->start(), iFixedRate);
			rtnString = "NewMonitorScheduler started!!!";
		} else	{
			rtnString = "NewMonitorScheduler already started!!!";
		}
		logger.info(rtnString);
		return rtnString;
	}

	public String scheduleStop() {
		String rtnString = "";
		task.cancel(true);
		if(task.isCancelled())	{
			rtnString = "NewMonitorScheduler stopped!!!";
		} else {
			rtnString = "NewMonitorScheduler stop failed!!!";
		}
		logger.info(rtnString);
		return rtnString;
	}
	
	public void start()	{
		
	}
}
