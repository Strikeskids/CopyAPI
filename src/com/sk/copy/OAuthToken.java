package com.sk.copy;
public class OAuthToken {

	private final String key;
	private final String secret;

	public OAuthToken(String key, String secret) {
		this.key = key;
		this.secret = secret;
	}

	public String getKey() {
		return key;
	}

	public String getSecret() {
		return secret;
	}

	@Override
	public String toString() {
		return String.format("OAuthToken: %s %s", key, secret);
	}

}
