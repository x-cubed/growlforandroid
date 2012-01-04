package com.growlforandroid.client;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

import com.growlforandroid.common.Database;
import com.growlforandroid.common.GrowlNotification;
import com.growlforandroid.common.IGrowlRegistry;
import com.growlforandroid.common.NotificationType;
import com.growlforandroid.common.Utility;

public class NotificationListAdapter extends BaseAdapter implements ListAdapter {
	private final Context _context;
	private final LayoutInflater _inflater;
	private final IGrowlRegistry _registry;
	private final Cursor _cursor;
	private List<GrowlNotification> _notifications;

	public NotificationListAdapter(Context context, LayoutInflater inflater, IGrowlRegistry registry, Cursor cursor) {
		_context = context;
		_inflater = inflater;
		_registry = registry;
		_cursor = cursor;

		_notifications = new ArrayList<GrowlNotification>();

		loadFromCursor();
	}

	public void loadFromCursor() {
		synchronized (_notifications) {
			_notifications.clear();
			if (!_cursor.moveToFirst()) {
				return;
			}
			do {
				GrowlNotification notification = fromCursor(_cursor);
				_notifications.add(notification);
			} while (_cursor.moveToNext());
		}
	}

	private GrowlNotification fromCursor(Cursor cursor) {
		final int ID_COLUMN = cursor.getColumnIndex(Database.KEY_ROWID);
		final int TYPE_ID_COLUMN = cursor.getColumnIndex(Database.KEY_TYPE_ID);
		final int TITLE_COLUMN = cursor.getColumnIndex(Database.KEY_TITLE);
		final int MESSAGE_COLUMN = cursor.getColumnIndex(Database.KEY_MESSAGE);
		final int ICON_URL_COLUMN = cursor.getColumnIndex(Database.KEY_ICON_URL);
		final int ORIGIN_COLUMN = cursor.getColumnIndex(Database.KEY_ORIGIN);
		final int RECEIVED_AT_COLUMN = cursor.getColumnIndex(Database.KEY_RECEIVED_AT);

		int id = cursor.getInt(ID_COLUMN);
		int typeId = cursor.getInt(TYPE_ID_COLUMN);
		String title = cursor.getString(TITLE_COLUMN);
		String message = cursor.getString(MESSAGE_COLUMN);
		String origin = cursor.getString(ORIGIN_COLUMN);
		long receivedAtMS = cursor.getLong(RECEIVED_AT_COLUMN);

		String icon = cursor.getString(ICON_URL_COLUMN);
		URL iconUrl = null;
		try {
			iconUrl = icon == null ? null : new URL(icon);
		} catch (Exception x) {
			x.printStackTrace();
		}

		NotificationType type = _registry.getNotificationType(typeId);
		return new GrowlNotification(id, type, title, message, iconUrl, origin, receivedAtMS);
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

		Utility.setImage(child, R.id.imgNotificationIcon, notification.getIcon(_context));
		Utility.setText(child, R.id.txtNotificationTitle, notification.getTitle());
		Utility.setText(child, R.id.txtNotificationMessage, notification.getMessage());
		Utility.setText(child, R.id.txtNotificationApp, notification.getType().Application.getName());

		return child;
	}
}
