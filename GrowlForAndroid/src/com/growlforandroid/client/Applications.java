package com.growlforandroid.client;

import com.growlforandroid.client.GrowlListenerService.StatusChangedHandler;
import com.growlforandroid.common.Database;
import com.growlforandroid.common.GrowlApplication;
import com.growlforandroid.common.GrowlNotification;
import com.growlforandroid.common.NotificationType;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class Applications
	extends ListActivity
	implements StatusChangedHandler {
	
	private static final int DIALOG_ITEM_MENU = 1;
	private static final int DIALOG_DELETE_PROMPT = 2;
	
	private static final int ITEM_MENU_PREFERENCES = 0;
	private static final int ITEM_MENU_DELETE = 1;
	
	private ListenerServiceConnection _service;

	private Database _database;
	private Cursor _cursor;
	private long _appId;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.applications_title);

        _service = new ListenerServiceConnection(this, this);      
        _database = new Database(this);
        refresh();
        
        // Use an existing ListAdapter that will map an array
        // of strings to TextViews
        setListAdapter(new SimpleCursorAdapter(this, R.layout.history_list_item,
                _cursor,
                new String[] { Database.KEY_NAME, Database.KEY_TYPE_LIST },
                new int[] { R.id.txtNotificationTitle, R.id.txtNotificationMessage }));
        getListView().setTextFilterEnabled(true);
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

    private void refresh() {
    	Log.i("Applications.refresh", "Updating...");
    	if (_cursor == null) {
    		_cursor = _database.getAllApplicationsAndTypes();
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
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	_appId = id;
    	showDialog(DIALOG_ITEM_MENU);
    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	String appName = null;
    	Cursor cursor = _database.getApplication(_appId);
    	if (cursor.moveToFirst()) {
    		appName = cursor.getString(cursor.getColumnIndex(Database.KEY_NAME));
    		Log.i("Applications.onCreateDialog", "App ID = " + _appId + ", Name = " + appName);
    		dialog.setTitle(appName);
    	}
    	cursor.close();
    }
    
    @Override
    public Dialog onCreateDialog(int id) {
    	final Applications apps = this;
    	switch (id) {
	    	case DIALOG_ITEM_MENU:
	            return new AlertDialog.Builder(this)
	            	.setTitle("Application")
	                .setItems(R.array.applications_item_menu, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int which) {
	                    	switch (which) {
	                    		case ITEM_MENU_PREFERENCES:
	                    			Intent appPrefs = new Intent(apps, Application.class);
	                    			appPrefs.putExtra("ID", _appId);
	                    			startActivity(appPrefs);
	                    			break;
	                    	
	                    		case ITEM_MENU_DELETE:
	                    			showDialog(DIALOG_DELETE_PROMPT);
	                    			break;
	                    			
                    			default:
                    				Log.e("Applications.ItemMenu.onClick",
                    						"Unknown menu item " + which);
	                    	}
	                    }
	                })
	                .create();
	            
	    	case DIALOG_DELETE_PROMPT:
	    		return new AlertDialog.Builder(this)
	    			.setTitle("Application")
	                .setMessage(R.string.applications_delete_prompt)
	                .setPositiveButton(R.string.button_delete, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							_database.deleteApplication(_appId, true);
	                    	refresh();
						}
					})
					.setNegativeButton(R.string.button_cancel, null)
	                .create();
    	}   	
    	return null;
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

    public void onApplicationRegistered(GrowlApplication app) {
    	// We don't need to handle this here, as the application will be registering at least one type as well
    }
    
    public void onNotificationTypeRegistered(NotificationType type) {
	    refreshOnUiThread();
    }
    
	public void onDisplayNotification(GrowlNotification notification) {	
	}

	public void onIsRunningChanged(boolean isRunning) {
	}
}
