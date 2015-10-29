package com.growlforandroid.client;

import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

import com.growlforandroid.common.GrowlApplication;
import com.growlforandroid.common.IGrowlRegistry;
import com.growlforandroid.common.NotificationType;
import com.growlforandroid.common.Utility;

public class TypeListAdapter extends BaseAdapter implements ListAdapter {
	private final Context _context;
	private final LayoutInflater _inflater;
	private final IGrowlRegistry _registry;
	private final GrowlApplication _application;
	private List<NotificationType> _types;

	public TypeListAdapter(Context context, LayoutInflater inflater, IGrowlRegistry registry, GrowlApplication application) {
		_context = context;
		_inflater = inflater;
		_registry = registry;
		_application = application;
		refresh();
	}

	public void refresh() {
		_types = _registry.getNotificationTypes(_application); 
		notifyDataSetChanged();
	}
	
	public int getCount() {
		return _types.size();
	}

	public Object getItem(int index) {
		return _types.get(index);
	}

	public long getItemId(int index) {
		return _types.get(index).getId();
	}

	public View getView(int position, View convertView, ViewGroup viewGroup) {
		View child;
		if (convertView != null) {
			child = convertView;
		} else {
			child = _inflater.inflate(R.layout.history_list_item, viewGroup, false);
		}

		Resources resources = _context.getResources();
		
		NotificationType type = _types.get(position);
		Bitmap icon = type.getIcon();
		if (icon == null) {
			// Default icon
			icon = BitmapFactory.decodeResource(resources, R.drawable.launcher);
		}
		
		String displayProfileName = _registry.getDisplayProfileName(type.getDisplayId());
		if (displayProfileName == null) {
			displayProfileName = resources.getText(R.string.type_display_default).toString();
		}
		
		int enabledId = type.isEnabled() ?
				R.string.application_option_enabled : R.string.application_option_disabled;
		String isEnabled = resources.getString(enabledId);
		
		Utility.setImage(child, R.id.imgNotificationIcon, icon);
		Utility.setText(child, R.id.txtNotificationTitle, type.getDisplayName());
		Utility.setText(child, R.id.txtNotificationMessage, displayProfileName);
		Utility.setText(child, R.id.txtNotificationApp, isEnabled);

		return child;
	}
}