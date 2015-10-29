package com.growlforandroid.common;

import java.net.URL;
import java.util.List;
import java.util.Map;

import com.growlforandroid.gntp.HashAlgorithm;

import android.graphics.Bitmap;

/**
 * A repository of known Growl applications
 */
public interface IGrowlRegistry {
	GrowlApplication registerApplication(String name, URL icon);

	GrowlApplication getApplication(long id);
	
	GrowlApplication getApplication(String name);

	List<GrowlApplication> getApplications();

	String getDisplayProfileName(Integer profileId);
	
	Bitmap getIcon(URL icon);

	GrowlResource registerResource(Map<String, String> headers);
	
	NotificationType getNotificationType(int id);

	NotificationType getNotificationType(GrowlApplication application, String typeName);
	
	List<NotificationType> getNotificationTypes(GrowlApplication application);

	NotificationType registerNotificationType(GrowlApplication application, String typeName, String displayName,
			boolean enabled, URL iconUrl);
	
	GrowlNotification getNotificationFromHistory(long id);
	
	List<GrowlNotification> getNotificationHistory(int limit);

	byte[] getMatchingKey(String subscriberId, HashAlgorithm algorithm, String hash, String salt);

	void addEventHandler(EventHandler handler);

	void removeEventHandler(EventHandler handler);

	public interface EventHandler {
		void onNotificationTypeRegistered(NotificationType type);

		void onApplicationRegistered(GrowlApplication app);
	}
}
