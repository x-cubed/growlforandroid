package com.growlforandroid.client;

import com.growlforandroid.common.Database;
import com.growlforandroid.common.GrowlApplication;
import com.growlforandroid.common.GrowlNotification;
import com.growlforandroid.common.NotificationType;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class Subscriptions
	extends ListActivity
	implements GrowlListenerService.StatusChangedHandler {
	
	private final int DIALOG_ADD_SUBSCRIPTION = 0;
	private final int DIALOG_ITEM_MENU = 1;
	
	private ListenerServiceConnection _service;
	
	private Database _database;
	private Cursor _cursor;
	private MenuItem _mniAddSubscription;
	private long _itemId = -1;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.subscriptions_title);
        
        _service = new ListenerServiceConnection(this, this);
        _database = new Database(this);
        refresh();
        
        // Use an existing ListAdapter that will map an array
        // of strings to TextViews
        setListAdapter(new SimpleCursorAdapter(this,
        		R.layout.history_list_item, _cursor,
                new String[] { Database.KEY_NAME, Database.KEY_STATUS, Database.KEY_ADDRESS },
                new int[] { R.id.txtNotificationTitle, R.id.txtNotificationMessage, R.id.txtNotificationApp }));
    }
    
    private void refresh() {
    	if (_cursor == null) {
    		_cursor = _database.getSubscriptions();
    	} else {
    		_cursor.requery();
    	}
    }
    
    private void refreshOnUiThread() {
    	this.runOnUiThread(new Runnable() {
			public void run() {
				refresh();
			}
		});
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	_service.bind();
    }
    
    @Override
    public void onPause() {
    	_service.unbind();
    	super.onPause();
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
    	_mniAddSubscription = menu.add(R.string.subscriptions_add_subscription);
    	_mniAddSubscription.setIcon(android.R.drawable.ic_menu_add);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item == _mniAddSubscription) {
    		showDialog(DIALOG_ADD_SUBSCRIPTION);
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
	    	case DIALOG_ADD_SUBSCRIPTION:
	        	LayoutInflater factory = LayoutInflater.from(this);
	            final View textEntryView = factory.inflate(R.layout.add_subscription_dialog, null);
	            final EditText txtName = (EditText) textEntryView.findViewById(R.id.txtSubscriptionName);
	            final EditText txtAddress = (EditText) textEntryView.findViewById(R.id.txtSubscriptionAddress);
	            final EditText txtPassword = (EditText) textEntryView.findViewById(R.id.txtSubscriptionPassword);
	            
	            return new AlertDialog.Builder(this)
	                .setTitle(R.string.add_subscription_dialog_title)
	                .setView(textEntryView)
	                .setPositiveButton(R.string.dialog_add, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                    	// Add the password to the database
	                    	String name = txtName.getText().toString();
	                    	String address = txtAddress.getText().toString();
	                    	String password = txtPassword.getText().toString();
	                    	String status = getText(R.string.subscriptions_status_unregistered).toString();
	                    	if (name.equals("") || address.equals("") || password.equals("")) {
	                    		return;
	                    	}	                    	
	                    	_database.insertSubscription(name, address, password, status);
	                    	refresh();
	                    }
	                })
	                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                        /* User clicked cancel so don't do anything */
	                    }
	                })
	                .create();
	            
	    	case DIALOG_ITEM_MENU:
	            return new AlertDialog.Builder(this)
	                .setTitle(R.string.subscriptions_menu_title)
	                .setItems(R.array.subscriptions_menu, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int which) {
	                    	// Only option is delete
	                    	_database.deleteSubscription(_itemId);
	                    	refresh();
	                    }
	                })
	                .create();
    	}
    	
    	return null;
    }
    

    public void onApplicationRegistered(GrowlApplication app) {
    }
    
    public void onNotificationTypeRegistered(NotificationType type) {
    }
    
	public void onDisplayNotification(GrowlNotification notification) {	
	}

	public void onIsRunningChanged(boolean isRunning) {
	}
	
	public void onSubscriptionStatusChanged(long id, String status) {
		refreshOnUiThread();
	}
}