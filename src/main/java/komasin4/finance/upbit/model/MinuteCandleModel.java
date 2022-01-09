package komasin4.finance.upbit.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinuteCandleModel extends CommonCandleModel {
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	String candle_time = new String();
	int unit;
	
	double avg_5;
	double avg_20;
	double avg_60;
	double avg_120;
	double avg_240;
	double bb_upper;
	double bb_lower;
	
	public String getCandle_time() {
		if(candle_date_time_kst != null && candle_date_time_kst.length() > 15)	{
			candle_time = candle_date_time_kst.substring(0, 16).replaceAll("[^0-9]","");
		} 
		return candle_time;
	}

	public double getAvg_20() {
		return avg_20;
	}

	public double getBb_upper() {
		return bb_upper;
	}

	public double getBb_lower() {
		return bb_lower;
	}

	@Override
	public String toString() {
		return "CandleModel [logger=" + logger + ", candle_time=" + candle_time + ", market=" + market
				+ ", candle_date_time_utc=" + candle_date_time_utc + ", candle_date_time_kst=" + candle_date_time_kst
				+ ", opening_price=" + opening_price + ", high_price=" + high_price + ", low_price=" + low_price
				+ ", trade_price=" + trade_price + ", timestamp=" + timestamp + ", candle_acc_trade_price="
				+ candle_acc_trade_price + ", candle_acc_trade_volume=" + candle_acc_trade_volume + ", unit=" + unit
				+ ", avg_5=" + avg_5 + ", avg_20=" + avg_20 + ", avg_60=" + avg_60 + ", avg_120=" + avg_120
				+ ", avg_240=" + avg_240 + ", bb_upper=" + bb_upper + ", bb_lower=" + bb_lower + "]";
	}
}
