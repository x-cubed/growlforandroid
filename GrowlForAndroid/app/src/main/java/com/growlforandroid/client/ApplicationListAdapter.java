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

/***
 * Create application views from a cursor of applications.
 * @author Carey
 *
 */
public class ApplicationListAdapter extends BaseAdapter implements ListAdapter {
	private final Context _context;
	private final LayoutInflater _inflater;
	private final IGrowlRegistry _registry;
	private List<GrowlApplication> _applications;

	public ApplicationListAdapter(Context context, LayoutInflater inflater, IGrowlRegistry registry) {
		_context = context;
		_inflater = inflater;
		_registry = registry;
		refresh();
	}

	public void refresh() {
		_applications = _registry.getApplications();
		notifyDataSetChanged();
	}
	
	public int getCount() {
		return _applications.size();
	}

	public Object getItem(int index) {
		return _applications.get(index);
	}

	public long getItemId(int index) {
		return _applications.get(index).getId();
	}

	public View getView(int position, View convertView, ViewGroup viewGroup) {
		View child;
		if (convertView != null) {
			child = convertView;
		} else {
			child = _inflater.inflate(R.layout.history_list_item, viewGroup, false);
		}

		Resources resources = _context.getResources();
		
		GrowlApplication application = _applications.get(position);
		Bitmap icon = application.getIcon();
		if (icon == null) {
			// Default icon
			icon = BitmapFactory.decodeResource(resources, R.drawable.launcher);
		}
		
		int enabledId = application.isEnabled() ?
				R.string.application_option_enabled : R.string.application_option_disabled;
		String isEnabled = resources.getString(enabledId);
		
		Utility.setImage(child, R.id.imgNotificationIcon, icon);
		Utility.setText(child, R.id.txtNotificationTitle, application.getName());
		Utility.setText(child, R.id.txtNotificationMessage, getTypes(application));
		Utility.setText(child, R.id.txtNotificationApp, isEnabled);

		return child;
	}

	private String getTypes(GrowlApplication application) {
		String types = "";
		List<NotificationType> typeList = _registry.getNotificationTypes(application);
		for(NotificationType type:typeList) {
			if (types.equals("")) {
				types = type.getDisplayName();
			} else {
				types += ", " + type.getDisplayName();
			}
		}
		return types;
	}
}
