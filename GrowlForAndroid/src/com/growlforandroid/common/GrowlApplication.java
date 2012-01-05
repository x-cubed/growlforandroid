package com.growlforandroid.common;

import java.net.URL;

import android.graphics.Bitmap;
import android.util.Log;

/**
 * Represents a client application that provides Growl notifications
 * 
 * @author Carey Bishop
 *
 */
public class GrowlApplication {
	private final int _id;
	private final IGrowlRegistry _registry;
	private final String _name;
	
	private Boolean _enabled;
	private URL _iconUrl;
	private Integer _displayId;
	
	public GrowlApplication(IGrowlRegistry registry, int id, String name, Boolean enabled, URL icon, Integer displayId) {
		_id = id;
		_registry = registry; 
		_name = name;
		_enabled = enabled;
		_iconUrl = icon;
		_displayId = displayId;
	}
	
	public boolean isEnabled() {
		return _enabled;
	}
	
	public Bitmap getIcon() {
		return _registry.getIcon(_iconUrl);
	}
	
	public int getId() {
		return _id;
	}
	
	public Integer getDisplayId() {
		return _displayId;
	}
	
	public String getName() {
		return _name;
	}
	
	public IGrowlRegistry getRegistry() {
		return _registry;
	}
	
	// Registers a new notification type, or updates an existing one with the same type name
	public NotificationType registerNotificationType(String typeName, String displayName, boolean enabled, URL iconUrl) {
		NotificationType oldType = _registry.getNotificationType(this, typeName);
		if (oldType != null) {
			Log.i("Application.registerNotificationType", "Application \"" + getName() + "\" is re-registering notification type \"" + typeName + "\"");
			oldType.setDisplayName(displayName);
			// Enabled value shouldn't change, as this is a user option
			oldType.setIconUrl(iconUrl);
			return oldType;
		} else {
			Log.i("Application.registerNotificationType", "Application \"" + getName() + "\" is registering notification type \"" + typeName + "\"");
			return _registry.registerNotificationType(this, typeName, displayName, enabled, iconUrl);
		}
	}
	
	public NotificationType getNotificationType(String typeName) {
		return _registry.getNotificationType(this, typeName);
	}
}
