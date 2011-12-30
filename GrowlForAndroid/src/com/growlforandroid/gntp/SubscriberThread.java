package com.growlforandroid.gntp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.util.Log;

import com.growlforandroid.common.Subscriber;

public class SubscriberThread extends Thread {
	private final Subscriber _subscriber;
	private final long _id;
	private final String _hostName;
	private final InetAddress[] _addresses;
	private final int _port;
	private final String _password;
	
	public SubscriberThread(Subscriber subscriber, long id, InetAddress[] addresses, int port, String password) {
		this(subscriber, id, null, addresses, port, password);
	}
	
	private SubscriberThread(Subscriber subscriber, long id, String hostName, InetAddress[] addresses, int port, String password) {
		_id = id;
		_subscriber = subscriber;
		_hostName = hostName;
		_addresses = addresses;
		_port = port;
		_password = password;
	}

	public void run() {
		Exception error = null;
		try {
			Request request = new Request(RequestType.Subscribe, EncryptionType.None, HashAlgorithm.MD5, _password);
			request.addHeader(Constants.HEADER_SUBSCRIPTION_ID, _subscriber.getId().toString());
			request.addHeader(Constants.HEADER_SUBSCRIPTION_NAME, _subscriber.getName());
			request.addCommonHeaders(_subscriber.getContext());
			send(request);
			
		} catch (Exception x) {
			error = x;
		}
		_subscriber.onSubscriptionComplete(this, error);
	}

	public long getSubscriptionId() {
		return _id;
	}
	
	private void send(Request request) throws GntpException, UnknownHostException {
		InetAddress[] addresses = _addresses;
		if (addresses == null) {
			addresses = InetAddress.getAllByName(_hostName);
		}
		send(request, addresses);
	}
	
	private void send(Request request, InetAddress[] addresses) throws GntpException, UnknownHostException {
		for(InetAddress ipAddress:addresses) {
			try {
				String address = ipAddress.getHostAddress();
				Log.d("SubscriberThread[" + _id + "].send", "Sending subscribe request to " + address + ":" + _port + "...");
				request.sendTo(_id, address, _port);
				Log.d("SubscriberThread[" + _id + "].send", "Success");
				return;
			} catch (UnknownHostException e) {
			} catch (IOException e) {
			}
		}
		Log.d("SubscriberThread[" + _id + "].send", "Failed to send to any of the " + addresses.length + " addresses");
		throw new UnknownHostException("Unable to connect to any of the host addresses");
	}
	
	public static SubscriberThread create(Subscriber subscriber, long id, String address, String password) {
		String host = address;
		int port = Constants.GNTP_PORT;
		
		int colon = address.indexOf(':');
		if (colon >= 0) {
			port = Integer.parseInt(address.substring(colon));
			host = address.substring(0, colon);
		}
		return new SubscriberThread(subscriber, id, host, null, port, password);
	}
}
