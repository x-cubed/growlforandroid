package com.growlforandroid.gntp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import android.os.Build;

import com.growlforandroid.common.Utility;

public class Request {
	private RequestState _currentState = RequestState.Connected;
	
	private RequestType _requestType;
	private EncryptionType _encryptionType;
	private HashAlgorithm _hashAlgorithm;
	private String _initVector = "";
	private String _hash = "";
	private String _salt = "";
	private Map<String, String> _requestHeaders = new HashMap<String, String>();
	
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

	public void sendTo(String address) throws GntpException {
		// TODO Auto-generated method stub
		
	}

	public void addHeader(String name, String value) {
		_requestHeaders.put(name, value);
	}
	
	public void addCommonHeaders() {
		addHeader(Constants.HEADER_ORIGIN_MACHINE_NAME, Build.DEVICE);
		addHeader(Constants.HEADER_ORIGIN_SOFTWARE_NAME, "Growl for Android");
		addHeader(Constants.HEADER_ORIGIN_SOFTWARE_VERSION, "0.1");
		addHeader(Constants.HEADER_ORIGIN_PLATFORM_NAME, Build.DISPLAY);
		addHeader(Constants.HEADER_ORIGIN_PLATFORM_VERSION, Build.VERSION.RELEASE);
	}
}
