
package komasin4.finance.upbit.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	
	public List<DayCandleModel> getDayCandle()	{
		List<DayCandleModel> rtnList = new ArrayList<DayCandleModel>();
		String authenticationToken = AuthUtil.getAuthToken();

		logger.debug("authenticationToken:" + authenticationToken);

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
}
