package com.growlforandroid.gntp;

import com.growlforandroid.common.Subscriber;

public class SubscriberThread extends Thread {
	private final Subscriber _subscriber;
	private final long _id;
	private final String _address;
	private final String _password;
	
	public SubscriberThread(Subscriber subscriber, long id, String address, String password) {
		_id = id;
		_subscriber = subscriber;
		_address = address;
		_password = password;
	}
	
	public void run() {
		Exception error = null;
		try {
			Request request = new Request(RequestType.Subscribe, EncryptionType.None, HashAlgorithm.MD5, _password);
			request.addHeader(Constants.HEADER_SUBSCRIPTION_ID, _subscriber.getId().toString());
			request.addHeader(Constants.HEADER_SUBSCRIPTION_NAME, _subscriber.getName());
			request.addCommonHeaders(_subscriber.getContext());
			request.sendTo(_id, _address);
			
		} catch (Exception x) {
			error = x;
		}
		_subscriber.onSubscriptionComplete(this, error);
	}

	public long getSubscriptionId() {
		return _id;
	}
}
