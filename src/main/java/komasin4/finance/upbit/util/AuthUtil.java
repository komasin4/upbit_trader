package komasin4.finance.upbit.util;

import java.util.UUID;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

public class AuthUtil {
	public static String getAuthToken()	{
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
}
