package com.growlforandroid.gntp;

import java.io.IOException;

import com.growlforandroid.common.ChannelWriter;

import android.os.Build;
import android.util.Log;

/*
 * Builds a GNTP Response in preparation for writing to a socket
 */
public class Response {
	private final ResponseType _type;
	private final GntpError _error;
	private final String _errorDescription;
	private final EncryptionType _encryptionType = EncryptionType.None;
	
	public Response(ResponseType messageType) throws Exception {
		this(messageType, null);
	}
	
	public Response(ResponseType messageType, GntpError error) throws Exception {
		this(messageType, error, null);
	}
	
	public Response(ResponseType messageType, GntpError error, String errorDescription) throws Exception {
		switch(messageType) {
			case OK:
				if (error != null)
					throw new Exception("Type is OK, but error details were provided");
				_errorDescription = "";
				break;
				
			case Error:
				if (error == null)
					throw new Exception("Type is Error, but no error details were provided");
				_errorDescription = (errorDescription == null) ? error.Description : errorDescription;
				break;
			
			default:
				throw new Exception("Invalid message type provided");		
		}		
		
		_type = messageType;
		_error = error;
	}
	
	public ResponseType getType() {
		return _type;
	}
	
	public void write(ChannelWriter channelWriter) throws Exception {
		writeResponseLine(channelWriter, _type, _encryptionType);
		if (_error != null) {
			Log.w("Response.write", "Sending error response \"" + _errorDescription + "\"");
			writeHeader(channelWriter, Constants.HEADER_ERROR_CODE, String.valueOf(_error.ErrorCode));
			writeHeader(channelWriter, Constants.HEADER_ERROR_DESCRIPTION, _errorDescription);
		}
		writeOriginResponseHeaders(channelWriter);
		channelWriter.write(Constants.END_OF_LINE);
	}

	private static void writeResponseLine(ChannelWriter out, ResponseType messageType, EncryptionType encryptionType) throws IOException {
		out.write(Constants.RESPONSE_PROTOCOL_VERSION + " " + messageType + " " +
			encryptionType.toString() + Constants.END_OF_LINE);
	}

	private static void writeOriginResponseHeaders(ChannelWriter out) throws Exception {
		writeHeader(out, Constants.HEADER_ORIGIN_MACHINE_NAME, Build.DEVICE);
		writeHeader(out, Constants.HEADER_ORIGIN_SOFTWARE_NAME, "Growl for Android");
		writeHeader(out, Constants.HEADER_ORIGIN_SOFTWARE_VERSION, "0.1");
		writeHeader(out, Constants.HEADER_ORIGIN_PLATFORM_NAME, Build.DISPLAY);
		writeHeader(out, Constants.HEADER_ORIGIN_PLATFORM_VERSION, Build.VERSION.RELEASE);
	}

	private static void writeHeader(ChannelWriter out, String key, String value) throws Exception {
		if (key.contains(":"))
			throw new Exception("Invalid header key: " + key);
		
		out.write(key + ": " + escapeGntpString(value) + Constants.END_OF_LINE);
	}
	
	private static String escapeGntpString(String source) {
		return source.replace('\r', '\n');
	}
}
