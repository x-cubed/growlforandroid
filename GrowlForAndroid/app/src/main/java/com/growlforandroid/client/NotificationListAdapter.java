package com.growlforandroid.client;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

import com.growlforandroid.common.GrowlNotification;
import com.growlforandroid.common.IGrowlRegistry;
import com.growlforandroid.common.Utility;

/***
 * Create notification views from a cursor of notification history.
 * @author Carey
 *
 */
public class NotificationListAdapter extends BaseAdapter implements ListAdapter {
	private final Context _context;
	private final LayoutInflater _inflater;
	private final IGrowlRegistry _registry;
	private final int _limit;
	private List<GrowlNotification> _notifications;

	public NotificationListAdapter(Context context, LayoutInflater inflater, IGrowlRegistry registry, int limit) {
		_context = context;
		_inflater = inflater;
		_registry = registry;
		_limit = limit;
		
		refresh();
	}

	public void refresh() {
		_notifications = _registry.getNotificationHistory(_limit);
		notifyDataSetChanged();
	}

	public int getCount() {
		return _notifications.size();
	}

	public Object getItem(int index) {
		return _notifications.get(index);
	}

	public long getItemId(int index) {
		return _notifications.get(index).getId();
	}

	public View getView(int position, View convertView, ViewGroup viewGroup) {
		View child;
		if (convertView != null) {
			child = convertView;
		} else {
			child = _inflater.inflate(R.layout.history_list_item, viewGroup, false);
		}

		GrowlNotification notification = _notifications.get(position);

		Bitmap icon = notification.getIcon();
		if (icon == null) {
			// Default icon
			icon = BitmapFactory.decodeResource(_context.getResources(), R.drawable.launcher);
		}
		
		Utility.setImage(child, R.id.imgNotificationIcon, icon);
		Utility.setText(child, R.id.txtNotificationTitle, notification.getTitle());
		Utility.setText(child, R.id.txtNotificationMessage, notification.getMessage());
		Utility.setText(child, R.id.txtNotificationApp, notification.getType().Application.getName());

		return child;
	}
}
