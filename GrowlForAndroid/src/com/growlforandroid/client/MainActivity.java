package com.growlforandroid.client;


import android.app.*;
import android.content.*;
import android.os.*;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;

public class MainActivity extends Activity {
	private final GrowlListenerConnection _service = new GrowlListenerConnection();
	private ListView _lsvNotifications;
	private ToggleButton _tglServiceState;
	private TextView _txtServiceState;
	
	private MenuItem _mniApplications;
	private MenuItem _mniSettings;
	
	private String[] _items = new String[] { "Foo", "Bar", "Baz" };
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // List the recent notifications
        _lsvNotifications = (ListView)findViewById(R.id.lsvNotifications);
        _lsvNotifications.setAdapter(new ArrayAdapter<String>(this, 
                R.layout.notification_list_item, R.id.txtText, _items));
        
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	_mniApplications = menu.add("Applications");
    	_mniSettings = menu.add("Settings");
    	
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item == _mniApplications) {
    		startActivity(new Intent(this, Applications.class));
    		return true;
    		
    	} else if (item == _mniSettings) {
    		startActivity(new Intent(this, Passwords.class));
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
    	
    	_txtServiceState.setText(isRunning ? "Growl is running" : "Growl is stopped");
    }
    
    
    private class GrowlListenerConnection implements ServiceConnection {
    	private boolean _isBound;
    	private GrowlListenerService _instance;
    	
    	public boolean isRunning() {
    		if (_instance == null) {
    			return false;
    		} else {
    			return _instance.isRunning();
    		}
    	}
    	
    	public void start() {
    		if (!bindService(new Intent(MainActivity.this, GrowlListenerService.class), this, BIND_AUTO_CREATE)) {
            	Log.e("GrowlListenerConnection.start", "Unable to bind to service");
            	return;
            }
    		
    		_isBound = true;
    		startService(new Intent(MainActivity.this, GrowlListenerService.class));
    		updateServiceState();
    	}
    	
    	public void stop() {
    		if (_isBound) {
    			Log.i("GrowlListenerConnection.stop", "Unbinding from service");
    			unbindService(this);
    			_isBound = false;
    			onServiceDisconnected(null);
    		}
    		if (!stopService(new Intent(MainActivity.this, GrowlListenerService.class))) {
    			Log.e("GrowlListenerConnection.stop", "Unable to stop service");
            	return;
    		}
    		updateServiceState();
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
    }
}