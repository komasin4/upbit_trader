package komasin4.finance.upbit.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.UUID;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;


public class AuthUtil {
	private static String accessKey = "dF1FqCxo8SMVjtyF0GfdYe6z7B2vycspmgickczU";
	private static String secretKey = "B0yzxqTXejqfhcE34fr4xUamdFWlVNQ9RX0XwgaW";

	public static String getAuthToken()	{
		//      String accessKey = System.getenv("UPBIT_OPEN_API_ACCESS_KEY");
		//      String secretKey = System.getenv("UPBIT_OPEN_API_SECRET_KEY");
		//      String serverUrl = System.getenv("UPBIT_OPEN_API_SERVER_URL");

		Algorithm algorithm = Algorithm.HMAC256(secretKey);
		String jwtToken = JWT.create()
				.withClaim("access_key", accessKey)
				.withClaim("nonce", UUID.randomUUID().toString())
				.sign(algorithm);

		String authenticationToken = "Bearer " + jwtToken;

		return authenticationToken;
	}

	public static String getAuthTokenForOrder(ArrayList<String> queryElements)	{
		String authenticationToken = null;


		try {
			String queryString = String.join("&", queryElements.toArray(new String[0]));
			MessageDigest md;
			md = MessageDigest.getInstance("SHA-512");
			md.update(queryString.getBytes("UTF-8"));

			String queryHash = String.format("%0128x", new BigInteger(1, md.digest()));

			Algorithm algorithm = Algorithm.HMAC256(secretKey);
			String jwtToken = JWT.create()
					.withClaim("access_key", accessKey)
					.withClaim("nonce", UUID.randomUUID().toString())
					.withClaim("query_hash", queryHash)
					.withClaim("query_hash_alg", "SHA512")
					.sign(algorithm);

			authenticationToken = "Bearer " + jwtToken;

		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return authenticationToken;

	}
}
