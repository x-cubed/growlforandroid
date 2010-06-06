package com.growlforandroid.client;

import com.growlforandroid.common.Database;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class Application extends Activity {
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
        
        lsvNotificationTypes.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				_typeId = id;				
			}
        });
    }

    private void refresh() {
    	Cursor appCursor = _database.getApplication(_appId);
    	if (appCursor.moveToFirst()) {
    		String appName = appCursor.getString(appCursor.getColumnIndex(Database.KEY_NAME));
    		boolean enabled = appCursor.getInt(appCursor.getColumnIndex(Database.KEY_ENABLED)) != 0;
    		
    		TextView txtTitle = (TextView)findViewById(R.id.txtAppName);
    		txtTitle.setText(appName);
    		
    		CheckBox chkEnabled = (CheckBox)findViewById(R.id.chkAppEnabled);
    		chkEnabled.setChecked(enabled);
    	}
    	appCursor.close();
    	
    	if (_cursor == null) {
    		_cursor = _database.getNotificationTypes(_appId);
    	} else {
    		_cursor.requery();
    	}
    }
}
