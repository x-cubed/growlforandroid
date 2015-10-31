package com.growlforandroid.common;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

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
			WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			_mcLock = wifiMgr.createMulticastLock("GrowlSubscriptions");
			_mcLock.setReferenceCounted(true);
			_mcLock.acquire();
		} catch (Exception x) {
			Log.e("ZeroConf.initialise", "Failed to get a multicast lock: " + x.toString());
		}

		try {
			InetAddress announceAddr = Utility.getLocalIpAddress();
			if (announceAddr != null) {
				_jmDNS = JmDNS.create(announceAddr);
			}
		} catch (Exception x) {
			Log.e("ZeroConf.initialise", x.toString());
		}
	}

	public void close() {
		_instance = null;
		try {
			finalize();
		} catch (Throwable t) {
		}
	}

	protected void finalize() throws Throwable {
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
					_mcLock.release();
				} catch (Exception x) {
					Log.e("ZeroConf", "Failed to release the multicast lock: " + x.toString());
				}
			}
			_mcLock = null;
		}
		super.finalize();
	}

	public static synchronized ZeroConf getInstance(Context context) {
		if (_instance == null) {
			_instance = new ZeroConf(context);
		}
		return _instance;
	}

	public void registerService(String type, String name, int port, String text) {
		final ServiceInfo service = ServiceInfo.create(type, name, port, text);
		new Thread(new Runnable() {
			public void run() {
				try {
					if (_jmDNS != null) {
						_jmDNS.registerService(service);
						_registeredServices.add(service);
						Log.i("ZeroConf.registerServic", "Service \"" + service.getName()
								+ "\" registered successfully");
					}
				} catch (Exception x) {
					Log.e("ZeroConf.registerServic", x.toString());
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
		if (_jmDNS == null) {
			// No multicast DNS available, no services can be found
			return new ServiceInfo[0];
		}

		ServiceInfo[] services = _jmDNS.list(type, timeoutMS);

		ArrayList<ServiceInfo> nonLocalServices = new ArrayList<ServiceInfo>();
		for (ServiceInfo service : services) {
			if ((service != null) && !isLocalService(service)) {
				nonLocalServices.add(service);
			}
		}

		ServiceInfo[] results = new ServiceInfo[nonLocalServices.size()];
		results = nonLocalServices.toArray(results);
		return results;
	}

	public static List<InetAddress> getLocalAddresses() {
		ArrayList<InetAddress> allAddresses = new ArrayList<InetAddress>();
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface iface = interfaces.nextElement();
				Enumeration<InetAddress> addresses = iface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress address = addresses.nextElement();
					allAddresses.add(address);
				}
			}
		} catch (Exception x) {
			Log.w("ZeroConf.getLocalAddres", x.toString());
		}
		return allAddresses;
	}

	private boolean isLocalService(ServiceInfo service) {
		boolean isLocal = false;
		List<InetAddress> localAddresses = getLocalAddresses();
		for (InetAddress serviceAddress : service.getInetAddresses()) {
			for (InetAddress localAddress : localAddresses) {
				if (serviceAddress.equals(localAddress)) {
					Log.d("ZeroConf.findServices", "Ignoring service " + service.getName() + " because it's local");
					isLocal = true;
					break;
				}
			}
			if (isLocal) {
				break;
			}
		}
		return isLocal;
	}

	public void addServiceListener(String type, final Listener listener) {
		ListenerWrapper wrapper = new ListenerWrapper(type, listener);
		_wrappers.add(wrapper);
		_jmDNS.addServiceListener(type, wrapper);
	}

	public boolean removeServiceListener(String type, Listener listener) {
		if (_jmDNS == null) {
			return false;
		}

		for (ListenerWrapper wrapper : _wrappers) {
			if (wrapper.getType().equals(type) && (wrapper.getListener() == listener)) {
				_wrappers.remove(wrapper);
				_jmDNS.removeServiceListener(type, wrapper);
				return true;
			}
		}
		Log.w("ZeroConf.removeServiceL", "Failed to remove listener " + listener.getClass().getName());
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
			if ((service != null) && !isLocalService(service)) {
				_listener.serviceAdded(service, event);
			}
		}

		public void serviceRemoved(ServiceEvent event) {
			ServiceInfo service = getServiceInfo(event.getType(), event.getName());
			if (service != null) {
				_listener.serviceRemoved(service, event);
			}
		}

		public void serviceResolved(ServiceEvent event) {
		}
	}

	public interface Listener {
		public void serviceAdded(ServiceInfo service, ServiceEvent event);
		public void serviceRemoved(ServiceInfo service, ServiceEvent event);
	}
}
