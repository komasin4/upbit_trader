package komasin4.finance.upbit.configuration;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class CustomThreadPoolTaskScheduler extends ThreadPoolTaskScheduler {
	
	//private final static int POOL_SIZE = 10;
	
	public CustomThreadPoolTaskScheduler() {
		super();
		// TODO Auto-generated constructor stub
		//this.setPoolSize(POOL_SIZE);
		this.setThreadNamePrefix("cThread-");
	}

	@Override 
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) 
	{ 
		if (period <= 0) {
			return null; 
		} 
		ScheduledFuture<?> future = super.scheduleAtFixedRate(task, period); 
		return future; 
	} 
	
	@Override 
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
		if (period <= 0) { 
			return null; 
		} 
		ScheduledFuture<?> future = super.scheduleAtFixedRate(task, startTime, period); 
		return future; 
	}
}