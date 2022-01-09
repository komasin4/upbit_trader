package komasin4.finance.upbit.service;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.auth0.jwt.algorithms.Algorithm;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import komasin4.finance.upbit.base.Constants;
import komasin4.finance.upbit.model.DayCandleModel;
import komasin4.finance.upbit.model.OrderModel;
import komasin4.finance.upbit.model.OrderResultModel;
import komasin4.finance.upbit.util.AuthUtil;

@Service
public class OrderService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public boolean order(OrderModel order)	{
		
		boolean bOrder = false;
		
//		if("ask".equals(order.getSide()))
//			return bOrder;
		
		logger.info("order start");

		try	{
			HashMap<String, String> params = new HashMap<>();
			params.put("market", "KRW-BTC");
			//params.put("side", "bid");
			params.put("side", order.getSide());
			//params.put("volume", "0.01");
			params.put("volume", String.valueOf(order.getVolume()));
			//params.put("price", "100");
			params.put("price", String.valueOf(order.getPrice()));
			params.put("ord_type", "limit");

			ArrayList<String> queryElements = new ArrayList<>();
			for(Map.Entry<String, String> entity : params.entrySet()) {
				queryElements.add(entity.getKey() + "=" + entity.getValue());
			}

			/*
	        String queryString = String.join("&", queryElements.toArray(new String[0]));

	        MessageDigest md = MessageDigest.getInstance("SHA-512");
	        md.update(queryString.getBytes("UTF-8"));

	        String queryHash = String.format("%0128x", new BigInteger(1, md.digest()));

	        Algorithm algorithm = Algorithm.HMAC256(secretKey);
	        String jwtToken = JWT.create()
	                .withClaim("access_key", accessKey)
	                .withClaim("nonce", UUID.randomUUID().toString())
	                .withClaim("query_hash", queryHash)
	                .withClaim("query_hash_alg", "SHA512")
	                .sign(algorithm);

	        String authenticationToken = "Bearer " + jwtToken;
	        */
			
	        String authenticationToken = AuthUtil.getAuthTokenForOrder(queryElements);

			try {
				HttpClient client = HttpClientBuilder.create().build();
				HttpPost request = new HttpPost(Constants.serverUrl + "/v1/orders");
				request.setHeader("Content-Type", "application/json");
				request.addHeader("Authorization", authenticationToken);
				request.setEntity(new StringEntity(new Gson().toJson(params)));

				HttpResponse response = client.execute(request);
				HttpEntity entity = response.getEntity();

				String rtnString = EntityUtils.toString(entity, "UTF-8");
				OrderResultModel result = new Gson().fromJson(rtnString, OrderResultModel.class);
				
				logger.info(rtnString);
				logger.info(result.toString());
				
				if(result != null && result.getState() != null)
					bOrder = true;
				//System.out.println(EntityUtils.toString(entity, "UTF-8"));
			} catch (IOException e) {
				logger.error("message", e);
			}
		} catch (Exception e)	{
			logger.error("message", e);
		}
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		logger.info("order end:" + bOrder);
		
		return bOrder;
	}
}
