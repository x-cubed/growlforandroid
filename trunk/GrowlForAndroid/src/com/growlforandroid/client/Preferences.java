package com.growlforandroid.client;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

public class Preferences extends PreferenceActivity {
	public static final String START_AUTOMATICALLY = "start_automatically";
	public static final String WAS_RUNNING = "was_running";
	public static final String ANNOUNCE_USING_ZEROCONF = "announce_using_zeroconf";
	
	private ListenerServiceConnection _service;
	
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
    }
    
    @Override
    protected void onPause() {
    	super.onPause();

    	Log.d("Preferences.onPause", "Restarting the GrowlListenerService...");
    	_service.restart();
    	_service.unbind();
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