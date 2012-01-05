package com.growlforandroid.client;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.*;
import java.util.*;

import com.growlforandroid.client.R;
import com.growlforandroid.common.*;
import com.growlforandroid.gntp.*;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.*;
import android.preference.PreferenceManager;
import android.util.Log;

public class GrowlListenerService extends Service implements IGrowlService {

	private static final int SERVICE_NOTIFICATION = 0xDEADBEEF;
	private static final String PLATFORM_NAME = "Google Android";

	private static Database _database;
	private static IGrowlRegistry _registry;

	private final Set<WeakReference<StatusChangedHandler>> _statusChangedHandlers = Collections
			.synchronizedSet(new HashSet<WeakReference<StatusChangedHandler>>());

	private ZeroConf _zeroConf;
	private NotificationManager _notifyMgr;
	private SocketAcceptor _socketAcceptor;
	private Subscriber _subscriber;

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		GrowlListenerService getService() {
			return GrowlListenerService.this;
		}
	}

	@Override
	public void onCreate() {
		if (_database == null) {
			_database = new Database(this.getApplicationContext());
			_registry = new GrowlRegistry(this, _database);
		}

		initializeCommonHeaders();

		_notifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// Display a notification about us starting. We put an icon in the
		// status bar.
		showNotification();
	}

	/**
	 * Initialize the headers sent in all GNTP messages
	 */
	private void initializeCommonHeaders() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		GntpMessage.clearCommonHeaders();

		String deviceName = Preferences.getDeviceName(prefs);
		GntpMessage.addCommonHeader(Constants.HEADER_ORIGIN_MACHINE_NAME, deviceName);

		String softwareName = getText(R.string.app_name).toString();
		GntpMessage.addCommonHeader(Constants.HEADER_ORIGIN_SOFTWARE_NAME, softwareName);

		String softwareVersion = getText(R.string.app_version).toString();
		GntpMessage.addCommonHeader(Constants.HEADER_ORIGIN_SOFTWARE_VERSION, softwareVersion);

		String platformName = PLATFORM_NAME + " " + Build.VERSION.SDK + " (" + Build.DISPLAY + ")";
		GntpMessage.addCommonHeader(Constants.HEADER_ORIGIN_PLATFORM_NAME, platformName);
		GntpMessage.addCommonHeader(Constants.HEADER_ORIGIN_PLATFORM_VERSION, Build.VERSION.RELEASE);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (_socketAcceptor != null) {
			// Already started
			return START_STICKY;
		}

		// Determine the device name to use
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String deviceName = Preferences.getDeviceName(prefs);
		UUID subscriberId = Preferences.getSubscriberId(prefs);

		// Determine the text size and color to use in the status bar
		AndroidNotificationStyle.grabNotificationStyle(this);

		try {
			// Start listening on GNTP_PORT, on all interfaces
			ISocketThreadFactory factory = new GntpListenerThreadFactory(this);
			_socketAcceptor = new SocketAcceptor(this, factory, Constants.GNTP_PORT);
			_socketAcceptor.start();

			// Register the GNTP service with Bonjour/ZeroConf
			if (prefs.getBoolean(Preferences.ANNOUNCE_USING_ZEROCONF, true)) {
				initialiseZeroConf(deviceName, this);
			}

			// Start subscribing to notifications from other devices
			final GrowlListenerService service = this;
			_subscriber = new Subscriber(service, subscriberId, deviceName) {
				public void onSubscriptionStatusChanged(long id, String status) {
					// Show the number of active subscriptions in the status bar
					int active = getActiveSubscriptions();
					String serviceStatus = null;
					if (active == 1) {
						serviceStatus = getText(R.string.growl_active_subscription).toString();
					} else if (active > 1) {
						String format = getText(R.string.growl_active_subscriptions).toString();
						serviceStatus = String.format(format, active);
					}
					showNotification(serviceStatus);

					// Notify the Subscriptions activity to update
					service.onSubscriptionStatusChanged(id, status);
				}
			};
			_subscriber.start();

		} catch (IOException e) {
			e.printStackTrace();
		}

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	private void initialiseZeroConf(String deviceName, Context context) {
		_zeroConf = ZeroConf.getInstance(context);

		// Advertise the GNTP service
		String appName = context.getText(R.string.app_name).toString();
		String format = context.getText(R.string.gntp_zeroconf_name_format).toString();
		final String serviceName = String.format(format, appName, deviceName);
		_zeroConf.registerService(Constants.GNTP_ZEROCONF_SERVICE_TYPE, serviceName, Constants.GNTP_PORT,
				Constants.GNTP_ZEROCONF_TEXT);
	}

	public Context getContext() {
		return getBaseContext();
	}

	public boolean isRunning() {
		return _socketAcceptor != null;
	}

	public void subscribeNow() {
		if (_subscriber == null) {
			return;
		}
		if (_subscriber.isRunning()) {
			_subscriber.subscribeNow();
		} else {
			_subscriber.start();
		}
	}

	@Override
	public void onDestroy() {
		stop();
	}

	private void stop() {
		// Stop listening for TCP connections
		try {
			if (_subscriber != null) {
				_subscriber.stop();
				_subscriber = null;
			}

			if (_socketAcceptor != null) {
				_socketAcceptor.stopListening();
				_socketAcceptor.closeConnections();
				_socketAcceptor = null;
			}
		} catch (Exception x) {
			Log.e("GrowlListenerService.stop", x.toString());
		}

		if (_database != null) {
			_database.close();
			_database = null;
		}

		if (_zeroConf != null) {
			// This can take a while, so spin up a separate thread
			final ZeroConf zeroConf = _zeroConf;
			_zeroConf = null;
			new Thread(new Runnable() {
				public void run() {
					zeroConf.unregisterAllServices();
					zeroConf.close();
					Log.d("GrowlListenerService.stop", "ZeroConf closed");
				}
			}).start();
		}
		
		// Cancel the persistent notification.
		_notifyMgr.cancel(SERVICE_NOTIFICATION);
	}

	protected void finalize() {
		stop();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	// This is the object that receives interactions from clients. See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();

	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification() {
		showNotification(null);
	}

	private void showNotification(String status) {
		if (status == null) {
			status = getText(R.string.growl_on_status).toString();
		}

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.statusbar_disabled, getText(R.string.growl_on_status),
				System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
		notification.setLatestEventInfo(this, getText(R.string.app_name), status, contentIntent);

		// Send the notification.
		_notifyMgr.notify(SERVICE_NOTIFICATION, notification);
	}

	public GrowlDisplayProfile getDisplayProfile(Integer id, int defaultProfileId) {
		if (id == null)
			return getDisplayProfile(defaultProfileId, 0);

		GrowlDisplayProfile result = null;
		Cursor cursor = _database.getDisplayProfile(id);
		if (cursor.moveToFirst()) {
			final int nameColumn = cursor.getColumnIndex(Database.KEY_NAME);
			final int logColumn = cursor.getColumnIndex(Database.KEY_LOG);
			final int sbDefaultsColumn = cursor.getColumnIndex(Database.KEY_STATUS_BAR_DEFAULTS);
			final int sbFlagsColumn = cursor.getColumnIndex(Database.KEY_STATUS_BAR_FLAGS);
			final int toastColumn = cursor.getColumnIndex(Database.KEY_TOAST_FLAGS);
			final int alertColumn = cursor.getColumnIndex(Database.KEY_ALERT_URL);

			String name = cursor.getString(nameColumn);
			boolean shouldLog = cursor.getInt(logColumn) != 0;
			Integer sbDefaults = null;
			if (!cursor.isNull(sbDefaultsColumn)) {
				sbDefaults = cursor.getInt(sbDefaultsColumn);
			}
			Integer sbFlags = null;
			if (!cursor.isNull(sbFlagsColumn)) {
				sbFlags = cursor.getInt(sbFlagsColumn);
			}
			Integer toastFlags = null;
			if (!cursor.isNull(toastColumn)) {
				toastFlags = cursor.getInt(toastColumn);
			}

			String alert = cursor.getString(alertColumn);
			URL alertUrl = null;
			if (alert != null) {
				try {
					alertUrl = new URL(alert);
				} catch (MalformedURLException mue) {
					Log.e("GrowlListenerService.getDisplayProfile", "Failed to parse alert URL:" + alert, mue);
				}
			}

			result = new GrowlDisplayProfile(id, name, shouldLog, sbDefaults, sbFlags, toastFlags, alertUrl);
		}
		cursor.close();
		return result;
	}

	public void displayNotification(GrowlNotification notification) {
		NotificationType type = notification.getType();
		GrowlApplication app = type.Application;
		if (!type.isEnabled()) {
			Log.i("GrowlListenerService.displayNotification", "Not displaying notification from \"" + app.getName()
					+ "\" " + "of type \"" + type.TypeName + "\" as notification type is disabled");
			return;
		}
		if (!app.isEnabled()) {
			Log.i("GrowlListenerService.displayNotification", "Not displaying notification from \"" + app.getName()
					+ "\" " + "of type \"" + type.TypeName + "\" as application is disabled");
			return;
		}

		// Get the notification properties
		String title = notification.getTitle();
		String message = notification.getMessage();
		URL iconUrl = notification.getIconUrl();
		String origin = notification.getOrigin();
		long receivedAtMS = notification.getReceivedAtMS();

		// Determine how the notification should be displayed
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
		int defaultDisplayId = prefs.getInt(Database.PREFERENCE_DEFAULT_DISPLAY_ID, 0);
		GrowlDisplayProfile displayProfile = getDisplayProfile(type.getDisplayId(), defaultDisplayId);

		Log.i("GrowlListenerService.displayNotification", "Displaying notification from \"" + app.getName() + "\" "
				+ "of type \"" + type.TypeName + "\" with title \"" + title + "\" and message \"" + message + "\" "
				+ "using display profile " + displayProfile.getId());

		if (displayProfile.shouldLog()) {
			// Log the message in the history
			int databaseId = _database.insertNotificationHistory(type.ID, title, message, iconUrl, origin, receivedAtMS);
			notification.setId(databaseId);
		}

		// Display the notification based on the preferences of the profile,
		// launching MainActivity if it is clicked
		displayProfile.displayNotification(this, notification, new Intent(this, MainActivity.class));

		// Notify the status changed handlers
		onDisplayNotification(notification);
	}
	
	private void onDisplayNotification(GrowlNotification notification) {
		Log.i("GrowlListenerService.onDisplayNotification", "Message: \"" + notification.getMessage() + "\"");
		for (WeakReference<StatusChangedHandler> reference : _statusChangedHandlers) {
			StatusChangedHandler handler = reference.get();
			if (handler != null) {
				handler.onDisplayNotification(notification);
			} else {
				// This reference has expired
				_statusChangedHandlers.remove(reference);
			}
		}
	}
	
	private void onSubscriptionStatusChanged(long id, String status) {
		for (WeakReference<StatusChangedHandler> reference : _statusChangedHandlers) {
			StatusChangedHandler handler = reference.get();
			if (handler != null) {
				handler.onSubscriptionStatusChanged(id, status);
			} else {
				// This reference has expired
				_statusChangedHandlers.remove(reference);
			}
		}
	}

	public void registerResource(GrowlResource resource) {
		_registry.registerResource(resource);
	}

	public boolean requiresPassword() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		return Preferences.getPasswordsRequired(prefs);
	}
	
	public void addStatusChangedHandler(StatusChangedHandler handler) {
		WeakReference<StatusChangedHandler> reference = new WeakReference<StatusChangedHandler>(handler);
		_statusChangedHandlers.add(reference);
		_registry.addEventHandler(handler);
	}
	
	public void removeStatusChangedHandler(StatusChangedHandler h) {
		_registry.removeEventHandler(h);
		for (WeakReference<StatusChangedHandler> reference : _statusChangedHandlers) {
			StatusChangedHandler handler = reference.get();
			if ((handler == null) || (handler == h)) {
				_statusChangedHandlers.remove(reference);
				break;
			}
		}
	}
	
	public interface StatusChangedHandler extends GrowlRegistry.EventHandler {
		void onIsRunningChanged(boolean isRunning);
		void onDisplayNotification(GrowlNotification notification);
		void onSubscriptionStatusChanged(long id, String status);
	}

	public Bitmap getIcon(URL icon) {
		return _registry.getIcon(icon);
	}
	
	public File getResourcesDir() {
		return _registry.getResourcesDir();
	}
	
	public GrowlApplication registerApplication(String name, URL icon) {
		return _registry.registerApplication(name, icon);
	}

	public GrowlApplication getApplication(long id) {
		return _registry.getApplication(id);
	}
	
	public GrowlApplication getApplication(String name) {
		return _registry.getApplication(name);
	}
	
	public List<GrowlApplication> getApplications() {
		return _registry.getApplications();
	}

	public NotificationType getNotificationType(int id) {
		return _registry.getNotificationType(id);
	}

	public NotificationType getNotificationType(GrowlApplication application, String typeName) {
		return _registry.getNotificationType(application, typeName);
	}
	
	public List<NotificationType> getNotificationTypes(GrowlApplication application) {
		return _registry.getNotificationTypes(application);
	}

	public NotificationType registerNotificationType(GrowlApplication application, String typeName, String displayName,
			boolean enabled, URL iconUrl) {
		return _registry.registerNotificationType(application, typeName, displayName, enabled, iconUrl);
	}

	public byte[] getMatchingKey(HashAlgorithm algorithm, String hash, String salt) {
		String subscriberId = _subscriber.getId().toString();
		return getMatchingKey(subscriberId, algorithm, hash, salt);
	}
	
	public byte[] getMatchingKey(String subscriberId, HashAlgorithm algorithm, String hash, String salt) {
		return _registry.getMatchingKey(subscriberId, algorithm, hash, salt);
	}

	public void addEventHandler(EventHandler handler) {
		_registry.addEventHandler(handler);
	}

	public void removeEventHandler(EventHandler handler) {
		_registry.removeEventHandler(handler);
	}

	public void connectionClosed(Thread thread) {
		_socketAcceptor.connectionClosed(thread);
	}
}
