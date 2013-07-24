package com.sk.copy;
import java.nio.ByteBuffer;

public class Base64Util {

	public static String encode(byte[] raw) {
		StringBuilder ret = new StringBuilder();
		int concat = 0;
		int count = 0;
		for (byte b : raw) {
			concat = (concat << 8) | (b & 0xff);
			count += 8;
			if (count == 24) {
				append(ret, concat, count);
				count = 0;
				concat = 0;
			}
		}
		if (count == 8) {
			concat <<= 4;
			count += 4;
			append(ret, concat, count);
		} else if (count == 16) {
			concat <<= 2;
			count += 2;
			append(ret, concat, count);
		}
		while (ret.length() % 4 != 0)
			ret.append("=");
		return ret.toString();
	}

	private static void append(StringBuilder builder, int concat, int count) {
		for (int i = count - 6; i >= 0; i -= 6) {
			builder.append(getChar(concat >> i));
		}
	}

	public static byte[] decode(String encoded) {
		ByteBuffer buf = ByteBuffer.allocate((encoded.length() / 4 + 1) * 3);
		int concat = 0;
		int count = 0;
		for (char c : encoded.toCharArray()) {
			int current = getValue(c);
			if (current == -1)
				continue;
			concat = (concat << 6) | current;
			count += 6;
			if (count == 24) {
				buf.put((byte) (concat >> 16));
				buf.put((byte) (concat >> 8));
				buf.put((byte) concat);
				count = 0;
				concat = 0;
			}
		}
		if (count == 12) {
			buf.put((byte) (concat >> 4));
		}
		if (count == 18) {
			buf.put((byte) (concat >> 10));
			buf.put((byte) (concat >> 2));
		}
		return buf.array();
	}

	private static final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
	private static final byte[] values = new byte[0xFF];
	static {
		for (int i = 0; i < values.length; ++i)
			values[i] = -1;
		for (int i = 0, len = chars.length(); i < len; ++i)
			values[chars.charAt(i)] = (byte) i;
	}

	private static int getValue(char c) {
		return c >= 0 && c < 0xFF ? values[c] : -1;
	}

	private static char getChar(int value) {
		return chars.charAt(value & 0x3f);
	}

}
