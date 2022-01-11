package komasin4.finance.upbit.mapper;

import java.util.List;
import java.util.Map;

import komasin4.finance.upbit.model.MinuteCandleModel;

public interface CandleMapper {
	public List<MinuteCandleModel> selectMinuteCandles(String candle_time, int limit) throws Exception;
	public Map<String, Double> selectMaxMinValue(String candle_time, int limit) throws Exception;
}
