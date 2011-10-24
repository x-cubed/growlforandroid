package com.growlforandroid.common;

import java.net.*;
import java.util.Enumeration;

import android.util.Log;

public final class Utility {
	private static final String HEXES = "0123456789ABCDEF";
	
	/**
	 * Converts a hex representation of a byte array into a byte array.
	 * 
	 * Borrowed from Stack Overflow: {@link http://stackoverflow.com/questions/140131/convert-a-string-representation-of-a-hex-dump-to-a-byte-array-using-java}
	 *  
	 * @param s
	 * @return
	 */
	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		
		if (len % 2 != 0)
			throw new IllegalArgumentException("String should have an even number of characters");
		
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {		
			data[i / 2] = (byte) ((getHexNibbleValue(s.charAt(i)) << 4) + getHexNibbleValue(s.charAt(i + 1)));
		}
		return data;
	}

	public static byte getHexNibbleValue(char c) {
		byte nibble = (byte)HEXES.indexOf(Character.toUpperCase(c));
		if (nibble < 0) {
			throw new IllegalArgumentException("Invalid hex character: " + c);
		}
		return nibble;
	}
	
	public static String getHexStringFromByteArray(byte[] raw) {
		return getHexStringFromByteArray(raw, 0, raw.length);
	}

	public static String getHexStringFromByteArray(byte[] raw, int offset) {
		return getHexStringFromByteArray(raw, offset, raw.length - offset);
	}

	
	public static String getHexStringFromByteArray(byte[] raw, int offset, int length) {
		if (raw == null) {
			return null;
		}
		final StringBuilder hex = new StringBuilder(2 * raw.length);
		for (int i = 0; i < length; i++) {
			byte b = raw[offset + i];
			char h1 = HEXES.charAt((b & 0xF0) >> 4);
			char h2 = HEXES.charAt((b & 0x0F));
			hex.append(h1).append(h2);
		}
		return hex.toString();
	}

	public static boolean compareArrays(byte[] array0, byte[] array1) {
		if (array0.length != array1.length)
			return false;

		for (int i = 0; i < array0.length; i++) {
			if (array0[i] != array1[i])
				return false;
		}
		return true;
	}

	public static void logByteArrayAsHex(String tag, byte[] data) {
		final int BLOCK_SIZE = 50;
		for(int offset = 0; offset < data.length; offset += BLOCK_SIZE) {
			int remaining = data.length - offset;
			int blockLength = remaining > BLOCK_SIZE ? BLOCK_SIZE : remaining;
			Log.i(tag, getHexStringFromByteArray(data, offset, blockLength));
		}		
	}
	
	public static InetAddress getLocalIpAddress() {
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress()) {
	                    return inetAddress;
	                }
	            }
	        }
	    } catch (SocketException ex) {
	        Log.e("Utility.getLocalIpAddress", ex.toString());
	    }
	    return null;
	}
}
