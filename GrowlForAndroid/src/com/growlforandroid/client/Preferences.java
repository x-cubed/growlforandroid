package com.growlforandroid.client;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity {
	public static final String START_AT_BOOT = "start_at_boot";
	public static final String WAS_RUNNING = "was_running";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setTitle(R.string.preferences_title);
        
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
}
