package com.growlforandroid.gntp;

import java.io.IOException;
import java.util.*;

import com.growlforandroid.common.*;

import android.content.Context;
import android.util.Log;

/**
 * Represents a basic GNTP message (currently either a Request or Response),
 * which is made up of a leader line followed by zero or more headers
 * 
 * @author Carey
 * 
 */
public abstract class GntpMessage {
	protected final int CONNECTION_TIMEOUT_MS = 10000;
	protected final int READ_TIMEOUT_MS = 10000;

	private final static ArrayList<String> _commonHeaderOrder = new ArrayList<String>();
	private final static Map<String, String> _commonHeaders = new HashMap<String, String>();

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

	public void addCommonHeaders(Context context) throws Exception {
		synchronized (_commonHeaders) {
			for (String key : _commonHeaderOrder) {
				String value = _commonHeaders.get(key);
				addHeader(key, value);
			}
		}
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
			Log.e("GntpMessage.getHeaderIn", "Header \"" + key + "\" has value \"" + value
					+ "\" that can't be parsed as an int");
		}
		return result;
	}

	public void write(ChannelWriter writer) throws IOException {
		String leaderLine = getLeaderLine();
		writer.write(leaderLine + Constants.END_OF_LINE);
		writeHeaders(writer);
		writer.write(Constants.END_OF_LINE);
	}

	protected abstract String getLeaderLine() throws IOException;

	protected void writeHeaders(ChannelWriter writer) throws IOException {
		for (String key : _headerOrder) {
			writeHeader(writer, key, getHeaderString(key));
		}
	}

	protected static void writeHeader(ChannelWriter writer, String key, String value) throws IOException {
		String header = key + ": " + value;
		writer.write(header + Constants.END_OF_LINE);
	}

	/**
	 * Reads a series of key/value pair headers from the specified source, until
	 * the end of the stream or a blank line is reached, calling addHeader on
	 * each item
	 * 
	 * @param reader
	 *            The source to read from
	 * @throws Exception
	 */
	protected void readHeaders(EncryptedChannelReader reader) throws IOException {
		String line = reader.readLine();
		while ((line != null) && !line.equals("")) {
			addHeader(line);
			line = reader.readLine();
		}
	}

	protected static String escapeGntpString(String source) {
		return source.replace('\r', '\n');
	}

	public static void clearCommonHeaders() {
		synchronized (_commonHeaders) {
			_commonHeaderOrder.clear();
			_commonHeaders.clear();
		}
	}

	public static void addCommonHeader(String key, String value) {
		synchronized (_commonHeaders) {
			_commonHeaderOrder.add(key);
			_commonHeaders.put(key, value);
		}
	}
}
