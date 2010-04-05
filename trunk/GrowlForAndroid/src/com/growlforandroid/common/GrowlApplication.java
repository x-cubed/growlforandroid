package com.growlforandroid.common;

import java.net.URL;
import java.util.*;


import android.graphics.drawable.Drawable;
import android.util.Log;

/**
 * Represents a client application that provides Growl notifications
 * 
 * @author Carey Bishop
 *
 */
public class GrowlApplication {
	public final int ID;
	private final IGrowlRegistry _registry;
	
	public final String Name;
	public Boolean Enabled;
	public URL IconUrl;
	
	public GrowlApplication(IGrowlRegistry registry, int id, String name, Boolean enabled, URL icon) {
		ID = id;
		_registry = registry; 
		Name = name;
		Enabled = enabled;
		IconUrl = icon;
	}
	
	public Drawable getIcon() {
		return _registry.getIcon(IconUrl);
	}

	// Registers a new notification type, or updates an existing one with the same type name
	public NotificationType registerNotificationType(String typeName, String displayName, boolean enabled, URL iconUrl) {
		NotificationType oldType = _registry.getNotificationType(this, typeName);
		if (oldType != null) {
			Log.i("Application.registerNotificationType", "Application \"" + Name + "\" is re-registering notification type \"" + typeName + "\"");
			oldType.setDisplayName(displayName);
			// Enabled value shouldn't change, as this is a user option
			oldType.setIconUrl(iconUrl);
			return oldType;
		} else {
			Log.i("Application.registerNotificationType", "Application \"" + Name + "\" is registering notification type \"" + typeName + "\"");
			return _registry.registerNotificationType(this, typeName, displayName, enabled, iconUrl);
		}
	}
	
	public NotificationType getNotificationType(String typeName) {
		return _registry.getNotificationType(this, typeName);
	}
}
