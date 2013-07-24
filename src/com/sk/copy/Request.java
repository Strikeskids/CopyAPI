package com.sk.copy;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Request {

	private final URL startUrl;
	private final URL baseUrl;
	private final String method;
	private URL url;
	private final Map<String, String> query = new HashMap<>(), oauth = new HashMap<>(), headers = new HashMap<>();

	public Request(String url, String method) throws MalformedURLException {
		this.method = method.toUpperCase();
		startUrl = new URL(url);
		baseUrl = new URL(startUrl, startUrl.getPath());
	}

	public void addHeader(String key, String value) {
		headers.put(key, value);
	}

	public void addQuery(String key, String value) {
		query.put(key, value);
	}

	public Map<String, String> getQuery() {
		return query;
	}

	public void addOAuthParam(String key, String value) {
		oauth.put("oauth_" + key, value);
	}

	private void finalizeUrl() {
		if (method.equals("GET")) {
			try {
				Map<String, String> params = getUrlParams();
				params.putAll(this.query);

				if (params.size() > 0)
					url = new URL(baseUrl, "?" + IOUtil.joinParams(params));
				else
					url = baseUrl;
			} catch (MalformedURLException ex) {
				ex.printStackTrace();
			}
		} else {
			url = startUrl;
		}
	}

	private Map<String, String> getUrlParams() {
		return IOUtil.splitParams(this.startUrl.getQuery());
	}

	public void signOAuth(OAuthToken consumer, OAuthToken token) {
		addOAuthParam("signature_method", "HMAC-SHA1");
		addOAuthParam("version", "1.0");
		addOAuthParam("nonce", IOUtil.generateNonce());
		addOAuthParam("timestamp", Objects.toString(System.currentTimeMillis() / 1000));

		addOAuthParam("consumer_key", consumer.getKey());
		if (token != null)
			addOAuthParam("token", token.getKey());
		TreeMap<String, String> allParams = new TreeMap<>();
		allParams.putAll(query);
		allParams.putAll(oauth);
		allParams.putAll(getUrlParams());
		finalizeUrl();

		StringBuilder paramStringBuilder = new StringBuilder();
		for (Entry<String, String> param : allParams.entrySet()) {
			paramStringBuilder.append("&");
			paramStringBuilder.append(IOUtil.urlEncode(param.getKey()));
			paramStringBuilder.append("=");
			paramStringBuilder.append(IOUtil.urlEncode(param.getValue()));
		}
		String paramString = paramStringBuilder.length() > 0 ? IOUtil.urlEncode(paramStringBuilder.substring(1))
				: "";
		StringBuilder signature = new StringBuilder();
		signature.append(method);
		signature.append("&");
		signature.append(IOUtil.urlEncode(baseUrl.toExternalForm()));
		signature.append("&");
		signature.append(paramString);
		StringBuilder secret = new StringBuilder();
		secret.append(IOUtil.urlEncode(new String(consumer.getSecret())));
		secret.append("&");
		if (token != null)
			secret.append(IOUtil.urlEncode(new String(token.getSecret())));
		try {
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(new SecretKeySpec(secret.toString().getBytes(), "HmacSHA1"));
			byte[] digest = mac.doFinal(signature.toString().getBytes());
			addOAuthParam("signature", IOUtil.urlEncode(Base64Util.encode(digest)));
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			e.printStackTrace();
		}
	}

	public URLConnection openConnection(boolean b) throws IOException {
		if (url == null)
			return null;
		URLConnection conn = url.openConnection();
		for (Entry<String, String> prop : headers.entrySet())
			conn.addRequestProperty(prop.getKey(), prop.getValue());
		StringBuilder oauthProp = new StringBuilder("OAuth ");
		for (Entry<String, String> prop : oauth.entrySet()) {
			if (oauthProp.length() > 6)
				oauthProp.append(",");
			oauthProp.append(prop.getKey());
			oauthProp.append("=\"");
			oauthProp.append(prop.getValue());
			oauthProp.append("\"");
		}
		conn.addRequestProperty("Authorization", oauthProp.toString());
		if (conn instanceof HttpURLConnection)
			((HttpURLConnection) conn).setRequestMethod(method);
		boolean postData = method.equals("POST") && query.size() > 0 && !b;
		conn.setDoOutput(b || postData);
		conn.connect();
		if (postData) {
			String query = IOUtil.joinParams(this.query);
			conn.getOutputStream().write(query.getBytes());
			conn.getOutputStream().close();
		}
		return conn;
	}

}
