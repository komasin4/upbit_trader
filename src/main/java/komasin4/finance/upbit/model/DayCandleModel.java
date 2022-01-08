package komasin4.finance.upbit.model;

public class DayCandleModel extends CommonCandleModel {
	
	private double	prev_closing_price;
	private double	change_price;
	private double	change_rate;
	private double	converted_trade_price;

	public double getPrev_closing_price() {
		return prev_closing_price;
	}
	public double getChange_price() {
		return change_price;
	}
	public double getChange_rate() {
		return change_rate;
	}
	public double getConverted_trade_price() {
		return converted_trade_price;
	}
	
	@Override
	public String toString() {
		return "DayCandleModel [prev_closing_price=" + prev_closing_price + ", change_price=" + change_price
				+ ", change_rate=" + change_rate + ", converted_trade_price=" + converted_trade_price + "]";
	}
}
