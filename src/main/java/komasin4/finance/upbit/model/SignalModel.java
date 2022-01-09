package komasin4.finance.upbit.model;

public class SignalModel {
	int trade_no;
	String time_utc;
	String time_kst;
	int signal_type;
	double signal_price;
	double volume;
	String trade_flag;
	int up_signal_type;
	double up_signal_price;
	String reg_dt;
	String up_dt;

	
	
	public SignalModel() {
		super();
		// TODO Auto-generated constructor stub
	}


	public SignalModel(String time_utc, String time_kst, int signal_type, double signal_price, double volume,
			String flag) {
		super();
		this.time_utc = time_utc;
		this.time_kst = time_kst;
		this.signal_type = signal_type;
		this.signal_price = signal_price;
		this.volume = volume;
		this.trade_flag = trade_flag;
	}

	
	public int getTrade_no() {
		return trade_no;
	}

	public String getTime_kst() {
		return time_kst;
	}

	public int getSignal_type() {
		return signal_type;
	}

	public void setSignal_type(int signal_type) {
		this.signal_type = signal_type;
	}

	public double getSignal_price() {
		return signal_price;
	}

	public void setSignal_price(double signal_price) {
		this.signal_price = signal_price;
	}

	public double getVolume() {
		return volume;
	}

	public void setVolume(double volume) {
		this.volume = volume;
	}

	

	public void setUp_signal_type(int up_signal_type) {
		this.up_signal_type = up_signal_type;
	}


	public void setUp_signal_price(double up_signal_price) {
		this.up_signal_price = up_signal_price;
	}


	@Override
	public String toString() {
		return "SignalModel [trade_no=" + trade_no + ", time_utc=" + time_utc + ", time_kst=" + time_kst
				+ ", signal_type=" + signal_type + ", signal_price=" + signal_price + ", volume=" + volume
				+ ", trade_flag=" + trade_flag + ", up_signal_type=" + up_signal_type + ", up_signal_price="
				+ up_signal_price + ", reg_dt=" + reg_dt + ", up_dt=" + up_dt + "]";
	}
	
	
}
