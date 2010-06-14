package com.growlforandroid.client;

import android.content.*;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.growlforandroid.client.GrowlListenerService.StatusChangedHandler;

public class ListenerServiceConnection implements ServiceConnection {
	private final Context _context;
	private final Intent _growlListenerService;
	private final StatusChangedHandler _handler;
	private boolean _isBound;
	private GrowlListenerService _instance;

	public ListenerServiceConnection(Context context,
			StatusChangedHandler handler) {
		
		_context = context;
		_growlListenerService = new Intent(context, GrowlListenerService.class);
		_handler = handler;
	}

	public ListenerServiceConnection(Context context) {
		this(context, null);
	}
	
	public boolean isRunning() {
		bind();
		if (_instance != null) {
			return _instance.isRunning();
		} else {
			return false;
		}
	}

	public void start() throws Exception {
		if (!bind()) {
			Log.e("ListenerServiceConnection.start", "Unable to bind to service");
			return;
		}

		_isBound = true;
		ComponentName name = _context.startService(_growlListenerService);
		if (name == null)
			throw new Exception("Unable to start service");
		
		// Service started successfully
		onIsRunningChanged(true);
	}

	public boolean stop() {
		return stop(false);
	}
	
	public boolean stop(boolean automated) {
		boolean wasStopped = false;
		if (!_context.stopService(_growlListenerService)) {
			// Service wasn't running
			Log.w("ListenerServiceConnection.stop", "Service wasn't running");
		} else {
			// Service stopped successfully
			wasStopped = true;
			if (!automated)
				setWasRunning(false);
			onIsRunningChanged(false);	
		}
		return wasStopped;
	}
	
	private void onIsRunningChanged(boolean isRunning) {
		if (_handler != null)
			_handler.onIsRunningChanged(isRunning);
	}
	
	private void setWasRunning(boolean wasRunning) {
		Log.i("ListenerServiceConnection.setWasRunning", "Was Running = " + wasRunning);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_context);
		prefs.edit().putBoolean(Preferences.WAS_RUNNING, wasRunning).commit();
	}
	
	public boolean bind() {
		if (_isBound)
			return _isBound;

		Log.i("ListenerServiceConnection.bind", "Binding to the service");
		_isBound = _context.bindService(_growlListenerService, this, 0);
		return _isBound;
	}

	public void unbind() {
		if (_isBound) {
			Log.i("ListenerServiceConnection.unbind", "Unbinding from service");
			_context.unbindService(this);
			_isBound = false;
			onServiceDisconnected(null);
		}
	}

	public void onServiceConnected(ComponentName name, IBinder service) {
		Log.i("ListenerServiceConnection.onServiceConnected",
				"Connected to "	+ name);
		_instance = ((GrowlListenerService.LocalBinder) service).getService();
		if (_handler != null)
			_instance.addStatusChangedHandler(_handler);
		onIsRunningChanged(isRunning());
	}

	public void onServiceDisconnected(ComponentName name) {
		if (_instance != null) {
			Log.i("ListenerServiceConnection.onServiceDisconnected",
					"Disconnected from " + name);
			if (_handler != null)
				_instance.removeStatusChangedHandler(_handler);
			_instance = null;
			onIsRunningChanged(false);
		}
	}

	protected void finalize() {
		unbind();
	}
}
