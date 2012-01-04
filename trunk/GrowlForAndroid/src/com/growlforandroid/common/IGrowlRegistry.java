package com.growlforandroid.common;

import java.io.File;
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

	NotificationType getNotificationType(int id);

	NotificationType getNotificationType(GrowlApplication application, String typeName);

	NotificationType registerNotificationType(GrowlApplication application, String typeName, String displayName,
			boolean enabled, URL iconUrl);

	boolean requiresPassword();

	byte[] getMatchingKey(String subscriberId, HashAlgorithm algorithm, String hash, String salt);

	File getCacheDir();

	void addEventHandler(EventHandler handler);
	
	void removeEventHandler(EventHandler handler);
	
	public interface EventHandler {
		void onNotificationTypeRegistered(NotificationType type);

		void onApplicationRegistered(GrowlApplication app);
	}
}
