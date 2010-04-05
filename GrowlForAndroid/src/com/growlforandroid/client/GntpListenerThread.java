package com.growlforandroid.client;

import java.io.*;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.nio.charset.*;
import java.util.*;

import com.growlforandroid.common.*;
import com.growlforandroid.gntp.*;

import android.util.Log;

public class GntpListenerThread extends Thread {
	private final Socket _socket;
	private final IGrowlRegistry _registry;
	private final SocketChannel _channel;
	private final ChannelReader _socketReader;
	private final ChannelWriter _socketWriter;
	private RequestState _currentState = RequestState.Connected;
	
	private RequestType _requestType;
	private EncryptionType _encryptionType;
	private String _initVector = "";
	private Map<String, String> _requestHeaders = new HashMap<String, String>();
	
	private int _notificationsCount = 0;
	private int _notificationIndex = 0;
	private Map<Integer, Map<String, String>> _notificationsHeaders = new HashMap<Integer, Map<String, String>>();
	
	public GntpListenerThread(IGrowlRegistry registry, SocketChannel channel)
		throws IllegalCharsetNameException, UnsupportedCharsetException, CharacterCodingException {
		
		super("GntpListenerThread");
		_registry = registry;
		
		_channel = channel;
		_socketReader = new ChannelReader(_channel, Constants.CHARSET);
		_socketWriter = new ChannelWriter(_channel, Constants.CHARSET);
		
		_socket = channel.socket();
	}

	public void run() {
		try {
			int remotePort = _socket.getPort();
			Log.i("GNTPListenerThread.run", "Connected to client on port " + remotePort);
			
			// Read lines from the socket until we've sent a response back	
			String inputLine;
			while ((_currentState != RequestState.ResponseSent) &&
					((inputLine = readLine()) != null)) {
				
				// Parse the input
				Log.i("GNTPListenerThread.run", "Read line \"" + inputLine + "\"");
				try {
					// Parse the input
					switch (_currentState) {
						case Connected:
							// Parse first row of request: GNTP/1.0 REGISTER ...
							_currentState = parseRequestLine(inputLine);
							break;
							
						case ReadingRequestHeaders:
							// Parse request header rows: Application-Name: Growl for Android
							_currentState = parseRequestHeader(inputLine);
							break;
							
						case ReadingNotificationHeaders:
							// Parse notification type header rows: Notification-Name: New Mail
							_currentState = parseNotificationHeader(inputLine);
							break;
					}
					
					// Are we ready to reply?
					if (_currentState == RequestState.EndOfRequest) {
						switch (_requestType) {
							case Register:
								doRegister();
								break;
								
							case Notify:
								doNotify();
								break;
								
							case Subscribe:
								doSubscribe();
								break;
							
							case Ignore:
								// Notification hash is invalid, silently ignore this notification
								Log.w("GntpListenerThread.run", "Ignoring notification with invalid hash");
								break;
								
							default:
								throw new GntpException(GntpError.InvalidRequest, "Unexpected message type");
						}

						// Send the response
						new Response(ResponseType.OK).write(_socketWriter);
						_currentState = RequestState.ResponseSent;
					}
					
				} catch (Exception x) {
					// Parsing error or something unexpected					
					Log.e("GntpListenerThread.run", "Unexpected error while reading from socket", x);

					// Send the error response
					GntpError error = GntpError.getErrorFrom(x);
					new Response(ResponseType.Error, error).write(_socketWriter);
					_currentState = RequestState.ResponseSent;
				}
			}
			
			Log.i("GNTPListenerThread.run", "No more input data, closing client connection from port " + remotePort);
			_channel.close();
			_socket.close();

		} catch (Exception x) {
			Log.e("GNTPListenerThread.run", "Unexpected exception while reading from socket", x);
		}
	}

	private String readLine() throws IOException {
		String line = _socketReader.readCharsUntil(Constants.END_OF_LINE);
		int withoutDelimiter = line.length() - Constants.END_OF_LINE.length();
		if ((line != null) && (withoutDelimiter >= 0))
			line = line.substring(0, withoutDelimiter);
		return line;
	}
	
	private RequestState parseRequestHeader(String inputLine) throws GntpException {
		if (inputLine.equals("")) {
			if (_requestType == RequestType.Register) {
				// Prepare to read the headers for each notification type
				_notificationsCount = Integer.valueOf(_requestHeaders.get(Constants.HEADER_NOTIFICATIONS_COUNT));
				return RequestState.ReadingNotificationHeaders;
			} else {
				// This request has no notification headers
				return RequestState.EndOfRequest;
			}
		}

		// Parse the header into the _requestHeaders map
		parseHeader(inputLine, _requestHeaders);
		return RequestState.ReadingRequestHeaders; 
	}

	private RequestState parseNotificationHeader(String inputLine) throws GntpException {
		if (inputLine.equals("")) {
			if (_notificationIndex < (_notificationsCount - 1)) {
				// There are more notifications to go
				_notificationIndex ++;
				Log.i("GntpListenerThread.parseNotificationHeader", "Preparing to read notification type " + _notificationIndex);
			} else {
				return RequestState.EndOfRequest;
			}
		} else {
			Map<String, String> notificationHeaders = _notificationsHeaders.get(_notificationIndex);
			if (notificationHeaders == null) {
				notificationHeaders = new HashMap<String, String>();
				_notificationsHeaders.put(_notificationIndex, notificationHeaders);
			}
			parseHeader(inputLine, notificationHeaders);
		}
		return RequestState.ReadingNotificationHeaders;
	}
	
	private void doSubscribe() throws GntpException, MalformedURLException {
		throw new GntpException(GntpError.InternalServerError);
	}

