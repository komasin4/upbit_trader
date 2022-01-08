package komasin4.finance.upbit;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import komasin4.finance.upbit.model.MinuteCandleModel;

@Controller
public class TestController {
	final static Logger logger = LoggerFactory.getLogger(TestController.class);
	final static String serverUrl = "https://api.upbit.com";

	public static void main(String[] args) {
		TestClass test = new TestClass();
		logger.debug(test.convertToDateString("202201071006"));
		//test.getDayCandle();
		// TODO Auto-generated method stub

		/*
		long startTime = System.currentTimeMillis();
		Date dt = new Date(startTime);
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		
		logger.debug("startTime:" + format.format(dt));
		
		long baseTime = startTime;
		
		int totalCount = 0;
		int loopCount = totalCount;
		
		for(int i = 0; i < 600; i++) 	{
			long currentTime = System.currentTimeMillis();

			totalCount++;
			loopCount++;

			logger.debug(totalCount + ":" + loopCount + ":currentTime-baseTime:" + (currentTime - baseTime));

			long gapTime = currentTime-baseTime;
			
			if((gapTime < 10000) &&  (loopCount >= 10))	{
				
				try {
					long sleepTime = 10000-gapTime;
					logger.debug("Sleep:" + sleepTime);
					Thread.sleep(10000-gapTime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				baseTime = currentTime;
				loopCount = 0;
			}
			
		}
		
		long endTime = System.currentTimeMillis();
		
		logger.debug(totalCount + ":" + loopCount + ":endTime-startTime:" + (endTime-startTime));
		*/
	}
}
