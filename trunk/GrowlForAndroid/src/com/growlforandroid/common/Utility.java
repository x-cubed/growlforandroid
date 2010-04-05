package com.growlforandroid.common;

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
			data[i / 2] = (byte) (
				(HEXES.indexOf(s.charAt(i)) << 4) +
				 HEXES.indexOf(s.charAt(i + 1)));
		}
		return data;
	}

	public static String getHexStringFromByteArray(byte[] raw) {
		if (raw == null) {
			return null;
		}
		final StringBuilder hex = new StringBuilder(2 * raw.length);
		for (final byte b : raw) {
			hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(
					HEXES.charAt((b & 0x0F)));
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
}
