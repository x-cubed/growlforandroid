package com.growlforandroid.gntp;

import java.io.*;
import java.net.*;
import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;
import java.util.*;

import android.util.Log;

import com.growlforandroid.common.*;

public class Request
	extends GntpMessage {
	
	private RequestState _currentState = RequestState.Connected;
	
	private RequestType _requestType;
	private EncryptionType _encryptionType;
	private HashAlgorithm _hashAlgorithm;
	private String _initVector = "";
	private String _hash = "";
	private String _salt = "";
	
	private int _notificationsCount = 0;
	private int _notificationIndex = 0;
	private Map<Integer, Map<String, String>> _notificationsHeaders = new HashMap<Integer, Map<String, String>>();
	
	public Request(RequestType type, EncryptionType encryption, HashAlgorithm algorithm, String password) throws Exception {
		_requestType = type;
		_encryptionType = encryption;
		_hashAlgorithm = algorithm;
		
		if (_encryptionType != EncryptionType.None)
			throw new Exception("Encryption is not yet supported");
		
		// Generate the hash and salt
		_salt = UUID.randomUUID().toString();
		byte[] salt = Utility.hexStringToByteArray(_salt);
		byte[] hash = _hashAlgorithm.calculateKey(password, salt);
		_hash = Utility.getHexStringFromByteArray(hash);
	}
	
	private enum RequestState {
		Connected,
		ReadingRequestHeaders,
		ReadingNotificationHeaders,
		EndOfRequest,
		ResponseSent
	}

	public void sendTo(String hostAndPort) throws GntpException, UnknownHostException, IOException {
		String host = hostAndPort;
		int port = 23053;
		
		int colon = hostAndPort.indexOf(':');
		if (colon >= 0) {
			port = Integer.parseInt(hostAndPort.substring(colon));
			host = hostAndPort.substring(0, colon);
		}
		
		sendTo(host, port);
	}
	
	public void sendTo(String host, int port) throws GntpException, UnknownHostException, IOException {
		Socket socket = null;
		SocketChannel channel = null;
		try {
			Log.i("Request.sendTo", "Connecting to " + host + " on port " + port + "...");
			socket = new Socket(host, port);
			
			channel = socket.getChannel();
			ChannelWriter writer = new ChannelWriter(channel, Constants.CHARSET);
			EncryptedChannelReader reader = new EncryptedChannelReader(channel);
			
			// Write the request line and headers
			Log.i("Request.sendTo", "Sending " + _requestType.toString() + " request...");
			write(writer);
			
			// Wait for the response
			Log.i("Request.sendTo", "Waiting for response...");
			Response response = Response.read(reader);
			GntpException error = response.getError();
			if (error != null) {
				Log.i("Request.sendTo", "Failed: " + error.getMessage());
				throw error;
			}
			Log.i("Request.sendTo", "Succeeded");
			
		} finally {
			if (channel != null) {
				channel.close();
				channel = null;
			}
			if (socket != null) {
				socket.close();
				socket = null;
			}
			Log.i("Request.sendTo", "Done");
		}
	}

	protected void writeLeaderLine(ChannelWriter writer) throws IOException {
		writer.write(Constants.GNTP_PROTOCOL_VERSION + " ");
		writer.write(_requestType.toString() + " ");
		writer.write(_encryptionType.toString());
		if (_initVector != null) {
			writer.write(":" + _initVector);
		}
		if (_hashAlgorithm != HashAlgorithm.NONE) {
			writer.write(" " + _hashAlgorithm.toString() + ":" + _hash + "." + _salt);
		}
		writer.write(Constants.END_OF_LINE);
	}
}
