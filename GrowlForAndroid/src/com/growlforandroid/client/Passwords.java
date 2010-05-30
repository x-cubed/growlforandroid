package com.growlforandroid.client;

import com.growlforandroid.common.Database;

import android.app.*;
import android.content.DialogInterface;
import android.database.Cursor;
import android.view.*;
import android.widget.*;
import android.os.*;

public class Passwords extends ListActivity {
	private final int DIALOG_ADD_PASSWORD = 0;
	private final int DIALOG_ITEM_MENU = 1;
	
	private Database _database;
	private Cursor _cursor;
	private MenuItem _mniAddPassword;
	private long _itemId = -1;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.passwords_title);
        
        _database = new Database(this);
        refresh();
        
        // Use an existing ListAdapter that will map an array
        // of strings to TextViews
        setListAdapter(new SimpleCursorAdapter(this,
        		R.layout.password_list_item, _cursor,
                new String[] { Database.KEY_NAME, Database.KEY_PASSWORD },
                new int[] { R.id.lblPasswordName, R.id.lblPasswordText}));
    }
    
    private void refresh() {
    	if (_cursor == null) {
    		_cursor = _database.getAllPasswordsAndNames();
    	} else {
    		_cursor.requery();
    	}
    }
    
    protected void finalize() throws Throwable {
    	if (_cursor != null) {
    		_cursor.close();
    	}
    	super.finalize();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	_mniAddPassword = menu.add(R.string.passwords_add_password);
    	_mniAddPassword.setIcon(android.R.drawable.ic_menu_add);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item == _mniAddPassword) {
    		showDialog(DIALOG_ADD_PASSWORD);
    		return true;
    	}
    	return false;
    }
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
    	_itemId = id;
    	showDialog(DIALOG_ITEM_MENU);
    }
    
    @Override
    public Dialog onCreateDialog(int id) {
    	switch (id) {
	    	case DIALOG_ADD_PASSWORD:
	        	LayoutInflater factory = LayoutInflater.from(this);
	            final View textEntryView = factory.inflate(R.layout.add_password_dialog, null);
	            final EditText txtName = (EditText) textEntryView.findViewById(R.id.txtPasswordName);
	            final EditText txtPassword = (EditText) textEntryView.findViewById(R.id.txtPasswordPassword);
	            
	            return new AlertDialog.Builder(this)
	                .setTitle(R.string.add_password_dialog_title)
	                .setView(textEntryView)
	                .setPositiveButton(R.string.add_password_dialog_ok, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                    	// Add the password to the database
	                    	String name = txtName.getText().toString();
	                    	String password = txtPassword.getText().toString();
	                    	if (name.equals("") || password.equals("")) {
	                    		return;
	                    	}	                    	
	                    	_database.insertPassword(name, password);
	                    	refresh();
	                    }
	                })
	                .setNegativeButton(R.string.add_password_dialog_cancel, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                        /* User clicked cancel so don't do anything */
	                    }
	                })
	                .create();
	            
	    	case DIALOG_ITEM_MENU:
	            return new AlertDialog.Builder(this)
	                .setTitle("Password")
	                .setItems(R.array.passwords_item_menu, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int which) {
	                    	// Only option is delete
	                    	_database.deletePassword(_itemId);
	                    	refresh();
	                    }
	                })
	                .create();
    	}
    	
    	return null;
    }
}
