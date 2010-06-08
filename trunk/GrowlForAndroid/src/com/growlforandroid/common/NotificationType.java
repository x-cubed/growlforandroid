package com.growlforandroid.common;

import java.net.URL;


public class NotificationType {
	public final int ID;
	public final GrowlApplication Application;
	public final String TypeName;
	private String _displayName;
	private boolean _enabled;
	private URL _iconUrl;
	
	public NotificationType(int id, GrowlApplication application, String typeName, String displayName, boolean enabled, URL iconUrl) {
		ID = id;
		Application = application;
		TypeName = typeName;
		_displayName = displayName;
		_enabled = enabled;
		_iconUrl = iconUrl;
	}
	
	public String getDisplayName() {
		return _displayName;
	}
	
	public void setDisplayName(String displayName) {
		_displayName = displayName;
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
}
