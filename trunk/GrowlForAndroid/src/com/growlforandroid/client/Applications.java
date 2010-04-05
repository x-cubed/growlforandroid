package com.growlforandroid.client;

import com.growlforandroid.common.Database;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.SimpleCursorAdapter;

public class Applications extends ListActivity {
	private Database _database;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _database = new Database(this);
        Cursor cursor = _database.getAllApplications();
        
        // Use an existing ListAdapter that will map an array
        // of strings to TextViews
        setListAdapter(new SimpleCursorAdapter(this, R.layout.notification_list_item,
                cursor,
                new String[] { Database.KEY_NAME },
                new int[] { R.id.txtText }));
        getListView().setTextFilterEnabled(true);
    }
}
