package com.growlforandroid.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class DeviceEventReceiver
	extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		try {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
				onConnectivityChanged(context, intent, prefs);
				
			} else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
				onWifiNetworkStateChanged(context, intent, prefs);
				
			} else {
				Log.i("DeviceEventReceiver.onR", "Unsupported action: " + action);
			}
		} catch (Exception x) {
			Log.e("DeviceEventReceiver.onR", "Exception while processing action " + action + ": " + x.toString());
		}
	}
	
	private void onConnectivityChanged(Context context, Intent intent, SharedPreferences prefs) throws Exception {
		String message = "Disconnected";
		boolean isConnected = false;
		NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
		if (info != null) {
			message = info.getTypeName() + " [" + info.getSubtypeName() + "]";		
			isConnected = info.isConnected();
			if (isConnected)
				message += " (connected)";
		}
		
		Log.i("DeviceEventReceiver.onC", message);
		
		if (!isConnected) {
			stopServiceIfAutoStartOn(context, prefs);
		} else {
			startServiceIfAutoStartOn(context, prefs);
		}
	}
	
	private void onWifiNetworkStateChanged(Context context, Intent intent, SharedPreferences prefs) throws Exception {
		NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
		String bssid = intent.getStringExtra(WifiManager.EXTRA_BSSID);
		if (!info.isConnected()) {
			Log.i("DeviceEventReceiver.onW", "Disconnected");
			stopService(context);
		} else {
			Log.i("DeviceEventReceiver.onW", "Connected to " + bssid);
			startServiceIfWasRunning(context, prefs);
		}
	}
	
	private void startServiceIfWasRunning(Context context, SharedPreferences prefs) throws Exception {
		boolean wasRunning = prefs.getBoolean(Preferences.WAS_RUNNING, false);
		if (wasRunning) {
			Log.i("DeviceEventReceiver.sta", "Restarting listener service...");
			startService(context);
		} else {
			Log.i("DeviceEventReceiver.sta", "Not starting listener service");
		}
	}
	
	private void startServiceIfAutoStartOn(Context context, SharedPreferences prefs) throws Exception {
		boolean startOnBoot = prefs.getBoolean(Preferences.START_AUTOMATICALLY, false);
		if (startOnBoot) {
			Log.i("DeviceEventReceiver.sta", "Starting listener service...");
			startService(context);
		}
	}
	
	private void startService(Context context) throws Exception {
		Intent growlListenerService = new Intent(context, GrowlListenerService.class);
		context.startService(growlListenerService);
	}
	
	private void stopServiceIfAutoStartOn(Context context, SharedPreferences prefs) throws Exception {
		boolean startOnBoot = prefs.getBoolean(Preferences.START_AUTOMATICALLY, false);
		if (startOnBoot) {
			Log.i("DeviceEventReceiver.sto", "Stopping listener service...");
			stopService(context);
		}
	}
	
	private boolean stopService(Context context) {
		Intent growlListenerService = new Intent(context, GrowlListenerService.class);
		return context.stopService(growlListenerService);
	}
}
