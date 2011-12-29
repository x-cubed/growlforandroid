package com.growlforandroid.common;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import javax.jmdns.*;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

public class ZeroConf {
	private static ZeroConf _instance;
	private final ArrayList<ServiceInfo> _registeredServices = new ArrayList<ServiceInfo>();
	private final ArrayList<ListenerWrapper> _wrappers = new ArrayList<ListenerWrapper>();
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
				Log.e("ZeroConf.finalize", e.toString());
			}
			_registeredServices.clear();
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
				Log.i("ZeroConf.registerService", "Registering service \"" + service.getName()
						+ "\" with ZeroConf");
				try {
					if (_jmDNS != null) {
						_jmDNS.registerService(service);
						_registeredServices.add(service);
						Log.i("ZeroConf.registerService", "Service \"" + service.getName()
								+ "\" registered successfully");
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
			_registeredServices.clear();
		}
	}

	public ServiceInfo[] findServices(String type, int timeoutMS) {
		ServiceInfo[] services = _jmDNS.list(type, timeoutMS);
		
		ArrayList<ServiceInfo> nonLocalServices = new ArrayList<ServiceInfo>();
		for (ServiceInfo service : services) {
			if (!isLocalService(service)) {
				nonLocalServices.add(service);
			}
		}

		ServiceInfo[] results = new ServiceInfo[nonLocalServices.size()];
		results = nonLocalServices.toArray(results);
		return results;
	}

	private boolean isLocalService(ServiceInfo service) {
		InetAddress localAddress;
		try {
			localAddress = _jmDNS.getInterface();
		} catch (Exception x) {
			localAddress = null;
		}
		
		boolean isLocal = false;
		for(InetAddress serviceAddress:service.getInetAddresses()) {
			if (serviceAddress.equals(localAddress)) {
				Log.d("ZeroConf.findServices", "Ignoring service " + service.getName()
						+ " because it's local");
				isLocal = true;
				break;
			}
		}
		return isLocal;
	}


	public void addServiceListener(String type, final Listener listener) {
		Log.i("ZeroConf.addServiceListener", "Adding listener "	+ listener.getClass().getName());
		ListenerWrapper wrapper = new ListenerWrapper(type, listener);
		_wrappers.add(wrapper);
		_jmDNS.addServiceListener(type, wrapper);
	}
	
	public boolean removeServiceListener(String type, Listener listener) {
		if (_jmDNS == null) {
			return false;
		}
		
		for(ListenerWrapper wrapper:_wrappers) {
			if (wrapper.getType().equals(type) && (wrapper.getListener() == listener)) {
				_wrappers.remove(wrapper);
				_jmDNS.removeServiceListener(type, wrapper);
				Log.i("ZeroConf.removeServiceListener", "Removed listener " + listener.getClass().getName());
				return true;
			}
		}
		Log.w("ZeroConf.removeServiceListener", "Failed to remove listener " + listener.getClass().getName());
		return false;
	}

	public ServiceInfo getServiceInfo(String type, String name) {
		return _jmDNS.getServiceInfo(type, name);
	}
	
	private class ListenerWrapper implements ServiceListener {
		private final String _type;
		private final Listener _listener;
		
		public ListenerWrapper(String type, Listener listener) {
			_type = type;
			_listener = listener;
		}
		
		public String getType() {
			return _type;
		}
		
		public Listener getListener() {
			return _listener;
		}
		
		public void serviceAdded(ServiceEvent event) {
			ServiceInfo service = getServiceInfo(event.getType(), event.getName());
			if (!isLocalService(service)) {
				_listener.serviceAdded(service, event);
			}
		}

		public void serviceRemoved(ServiceEvent event) {
			ServiceInfo service = getServiceInfo(event.getType(), event.getName());
			_listener.serviceRemoved(service, event);
		}

		public void serviceResolved(ServiceEvent event) {
		}
	}
	
	public interface Listener {
		public void serviceAdded(ServiceInfo service, ServiceEvent event);
		public void serviceRemoved(ServiceInfo service, ServiceEvent event);
	}
}
