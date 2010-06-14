package com.growlforandroid.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class DeviceEventReceiver
	extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		try {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
				onBootCompleted(context, intent, prefs);
				
			} else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
				onConnectivityChanged(context, intent, prefs);
				
			} else {
				Log.i("DeviceEventReceiver.onReceive", "Unsupported action: " + action);
			}
		} catch (Exception x) {
			Log.e("DeviceEventReceiver.onReceive", "Exception while processing action " + action + ": " + x.toString());
		}
	}

	private void onBootCompleted(Context context, Intent intent, SharedPreferences prefs) throws Exception {
		boolean startOnBoot = prefs.getBoolean(Preferences.START_AT_BOOT, false);
		if (startOnBoot) {
			Log.i("DeviceEventReceiver.onBootCompleted", "Starting listener service...");
			startService(context);
		}
	}
	
	private void onConnectivityChanged(Context context, Intent intent, SharedPreferences prefs) throws Exception {
		boolean disconnected = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
		if (disconnected) {
			Log.i("DeviceEventReceiver.onConnectivityChanged", "Disconnected");
			stopService(context);
		} else {
			Log.i("DeviceEventReceiver.onConnectivityChanged", "Connected");
			startServiceIfWasRunning(context, prefs);
		}
	}
	
	private void startServiceIfWasRunning(Context context, SharedPreferences prefs) throws Exception {
		boolean wasRunning = prefs.getBoolean(Preferences.WAS_RUNNING, false);
		if (wasRunning) {
			Log.i("DeviceEventReceiver.startServiceIfWasRunning", "Restarting listener service...");
			startService(context);
		}
	}
	
	private void startService(Context context) throws Exception {
		Intent growlListenerService = new Intent(context, GrowlListenerService.class);
		context.startService(growlListenerService);
	}
	
	private boolean stopService(Context context) {
		Intent growlListenerService = new Intent(context, GrowlListenerService.class);
		return context.stopService(growlListenerService);
	}
}
