package com.growlforandroid.client;

import com.growlforandroid.common.Database;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;

public class Applications extends ListActivity {
	private Database _database;
	private Cursor _cursor;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.applications_title);
        
        _database = new Database(this);
        String typeCountCaption = getText(R.string.database_notification_types).toString(); 
        _cursor = _database.getAllApplications(typeCountCaption);
        
        // Use an existing ListAdapter that will map an array
        // of strings to TextViews
        setListAdapter(new SimpleCursorAdapter(this, R.layout.history_list_item,
                _cursor,
                new String[] { Database.KEY_NAME, Database.KEY_TYPE_COUNT },
                new int[] { R.id.txtNotificationTitle, R.id.txtNotificationMessage }));
        getListView().setTextFilterEnabled(true);
    }
    
    protected void finalize() throws Throwable {
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
}
