package komasin4.finance.upbit.util;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

public class DateUtil {
	
	public static final DateTimeFormatter baseDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	
	public static OffsetDateTime getKSTTime(String dateString) {
		LocalDateTime ofDateTime = LocalDateTime.parse(dateString, baseDate);
		return OffsetDateTime.of(ofDateTime, ZoneOffset.of("+9"));
	}
	
    public static String getCurrentTime(){
        //return getCurrentTime("YYYY-MM-DD HH:mm:ss.SSS");
        return getCurrentTime("YYYY-MM-DD HH:mm:ss.SSS", "Asia/Seoul");
    }
	
    public static String getCurrentTime(String timeFormat, String strTimeZone){
    	
    	if(strTimeZone == null || strTimeZone.isEmpty())
    		strTimeZone = "Asia/Seoul";
    	
    	TimeZone tz = TimeZone.getTimeZone(strTimeZone);
    	
    	SimpleDateFormat df = new SimpleDateFormat(timeFormat);
    	df.setTimeZone(tz);
    	
    	Date date = new Date(System.currentTimeMillis());
    	
        return df.format(date);
        //return new SimpleDateFormat(timeFormat).format(System.currentTimeMillis());
    }
    
	public static String convertToDateString(String str)	{
		String regEx = "(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{2})";
		return str.replaceAll(regEx, "$1-$2-$3 $4:$5");
	}
}
