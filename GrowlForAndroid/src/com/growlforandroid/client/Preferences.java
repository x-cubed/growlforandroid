package com.growlforandroid.client;

import android.content.*;
import android.os.Bundle;
import android.preference.*;
import android.util.Log;

public class Preferences extends PreferenceActivity {
	public static final String START_AUTOMATICALLY = "start_automatically";
	public static final String WAS_RUNNING = "was_running";
	public static final String ANNOUNCE_USING_ZEROCONF = "announce_using_zeroconf";
	public static final String DEVICE_NAME = "device_name";
	
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
}