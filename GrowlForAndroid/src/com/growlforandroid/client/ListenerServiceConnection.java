package com.growlforandroid.client;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.growlforandroid.client.GrowlListenerService.StatusChangedHandler;

public class ListenerServiceConnection implements ServiceConnection {
	private final Activity _activity;
	private final Intent _growlListenerService;
	private final StatusChangedHandler _handler;
	private boolean _isBound;
	private GrowlListenerService _instance;

	public ListenerServiceConnection(Activity activity,
			StatusChangedHandler handler) {
		_activity = activity;
		_growlListenerService = new Intent(activity, GrowlListenerService.class);
		_handler = handler;
	}

	public boolean isRunning() {
		Log.i("ListenerServiceConnection.isRunning", "IsBound = " + _isBound
				+ ", Has Instance = " + (_instance != null));
		if (!_isBound) {
			bind();
			return _isBound && (_instance != null) ?
					_instance.isRunning() : false;
		} else if (_instance != null) {
			return _instance.isRunning();
		} else {
			return false;
		}
	}

	public void start() {
		if (!bind()) {
			Log.e("ListenerServiceConnection.start", "Unable to bind to service");
			return;
		}

		_isBound = true;
		_activity.startService(_growlListenerService);
		_handler.onIsRunningChanged(true);
	}

	public void stop() {
		unbind();
		if (!_activity.stopService(_growlListenerService)) {
			Log.e("ListenerServiceConnection.stop", "Unable to stop service");
			return;
		}
		_handler.onIsRunningChanged(false);
	}

	public boolean bind() {
		if (_isBound)
			return _isBound;

		Log.i("ListenerServiceConnection.bind", "Binding to the service");
		_isBound = _activity.bindService(_growlListenerService, this, 0);
		return _isBound;
	}

	public void unbind() {
		if (_isBound) {
			Log.i("ListenerServiceConnection.unbind", "Unbinding from service");
			_activity.unbindService(this);
			_isBound = false;
			onServiceDisconnected(null);
		}
	}

	public void onServiceConnected(ComponentName name, IBinder service) {
		Log.i("ListenerServiceConnection.onServiceConnected",
				"Connected to "	+ name);
		_instance = ((GrowlListenerService.LocalBinder) service).getService();
		_instance.addStatusChangedHandler(_handler);
		_handler.onIsRunningChanged(isRunning());
	}

	public void onServiceDisconnected(ComponentName name) {
		Log.i("ListenerServiceConnection.onServiceDisconnected",
				"Disconnected from " + name);
		_instance.removeStatusChangedHandler(_handler);
		_instance = null;
		_handler.onIsRunningChanged(false);
	}

	protected void finalize() {
		unbind();
	}
}
