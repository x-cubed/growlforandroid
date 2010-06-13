package com.growlforandroid.gntp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.growlforandroid.common.ChannelWriter;
import com.growlforandroid.common.EncryptedChannelReader;

import android.os.Build;
import android.util.Log;

public abstract class GntpMessage {
	private Map<String, String> _headers = new HashMap<String, String>();
	
	public void addHeader(String key, int value) {
		addHeader(key, Integer.toString(value));
	}
	
	public void addHeader(String key, String value) {
		_headers.put(key, escapeGntpString(value));
	}
	
	public void addCommonHeaders() throws Exception {
		addHeader(Constants.HEADER_ORIGIN_MACHINE_NAME, Build.DEVICE);
		addHeader(Constants.HEADER_ORIGIN_SOFTWARE_NAME, "Growl for Android");
		addHeader(Constants.HEADER_ORIGIN_SOFTWARE_VERSION, "0.9");
		addHeader(Constants.HEADER_ORIGIN_PLATFORM_NAME, Build.DISPLAY);
		addHeader(Constants.HEADER_ORIGIN_PLATFORM_VERSION, Build.VERSION.RELEASE);
	}
	
	public String getHeaderString(String key) {
		return _headers.get(key);
	}
	
	public int getHeaderInt(String key, int defaultValue) {
		String value = getHeaderString(key);
		int result = defaultValue;
		try {
			result = Integer.parseInt(value);
		} catch (NumberFormatException nfe) {
			Log.e("GntpMessage.getHeaderInt", "Header \"" + key + "\" has value \"" + value + "\" that can't be parsed as an int");
		}
		return result;
	}
	
	public void write(ChannelWriter writer) throws IOException {
		writeLeaderLine(writer);
		writeHeaders(writer);
		writer.write(Constants.END_OF_LINE);
	}

	protected abstract void writeLeaderLine(ChannelWriter writer) throws IOException;
	
	protected void writeHeaders(ChannelWriter writer) throws IOException {
		for(String key: _headers.keySet()) {
			writeHeader(writer, key, getHeaderString(key));
		}
	}
	
	/**
	 * Reads a series of key/value pair headers from the specified source,
	 * until the end of the stream or a blank line is reached
	 * @param reader		The source to read from
	 * @throws Exception
	 */
	protected void readHeaders(EncryptedChannelReader reader) throws IOException {
		String line = reader.readLine();
		while ((line != null) && !line.equals("")) {
			String[] keyAndValue = line.split(":");
			addHeader(keyAndValue[0], keyAndValue[1].trim());
			line = reader.readLine();
		}
	}
	
	protected static void writeHeader(ChannelWriter writer, String key, String value) throws IOException {
		writer.write(key + ": " + value + Constants.END_OF_LINE);
	}
	
	protected static String escapeGntpString(String source) {
		return source.replace('\r', '\n');
	}
}
