package com.growlforandroid.common;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.jmdns.ServiceInfo;

public class Subscription {
	private final int _id;
	private final String _name;
	private final String _status;
	private final String _address;
	private final InetAddress[] _ipAddresses;
	private final String _password;
	private final boolean _subscribed;
	
	public Subscription(int id, String name, String status, String address, String password, boolean subscribed) {
		this(0, name, status, address, lookupInetAddress(address), password, subscribed);
	}
	
	public Subscription(int id, String name, String status, String address, InetAddress[] ipAddresses, String password, boolean subscribed) {
		_id = id;
		_name = name;
		_status = status;
		_address = address;
		_ipAddresses = ipAddresses;
		_password = password;
		_subscribed = subscribed;
	}
	
	public Subscription(ServiceInfo service, String status, boolean subscribed) {
		this(0, service.getName(), status, service.getServer(), service.getInetAddresses(), "", subscribed);
	}

	private static InetAddress[] lookupInetAddress(String address) {
		try {
			return InetAddress.getAllByName(address);
		} catch (UnknownHostException e) {
			return new InetAddress[0];
		}
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
	
	public InetAddress[] getInetAddresses() {
		return _ipAddresses;
	}
	
	public String getPassword() {
		return _password;
	}
	
	public boolean isSubscribed() {
		return _subscribed;
	}
	
	/***
	 * True, if the subscription can be resolved to an IP address
	 * @return
	 */
	public boolean isValid() {
		boolean hasIpAddress = (_ipAddresses != null) && (_ipAddresses.length > 0);
		return hasIpAddress;
	}

	public boolean matchesAny(InetAddress[] addresses) {
		for(InetAddress ipAddress:_ipAddresses) {
			for(InetAddress address:addresses) {
				if (address.equals(ipAddress)) {
					return true;
				}
			}
		}
		return false;
	}
}
