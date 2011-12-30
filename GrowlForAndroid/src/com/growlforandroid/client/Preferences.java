package com.growlforandroid.client;

import java.util.UUID;

import android.content.*;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.preference.*;
import android.util.Log;

public class Preferences extends PreferenceActivity {
	public static final String START_AUTOMATICALLY = "start_automatically";
	public static final String WAS_RUNNING = "was_running";
	public static final String ANNOUNCE_USING_ZEROCONF = "announce_using_zeroconf";
	public static final String DEVICE_NAME = "device_name";
	public static final String SUBSCRIBER_ID = "subscriber_id";
	
	private ListenerServiceConnection _service;
	private boolean _oldAnnounceUsingZeroConf;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setTitle(R.string.preferences_title);
        
        _service = new ListenerServiceConnection(this);
        
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	_service.bind();
    	
    	Context context = getBaseContext();
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	_oldAnnounceUsingZeroConf = prefs.getBoolean(ANNOUNCE_USING_ZEROCONF, false);
    }
    
    private boolean needsRestart() {
    	Context context = getBaseContext();
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	if (_oldAnnounceUsingZeroConf != prefs.getBoolean(ANNOUNCE_USING_ZEROCONF, false)) {
    		return true;
    	}
    	return false;
    }
    
    @Override
    protected void onPause() {
    	super.onPause();

    	if (needsRestart()) {
    		Log.d("Preferences.onPause", "Restarting the GrowlListenerService...");
	    	_service.restart();
	    	_service.unbind();
		}
    }
    
    @Override
    public void onDestroy() {
    	if (_service != null) {
    		_service.unbind();
    		_service = null;
    	}
    	
    	super.onDestroy();
    }
    
    protected void finalize() throws Throwable {
    	onDestroy();
    	super.finalize();
    }
    
    public static String getDeviceName(SharedPreferences prefs) {
        String deviceName = prefs.getString(DEVICE_NAME, "");
    	if (deviceName == "") {
    		// Use the model name by default
    		deviceName = Build.MODEL;
        	Editor editor = prefs.edit();
        	editor.putString(DEVICE_NAME, deviceName);
        	editor.commit();
    	}
    	return deviceName;
    }
    
    public static UUID getSubscriberId(SharedPreferences prefs) {
        String uuid = prefs.getString(SUBSCRIBER_ID, null);
 		UUID id = null;
 		if (uuid != null) {
 			// We have a subscriber ID from last time
 			try {
 				id = UUID.fromString(uuid);
 				Log.i("Preferences.getSubscriberId", "Using existing subscriber ID: " + id.toString());
 			} catch (IllegalArgumentException iae) {
 			}
 		}
 		if (id == null) {
 			// Generate a new subscriber ID and save it in the preferences for next time
 			id = UUID.randomUUID();
 			Editor editor = prefs.edit();
 			editor.putString(SUBSCRIBER_ID, id.toString());
 			editor.commit();
 			Log.i("Preferences.getSubscriberId", "Created new subscriber ID: " + id.toString());
 		}
 		return id;
    }
}