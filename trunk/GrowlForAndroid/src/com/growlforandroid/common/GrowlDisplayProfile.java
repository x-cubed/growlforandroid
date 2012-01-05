package com.growlforandroid.common;

import java.net.URL;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.*;

import com.growlforandroid.client.R;

public class GrowlDisplayProfile {
	private static final int LED_COLOUR = 0xffffff00;
	private static final int LED_OFF_MS = 100;
	private static final int LED_ON_MS = 900;
	
	private final int _id;
	private final String _name;
	private final boolean _shouldLog;
	private final Integer _statusBarDefaults;
	private final Integer _statusBarFlags;
	private final Integer _toastFlags;
	private final URL _alertUrl;
	
	public GrowlDisplayProfile(int id, String name, boolean shouldLog, Integer statusBarDefaults, Integer statusBarFlags, Integer toastFlags, URL alert) {
		_id = id;
		_name = name;
		_shouldLog = shouldLog;
		_statusBarDefaults = statusBarDefaults;
		_statusBarFlags = statusBarFlags;
		_toastFlags = toastFlags;
		_alertUrl = alert;
	}
	
	public int getId() {
		return _id;
	}
	
	public String getName() {
		return _name;
	}
	
	public boolean shouldLog() {
		return _shouldLog;
	}
	
	public boolean showInStatusBar() {
		return _statusBarFlags != null;
	}
	
	public int getStatusBarFlags() {
		return _statusBarFlags;
	}
		
	public URL getAlertSound() {
		return _alertUrl;
	}

	public boolean showAsToast() {
		return _toastFlags != null;
	}
	
	public int getToastFlags() {
		return _toastFlags;
	}
	
	public void displayNotification(Context context, GrowlNotification notification, Intent intent) {
		if (showInStatusBar()) {
			Log.i("GrowlDisplayProfile.displayNotification", "Displaying in status bar");
			displayNotificationInStatusBar(context, notification, intent);
		}
		if (showAsToast()) {
			Log.i("GrowlDisplayProfile.displayNotification", "Displaying toast");
			displayNotificationAsToast(context, notification);
		}
	}
	
	private void displayNotificationInStatusBar(Context context, GrowlNotification notification, Intent intent) {		
		String message = notification.getMessage();
		long receivedAtMS = notification.getReceivedAtMS();
		
		Notification statusBarPanel = new Notification(R.drawable.statusbar_enabled, message, receivedAtMS);
		
        // Determine the display options
		int defaults = _statusBarDefaults;
		boolean defaultLights = (defaults & Notification.DEFAULT_LIGHTS) == Notification.DEFAULT_LIGHTS;
		boolean defaultSound = (defaults & Notification.DEFAULT_SOUND) == Notification.DEFAULT_SOUND;
		boolean defaultVibrate = (defaults & Notification.DEFAULT_VIBRATE) == Notification.DEFAULT_VIBRATE;
				
		if (defaultVibrate) {
			defaults |= Notification.DEFAULT_VIBRATE;
			statusBarPanel.defaults |= Notification.DEFAULT_VIBRATE;
		}
		
		if (defaultSound) {
			defaults |= Notification.DEFAULT_SOUND;
			if (_alertUrl == null) {
				// Use the default alert sound
				statusBarPanel.defaults |= Notification.DEFAULT_SOUND;
			} else {
				// Don't use the default sound, we've got a custom one we can use
				String alertUrl = _alertUrl.toString();
				Log.i("GrowlDisplayProfile.displayNotificationInStatusBar", "Alert Sound: " + alertUrl);
				statusBarPanel.sound = Uri.parse(alertUrl);
			}
		}

		if (defaultLights) {
			// We don't use the default LED settings, we flash in yellow instead
			defaults |= Notification.DEFAULT_LIGHTS;
			defaults &= Notification.FLAG_SHOW_LIGHTS;
		}
		
		Log.i("GrowlDisplayProfile.displayNotificationInStatusBar",
				"Profile: " + _id + ", " +
				"Default Lights: " + defaultLights + ", " +
				"Default Sound: " + defaultSound + ", " +
				"Default Vibrate: " + defaultVibrate);
		
		statusBarPanel.ledARGB = LED_COLOUR;
        statusBarPanel.ledOffMS = LED_OFF_MS;
        statusBarPanel.ledOnMS = LED_ON_MS;
        statusBarPanel.flags = _statusBarFlags;
        
        // Use our custom layout for the notification panel, so that we can insert the application icon
        statusBarPanel.contentView = createNotificationView(context, notification);

        // The PendingIntent to launch our activity if the user selects this notification
        statusBarPanel.contentIntent = PendingIntent.getActivity(context, 0, intent, 0);

        // Send the notification to the status bar
        GrowlApplication app = notification.getType().Application;
        NotificationManager notifyMgr = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notifyMgr.notify(app.getId(), statusBarPanel);
	}

	private RemoteViews createNotificationView(Context context, GrowlNotification notification) {
		GrowlApplication app = notification.getType().Application;
		
		Bitmap icon = notification.getIcon();
		if (icon == null) {
			// Default icon
			icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.launcher);
		}
		
		RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification_list_item);
        contentView.setImageViewBitmap(R.id.imgNotificationIcon, icon);
        
        // Title
        contentView.setTextViewText(R.id.txtNotificationTitle, notification.getTitle());
        contentView.setTextColor(R.id.txtNotificationTitle, AndroidNotificationStyle.getTitleTextColor());
        contentView.setFloat(R.id.txtNotificationTitle, "setTextSize", AndroidNotificationStyle.getTitleTextSize());
        
        // Message
        contentView.setTextViewText(R.id.txtNotificationMessage, notification.getMessage().replace("\n", " / "));
        contentView.setTextColor(R.id.txtNotificationMessage, AndroidNotificationStyle.getMessageTextColor());
        contentView.setFloat(R.id.txtNotificationTitle, "setTextSize", AndroidNotificationStyle.getMessageTextSize());
        
        // Application Name
        contentView.setTextViewText(R.id.txtNotificationApp, app.getName());
        contentView.setTextColor(R.id.txtNotificationApp, AndroidNotificationStyle.getMessageTextColor());
        contentView.setFloat(R.id.txtNotificationTitle, "setTextSize", AndroidNotificationStyle.getMessageTextSize());
		return contentView;
	}

	private void displayNotificationAsToast(Context context, GrowlNotification notification) {
		GrowlApplication app = notification.getType().Application;
		String title = notification.getTitle();
		String message = notification.getMessage();
		
        // FIXME: Find a way to enqueue the displaying of the Toast on the UI threads Looper
        final String toastText = "Growl from " + app.getName() + "\n\n" + title + "\n" + message;
       	Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG);
        toast.show();	
	}

}
