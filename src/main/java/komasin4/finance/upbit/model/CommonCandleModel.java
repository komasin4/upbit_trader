package komasin4.finance.upbit.model;

public class CommonCandleModel {
	String market;					//마켓명
	String candle_date_time_utc;	//캔들 기준 시각(UTC기준)
	String candle_date_time_kst;	//캔들 기준 시각(KST기준)
	double opening_price;			//시간
	double high_price;				//고가
	double low_price;				//저가
	double trade_price;				//종가
	long timestamp;					//해당캔들에서 마지막 틱이 저장된 시각
	double candle_acc_trade_price;	//누적 거래 금액
	double candle_acc_trade_volume; //누적 거래량
	public String getMarket() {
		return market;
	}
	public String getCandle_date_time_utc() {
		return candle_date_time_utc;
	}
	public String getCandle_date_time_kst() {
		return candle_date_time_kst;
	}
	public double getOpening_price() {
		return opening_price;
	}
	public double getHigh_price() {
		return high_price;
	}
	public double getLow_price() {
		return low_price;
	}
	public double getTrade_price() {
		return trade_price;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public double getCandle_acc_trade_price() {
		return candle_acc_trade_price;
	}
	public double getCandle_acc_trade_volume() {
		return candle_acc_trade_volume;
	}
}
