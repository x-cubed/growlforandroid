package com.growlforandroid.client;

import com.growlforandroid.common.Database;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class Application
	extends Activity {
	
	private static final int DIALOG_CHOOSE_DISPLAY = 0;
	
	private long _appId = 1;
	
	private Database _database;
	private Cursor _cursor;
	private long _typeId;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setTitle(R.string.application_title);
        setContentView(R.layout.application_dialog);

        _appId = this.getIntent().getLongExtra("ID", -1);
        _database = new Database(this);
        refresh();
        
        // Use an existing ListAdapter that will map an array
        // of strings to TextViews
        ListView lsvNotificationTypes = (ListView)findViewById(R.id.lsvNotificationTypes);
        lsvNotificationTypes.setAdapter(new SimpleCursorAdapter(this, R.layout.history_list_item,
                _cursor,
                new String[] { Database.KEY_DISPLAY_NAME },
                new int[] { R.id.txtNotificationTitle }));
        lsvNotificationTypes.setTextFilterEnabled(true);
        
        final Application app = this;
        lsvNotificationTypes.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				_typeId = id;
				
				Intent typePrefs = new Intent(app, TypePreferences.class);
    			typePrefs.putExtra("ID", _typeId);
				startActivity(typePrefs);
			}
        });
    }

    private void refresh() {
    	Cursor appCursor = _database.getApplication(_appId);
    	if (appCursor.moveToFirst()) {
    		String appName = appCursor.getString(appCursor.getColumnIndexOrThrow(Database.KEY_NAME));
    		boolean enabled = appCursor.getInt(appCursor.getColumnIndexOrThrow(Database.KEY_ENABLED)) != 0;
    		int displayId = appCursor.getInt(appCursor.getColumnIndexOrThrow(Database.KEY_DISPLAY_ID));
    		String displayName = _database.getDisplayProfileName(displayId);
    		if (displayName == null) {
    			displayName = getText(R.string.application_option_display_default).toString();
    		}
    		
    		TextView txtTitle = (TextView)findViewById(R.id.txtAppName);
    		txtTitle.setText(appName);
    		
    		PreferenceCheckBoxView chkEnabled = (PreferenceCheckBoxView)findViewById(R.id.chkEnabled);
    		chkEnabled.setChecked(enabled);
    		chkEnabled.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView,	boolean isChecked) {
					_database.setApplicationEnabled(_appId, isChecked);
				}
    		});
    		
    		PreferenceDropDownView drpDisplay = (PreferenceDropDownView)findViewById(R.id.drpDisplayProfile);
    		drpDisplay.setSummary(displayName);
    		drpDisplay.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					showDialog(DIALOG_CHOOSE_DISPLAY);
				}
    		});
    	}
    	appCursor.close();
    	
    	if (_cursor == null) {
    		_cursor = _database.getNotificationTypes(_appId);
    	} else {
    		_cursor.requery();
    	}
    }
    
    
    @Override
    public Dialog onCreateDialog(int id) {
    	switch (id) {
	    	case DIALOG_CHOOSE_DISPLAY:
	    		return (new ChooseDisplayDialog(this, _database, true) {
	    			public void onDisplayChosen(Integer displayId) {
	    				Log.i("Application.onDisplayChosen", "App Id: " + _appId + ", Display Id: " + displayId);
	    				_database.setApplicationDisplay(_appId, displayId);
	    				refresh();
	    			}
	    		}).create();
    	}
    	return null;
    }
    
    @Override
    public void onDestroy() {
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
}
