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
	private MenuItem _mniClearHistory;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _service = new GrowlListenerConnection();
        _database = new Database(this);
        
        // Load the default preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        
        setContentView(R.layout.main);
        setTitle(R.string.app_name);

        // List the recent notifications
        _lsvNotifications = (ListView)findViewById(R.id.lsvNotifications);
        
        // Watch for button clicks.
        _tglServiceState = (ToggleButton)findViewById(R.id.tglServiceState);
        _tglServiceState.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (!_tglServiceState.isChecked()) {
                	_service.stop();
                } else {
                	_service.start();
                }
            }
        });
    }
    
    @Override
    public void onResume() {
    	super.onResume();

    	Log.i("MainActivity.onResume", "Updating...");
    	_txtServiceState = (TextView)findViewById(R.id.txtServiceState);
        updateServiceState();

        // Update the notification history, then scroll to the top (most recent)
        refresh();
        _lsvNotifications.setSelection(0);
    }
    
    private void refresh() {
    	if (_cursor == null) {
    		_cursor = _database.getNotificationHistory();
    		_lsvNotifications.setAdapter(new SimpleCursorAdapter(this,
            		R.layout.history_list_item, _cursor,
                    new String[] { Database.KEY_TITLE, Database.KEY_MESSAGE, Database.KEY_APP_NAME },
                    new int[] { R.id.txtNotificationTitle, R.id.txtNotificationMessage, R.id.txtNotificationApp }));
    	} else {
    		_cursor.requery();
    	}
    }

    @Override
    public void onDestroy() {
    	if (_service != null) {
    		_service.unbind();
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
    	super.onDestroy();
    }
    
    protected void finalize() throws Throwable {
    	onDestroy();
    	super.finalize();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	_mniApplications = menu.add(R.string.menu_applications);
    	_mniApplications.setIcon(android.R.drawable.ic_menu_manage);
    	
    	_mniPreferences = menu.add(R.string.menu_preferences);
    	_mniPreferences.setIcon(android.R.drawable.ic_menu_preferences);
    	
    	_mniClearHistory = menu.add(R.string.menu_clear_history);
    	_mniClearHistory.setIcon(android.R.drawable.ic_menu_delete);
    	
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
    		
    	} else if (item == _mniClearHistory) {
    		_database.deleteNotificationHistory();
    		refresh();
    		return true;
    		
    	} else {
    		Log.e("MainActivity.onOptionsItemSelected", "Unknown menu item: " + item.getTitle());
    		return false;
    	}
    }
    
    protected void updateServiceState() {
    	updateServiceState(_service.isRunning());
    }
    
    protected void updateServiceState(boolean isRunning) {   	
    	if (_tglServiceState.isChecked() != isRunning)
    		_tglServiceState.setChecked(isRunning);
    	_txtServiceState.setText(isRunning ? R.string.growl_on_status : R.string.growl_off_status);
    }
    
    
    private class GrowlListenerConnection implements ServiceConnection {
    	private final Intent _growlListenerService = new Intent(MainActivity.this, GrowlListenerService.class);
    	private boolean _isBound;
    	private GrowlListenerService _instance;
    	
    	public boolean isRunning() {
			Log.i("GrowlListenerConnection.isRunning",
					"IsBound = " + _isBound + ", Has Instance = " + (_instance != null));
    		if (!_isBound) {
    			bind();
    			return _isBound && (_instance != null) ? _instance.isRunning() : false;   			
    		} else if (_instance != null) {
    			return _instance.isRunning();
    		} else {
    			return false;
    		}
    	}
    	
    	public void start() {
    		if (!bind()) {
            	Log.e("GrowlListenerConnection.start", "Unable to bind to service");
            	return;
            }
    		
    		_isBound = true;
    		startService(new Intent(MainActivity.this, GrowlListenerService.class));
    		updateServiceState(true);
    	}
    	
    	public void stop() {
    		unbind();
    		if (!stopService(_growlListenerService)) {
    			Log.e("GrowlListenerConnection.stop", "Unable to stop service");
            	return;
    		}
    		updateServiceState(false);
    	}
    	
    	private boolean bind() {
    		if (_isBound)
    			return _isBound;
    		
    		Log.i("GrowlListenerConnection.bind", "Binding to the service");
    		_isBound = bindService(_growlListenerService, this, 0);
    		return _isBound;
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
			updateServiceState(isRunning());
		}

		public void onServiceDisconnected(ComponentName name) {
			Log.i("GrowlListenerConnection.onServiceDisconnected", "Disconnected from " + name);
			_instance = null;
			updateServiceState(false);
		}
		
		protected void finalize() {
			unbind();
		}
    }
}