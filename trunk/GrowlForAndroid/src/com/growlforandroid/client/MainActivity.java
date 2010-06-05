package com.growlforandroid.client;

import com.growlforandroid.common.Database;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.os.*;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;

public class MainActivity extends Activity {
	private GrowlListenerConnection _service;
	private Database _database;
	private Cursor _cursor;
	
	private ListView _lsvNotifications;
	private ToggleButton _tglServiceState;
	private TextView _txtServiceState;

	private MenuItem _mniApplications;
	private MenuItem _mniPreferences;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _service = new GrowlListenerConnection();
        _database = new Database(this);
        refresh();
        
        // Load the default preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        
        setContentView(R.layout.main);
        setTitle(R.string.app_name);

        // List the recent notifications
        _lsvNotifications = (ListView)findViewById(R.id.lsvNotifications);
        _lsvNotifications.setAdapter(new SimpleCursorAdapter(this,
        		R.layout.history_list_item, _cursor,
                new String[] { Database.KEY_TITLE, Database.KEY_MESSAGE, Database.KEY_APP_NAME },
                new int[] { R.id.txtNotificationTitle, R.id.txtNotificationMessage, R.id.txtNotificationApp }));
        
        // Watch for button clicks.
        _tglServiceState = (ToggleButton)findViewById(R.id.tglServiceState);
        _tglServiceState.setOnClickListener(new OnClickListener() {
            public void onClick(View v)
            {
                if (!_tglServiceState.isChecked()) {
                	_service.stop();
                } else {
                	_service.start();
                }
            }
        });
        
        _txtServiceState = (TextView)findViewById(R.id.txtServiceState);
        updateServiceState();
    }
    
    private void refresh() {
    	if (_cursor == null) {
    		_cursor = _database.getNotificationHistory();
    	} else {
    		_cursor.requery();
    	}
    }

    protected void finalize() throws Throwable {
    	if (_service != null) {
    		_service.finalize();
    		_service = null;
    	}
    	
    	if (_cursor != null) {
    		_cursor.close();
    		_cursor = null;
    	}
    	
    	if (_database != null) {
    		_database.close();
    		_database = null;
    	}
    	
    	super.finalize();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	_mniApplications = menu.add(R.string.menu_applications);
    	_mniApplications.setIcon(android.R.drawable.ic_menu_manage);
    	
    	_mniPreferences = menu.add(R.string.menu_preferences);
    	_mniPreferences.setIcon(android.R.drawable.ic_menu_preferences);
    	
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item == _mniApplications) {
    		startActivity(new Intent(this, Applications.class));
    		return true;
    		
    	} else if (item == _mniPreferences) {
    		startActivity(new Intent(this, Preferences.class));
    		return true;
    		
    	} else {
    		Log.e("MainActivity.onOptionsItemSelected", "Unknown menu item: " + item.getTitle());
    		return false;
    	}
    }
    
    protected void updateServiceState() {
    	boolean isRunning = _service.isRunning();
    	
    	if (_tglServiceState.isChecked() != isRunning)
    		_tglServiceState.setChecked(isRunning);
    	
    	_txtServiceState.setText(isRunning ? R.string.growl_on_status : R.string.growl_off_status);
    }
    
    
    private class GrowlListenerConnection implements ServiceConnection {
    	private final Intent _growlListenerService = new Intent(MainActivity.this, GrowlListenerService.class);
    	private boolean _isBound;
    	private GrowlListenerService _instance;
    	
    	public boolean isRunning() {
    		if (_instance == null) {
    			_isBound = bindService(_growlListenerService, this, 0);
    			Log.i("GrowlListenerConnection.isRunning",
    					"IsBound = " + _isBound + ", Has Instance = " + (_instance != null));
    			return _isBound && (_instance != null) ? _instance.isRunning() : false;   			
    		} else {
    			return _instance.isRunning();
    		}
    	}
    	
    	public void start() {
    		if (!bindService(_growlListenerService, this, BIND_AUTO_CREATE)) {
            	Log.e("GrowlListenerConnection.start", "Unable to bind to service");
            	return;
            }
    		
    		_isBound = true;
    		startService(new Intent(MainActivity.this, GrowlListenerService.class));
    		updateServiceState();
    	}
    	
    	public void stop() {
    		unbind();
    		if (!stopService(_growlListenerService)) {
    			Log.e("GrowlListenerConnection.stop", "Unable to stop service");
            	return;
    		}
    		updateServiceState();
    	}
    	
    	private void unbind() {
    		if (_isBound) {
    			Log.i("GrowlListenerConnection.unbind", "Unbinding from service");
    			unbindService(this);
    			_isBound = false;
    			onServiceDisconnected(null);
    		}
    	}
    	
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i("GrowlListenerConnection.onServiceConnected", "Connected to " + name);
			_instance = ((GrowlListenerService.LocalBinder)service).getService();
			updateServiceState();
		}

		public void onServiceDisconnected(ComponentName name) {
			Log.i("GrowlListenerConnection.onServiceDisconnected", "Disconnected from " + name);
			_instance = null;
			updateServiceState();
		}
		
		protected void finalize() {
			unbind();
		}
    }
}