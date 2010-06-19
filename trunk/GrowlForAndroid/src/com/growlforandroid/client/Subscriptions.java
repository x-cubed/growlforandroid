package com.growlforandroid.client;

import javax.jmdns.*;

import com.growlforandroid.common.Database;
import com.growlforandroid.common.GrowlApplication;
import com.growlforandroid.common.GrowlNotification;
import com.growlforandroid.common.NotificationType;
import com.growlforandroid.common.ZeroConf;
import com.growlforandroid.gntp.Constants;

import android.app.*;
import android.app.AlertDialog.Builder;
import android.content.*;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

public class Subscriptions
	extends ListActivity
	implements ServiceListener, GrowlListenerService.StatusChangedHandler {
	
	private final int DIALOG_ADD_SUBSCRIPTION = 0;
	private final int DIALOG_EDIT_SUBSCRIPTION = 1;
	private final int DIALOG_ITEM_MENU = 2;
	
	private ZeroConf _zeroConf;
	private ListenerServiceConnection _service;
	
	private Database _database;
	private Cursor _cursor;
	private MenuItem _mniAddSubscription;
	private MenuItem _mniSubscribeNow;
	private long _itemId = -1;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.subscriptions_title);

        _zeroConf = ZeroConf.getInstance(this);
        _service = new ListenerServiceConnection(this, this);
        _database = new Database(this);
        _cursor = _database.getSubscriptions();
        
        // Use an existing ListAdapter that will map an array
        // of strings to TextViews
        setListAdapter(new SimpleCursorAdapter(this,
        		R.layout.history_list_item, _cursor,
                new String[] { Database.KEY_NAME, Database.KEY_STATUS, Database.KEY_ADDRESS },
                new int[] { R.id.txtNotificationTitle, R.id.txtNotificationMessage, R.id.txtNotificationApp }));
    }
    
    private void refresh() {
		_cursor.requery();
    	
    	new Thread(new Runnable() {
    		public void run() {
    			Log.i("Subscriptions.refresh", "Querying available GNTP services...");
    			ServiceInfo[] infos = _zeroConf.findServices(Constants.GNTP_ZEROCONF_SERVICE_TYPE);
    			for (int i=0; i < infos.length; i++) {
    	            Log.i("Subscriptions.refresh", infos[i].toString());
    	        }
    			Log.i("Subscriptions.refresh", "Finished querying");
    		}
    	}).start();
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
    	
    	Log.i("Subscriptions.onResume", "Listening for GNTP service announcements...");
        _zeroConf.addServiceListener(Constants.GNTP_ZEROCONF_SERVICE_TYPE, this);    	
    	refresh();
    }
    
    @Override
    public void onPause() {
		Log.i("Subscriptions.onPause", "Stop listening for GNTP service announcements");
		_zeroConf.removeServiceListener(Constants.GNTP_ZEROCONF_SERVICE_TYPE, this);
    	
    	_service.unbind();
    	super.onPause();
    }
    
    @Override
    public void onDestroy() {
    	stop();
    	super.onDestroy();
    }
    
    private void stop() {
    	_zeroConf.removeServiceListener(Constants.GNTP_ZEROCONF_SERVICE_TYPE, this);
    	
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
    }
    
    protected void finalize() throws Throwable {
    	stop();
    	super.finalize();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	_mniAddSubscription = menu.add(R.string.subscriptions_add_subscription);
    	_mniAddSubscription.setIcon(android.R.drawable.ic_menu_add);
    	_mniSubscribeNow = menu.add(R.string.subscriptions_subscribe_now);
    	_mniSubscribeNow.setIcon(android.R.drawable.ic_menu_rotate);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item == _mniAddSubscription) {
    		showDialog(DIALOG_ADD_SUBSCRIPTION);
    		return true;
    	} else if (item == _mniSubscribeNow) {
    		if (_service.isRunning())
    			_service.subscribeNow();
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
	    		return new EditDialog(this).create();
	        
	    	case DIALOG_EDIT_SUBSCRIPTION:
	    		Cursor subscription = _database.getSubscription(_itemId);
	    		if (subscription.moveToFirst()) {
		    		String name = subscription.getString(subscription.getColumnIndex(Database.KEY_NAME));
		        	String address = subscription.getString(subscription.getColumnIndex(Database.KEY_ADDRESS));
		        	String password = subscription.getString(subscription.getColumnIndex(Database.KEY_PASSWORD));
		    		subscription.close();
		        	return new EditDialog(this, _itemId, name, address, password).create();
	    		}
	    		subscription.close();
	    		break;
		    	
	    		
	    	case DIALOG_ITEM_MENU:
	            return new AlertDialog.Builder(this)
	                .setTitle(R.string.subscriptions_menu_title)
	                .setItems(R.array.subscriptions_menu, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int which) {
	                    	if (which == 0) {
	                    		// Edit
	                    		showDialog(DIALOG_EDIT_SUBSCRIPTION);
	                    	} else if (which == 1) {
	                    		// Delete
	                    		_database.deleteSubscription(_itemId);
		                    	refresh();	
	                    	} else {
	                    		Log.w("Subscriptions.ItemMenu.onClick", "Unknown option " + which);
	                    	}
	                    }
	                })
	                .create();
    	}
    	
    	return null;
    }

	private class EditDialog {
		private final Context _context;
		private LayoutInflater _factory;
		private View _textEntryView;
		private EditText _txtName;
		private EditText _txtAddress;
		private EditText _txtPassword;
		
		private Builder _builder;

		private long _id;
		
		public EditDialog(Context context) {
			_context = context;
			initialise();
			
			_builder.setTitle(R.string.subscriptions_add_dialog_title);
		    _builder.setPositiveButton(R.string.dialog_add, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int whichButton) {
		        	// Get the field values from the dialog
		        	String name = _txtName.getText().toString();
		        	String address = _txtAddress.getText().toString();
		        	String password = _txtPassword.getText().toString();
		        	String status = getText(R.string.subscriptions_status_unregistered).toString();
		        	if (name.equals("") || address.equals("") || password.equals("")) {
		        		return;
		        	}	                    	
		        	
		        	// Add the subscription to the database, then refresh the list
		        	_database.insertSubscription(name, address, password, status);
		        	refresh();
		        	
		        	// Get the service to subscribe now
		        	if (_service.isRunning())
		        		_service.subscribeNow();
		        }
		    });
		}
		
		public EditDialog(Context context, long id, String name, String address, String password) {
			_context = context;
			_id = id;
			initialise();
			
			_txtName.setText(name);
			_txtAddress.setText(address);
			_txtPassword.setText(password);
			
			_builder.setTitle(R.string.subscriptions_edit_dialog_title);
		    _builder.setPositiveButton(R.string.dialog_save, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int whichButton) {
		        	// Get the field values from the dialog
		        	String name = _txtName.getText().toString();
		        	String address = _txtAddress.getText().toString();
		        	String password = _txtPassword.getText().toString();
		        	String status = getText(R.string.subscriptions_status_unregistered).toString();
		        	if (name.equals("") || address.equals("") || password.equals("")) {
		        		return;
		        	}	                    	
		        	
		        	// Add the subscription to the database, then refresh the list
		        	_database.updateSubscription(_id, name, address, password, status);
		        	refresh();
		        	
		        	// Get the service to subscribe now
		        	if (_service.isRunning())
		        		_service.subscribeNow();
		        }
		    });
		}
		
		private void initialise() {
			_factory = LayoutInflater.from(_context);
			_textEntryView = _factory.inflate(R.layout.add_subscription_dialog, null);
			_txtName = (EditText) _textEntryView.findViewById(R.id.txtSubscriptionName);
			_txtAddress = (EditText) _textEntryView.findViewById(R.id.txtSubscriptionAddress);
			_txtPassword = (EditText) _textEntryView.findViewById(R.id.txtSubscriptionPassword);
			
			_builder = new AlertDialog.Builder(_context)			    
			    .setView(_textEntryView)
			    .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int whichButton) {
			            /* User clicked cancel so don't do anything */
			        }
			    });
		}
		
		public Dialog create() {
			return _builder.create();
		}
	}
    
    public void serviceAdded(ServiceEvent event) {
        Log.i("Subscriptions.serviceAdded", "Service added   : " + event.getName() + "." + event.getType());
    }

    public void serviceRemoved(ServiceEvent event) {
    	Log.i("Subscriptions.serviceRemoved", "Service removed : " + event.getName() + "." + event.getType());
    }

    public void serviceResolved(ServiceEvent event) {
    	Log.i("Subscriptions.serviceResolved", "Service resolved: " + event.getInfo());
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