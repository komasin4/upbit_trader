package komasin4.finance.upbit.util;

import komasin4.finance.upbit.model.MinuteCandleModel;

public class MonitorUtil {
	public static int getLocation(MinuteCandleModel candle)	{
		int rtnLocation = 0;
		
		if(candle.getTrade_price() > candle.getBb_upper())
			rtnLocation = 3;
		else if(candle.getTrade_price() == candle.getBb_upper())
			rtnLocation = 2;
		else if(candle.getTrade_price() > candle.getAvg_20())
			rtnLocation = 1;
		else if(candle.getTrade_price() == candle.getAvg_20())
			rtnLocation = 0;
		else if(candle.getTrade_price() < candle.getBb_lower())
			rtnLocation = -3;
		else if(candle.getTrade_price() == candle.getBb_lower())
			rtnLocation = -2;
		else if(candle.getTrade_price() < candle.getAvg_20())
			rtnLocation = -1;

		return rtnLocation;
	}
	
	public static int getSignalType(int iLocation, int iDiff)	{
		
		int rtnSignal = 0;

		switch(iLocation)	{
		case(3):				//BB상단 위
			if(iDiff > 0)		//상승하여 BB 상단 터치 - 매도
				rtnSignal = 2;
			break;
		case(2):				//BB상단
			if(iDiff > 0)		//상승하여 BB상단 터치 - 매도
				rtnSignal = 2;
			break;
		case(1):				//20선 위
			if(iDiff > 0)		//상승하여 20선 터치 - 매도
				rtnSignal = 1;
			break;
		case(0):				//20선
			if(iDiff > 0)		//상승 하여 20선 터치 - 매도
				rtnSignal = 1;		
			else if(iDiff < 0)	//하락하여 20선 터치 - 매수
				rtnSignal = -1;
			break;
		case(-1):				//20선 아래
			if(iDiff < 0)		//하락하여 20선 터치 - 매수
				rtnSignal = -1;
			break;
		case(-2):				//BB하단
			if(iDiff < 0)		//하락하여 BB하단 - 매수
				rtnSignal = -2;
			break;
		case(-3):				//BB하단 아래
			if(iDiff < 0)		//하락하여 BB하단 - 매수
				rtnSignal = -2;
			break;
		}

		return rtnSignal;
	}
}
