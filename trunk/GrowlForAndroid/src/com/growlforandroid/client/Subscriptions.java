package com.growlforandroid.client;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import javax.jmdns.*;

import com.growlforandroid.common.*;
import com.growlforandroid.gntp.Constants;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

public class Subscriptions extends ListActivity implements SubscriptionDialog.Listener, ZeroConf.Listener,
		GrowlListenerService.StatusChangedHandler {

	private final int DIALOG_ADD_SUBSCRIPTION = 0;
	private final int DIALOG_EDIT_SUBSCRIPTION = 1;
	private final int DIALOG_ITEM_MENU = 2;

	private final int ZEROCONF_TIMEOUT_MS = 100;

	private ZeroConf _zeroConf;
	private ListenerServiceConnection _service;
	private SubscriptionListAdapter _adapter;
	private Database _database;

	private MenuItem _mniAddSubscription;
	private MenuItem _mniSubscribeNow;

	private int _position = -1;

	@Override
	protected void onStop() {
		Log.d("Subscriptions.onStop", "Invoked");
		super.onStop();
		getListView().setVisibility(View.GONE);
	}

	@Override
	protected void onRestart() {
		Log.d("Subscriptions.onRestart", "Invoked");
		super.onRestart();
		getListView().setVisibility(View.VISIBLE);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d("Subscriptions.onCreate", "Invoked");
		super.onCreate(savedInstanceState);
		setTitle(R.string.subscriptions_title);

		// Enable use of a spinner to show loading progress
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminate(true);

		// Show a label if there's nothing listed
		Utility.setEmptyLabel(this, R.string.subscriptions_empty_label);
		
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
			Subscription subscription = new Subscription(id, name, status, address, password, false, true);
			Log.d("Subscriptions.refresh", "Subscription " + id + ": " + name);
			subscriptions.add(subscription);
		}
		Log.i("Subscriptions.refresh", "Updating adapter");
		_adapter.replace(subscriptions);

		findZeroConfServices();
	}

	private void findZeroConfServices() {
		final String statusAvailable = this.getResources().getString(R.string.subscriptions_status_available);
		final Subscriptions activity = this;
		activity.setProgressBarIndeterminateVisibility(true);

		// Spawn a background thread to look for GNTP services on the network
		new Thread(new Runnable() {
			public void run() {
				// Query the available services using ZeroConf
				Log.i("Subscriptions.findZeroConfServices", "Querying available GNTP services...");
				final ServiceInfo[] services = _zeroConf.findServices(
						Constants.GNTP_ZEROCONF_SERVICE_TYPE, ZEROCONF_TIMEOUT_MS);
				Log.i("Subscriptions.findZeroConfServices", "Found " + services.length + " available GNTP services");

				// Switch back to the UI thread to update the list view
				activity.runOnUiThread(new Runnable() {
					public void run() {
						_adapter.addServices(services, statusAvailable);
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
		_adapter.notifyDataSetChanged();

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
		Subscription subscription = (Subscription) _adapter.getItem(_position);
		if (subscription.isSubscribed()) {
			showDialog(DIALOG_ITEM_MENU);
		} else {
			showDialog(DIALOG_EDIT_SUBSCRIPTION);
		}
	}

	@Override
	public Dialog onCreateDialog(int id) {
		final Subscription selected = (Subscription) _adapter.getItem(_position);

		switch (id) {
		case DIALOG_ADD_SUBSCRIPTION:
			return SubscriptionDialog.createAddDialog(this, this);

		case DIALOG_EDIT_SUBSCRIPTION:
			if (selected == null) {
				return null;
			}
			Log.d("Subscriptions.onCreateDialog", "Edit subscription " + selected.getId() + ": " + selected.getName());
			return SubscriptionDialog.createEditDialog(this, this, selected);

		case DIALOG_ITEM_MENU:
			if (selected == null) {
				return null;
			}
			Log.d("Subscriptions.onCreateDialog", "Subscription menu " + selected.getId() + ": " + selected.getName());
			return new AlertDialog.Builder(this).setTitle(R.string.subscriptions_menu_title)
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
					}).create();
		}

		return null;
	}

	public void addSubscription(String name, String address, String password) {
		String unregistered = getText(R.string.subscriptions_status_unregistered).toString();
		_database.insertSubscription(name, address, password, unregistered);

		refresh();
		if (_service.isRunning()) {
			_service.subscribeNow();
		}
	}

	public void updateSubscription(long id, String name, String address, String password) {
		String unregistered = getText(R.string.subscriptions_status_unregistered).toString();
		_database.updateSubscription(id, name, address, password, unregistered);

		refresh();
		if (_service.isRunning()) {
			_service.subscribeNow();
		}
	}

	private class SubscriptionListAdapter extends BaseAdapter implements ListAdapter {
		private List<Subscription> _subscriptions;

		public SubscriptionListAdapter() {
			_subscriptions = new ArrayList<Subscription>();
		}

		public void addServices(ServiceInfo[] services, String status) {
			for (int i = 0; i < services.length; i++) {
				addService(services[i], status);
			}
		}

		public void addService(ServiceInfo service, String status) {
			Subscription subscription = new Subscription(service, status, false);
			if (subscription.isValid()) {
				InetAddress[] addresses = subscription.getInetAddresses();
				
				boolean isUnique = true;
				for (Subscription previouslySeen : _subscriptions) {
					if (previouslySeen.matchesAny(addresses)) {
						Log.d("SubscriptionListAdapter.addService", "Subscription to " + subscription.getName()
								+ " has already been seen");
						isUnique = false;
						break;
					}
				}

				if (isUnique) {
					addSubscription(subscription);
				}
			}
		}
		
		public void removeService(ServiceInfo service) {
			synchronized (_subscriptions) {
				for(Subscription subscription:_subscriptions) {
					if (!subscription.isSubscribed() && subscription.matchesService(service)) {
						_subscriptions.remove(subscription);
						notifyDataSetChanged();
						return;
					}
				}
			}
		}

		private void addSubscription(Subscription subscription) {
			synchronized (_subscriptions) {
				_subscriptions.add(subscription);
				notifyDataSetChanged();
			}
		}

		public void replace(ArrayList<Subscription> subscriptions) {
			synchronized (_subscriptions) {
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

	public void serviceAdded(ServiceInfo serviceInfo, ServiceEvent event) {
		try {
			Log.i("Subscriptions.serviceAdded", "Service added   : " + event.getName() + "." + event.getType());
			final ServiceInfo service = serviceInfo;
			final String statusAvailable = this.getResources().getString(R.string.subscriptions_status_available);
			runOnUiThread(new Runnable() {
				public void run() {
					_adapter.addService(service, statusAvailable);
				}
			});
		} catch (Exception x) {
			Log.e("Subscriptions.serviceAdded", x.toString());
		}
	}

	public void serviceRemoved(ServiceInfo serviceInfo, ServiceEvent event) {
		Log.i("Subscriptions.serviceRemoved", "Service removed : " + event.getName() + "." + event.getType());
		final ServiceInfo service = serviceInfo;
		runOnUiThread(new Runnable() {
			public void run() {
				_adapter.removeService(service);
			}
		});
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