package com.sk.copy;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLConnection;

public class CopyUtil {

	private final OAuthToken consumer, access;

	public CopyUtil(OAuthToken consumer, OAuthToken access) {
		this.consumer = consumer;
		this.access = access;
	}

	public boolean getFile(String path, OutputStream location) throws IOException {
		if (!path.startsWith("/"))
			throw new IllegalArgumentException("Bad Path");
		byte[] buffer = new byte[8196];
		Request req = basicRequest(API_ACCESS + FILE_ACCESSPOINT + path, "GET");
		req.signOAuth(consumer, access);
		HttpURLConnection conn = (HttpURLConnection) req.openConnection(false);
		if (conn.getResponseCode() != 200)
			return false;
		InputStream fromSource = conn.getInputStream();
		int count;
		while ((count = fromSource.read(buffer)) > 0) {
			location.write(buffer, 0, count);
		}
		fromSource.close();
		location.close();
		return true;
	}

	public boolean putFile(String path, InputStream data, String mimeType) throws IOException {
		int lastSplit = path.lastIndexOf('/') + 1;
		if (!path.startsWith("/"))
			throw new IllegalArgumentException("Bad Path");
		String fileName = path.substring(lastSplit);
		String filePath = path.substring(0, lastSplit);
		Request req = basicRequest(API_ACCESS + FILE_ACCESSPOINT + filePath, "POST");
		String delim = IOUtil.generateString(DELIM_ALPHABET, 8);

		req.addHeader("Content-Type", "multipart/form-data; boundary=" + delim);
		req.signOAuth(consumer, access);
		URLConnection conn = req.openConnection(true);
		DataOutputStream output = new DataOutputStream(new BufferedOutputStream(conn.getOutputStream()));
		output.writeBytes(ddash + delim + crlf);
		output.writeBytes("Content-Disposition: form-data; name=\"\file\"; filename=\"" + fileName + "\"" + crlf);
		output.writeBytes("Content-Type: " + mimeType + crlf);
		output.writeBytes(crlf);
		byte[] pass = new byte[8192];
		int count;
		while ((count = data.read(pass)) > 0) {
			output.write(pass, 0, count);
		}
		output.writeBytes(crlf + ddash + delim + ddash + crlf);
		output.flush();

		output.close();

		String in = IOUtil.readFrom(conn.getInputStream());
		System.out.println(in);
		return true;
	}

	public boolean putFile(String path, byte[] data, String mimeType) throws IOException {
		return putFile(path, new ByteArrayInputStream(data), mimeType);
	}

	private Request basicRequest(String loc, String method) throws MalformedURLException {
		Request ret = new Request(loc, method);
		ret.addHeader("X-Api-Version", "1");
		ret.addHeader("Accept", "application/json");
		return ret;
	}

	private static final String API_ACCESS = "https://api.copy.com/rest";
	private static final String FILE_ACCESSPOINT = "/files";
	private static final String DELIM_ALPHABET = "1234567890qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM";
	private static final String crlf = "\r\n", ddash = "--";

}
