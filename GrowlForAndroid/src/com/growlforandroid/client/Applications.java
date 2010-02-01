package com.growlforandroid.client;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;

public class Applications extends ListActivity {
	private final String[] _items = new String[] { "Foo", "Bar", "Baz" };
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use an existing ListAdapter that will map an array
        // of strings to TextViews
        setListAdapter(new ArrayAdapter<String>(this, 
                R.layout.notification_list_item, R.id.txtText, _items));
        getListView().setTextFilterEnabled(true);
    }
}
