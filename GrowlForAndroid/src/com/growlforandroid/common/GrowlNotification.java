package com.growlforandroid.common;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.growlforandroid.client.R;
import com.growlforandroid.gntp.Constants;

public class GrowlNotification {
	private final NotificationType _type;
	private final String _id;
	private final String _title;
	private final String _text;
	private final URL _iconUrl;
	private final String _origin;
	private final Map<String, GrowlResource> _resources;
	private final long _receivedAtMS;
	
	public GrowlNotification(NotificationType type, String id, String title, String text, URL icon) {
		_id = id;
		_type = type;
		_title = title;
		_text = text;
		_iconUrl = icon;
		
		_resources = new HashMap<String, GrowlResource>();
		_origin = null;
		_receivedAtMS = System.currentTimeMillis();
	}
	
	public GrowlNotification(NotificationType type,	Map<String, String> headers, Map<String, GrowlResource> resources, long receivedAtMS)
		throws MalformedURLException {
		
		_id = headers.get(Constants.HEADER_NOTIFICATION_ID);
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
	
	public String getId() {
		return _id;
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
	
	public Drawable getIcon(Context context) {
		//URL source = _iconUrl != null ? _iconUrl : _type.getIconUrl();
		// FIXME: resolve source
		return context.getResources().getDrawable(R.drawable.icon);
	}

	public String getOrigin() {
		return _origin;
	}
}
