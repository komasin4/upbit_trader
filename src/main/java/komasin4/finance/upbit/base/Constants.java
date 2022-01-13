package komasin4.finance.upbit.base;

public class Constants {
	final public static String serverUrl = "https://api.upbit.com";
	
	private double	price_unit		 	= 1000;   	//호가단위 1,000원
	private double	volume_unit 		= 100000;	//매수단위 10,000원
	private double	incomeLimitPercent	= 0.002;	//수익한도 0.2%			(매수 가격보다 incomeLimit 만큼 비싸게 팔아야 수수료 빼고 수익)
	private int 	multi_BB			= 2;		//BB 하단 도달시 매수 비율	(volume_unit * multi_BB)
	private int		multi_MIN			= 3;  		//최저가 도달시 매수 비율  	(volume_unit * multi_MIN)
	private int		iMinBaseUnit		= 120;		//최저가 기준 봉 갯수

	final public static String telegramUrl = "https://api.telegram.org/bot";
}
