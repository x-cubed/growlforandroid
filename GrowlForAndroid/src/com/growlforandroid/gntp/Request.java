package com.growlforandroid.gntp;

import java.io.*;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.util.*;

import android.util.Log;

import com.growlforandroid.common.*;

public class Request extends GntpMessage {

	private RequestType _requestType;
	private EncryptionType _encryptionType;
	private HashAlgorithm _hashAlgorithm;
	private String _initVector = null;
	private String _hash;
	private String _salt;

	public Request(RequestType type, EncryptionType encryption, HashAlgorithm algorithm, String password)
			throws GntpException {
		_requestType = type;
		_encryptionType = encryption;
		_hashAlgorithm = algorithm;

		if (_encryptionType != EncryptionType.None)
			throw new GntpException(GntpError.NotAuthorized, "Encryption is not yet supported");

		// Generate the hash and salt
		_salt = UUID.randomUUID().toString().replace("-", "").toUpperCase();
		byte[] salt = Utility.hexStringToByteArray(_salt);
		byte[] hash = _hashAlgorithm.calculateHash(password, salt);
		_hash = Utility.getHexStringFromByteArray(hash);
	}

	public void sendTo(long connectionID, String host, int port) throws GntpException, UnknownHostException,
			IOException {
		SocketChannel channel = null;
		try {
			channel = SocketChannel.open();
			Socket socket = channel.socket();
			socket.setSoTimeout(READ_TIMEOUT_MS); // Maximum time to block while
													// waiting for data
			socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT_MS);

			ChannelWriter writer = new ChannelWriter(channel, Constants.CHARSET);
			EncryptedChannelReader reader = new EncryptedChannelReader(channel);

			// Write the request line and headers
			try {
				write(writer);
			} catch (SocketException se) {
				// We capture this and ignore it, as the socket may have
				// prematurely closed,
				// but we may still have received response (eg: bad password)
				se.printStackTrace();
			}

			// Wait for the response
			Response response = Response.read(reader);
			GntpException error = response.getError();
			if (error != null) {
				Log.e("Request.sendTo[" + connectionID + "]", "Failed: " + error.getMessage());
				throw error;
			}

		} finally {
			if (channel != null) {
				channel.close();
				channel = null;
			}
		}
	}

	protected String getLeaderLine() throws IOException {
		String leaderLine = Constants.GNTP_PROTOCOL_VERSION + " " + _requestType.toString() + " "
				+ _encryptionType.toString();
		if (_initVector != null) {
			leaderLine += ":" + _initVector;
		}
		if (_hashAlgorithm != HashAlgorithm.NONE) {
			leaderLine += " " + _hashAlgorithm.toString() + ":" + _hash + "." + _salt;
		}
		return leaderLine;
	}
}
