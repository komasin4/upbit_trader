package komasin4.finance.upbit.model;

public class OrderModel {
	private String side;
	private double price;
	private double volume;
	public OrderModel(String side, double price, double volume) {
		super();
		this.side = side;
		this.price = price;
		this.volume = volume;
	}
	
	
	
	public String getSide() {
		return side;
	}



	public double getPrice() {
		return price;
	}



	public double getVolume() {
		return volume;
	}



	@Override
	public String toString() {
		return "OrderModel [side=" + side + ", price=" + price + ", volume=" + volume + "]";
	}
}
