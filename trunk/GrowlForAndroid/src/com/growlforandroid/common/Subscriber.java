package com.growlforandroid.common;

import java.util.*;

import com.growlforandroid.client.R;
import com.growlforandroid.gntp.SubscriberThread;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class Subscriber {
	private final int SUBSCRIPTION_INTERVAL_MS = 60 * 1000;

	private final Context _context;
	private final UUID _id;
	private Database _database;
	private Timer _timer;
	
	public Subscriber(Context context) {
		_context = context;
		_id = UUID.randomUUID();
	}
	
	public UUID getId() {
		return _id;
	}
	
	public String getName() {
		// TODO Auto-generated method stub
		return null;
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
	
	public void stop() {
		if (_timer != null) {
			Log.i("Subscriber.stop", "Stopping the subscriber " + _id + "...");
			_timer.cancel();
			_timer.purge();
			_timer = null;
			
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
			final String STATUS_REGISTERING = _context.getText(R.string.subscriptions_status_registering).toString();
			
			int count = 0;
			do {
				long id = subscriptions.getLong(ID_COLUMN);
				String address = subscriptions.getString(ADDRESS_COLUMN);
				String password = subscriptions.getString(PASSWORD_COLUMN);
				Thread subscribe = new SubscriberThread(this, id, address, password);
				_database.updateSubscription(id, STATUS_REGISTERING);
				subscribe.start();
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
	
	public void onSubscriptionComplete(SubscriberThread subscribe, Exception error) {
		long id = subscribe.getSubscriptionId();
		int statusId = (error == null) ? R.string.subscriptions_status_registered : R.string.subscriptions_status_unregistered;
		_database.updateSubscription(id, _context.getText(statusId).toString());
	}
	
	protected void finalize() throws Throwable {
		stop();
		super.finalize();
	}
}
