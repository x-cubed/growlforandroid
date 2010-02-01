package com.growlforandroid.gntp;

import java.util.HashMap;
import java.util.Map;

public class Request {
	private RequestState _currentState = RequestState.Connected;
	
	private RequestType _requestType;
	private EncryptionType _encryptionType;
	private String _initVector = "";
	private Map<String, String> _requestHeaders = new HashMap<String, String>();
	
	private int _notificationsCount = 0;
	private int _notificationIndex = 0;
	private Map<Integer, Map<String, String>> _notificationsHeaders = new HashMap<Integer, Map<String, String>>();
	
	
	
	private enum RequestState {
		Connected,
		ReadingRequestHeaders,
		ReadingNotificationHeaders,
		EndOfRequest,
		ResponseSent
	}
}
