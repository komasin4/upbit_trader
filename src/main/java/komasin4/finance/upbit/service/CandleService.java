
package komasin4.finance.upbit.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import komasin4.finance.upbit.base.Constants;
import komasin4.finance.upbit.mapper.CandleMapper;
import komasin4.finance.upbit.mapper.CandleMapperImpl;
import komasin4.finance.upbit.model.DayCandleModel;
import komasin4.finance.upbit.model.MinuteCandleModel;
import komasin4.finance.upbit.util.AuthUtil;
import komasin4.finance.upbit.util.DateUtil;

@Service
public class CandleService {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	CandleMapperImpl candleMapper;
	
	public int insertCandle(MinuteCandleModel candle, boolean candle30) {
		int rtn = -1;
		try {
			rtn = candle30?candleMapper.insertMinuteCandle30(candle):candleMapper.insertMinuteCandle(candle);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("message", e);
		}
		return rtn;
	}

	public List<MinuteCandleModel> getCandle(String fromTime, String toTime, int unit, int count, boolean dbInsert)	{
		logger.debug(fromTime + " ~ " + toTime);
		
		boolean bCandle30 = unit==30?true:false;
		List<MinuteCandleModel> candleList = new ArrayList<MinuteCandleModel>();
		
		OffsetDateTime to = DateUtil.getKSTTime(fromTime);
		OffsetDateTime current = to;
		
		
		int  totalCount = 0;
		int  insertedRows = 0;
		
		long startTime = System.currentTimeMillis();

		do {
			//candleList.addAll(getCandle(unit, current, count));
			candleList.addAll(getMinuteCandle(unit, current.toString(), count));
			String currentDateString = candleList.get(candleList.size()-1).getCandle_date_time_kst();
			current = LocalDateTime.parse(currentDateString).atOffset(ZoneOffset.of("+9"));
			
			totalCount++;

			long currentTime = System.currentTimeMillis();
			logger.debug("call:" + totalCount + ":time:" + (currentTime-startTime));
			
			if(totalCount % 5 == 0)	{
				insertedRows += insertCandleAndSleep(candleList, bCandle30);
				candleList.clear();
				logger.debug("*** " + insertedRows + " inserted!!");
			}
		} while (current.isAfter(DateUtil.getKSTTime(toTime)));
		
		insertedRows += insertCandleAndSleep(candleList, bCandle30);
		logger.debug("*** " + insertedRows + " inserted!!");
		
		long endTime = System.currentTimeMillis();

		logger.debug("end call:" + totalCount + ":loopCount:" + (endTime-startTime));

		return candleList;
	}
	
	private int insertCandleAndSleep(List<MinuteCandleModel> candleList, boolean bCandle30)	{
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("message", e);
		}
		
		int cnt = 0;
		
		for(MinuteCandleModel candle : candleList)	{
			try	{
				int rows = insertCandle(candle, bCandle30);
				cnt += rows;
			} catch (Exception e)	{
				e.printStackTrace();
				logger.error("message", e);
			}
			if(cnt%100 == 0)	{
				logger.debug("@@@ " + cnt + " rows inserted!!!");
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					//TODO Auto-generated catch block
					e.printStackTrace();
					logger.error("message", e);
				}
			}
		}
		
		return cnt;
	}
	
	
	public List<DayCandleModel> getDayCandle()	{
		logger.debug("Get DayCandle");
		List<DayCandleModel> rtnList = new ArrayList<DayCandleModel>();
		String authenticationToken = AuthUtil.getAuthToken();

		try {
			HttpClient client = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(Constants.serverUrl + "/v1/candles/days");
			request.setHeader("Content-Type", "application/json");
			request.addHeader("Authorization", authenticationToken);

			List nameValuePairs = new ArrayList();
			nameValuePairs.add(new BasicNameValuePair("market", "KRW-BTC"));
			nameValuePairs.add(new BasicNameValuePair("count", "1"));

			URI uri = new URIBuilder(request.getURI())
				      .addParameters(nameValuePairs)
				      .build();
			
			request.setURI(uri);

			HttpResponse response = client.execute(request);
			HttpEntity entity = response.getEntity();
			
			String rtnString = EntityUtils.toString(entity, "UTF-8");

			JSONArray ja = new JSONArray(rtnString);
			rtnList = new Gson().fromJson(ja.toString(), new TypeToken<ArrayList<DayCandleModel>>() {}.getType());
			
		} catch (IOException | URISyntaxException | JSONException e) {
			e.printStackTrace();
		}		
		return rtnList;
	}
	
//	public List<CandleModel> getCandle(int unit, OffsetDateTime to, int count)	{
	public List<MinuteCandleModel> getMinuteCandle(int unit, String to, int count)	{
		//https://api.upbit.com/v1/candles/minutes/5?market=KRW-BTC&count=3
		List<MinuteCandleModel> rtnList = new ArrayList<MinuteCandleModel>();
		String authenticationToken = AuthUtil.getAuthToken();

		try {
			HttpGet request = new HttpGet(Constants.serverUrl + "/v1/candles/minutes/" + unit);
			HttpClient client = HttpClientBuilder.create().build();
			request.setHeader("Content-Type", "application/json");
			request.addHeader("Authorization", authenticationToken);
			
			List nameValuePairs = new ArrayList();
			nameValuePairs.add(new BasicNameValuePair("market", "KRW-BTC"));
			nameValuePairs.add(new BasicNameValuePair("count", String.valueOf(count)));
			//nameValuePairs.add(new BasicNameValuePair("to", to.toString()));
			nameValuePairs.add(new BasicNameValuePair("to", to));
			
			URI uri = new URIBuilder(request.getURI())
				      .addParameters(nameValuePairs)
				      .build();
			
			request.setURI(uri);
			
			HttpResponse response = client.execute(request);
			HttpEntity entity = response.getEntity();
			
			String rtnString = EntityUtils.toString(entity, "UTF-8");

			JSONArray ja = new JSONArray(rtnString);
			rtnList = new Gson().fromJson(ja.toString(), new TypeToken<ArrayList<MinuteCandleModel>>() {}.getType());
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		return rtnList;
	}
	
	public List<MinuteCandleModel> getCandlesFromDB (String candle_time, int limit)	{
		List<MinuteCandleModel> candles = new ArrayList<MinuteCandleModel>();
		try {
			candles = candleMapper.selectMinuteCandles(candle_time, limit);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return candles;
	}
	
	public MinuteCandleModel getCandleFromDB(String dateString) 	{
		MinuteCandleModel candle = null;
		try {
			candle = candleMapper.selectMinuteCandle(dateString);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("message", e);
		}
		return candle;
	}

	public int updateCandle(MinuteCandleModel candle) 	{
		int rtn = -1;
		try {
			rtn = candleMapper.updateMinuteCandle(candle);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("message", e);
		}
		return rtn;
	}
}
