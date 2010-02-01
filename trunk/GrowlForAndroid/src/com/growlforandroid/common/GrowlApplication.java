package com.growlforandroid.common;

import java.net.URL;
import java.util.*;


import android.graphics.drawable.Drawable;
import android.util.Log;

public class GrowlApplication {
	private static int _lastId = 0;
	
	public final int ID;
	private final IGrowlRegistry _registry;
	private final Map<String, NotificationType> _notificationTypes = new HashMap<String, NotificationType>();
	
	public final String Name;
	public URL IconUrl;
	
	public GrowlApplication(IGrowlRegistry registry, String name, URL icon) {
		ID = _lastId++;
		_registry = registry; 
		Name = name;
		IconUrl = icon;
	}
	
	public Drawable getIcon() {
		return _registry.getIcon(IconUrl);
	}

	// Registers a new notification type, or updates an existing one with the same type name
	public NotificationType registerNotificationType(String typeName, String displayName, boolean enabled, URL iconUrl) {
		NotificationType oldType = _notificationTypes.get(typeName);
		if (oldType != null) {
			Log.i("Application.registerNotificationType", "Application \"" + Name + "\" is re-registering notification type \"" + typeName + "\"");
			oldType.setDisplayName(displayName);
			oldType.setEnabled(enabled);
			oldType.setIconUrl(iconUrl);
			return oldType;
		} else {
			Log.i("Application.registerNotificationType", "Application \"" + Name + "\" is registering notification type \"" + typeName + "\"");
			NotificationType newType = new NotificationType(this, typeName, displayName, enabled, iconUrl);
			_notificationTypes.put(typeName, newType);
			return newType;
		}
	}
	
	public NotificationType getNotificationType(String typeName) {
		return _notificationTypes.get(typeName);
	}
}
