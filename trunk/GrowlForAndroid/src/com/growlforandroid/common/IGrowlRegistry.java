package com.growlforandroid.common;

import java.net.URL;

import com.growlforandroid.gntp.HashAlgorithm;


import android.graphics.drawable.Drawable;

/**
 * A repository of known Growl applications
 */
public interface IGrowlRegistry {
	GrowlApplication registerApplication(String name, URL icon);
	GrowlApplication getApplication(String name);
	Drawable getIcon(URL icon);
	void registerResource(GrowlResource resource);
	void displayNotification(NotificationType type, String ID, String title, String text, URL icon);
	NotificationType getNotificationType(GrowlApplication application,	String typeName);
	NotificationType registerNotificationType(GrowlApplication application, String typeName, String displayName, boolean enabled, URL iconUrl);
	boolean requiresPassword();
	boolean isValidHash(HashAlgorithm algorithm, String hash, String salt);
}
