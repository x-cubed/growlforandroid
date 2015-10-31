package com.growlforandroid.common;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import javax.jmdns.*;

import com.growlforandroid.client.R;
import com.growlforandroid.gntp.*;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class Subscriber implements ZeroConf.Listener {
	private final int MINUTES_MS = 60 * 1000;
	private final int SUBSCRIPTION_INTERVAL_MS = (int)(1.5 * MINUTES_MS);
	private final int ZERO_CONF_TIMEOUT_MS = 100;
	
	private final String STATUS_UNREGISTERED;
	private final String STATUS_REGISTERING;
	private final String STATUS_REGISTERED;
	private final String STATUS_NOT_AUTHORIZED;
	private final String STATUS_UNKNOWN_HOST;

	private final Context _context;
	private final UUID _id;
	private final String _deviceName;
	private Database _database;
	private Timer _timer;
	private ZeroConf _zeroConf;
	
	public Subscriber(Context context, UUID id, String deviceName) {
		_context = context;
		_id = id;
		_deviceName = deviceName;

		// Cache the status labels
		STATUS_UNREGISTERED = _context.getText(R.string.subscriptions_status_unregistered).toString();
		STATUS_REGISTERING = _context.getText(R.string.subscriptions_status_registering).toString();
		STATUS_REGISTERED = _context.getText(R.string.subscriptions_status_registered).toString();
		STATUS_NOT_AUTHORIZED = _context.getText(R.string.subscriptions_status_not_authorized).toString();
		STATUS_UNKNOWN_HOST = _context.getText(R.string.subscriptions_status_unknown_host).toString();
	}
	
	public Context getContext() {
		return _context;
	}
	
	public UUID getId() {
		return _id;
	}
	
	/**
	 * Returns a friendly name for the subscription, as displayed in the remote Growl instance
	 * @return the name to be displayed
	 */
	public String getName() {
		return _deviceName;
	}

	private Database getDatabase() {
		if (_database == null) {
			_database = new Database(_context);
		}
		return _database;
	}
	
	public void start() {
		if (isRunning()) {
			Log.w("Subscriber.start", "Subscriber already started");
			return;
		}
		
		Log.i("Subscriber.start", "Starting the subscriber " + _id + "...");
		
		// Setup up the subscription renewal timer
		_timer = new Timer();
		_timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				subscribeNow();
			}
		}, 0, SUBSCRIPTION_INTERVAL_MS);
		
		// Subscribe to ZeroConf service announcements
		_zeroConf = ZeroConf.getInstance(_context);
		_zeroConf.addServiceListener(Constants.GNTP_ZEROCONF_SERVICE_TYPE, this);
	}
	
	public boolean isRunning() {
		return _timer != null;
	}
	
	public void stop() {
		if (isRunning()) {
			Log.i("Subscriber.stop", "Stopping the subscriber " + _id + "...");
			_timer.cancel();
			_timer.purge();
			_timer = null;

			// Mark all subscriptions as unregistered
			unsubscribeAll();
		}
		
		if (_database != null) {
			_database.close();
			_database = null;
		}
		
		if (_zeroConf != null) {
			_zeroConf.removeServiceListener(Constants.GNTP_ZEROCONF_SERVICE_TYPE, this);
			_zeroConf = null;
		}
	}
	
	public void subscribeNow() {
		Log.d("Subscriber.subscribeNow", "Subscribing...");
		subscribeToManualHosts();
		subscribeToZeroConfServices();
		Log.d("Subscriber.subscribeNow", "Done");
	}
	
	private void subscribeToManualHosts() {
		Cursor subscriptions = getDatabase().getManualSubscriptions();
		if (subscriptions.moveToFirst()) {
			final int ID_COLUMN = subscriptions.getColumnIndex(Database.KEY_ROWID);
			final int ADDRESS_COLUMN = subscriptions.getColumnIndex(Database.KEY_ADDRESS);
			final int PASSWORD_COLUMN = subscriptions.getColumnIndex(Database.KEY_PASSWORD);
			
			int count = 0;
			do {
				long id = subscriptions.getLong(ID_COLUMN);
				String address = subscriptions.getString(ADDRESS_COLUMN);
				String password = subscriptions.getString(PASSWORD_COLUMN);
				startSubscription(id, address, password);
				count ++;
			} while (subscriptions.moveToNext());
			Log.i("Subscriber.subscribeNow", "Subscribing to " + count + " manual sources");
		}
		subscriptions.close();
	}
	
	private void subscribeToZeroConfServices() {
		if (_zeroConf == null) {
			return;
		}
		ServiceInfo[] services = _zeroConf.findServices(Constants.GNTP_ZEROCONF_SERVICE_TYPE, ZERO_CONF_TIMEOUT_MS);
		for(ServiceInfo service:services) {
			subscribeToService(service);
		}
	}
	
	public void unsubscribeAll() {
		Cursor subscriptions = getDatabase().getSubscriptions();
		if (subscriptions.moveToFirst()) {
			final int ID_COLUMN = subscriptions.getColumnIndex(Database.KEY_ROWID);		
			int count = 0;
			do {
				long id = subscriptions.getLong(ID_COLUMN);
				getDatabase().updateSubscription(id, STATUS_UNREGISTERED);
				onSubscriptionStatusChanged(id, STATUS_UNREGISTERED);
				count ++;
			} while (subscriptions.moveToNext());
			Log.i("Subscriber.unsubscribeA", "Unsubscribed from " + count + " sources");
		}
		subscriptions.close();
	}
	
	public void startSubscription(long id, String address, String password) {
		startSubscription(SubscriberThread.create(this, id, address, password));
	}
	
	public void startSubscription(long id, InetAddress[] addresses, int port, String password) {		
		startSubscription(new SubscriberThread(this, id, addresses, port, password));
	}
	
	private void startSubscription(SubscriberThread subscriber) {
		long id = subscriber.getSubscriptionId();
		getDatabase().updateSubscription(id, STATUS_REGISTERING);
		onSubscriptionStatusChanged(id, STATUS_REGISTERING);
		
		subscriber.start();
	}
	
	private void subscribeToService(ServiceInfo service) {
		String name = service.getName();
		InetAddress[] addresses = service.getInetAddresses();
		if (addresses.length == 0) {
			return;
		}
		Cursor cursor = getDatabase().getZeroConfSubscription(name);
		if (cursor.moveToFirst()) {
			// We have an active subscription to this service
			final int ID_COLUMN = cursor.getColumnIndex(Database.KEY_ROWID);			
			final int PASSWORD_COLUMN = cursor.getColumnIndex(Database.KEY_PASSWORD);
			long id = cursor.getLong(ID_COLUMN);
			String password = cursor.getString(PASSWORD_COLUMN);
			Log.i("Subscriber.subscribeToS", "Subscribing to ZeroConf service " +
					"\"" + name + "\" with subscription ID " + id);
			startSubscription(id, addresses, service.getPort(), password);
		}
		cursor.close();
	}
	 
	private void unsubscribeFromService(ServiceInfo service) {
		String name = service.getName();
		Cursor cursor = getDatabase().getZeroConfSubscription(name);
		if (cursor.moveToFirst()) {
			// We have an active subscription to this service
			final int ID_COLUMN = cursor.getColumnIndex(Database.KEY_ROWID);
			int id = cursor.getInt(ID_COLUMN);
			getDatabase().updateSubscription(id, STATUS_UNREGISTERED);
		}
		cursor.close();
	}
	
	public int getActiveSubscriptions() {
		int active = 0;
		Cursor cursor = getDatabase().getSubscriptions();
		final int statusColumn = cursor.getColumnIndex(Database.KEY_STATUS);
		while (cursor.moveToNext()) {
			String status = cursor.getString(statusColumn);
			if (STATUS_REGISTERED.equals(status)) {
				active++;
			}
		}
		cursor.close();
		return active;
	}
	
	public void onSubscriptionComplete(SubscriberThread subscribe, Exception error) {
		long id = subscribe.getSubscriptionId();
		GntpError errorCode = null;
		if (error instanceof GntpException) {
			errorCode = ((GntpException)error).Error;
		}
		
		String status;
		if (error == null) {
			status = STATUS_REGISTERED;
			Log.i("Subscriber.onSubscripti", "Subscription " + id + " was successfully renewed");
		} else if (error instanceof UnknownHostException) {
			status = STATUS_UNKNOWN_HOST;
			Log.i("Subscriber.onSubscripti", "Subscription " + id + " failed due to an unknown host");
		} else if (errorCode == GntpError.NotAuthorized) {
			status = STATUS_NOT_AUTHORIZED;
			Log.i("Subscriber.onSubscripti", "Subscription " + id + " failed due to bad password");
		} else {
			status = STATUS_UNREGISTERED;
			Log.i("Subscriber.onSubscripti", "Subscription " + id + " failed: " + error.toString());
		}
		
		getDatabase().updateSubscription(id, status);
		
		onSubscriptionStatusChanged(id, status);
	}
	
	public void onSubscriptionStatusChanged(long id, String status) {
	}
	
	protected void finalize() throws Throwable {
		stop();
		super.finalize();
	}

	public void serviceAdded(ServiceInfo service, ServiceEvent event) {
		Log.d("Subscriber.serviceAdded", "Service: " + service.getName());
		subscribeToService(service);
	}

	public void serviceRemoved(ServiceInfo service, ServiceEvent event) {
		Log.d("Subscriber.serviceRemov", "Service: " + service.getName());
		unsubscribeFromService(service);
	}
}
