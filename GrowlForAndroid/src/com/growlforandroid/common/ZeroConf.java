package com.growlforandroid.common;

import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

public class ZeroConf {
	private static final int TIMEOUT = 2000;
	
	private static ZeroConf _instance;
	private JmDNS _jmDNS;
	private WifiManager.MulticastLock _mcLock;
	
	private ZeroConf(Context context) {
		initialise(context);
	}
	
	private void initialise(Context context) {
		try {
			Log.i("ZeroConf.initialise", "Getting a multicast lock...");
    		WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        	_mcLock = wifiMgr.createMulticastLock("GrowlSubscriptions");
        	_mcLock.setReferenceCounted(true);
        	_mcLock.acquire();
		} catch (Exception x) {
			Log.i("ZeroConf.initialise", "Failed to get a multicast lock: " + x.toString());	
		}
		
		try {
	    	InetAddress announceAddr = Utility.getLocalIpAddress();
	    	if (announceAddr != null) {
	    		Log.i("ZeroConf", "Initialising ZeroConf on " + announceAddr);
	    		_jmDNS = JmDNS.create(announceAddr);
	    	}
    	} catch (Exception x) {
    		Log.e("ZeroConf.initialise", x.toString());
    	}
	}

	public void close() {
		_instance = null;
		finalize();
	}
	
	protected void finalize() {
		Log.i("ZeroConf.finalize", "Finalizing");
		if (_jmDNS != null) {
			try {
				_jmDNS.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			_jmDNS = null;
		}
		
		if (_mcLock != null) {
			if (_mcLock.isHeld()) {
				try {
					Log.i("ZeroConf", "Releasing multicast lock...");
					_mcLock.release();
				} catch (Exception x) {
					Log.i("ZeroConf", "Failed to release the multicast lock: " + x.toString());	
				}
			}
			_mcLock = null;
		}
	}
	
	public static synchronized ZeroConf getInstance(Context context) {
		if (_instance == null)
			_instance = new ZeroConf(context);
		return _instance;
	}
	
	public void registerService(String type, String name, int port, String text) {
    	final ServiceInfo service = ServiceInfo.create(type, name, port, text);
    	new Thread(new Runnable() {
			public void run() {
				Log.i("ZeroConf.registerService", "Registering service \"" + service.getName() + "\" with ZeroConf");
				try {
					if (_jmDNS != null) {
						_jmDNS.registerService(service);
						Log.i("ZeroConf.registerService", "Service \"" + service.getName() + "\" registered successfully");
					}
				} catch (Exception x) {
					Log.e("ZeroConf.registerService", x.toString());
				}
			}            
		}).start();
	}
	
	public void unregisterAllServices() {
		if (_jmDNS != null) {
			_jmDNS.unregisterAllServices();
		}
	}

	public void addServiceListener(String type, ServiceListener serviceListener) {
		Log.i("ZeroConf.addServiceListener", "Adding listener " + serviceListener.getClass().getName());
		_jmDNS.addServiceListener(type, serviceListener);
	}

	public ServiceInfo[] findServices(String type) {
		return _jmDNS.list(type, TIMEOUT);
	}

	public void removeServiceListener(String type, ServiceListener serviceListener) {
		if (_jmDNS == null) {
			Log.i("ZeroConf.removeServiceListener", "Unable to remove listener " + serviceListener.getClass().getName());
		} else {
			Log.i("ZeroConf.removeServiceListener", "Removing listener " + serviceListener.getClass().getName());
			_jmDNS.removeServiceListener(type, serviceListener);
		}
	}

	public ServiceInfo getServiceInfo(String type, String name) {
		return _jmDNS.getServiceInfo(type, name);
	}
}
