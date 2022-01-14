package komasin4.finance.upbit.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import komasin4.finance.upbit.base.Constants;

@Service
public class SendMessageService {
	
	@Value("${bot.token}")
	private String sToken;
	
	@Value("${bot.chatid}")
	private String sChatId;
	
	public JsonObject send(String sMessage)	{

		JsonObject rtnObject = new JsonObject();
		
		try {
			HttpClient client = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(Constants.telegramUrl + sToken + "/sendmessage");
			request.setHeader("Content-Type", "application/json");
			//request.addHeader("Authorization", authenticationToken);

			List nameValuePairs = new ArrayList();
			nameValuePairs.add(new BasicNameValuePair("chat_id", sChatId));
			nameValuePairs.add(new BasicNameValuePair("text", sMessage));

			URI uri = new URIBuilder(request.getURI())
				      .addParameters(nameValuePairs)
				      .build();
			
			request.setURI(uri);

			HttpResponse response = client.execute(request);
			HttpEntity entity = response.getEntity();
			
			String rtnString = EntityUtils.toString(entity, "UTF-8");

			rtnObject = new Gson().fromJson(rtnString, JsonObject.class);
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}		
		return rtnObject;
	}
}
