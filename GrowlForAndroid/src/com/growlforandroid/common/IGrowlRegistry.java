package com.growlforandroid.common;

import java.net.URL;


import android.graphics.drawable.Drawable;

/*
 * A repository of known Growl applications
 */
public interface IGrowlRegistry {
	GrowlApplication registerApplication(String name, URL icon);
	GrowlApplication getApplication(String name);
	Drawable getIcon(URL icon);
	void displayNotification(NotificationType type, String ID, String title, String text, URL icon);
}
