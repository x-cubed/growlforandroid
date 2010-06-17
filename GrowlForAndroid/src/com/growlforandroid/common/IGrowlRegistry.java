package com.growlforandroid.common;

import java.io.File;
import java.net.URL;

import com.growlforandroid.gntp.HashAlgorithm;

import android.content.Context;
import android.graphics.drawable.Drawable;

/**
 * A repository of known Growl applications
 */
public interface IGrowlRegistry {
	GrowlApplication registerApplication(String name, URL icon);
	GrowlApplication getApplication(String name);
	Drawable getIcon(URL icon);
	void registerResource(GrowlResource resource);
	void displayNotification(GrowlNotification notification);
	NotificationType getNotificationType(GrowlApplication application,	String typeName);
	NotificationType registerNotificationType(GrowlApplication application, String typeName, String displayName, boolean enabled, URL iconUrl);
	boolean requiresPassword();
	byte[] getMatchingKey(HashAlgorithm algorithm, String hash, String salt);
	File getCacheDir();
	Context getContext();
}
