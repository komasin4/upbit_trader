package komasin4.finance.upbit.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import komasin4.finance.upbit.model.MinuteCandleModel;
import komasin4.finance.upbit.model.SignalModel;

@Mapper
public interface CandleMapper {
	public Map<String, Double> selectMaxMinValue(String candle_time, int limit) throws Exception;
	public List<MinuteCandleModel> selectMinuteCandles() throws Exception;
	public List<MinuteCandleModel> selectMinuteCandles(String candle_time, int limit) throws Exception;
	public MinuteCandleModel selectMinuteCandle(String candleTime) throws Exception;
	public int insertMinuteCandle(MinuteCandleModel candle) throws Exception;
	public int insertMinuteCandle30(MinuteCandleModel candle) throws Exception;
	public int updateMinuteCandle(MinuteCandleModel candle) throws Exception;
	public int insertTradeQueue(SignalModel signal) throws Exception;
	public List<SignalModel> selectTradeQueue(double price) throws Exception;
	public int updateTradeQueue(SignalModel signal) throws Exception;
//	public void insertCandles(List<CandleModel> candles) throws Exception;
}