	// Perform a notification
	private void doNotify() throws GntpException, MalformedURLException {
		String name = _requestHeaders.get(Constants.HEADER_APPLICATION_NAME);
		GrowlApplication application = _registry.getApplication(name);
		if (application == null)
			throw new GntpException(GntpError.UnknownApplication);
		
		String typeName = _requestHeaders.get(Constants.HEADER_NOTIFICATION_NAME);
		NotificationType type = application.getNotificationType(typeName);
		if (type == null)
			throw new GntpException(GntpError.UnknownNotification);
		
		String ID = _requestHeaders.get(Constants.HEADER_NOTIFICATION_ID);
		String icon = _requestHeaders.get(Constants.HEADER_NOTIFICATION_ICON);
		URL iconUrl = (icon != null) ? new URL(icon) : null;
		
		String title = _requestHeaders.get(Constants.HEADER_NOTIFICATION_TITLE);
		String text = _requestHeaders.get(Constants.HEADER_NOTIFICATION_TEXT);
		
		_registry.displayNotification(type, ID, title, text, iconUrl);
	}

	// Register an application and its notification types
	private void doRegister() throws GntpException, MalformedURLException {
		String name = _requestHeaders.get(Constants.HEADER_APPLICATION_NAME);
		String icon = _requestHeaders.get(Constants.HEADER_APPLICATION_ICON);
		URL iconUrl = (icon != null) ? new URL(icon) : null;
		GrowlApplication application = _registry.registerApplication(name, iconUrl);
		
		for(int i=0; i<_notificationsCount; i++) {
			// Register notification types
			Map<String, String> notificationHeaders = _notificationsHeaders.get(i);
			String typeName = notificationHeaders.get(Constants.HEADER_NOTIFICATION_NAME);
			String displayName = notificationHeaders.get(Constants.HEADER_NOTIFICATION_DISPLAY_NAME);
			boolean enabled = Boolean.valueOf(notificationHeaders.get(Constants.HEADER_NOTIFICATION_ENABLED));
			String typeIcon = notificationHeaders.get(Constants.HEADER_NOTIFICATION_ICON);
			URL typeIconUrl = (typeIcon != null) ? new URL(typeIcon) : null;
			
			application.registerNotificationType(typeName, displayName, enabled, typeIconUrl);
		}
	}

	private RequestState parseRequestLine(String inputLine) throws GntpException {
		// GNTP/<version> <messagetype> <encryptionAlgorithmID>[:<ivValue>][ <keyHashAlgorithmID>:<keyHash>.<salt>]
		String[] component = inputLine.split(Constants.FIELD_DELIMITER);
		if ((component.length < 3) || (component.length > 4))
			throw new GntpException(GntpError.InvalidRequest, "Expected 3 or 4 fields, found " + component.length + " fields");
		
		// Verify protocol and version are supported
		String[] protocolAndVersion = component[0].split("/");
		if (protocolAndVersion.length != 2)
			throw new GntpException(GntpError.InvalidRequest, "Expected GNTP/1.0 protocol header");
		if (!protocolAndVersion[0].equals(Constants.SUPPORTED_PROTOCOL))
			throw new GntpException(GntpError.UnknownProtocol);
		if (!protocolAndVersion[1].equals(Constants.SUPPORTED_PROTOCOL_VERSION))
			throw new GntpException(GntpError.UnknownProtocolVersion);

		// Message type
		_requestType = RequestType.fromString(component[1]);
		if (_requestType == null)
			throw new GntpException(GntpError.InvalidRequest, "Unknown message type: " + component[1]);
		
		// Encryption settings
		String[] encryptionTypeAndIV = component[2].split(":", 2);
		_encryptionType = EncryptionType.fromString(encryptionTypeAndIV[0]);
		_initVector = (encryptionTypeAndIV.length == 2) ? encryptionTypeAndIV[1] : "";
		if (_encryptionType == null)
			throw new GntpException(GntpError.InvalidRequest, "Unsupported encryption type: " + _encryptionType);
		
		// FIXME: Support other encryption types
		if (_encryptionType != EncryptionType.None)
			throw new GntpException(GntpError.InvalidRequest, "Unsupported encryption type: " + _encryptionType);
		
		// Authentication hash
		if (component.length == 4) {
			String[] algoAndHash = component[3].split(":");
			if (algoAndHash.length != 2)
				throw new GntpException(GntpError.NotAuthorized, "Unable to parse hash");
			
			String algorithmName = algoAndHash[0];
			HashAlgorithm algorithm = HashAlgorithm.fromString(algorithmName);
			if (algorithm == null)
				throw new GntpException(GntpError.InvalidRequest, "Unsupported hash type: " + algoAndHash[0]);
			
			String hashDotSalt = algoAndHash[1];
			int dot = hashDotSalt.indexOf('.');
			if ((dot < 1) || (dot == hashDotSalt.length() - 1))
				throw new GntpException(GntpError.NotAuthorized, "Unable to parse hash");
			String hash = hashDotSalt.substring(0, dot);
			String salt = hashDotSalt.substring(dot + 1);
			
			// Validate the hash
			if (!_registry.isValidHash(algorithm, hash, salt)) {
				_requestType = RequestType.Ignore;
			}
		}
		
		return RequestState.ReadingRequestHeaders;
	}

	private void parseHeader(String inputLine, Map<String, String> headers) throws GntpException {	
		String[] keyAndValue = inputLine.split(":", 2);
		if (keyAndValue.length != 2)
			throw new GntpException(GntpError.InvalidRequest, "Unable to parse header: " + inputLine);
		
		String key = keyAndValue[0];
		String value = keyAndValue[1].trim();
		headers.put(key, value);
	}
	
	private enum RequestState {
		Connected,
		ReadingRequestHeaders,
		ReadingNotificationHeaders,
		EndOfRequest,
		ResponseSent
	}
}