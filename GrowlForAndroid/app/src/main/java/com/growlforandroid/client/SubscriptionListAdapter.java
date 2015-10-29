package com.growlforandroid.client;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import javax.jmdns.ServiceInfo;

import android.util.Log;
import android.view.*;
import android.widget.*;

import com.growlforandroid.common.*;

/***
 * Provides the list of active and available subscriptions for the Subscriptions
 * activity
 * 
 * @author Carey
 * 
 */
public class SubscriptionListAdapter extends BaseAdapter implements ListAdapter {
	private final LayoutInflater _inflater;
	private List<Subscription> _subscriptions;

	public SubscriptionListAdapter(LayoutInflater inflater) {
		_inflater = inflater;
		_subscriptions = new ArrayList<Subscription>();
	}

	public void addServices(ServiceInfo[] services, String status) {
		for (int i = 0; i < services.length; i++) {
			addService(services[i], status);
		}
	}

	public void addService(ServiceInfo service, String status) {
		Subscription subscription = new Subscription(service, status);
		if (subscription.isValid()) {
			InetAddress[] addresses = subscription.getInetAddresses();

			boolean isUnique = true;
			for (Subscription previouslySeen : _subscriptions) {
				if (previouslySeen.isZeroConf() && previouslySeen.getName().equals(subscription.getName())) {
					Log.d("SubscriptionListAdapter.addService", "Subscription to " + subscription.getName()
							+ " matches existing ZeroConf subscription");
					isUnique = false;
					break;
				} else if (previouslySeen.matchesAny(addresses)) {
					Log.d("SubscriptionListAdapter.addService", "Subscription to " + subscription.getName()
							+ " matches existing manual subscription");
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
			for (Subscription subscription : _subscriptions) {
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
			child = _inflater.inflate(R.layout.history_list_item, viewGroup, false);
		}

		Subscription subscription = _subscriptions.get(position);
		Utility.setText(child, R.id.txtNotificationTitle, subscription.getName());
		Utility.setText(child, R.id.txtNotificationMessage, subscription.getStatus());
		Utility.setText(child, R.id.txtNotificationApp, subscription.getAddress());

		return child;
	}
}
