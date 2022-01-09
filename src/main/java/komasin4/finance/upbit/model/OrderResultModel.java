package komasin4.finance.upbit.model;

public class OrderResultModel {
//	uuid	주문의 고유 아이디	String
//	side	주문 종류	String
//	ord_type	주문 방식	String
//	price	주문 당시 화폐 가격	NumberString
//	avg_price	체결 가격의 평균가	NumberString
//	state	주문 상태	String
//	market	마켓의 유일키	String
//	created_at	주문 생성 시간	String
//	volume	사용자가 입력한 주문 양	NumberString
//	remaining_volume	체결 후 남은 주문 양	NumberString
//	reserved_fee	수수료로 예약된 비용	NumberString
//	remaining_fee	남은 수수료	NumberString
//	paid_fee	사용된 수수료	NumberString
//	locked	거래에 사용중인 비용	NumberString
//	executed_volume	체결된 양	NumberString
//	trade_count	해당 주문에 걸린 체결 수	Integer
	String message;
	String name;
	String uuid;				//주문의 고유 아이디	String
	String side;				//주문 종류	String
	String ord_type;			//주문 방식	String
	String price;				//주문 당시 화폐 가격	NumberString
	String avg_price;			//체결 가격의 평균가	NumberString
	String state;				//주문 상태	String
	String market;				//마켓의 유일키	String
	String created_at;			//주문 생성 시간	String
	String volume;				//사용자가 입력한 주문 양	NumberString
	String remaining_volume;	//체결 후 남은 주문 양	NumberString
	String reserved_fee;		//수수료로 예약된 비용	NumberString
	String remaining_fee;		//남은 수수료	NumberString
	String paid_fee;			//사용된 수수료	NumberString
	String locked;				//거래에 사용중인 비용	NumberString
	String executed_volume;		//체결된 양	NumberString
	int trade_count;			//해당 주문에 걸린 체결 수	Integer

	@Override
	public String toString() {
		return "OrderResultModel [message=" + message + ", name=" + name + ", uuid=" + uuid + ", side=" + side
				+ ", ord_type=" + ord_type + ", price=" + price + ", avg_price=" + avg_price + ", state=" + state
				+ ", market=" + market + ", created_at=" + created_at + ", volume=" + volume + ", remaining_volume="
				+ remaining_volume + ", reserved_fee=" + reserved_fee + ", remaining_fee=" + remaining_fee
				+ ", paid_fee=" + paid_fee + ", locked=" + locked + ", executed_volume=" + executed_volume
				+ ", trade_count=" + trade_count + "]";
	}

	public String getMessage() {
		return message;
	}

	public String getName() {
		return name;
	}

	public String getState() {
		return state;
	}

}
