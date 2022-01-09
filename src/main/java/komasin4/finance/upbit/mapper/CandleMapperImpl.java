package komasin4.finance.upbit.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import komasin4.finance.upbit.model.MinuteCandleModel;
import komasin4.finance.upbit.model.SignalModel;

@Repository
public class CandleMapperImpl implements CandleMapper {
	
	@Override
	public Map<String, Double> selectMaxMinValue(String candle_time, int limit) throws Exception {
		// TODO Auto-generated method stub
		Map<String, Object> paramMap = new HashMap<String,Object>();
		paramMap.put("candle_time",  candle_time);
		paramMap.put("limit", limit);
		return session.selectOne("selectMaxMinValue", paramMap);
	}

	@Autowired
	SqlSession session;

	@Override
	public List<MinuteCandleModel> selectMinuteCandles() throws Exception {
		// TODO Auto-generated method stub
		return session.selectList("selectMinuteCandles");
	}

	@Override
	public List<MinuteCandleModel> selectMinuteCandles(String candle_time, int limit) throws Exception {
		// TODO Auto-generated method stub
		Map<String, Object> paramMap = new HashMap<String,Object>();
		paramMap.put("candle_time",  candle_time);
		paramMap.put("limit", limit);
		return session.selectList("selectMinuteCandles", paramMap);
	}

	@Override
	public MinuteCandleModel selectMinuteCandle(String candleTime) throws Exception {
		// TODO Auto-generated method stub
		return session.selectOne("selectMinuteCandle", candleTime);
	}

	@Override
	public int insertMinuteCandle(MinuteCandleModel candle) throws Exception {
		// TODO Auto-generated method stub
		return session.insert("insertMinuteCandle", candle);
	}

	@Override
	public int insertMinuteCandle30(MinuteCandleModel candle) throws Exception {
		// TODO Auto-generated method stub
		return session.insert("insertMinuteCandle30", candle);
	}

	@Override
	public int updateMinuteCandle(MinuteCandleModel candle) throws Exception {
		// TODO Auto-generated method stub
		return session.update("updateMinuteCandle", candle);
	}

	@Override
	public int insertTradeQueue(SignalModel signal) throws Exception {
		// TODO Auto-generated method stub
		return session.insert("insertMinuteCandle", signal);
	}

	@Override
	public List<SignalModel> selectTradeQueue(double price) throws Exception {
		// TODO Auto-generated method stub
		return session.selectList("selectTradeQueue", price);
	}

	@Override
	public int updateTradeQueue(SignalModel signal) throws Exception {
		// TODO Auto-generated method stub
		return session.update("updateTradeQueue", signal);
	}
	
	
}
