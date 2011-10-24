package com.growlforandroid.client;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import javax.jmdns.*;

import com.growlforandroid.common.Database;
import com.growlforandroid.common.GrowlApplication;
import com.growlforandroid.common.GrowlNotification;
import com.growlforandroid.common.NotificationType;
import com.growlforandroid.common.Subscription;
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
	private SubscriptionListAdapter _adapter;
	private MenuItem _mniAddSubscription;
	private MenuItem _mniSubscribeNow;
	private int _position = -1;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.subscriptions_title);
        
        // Enable use of a spinner to show loading progress
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminate(true);

        _zeroConf = ZeroConf.getInstance(this);
        _service = new ListenerServiceConnection(this, this);
        _database = new Database(this);
        _adapter = new SubscriptionListAdapter();
        setListAdapter(_adapter);
    }
    
    private void refresh() {
    	final ArrayList<Subscription> subscriptions = new ArrayList<Subscription>();
    	
    	// Add the subscriptions from the database
    	Cursor cursor = _database.getSubscriptions();
    	int idIndex = cursor.getColumnIndex(Database.KEY_ROWID);
    	int nameIndex = cursor.getColumnIndex(Database.KEY_NAME);
		int statusIndex = cursor.getColumnIndex(Database.KEY_STATUS);
		int addressIndex = cursor.getColumnIndex(Database.KEY_ADDRESS);
		int passwordIndex = cursor.getColumnIndex(Database.KEY_PASSWORD);
    	while (cursor.moveToNext()) {
    		int id = cursor.getInt(idIndex);
    		String name = cursor.getString(nameIndex);
    		String status = cursor.getString(statusIndex);
    		String address = cursor.getString(addressIndex);
    		String password = cursor.getString(passwordIndex);
    		Subscription subscription = new Subscription(id, name, status, address, password, true);
    		subscriptions.add(subscription);
    	}
    	_adapter.update(subscriptions);
    	
    	findZeroConfServices(subscriptions);
    }
    
    private void findZeroConfServices(final ArrayList<Subscription> existing) {
    	final String statusAvailable = this.getResources().getString(R.string.subscriptions_status_available);
    	final Subscriptions activity = this;
    	activity.setProgressBarIndeterminateVisibility(true);

    	// Spawn a background thread to look for GNTP services on the network
    	new Thread(new Runnable() {
    		public void run() {
    			// Query the available services using ZeroConf
    			int added = 0;
    			Log.i("Subscriptions.refresh", "Querying available GNTP services...");
    			final ServiceInfo[] services = _zeroConf.findServices(Constants.GNTP_ZEROCONF_SERVICE_TYPE);
    			Log.i("Subscriptions.refresh", "Found " + services.length + " available GNTP services");
    	    	for (int i=0; i < services.length; i++) {
    	    		ServiceInfo service = services[i];

    	    		InetAddress[] addresses = service.getInetAddresses();
    	    		if ((addresses != null) && (addresses.length > 0)) {
	    	            Log.d("Subscriptions.refresh", "\"" + service.getName() + "\" at " +
	    	            		addresses[0] + ":" + service.getPort());
	    	            existing.add(new Subscription(service, statusAvailable, false));
	    	            added++;
    	    		}
    	    	}
    	    	Log.i("Subscriptions.refresh", "Finished querying");
    	    	
    	    	// Switch back to the UI thread to update the list view
    	    	final boolean modified = (added > 0);
    	    	activity.runOnUiThread(new Runnable() {
    	    		public void run() {
    	    			if (modified) {
    	    				_adapter.update(existing);
    	    			}
    	    	    	activity.setProgressBarIndeterminateVisibility(false);
    	    		}
    	    	});
    		}
    	}).start();
    }
    
    private static void setText(View child, int viewId, String text) {
    	TextView textView = (TextView) child.findViewById(viewId);
    	textView.setText(text);
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
    	_zeroConf.addServiceListener(Constants.GNTP_ZEROCONF_SERVICE_TYPE, this);
    	
    	refresh();
    }
    
    @Override
    public void onPause() {
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
    	if (_service != null) {
    		_service.unbind();
    		_service = null;
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
    	_position = position;
    	Subscription subscription = (Subscription)_adapter.getItem(_position);
    	if (subscription.isSubscribed()) {
    		showDialog(DIALOG_ITEM_MENU);
    	} else {
    		showDialog(DIALOG_ADD_SUBSCRIPTION);
    	}
    }
    
    @Override
    public Dialog onCreateDialog(int id) {
    	final Subscription selected = (Subscription) _adapter.getItem(_position);
    	
    	switch (id) {
	    	case DIALOG_ADD_SUBSCRIPTION:
	    		if ((selected != null) && !selected.isSubscribed()) {
	    			// Add from Bonjour
	    			return new AddEditDialog(this, selected.getName(), selected.getAddress()).create();
	    		} else {
	    			// Add manually
	    			return new AddEditDialog(this).create();
	    		}
	        
	    	case DIALOG_EDIT_SUBSCRIPTION:
	        	return new AddEditDialog(this, selected.getId(), selected.getName(),
	        			selected.getAddress(), selected.getPassword()).create();
		    	
	    		
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
	                    		_database.deleteSubscription(selected.getId());
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

    private class SubscriptionListAdapter extends BaseAdapter implements ListAdapter {
    	private List<Subscription> _subscriptions;
    	
    	public SubscriptionListAdapter() {
    		_subscriptions = new ArrayList<Subscription>();
    	}
    	
    	public void update(ArrayList<Subscription> subscriptions) {
    		synchronized (_subscriptions) {
	    		notifyDataSetInvalidated();
	    		_subscriptions = subscriptions;
	    		notifyDataSetChanged();
    		}
    	}
    	
		public int getCount() {
			return _subscriptions.size();
		}

		public Object getItem(int position) {
			synchronized (_subscriptions) {
				if (position < 0 || position >= _subscriptions.size()) {
					return null;
				}
				return _subscriptions.get(position);
			}
		}

		public long getItemId(int position) {
			synchronized (_subscriptions) {
				Subscription subscription = _subscriptions.get(position);
				return subscription.getId();
			}
		}

		public View getView(int position, View convertView, ViewGroup viewGroup) {
			View child;
			if (convertView != null) {
				child = convertView;
			} else {
				child = getLayoutInflater().inflate(R.layout.history_list_item, viewGroup, false);
			}
			
			Subscription subscription = _subscriptions.get(position);
			setText(child, R.id.txtNotificationTitle, subscription.getName());
            setText(child, R.id.txtNotificationMessage, subscription.getStatus());
            setText(child, R.id.txtNotificationApp, subscription.getAddress());
            
            return child;
		}
    }
    
	private class AddEditDialog {
		private final Context _context;
		private LayoutInflater _factory;
		private View _textEntryView;
		private EditText _txtName;
		private EditText _txtAddress;
		private EditText _txtPassword;
		
		private Builder _builder;

		private long _id;
		
		public AddEditDialog(Context context) {
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
		
		public AddEditDialog(Context context, String name, String address) {
			this(context);
			
			_txtName.setText(name);
			_txtName.setEnabled(false);
			_txtAddress.setText(address);
			_txtAddress.setEnabled(false);
		}
		
		public AddEditDialog(Context context, long id, String name, String address, String password) {
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
    	try {
	        Log.i("Subscriptions.serviceAdded", "Service added   : " + event.getName() + "." + event.getType());
	        ServiceInfo service = _zeroConf.getServiceInfo(event.getType(), event.getName());
	        dumpServiceInfo("Subscriptions.serviceAdded", service);
    	} catch (Exception x) {
    		Log.e("Subscriptions.serviceAdded", x.toString());
    	}
    }

    public void serviceRemoved(ServiceEvent event) {
    	Log.i("Subscriptions.serviceRemoved", "Service removed : " + event.getName() + "." + event.getType());
    }

    public void serviceResolved(ServiceEvent event) {
    	try {
	    	ServiceInfo service = event.getInfo();
	    	dumpServiceInfo("Subscriptions.serviceResolved", service);
    	} catch (Exception x) {
    		Log.e("Subscriptions.serviceAdded", x.toString());
    	}
    }
    
    private void dumpServiceInfo(String tag, ServiceInfo service) {
    	if (service == null) {
    		Log.i(tag, "No service information");
    		return;
    	}
    	
    	Log.d(tag, "\"" + service.getName() + "\" on " + service.getInetAddresses()[0].toString() + " port " + service.getPort());
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