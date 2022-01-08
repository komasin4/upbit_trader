package komasin4.finance.upbit;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import komasin4.finance.upbit.base.Constants;
import komasin4.finance.upbit.model.DayCandleModel;
import komasin4.finance.upbit.model.MinuteCandleModel;
import komasin4.finance.upbit.util.DateUtil;

public class TestClass {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	final static String serverUrl = "https://api.upbit.com";
	
	public String convertToDateString(String str)	{
		String regEx = "(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{2})";
		return str.replaceAll(regEx, "$1-$2-$3 $4:$5");
	}
	
	public List<MinuteCandleModel> getCandle(String fromTime, String toTime, int unit, int count)	{
		logger.debug(fromTime + " ~ " + toTime);
		
		List<MinuteCandleModel> candleList = new ArrayList<MinuteCandleModel>();
		
		OffsetDateTime to = DateUtil.getKSTTime(fromTime);
		OffsetDateTime current = to;
		
		do {
			candleList.addAll(getCandle(unit, current, count));
			String currentDateString = candleList.get(candleList.size()-1).getCandle_date_time_kst();
			current = LocalDateTime.parse(currentDateString).atOffset(ZoneOffset.of("+9"));
		} while (current.isAfter(DateUtil.getKSTTime(toTime)));
		
		return candleList;
	}
	
	private List<MinuteCandleModel> getCandle(int unit, OffsetDateTime to, int count)	{
		//https://api.upbit.com/v1/candles/minutes/5?market=KRW-BTC&count=3
		List<MinuteCandleModel> rtnList = new ArrayList<MinuteCandleModel>();
		String authenticationToken = getAuthToken();

		try {
			HttpGet request = new HttpGet(Constants.serverUrl + "/v1/candles/minutes/" + unit);
			HttpClient client = HttpClientBuilder.create().build();
			request.setHeader("Content-Type", "application/json");
			request.addHeader("Authorization", authenticationToken);
			
			List nameValuePairs = new ArrayList();
			nameValuePairs.add(new BasicNameValuePair("market", "KRW-BTC"));
			nameValuePairs.add(new BasicNameValuePair("count", String.valueOf(count)));
			nameValuePairs.add(new BasicNameValuePair("to", to.toString()));
			
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
	
	private String getAuthToken()	{
		//      String accessKey = System.getenv("UPBIT_OPEN_API_ACCESS_KEY");
		//      String secretKey = System.getenv("UPBIT_OPEN_API_SECRET_KEY");
		//      String serverUrl = System.getenv("UPBIT_OPEN_API_SERVER_URL");
		String accessKey = "dF1FqCxo8SMVjtyF0GfdYe6z7B2vycspmgickczU";
		String secretKey = "B0yzxqTXejqfhcE34fr4xUamdFWlVNQ9RX0XwgaW";

		Algorithm algorithm = Algorithm.HMAC256(secretKey);
		String jwtToken = JWT.create()
				.withClaim("access_key", accessKey)
				.withClaim("nonce", UUID.randomUUID().toString())
				.sign(algorithm);

		String authenticationToken = "Bearer " + jwtToken;
		
		return authenticationToken;
	}
	
	private void getBalance()	{
		
		String authenticationToken = getAuthToken();

		try {
			HttpClient client = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(serverUrl + "/v1/accounts");
			request.setHeader("Content-Type", "application/json");
			request.addHeader("Authorization", authenticationToken);

			HttpResponse response = client.execute(request);
			HttpEntity entity = response.getEntity();

			System.out.println(EntityUtils.toString(entity, "UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}


	public List<DayCandleModel> getDayCandle()	{
		logger.debug("Get DayCandle");
		List<DayCandleModel> rtnList = new ArrayList<DayCandleModel>();
		String authenticationToken = getAuthToken();

		try {
			HttpClient client = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(serverUrl + "/v1/candles/days");
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
			
			for(DayCandleModel tmp:rtnList)	{
				logger.debug("day candle:" + tmp.toString());
			}
			
		} catch (IOException | URISyntaxException | JSONException e) {
			e.printStackTrace();
		}		
		return null;
	}
}
