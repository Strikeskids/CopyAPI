package com.sk.copy;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Scanner;

public class CreateToken {

	private final OAuthToken consumer;
	private final String[] endpoints;
	private OAuthToken access;

	public CreateToken(OAuthToken consumer, String... endpoints) throws MalformedURLException {
		this.consumer = consumer;
		this.endpoints = endpoints;
	}

	public boolean init() {
		Request req;
		try {
			req = new Request(endpoints[0], "GET");
			req.addOAuthParam("callback", "http://www.strikeskids.com/printparams.php");
			req.signOAuth(consumer, null);
			OAuthToken middle = grabToken(read(req.openConnection(false)));
			if (middle == null)
				return false;

			URL browseBase = new URL(endpoints[1]);
			URI browseLoc = new URL(browseBase, browseBase.getPath() + "?oauth_token=" + middle.getKey()).toURI();
			System.out.printf("Browsing to %s%n", browseLoc);
			try {
				Desktop.getDesktop().browse(browseLoc);
			} catch (UnsupportedOperationException | SecurityException ex) {
				ex.printStackTrace();
			}
			Scanner sc = new Scanner(System.in);
			System.out.print("Enter \"oauth_verifier\": ");
			String verifier = sc.nextLine();
			sc.close();

			req = new Request(endpoints[2], "GET");
			req.addOAuthParam("verifier", verifier);
			req.signOAuth(consumer, middle);
			this.access = grabToken(read(req.openConnection(false)));

			return true;
		} catch (IOException | URISyntaxException ex) {
			ex.printStackTrace();
			return false;
		}
	}

	private OAuthToken grabToken(String result) {
		Map<String, String> params = IOUtil.splitParams(result);
		if (params.containsKey("oauth_token") && params.containsKey("oauth_token_secret")) {
			return new OAuthToken(params.get("oauth_token"), params.get("oauth_token_secret"));
		} else {
			return null;
		}
	}

	private String read(URLConnection conn) throws IOException {
		StringBuilder ret = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line;
		while ((line = reader.readLine()) != null) {
			ret.append(line);
			ret.append("\n");
		}
		reader.close();
		ret.deleteCharAt(ret.length() - 1);
		return ret.toString();
	}

	public OAuthToken getAccess() {
		return access;
	}

}
