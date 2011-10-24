package com.growlforandroid.common;

import javax.jmdns.ServiceInfo;

public class Subscription {
	private final int _id;
	private final String _name;
	private final String _status;
	private final String _address;
	private final String _password;
	private final boolean _subscribed;
	
	public Subscription(int id, String name, String status, String address, String password, boolean subscribed) {
		_id = id;
		_name = name;
		_status = status;
		_address = address;
		_password = password;
		_subscribed = subscribed;
	}
	
	public Subscription(ServiceInfo service, String status, boolean subscribed) {
		this(0, service.getName(), status, service.getServer(), "", subscribed);
	}

	public int getId() {
		return _id;
	}
	
	public String getName() {
		return _name;
	}
	
	public String getStatus() {
		return _status;
	}
	
	public String getAddress() {
		return _address;
	}
	
	public String getPassword() {
		return _password;
	}
	
	public boolean isSubscribed() {
		return _subscribed;
	}
}
