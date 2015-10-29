package com.growlforandroid.common;

import java.net.URL;

import android.graphics.Bitmap;


public class NotificationType {
	public final GrowlApplication Application;
	public final String TypeName;
	private final int _id;
	private String _displayName;	
	private boolean _enabled;
	private URL _iconUrl;
	private Integer _displayId;
	
	public NotificationType(int id, GrowlApplication application, String typeName, String displayName,
			boolean enabled, URL iconUrl, Integer displayId) {
		
		_id = id;
		Application = application;
		TypeName = typeName;
		_displayName = displayName;
		_enabled = enabled;
		_iconUrl = iconUrl;
		_displayId = displayId;
	}
	
	public int getId() {
		return _id;
	}
	
	public String getDisplayName() {
		return _displayName;
	}
	
	public void setDisplayName(String displayName) {
		_displayName = displayName;
	}

	public Integer getDisplayId() {
		if (_displayId != null)
			return _displayId;
		return Application.getDisplayId();
	}
	
	public boolean isEnabled() {
		return _enabled;
	}
	
	public void setEnabled(boolean enabled) {
		_enabled = enabled;
	}
	
	public URL getIconUrl() {
		return _iconUrl;
	}
	
	public void setIconUrl(URL iconUrl) {
		_iconUrl = iconUrl;
	}

	public Bitmap getIcon() {
		IGrowlRegistry registry = Application.getRegistry();
		Bitmap icon = registry.getIcon(_iconUrl);
		if (icon == null) {
			icon = Application.getIcon();
		}
		return icon;
	}
}
