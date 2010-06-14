package com.growlforandroid.gntp;

import java.io.IOException;
import java.util.*;

import com.growlforandroid.common.*;

import android.os.Build;
import android.util.Log;

/**
 * Represents a basic GNTP message (currently either a Request or Response),
 * which is made up of a leader line followed by zero or more headers 
 * @author Carey
 *
 */
public abstract class GntpMessage {
	protected final int CONNECTION_TIMEOUT_MS = 10000;
	protected final int READ_TIMEOUT_MS = 10000;
	
	private ArrayList<String> _headerOrder = new ArrayList<String>();
	private Map<String, String> _headers = new HashMap<String, String>();
	
	public void addHeader(String keyAndValue) {
		String[] parts = keyAndValue.split(":");
		addHeader(parts[0].trim(), parts[1].trim());
	}
	
	public void addHeader(String key, String value) {
		_headerOrder.add(key);
		_headers.put(key, escapeGntpString(value));
	}
	
	public void addHeader(String key, int value) {
		addHeader(key, Integer.toString(value));
	}
	
	public void addCommonHeaders() throws Exception {	
		addHeader(Constants.HEADER_ORIGIN_MACHINE_NAME, Utility.getDeviceFriendlyName());
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
			Log.e("GntpMessage.getHeaderInt",
				"Header \"" + key + "\" has value \"" + value + "\" that can't be parsed as an int");
		}
		return result;
	}
	
	public void write(ChannelWriter writer) throws IOException {
		String leaderLine = getLeaderLine();
		Log.i("GntpMessage.write", leaderLine);
		writer.write(leaderLine + Constants.END_OF_LINE);
		writeHeaders(writer);
		writer.write(Constants.END_OF_LINE);
	}

	protected abstract String getLeaderLine() throws IOException;
	
	protected void writeHeaders(ChannelWriter writer) throws IOException {
		for(String key: _headerOrder) {
			writeHeader(writer, key, getHeaderString(key));
		}
	}

	protected static void writeHeader(ChannelWriter writer, String key, String value) throws IOException {
		String header = key + ": " + value;
		Log.i("GntpMessage.writeHeader", header);
		writer.write(header + Constants.END_OF_LINE);
	}
	
	
	/**
	 * Reads a series of key/value pair headers from the specified source, until
	 * the end of the stream or a blank line is reached, calling addHeader on each item
	 * @param reader		The source to read from
	 * @throws Exception
	 */
	protected void readHeaders(EncryptedChannelReader reader) throws IOException {
		String line = reader.readLine();
		while ((line != null) && !line.equals("")) {
			Log.i("GntpMessage.readHeaders", line);
			addHeader(line);
			line = reader.readLine();
		}
	}

	protected static String escapeGntpString(String source) {
		return source.replace('\r', '\n');
	}
}
