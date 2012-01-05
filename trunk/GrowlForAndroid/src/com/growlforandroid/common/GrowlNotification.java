package com.growlforandroid.common;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import android.graphics.Bitmap;

import com.growlforandroid.gntp.Constants;

public class GrowlNotification {
	private final NotificationType _type;
	private final String _notificationId;
	private final String _title;
	private final String _text;
	private final URL _iconUrl;
	private final String _origin;
	private final Map<String, GrowlResource> _resources;
	private final long _receivedAtMS;
	private long _id = -1;
	
	public GrowlNotification(long id, NotificationType type, String title, String text, URL icon, String origin, long receivedAtMS) {
		_id = id;
		_notificationId = Long.toString(id);
		_type = type;
		_title = title;
		_text = text;
		_iconUrl = icon;
		
		_resources = new HashMap<String, GrowlResource>();
		_origin = origin;
		_receivedAtMS = receivedAtMS;
	}
	
	public GrowlNotification(NotificationType type,	Map<String, String> headers, Map<String, GrowlResource> resources, long receivedAtMS)
		throws MalformedURLException {
		
		_notificationId = headers.get(Constants.HEADER_NOTIFICATION_ID);
		_type = type;
		
		String icon = headers.get(Constants.HEADER_NOTIFICATION_ICON);
		_iconUrl = (icon != null) ? new URL(icon) : null;
		
		_title = headers.get(Constants.HEADER_NOTIFICATION_TITLE);
		_text = headers.get(Constants.HEADER_NOTIFICATION_TEXT);
		_origin = headers.get(Constants.HEADER_NOTIFICATION_ORIGIN);
		_resources = resources;
		_receivedAtMS = receivedAtMS;
	}

	public NotificationType getType() {
		return _type;
	}

	public String getMessage() {
		return _text;
	}
	
	public String getTitle() {
		return _title;
	}
	
	public long getId() {
		return _id;
	}
	
	public void setId(long id) {
		_id = id;
	}
	
	public String getNotificationId() {
		return _notificationId;
	}
	
	public URL getIconUrl() {
		return _iconUrl;
	}
	
	public long getReceivedAtMS() {
		return _receivedAtMS;
	}
	
	public GrowlResource getResource(String identifier) {
		return _resources.get(identifier);
	}
	
	public void putResource(GrowlResource resource) {
		_resources.put(resource.getIdentifier(), resource);
	}
	
	public Bitmap getIcon() {
		IGrowlRegistry registry = _type.Application.getRegistry();
		Bitmap icon = registry.getIcon(_iconUrl);
		if (icon == null) {
			icon = _type.getIcon();
		}
		return icon;
	}

	public String getOrigin() {
		return _origin;
	}
}
