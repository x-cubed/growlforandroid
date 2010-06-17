package com.growlforandroid.common;

import java.net.UnknownHostException;
import java.util.*;

import com.growlforandroid.client.R;
import com.growlforandroid.gntp.GntpError;
import com.growlforandroid.gntp.GntpException;
import com.growlforandroid.gntp.SubscriberThread;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;

public class Subscriber {
	public static final String PREFERENCE_SUBSCRIBER_ID = "subscriber_id";
	
	private final int MINUTES = 60 * 1000; // in milliseconds
	private final int SUBSCRIPTION_INTERVAL_MS = 2 * MINUTES;
	
	private final String STATUS_UNREGISTERED;
	private final String STATUS_REGISTERING;
	private final String STATUS_REGISTERED;
	private final String STATUS_NOT_AUTHORIZED;
	private final String STATUS_UNKNOWN_HOST;

	private final Context _context;
	private final UUID _id;
	private Database _database;
	private Timer _timer;
	
	public Subscriber(Context context) {
		_context = context;

		// Cache the status labels
		STATUS_UNREGISTERED = _context.getText(R.string.subscriptions_status_unregistered).toString();
		STATUS_REGISTERING = _context.getText(R.string.subscriptions_status_registering).toString();
		STATUS_REGISTERED = _context.getText(R.string.subscriptions_status_registered).toString();
		STATUS_NOT_AUTHORIZED = _context.getText(R.string.subscriptions_status_not_authorized).toString();
		STATUS_UNKNOWN_HOST = _context.getText(R.string.subscriptions_status_unknown_host).toString();
		
		// Determine the subscriber ID to use
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_context);
		String uuid = prefs.getString(PREFERENCE_SUBSCRIBER_ID, null);
		UUID id = null;
		if (uuid != null) {
			// We have a subscriber ID from last time
			try {
				id = UUID.fromString(uuid);
				Log.i("Subscriber.ctor", "Using existing subscriber ID: " + id.toString());
			} catch (IllegalArgumentException iae) {
			}
		}
		if (id == null) {
			// Generate a new subscriber ID and save it in the preferences for next time
			id = UUID.randomUUID();
			Editor editor = prefs.edit();
			editor.putString(PREFERENCE_SUBSCRIBER_ID, id.toString());
			editor.commit();
			Log.i("Subscriber.ctor", "Created new subscriber ID: " + id.toString());
		}
		
		_id = id;
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
		return Utility.getDeviceFriendlyName();
	}

	public void start() {
		Log.i("Subscriber.start", "Starting the subscriber " + _id + "...");
		_database = new Database(_context);
		_timer = new Timer();
		_timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				subscribeNow();
			}
		}, 0, SUBSCRIPTION_INTERVAL_MS);
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

			// Marker all subscriptions as unregistered
			unsubscribeAll();
			
			_database.close();
			_database = null;
		}
	}
	
	public void subscribeNow() {
		Cursor subscriptions = _database.getSubscriptions();
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
			Log.i("Subscriber.subscribeNow", "Subscribing to " + count + " sources");
		} else {
			// No current subscriptions
			Log.i("Subscriber.subscribeNow", "No current subscriptions, stopping");
			stop();
		}
		subscriptions.close();
	}
	
	public void unsubscribeAll() {
		Cursor subscriptions = _database.getSubscriptions();
		if (subscriptions.moveToFirst()) {
			final int ID_COLUMN = subscriptions.getColumnIndex(Database.KEY_ROWID);		
			int count = 0;
			do {
				long id = subscriptions.getLong(ID_COLUMN);
				_database.updateSubscription(id, STATUS_UNREGISTERED);
				onSubscriptionStatusChanged(id, STATUS_UNREGISTERED);
				count ++;
			} while (subscriptions.moveToNext());
			Log.i("Subscriber.unsubscribeAll", "Ubsubscribed from " + count + " sources");
		}
		subscriptions.close();
	}
	
	public void startSubscription(long id, String address, String password) {
		_database.updateSubscription(id, STATUS_REGISTERING);
		onSubscriptionStatusChanged(id, STATUS_REGISTERING);
		
		Thread subscribe = new SubscriberThread(this, id, address, password);
		subscribe.start();	
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
			Log.i("Subscriber.onSubscriptionComplete", "Subscription " + id + " was successfully renewed");
		} else if (error instanceof UnknownHostException) {
			status = STATUS_UNKNOWN_HOST;
			Log.i("Subscriber.onSubscriptionComplete", "Subscription " + id + " failed due to an unknown host");
		} else if (errorCode == GntpError.NotAuthorized) {
			status = STATUS_NOT_AUTHORIZED;
			Log.i("Subscriber.onSubscriptionComplete", "Subscription " + id + " failed due to bad password");
		} else {
			status = STATUS_UNREGISTERED;
			Log.i("Subscriber.onSubscriptionComplete", "Subscription " + id + " failed: " + error.toString());
		}
		
		if (_database != null) {
			_database.updateSubscription(id, status);
		}
		
		onSubscriptionStatusChanged(id, status);
	}
	
	public void onSubscriptionStatusChanged(long id, String status) {
	}
	
	protected void finalize() throws Throwable {
		stop();
		super.finalize();
	}
}
