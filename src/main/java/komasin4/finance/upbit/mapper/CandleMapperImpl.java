package komasin4.finance.upbit.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;

import komasin4.finance.upbit.model.MinuteCandleModel;

public class CandleMapperImpl implements CandleMapper {

	@Autowired
	SqlSession session;

	@Override
	public List<MinuteCandleModel> selectMinuteCandles(String candle_time, int limit) throws Exception {
		// TODO Auto-generated method stub
		Map<String, Object> paramMap = new HashMap<String,Object>();
		paramMap.put("candle_time",  candle_time);
		paramMap.put("limit", limit);
		return session.selectList("selectMinuteCandles", paramMap);
	}
	
	@Override
	public Map<String, Double> selectMaxMinValue(String candle_time, int limit) throws Exception {
		// TODO Auto-generated method stub
		Map<String, Object> paramMap = new HashMap<String,Object>();
		paramMap.put("candle_time",  candle_time);
		paramMap.put("limit", limit);
		return session.selectOne("selectMaxMinValue", paramMap);
	}
}
