package komasin4.finance.upbit.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
public class SchedulerConfig implements SchedulingConfigurer {
	
	private final static int POOL_SIZE = 10;

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		// TODO Auto-generated method stub
        final ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();

        threadPoolTaskScheduler.setPoolSize(POOL_SIZE);
        threadPoolTaskScheduler.setThreadNamePrefix("Thread-");
        threadPoolTaskScheduler.initialize();

        taskRegistrar.setTaskScheduler(threadPoolTaskScheduler);
	}

}
