package com.growlforandroid.client;

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
import android.graphics.drawable.Drawable;
import android.os.*;
import android.preference.PreferenceManager;
import android.util.Log;

public class GrowlListenerService
	extends Service
	implements IGrowlRegistry {

	private static final int SERVICE_NOTIFICATION = 0xDEADBEEF;
	private static final String PLATFORM_NAME = "Google Android";
	
	private static Database _database;
	private static GrowlResources _resources;
	
	private final Set<WeakReference<StatusChangedHandler>> _statusChangedHandlers =
			Collections.synchronizedSet(new HashSet<WeakReference<StatusChangedHandler>>());
	
	private ZeroConf _zeroConf;
    private NotificationManager _notifyMgr;
    private SocketAcceptor _socketAcceptor;
    private Subscriber _subscriber;
    
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
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
    		_resources = new GrowlResources();

    		// Register a protocol handler for x-growl-resource:// URLs
    		try {
    			URL.setURLStreamHandlerFactory(_resources);
    		} catch (Throwable t) {
    			Log.e("GrowlListenerService.onCreate", "Failed to register protocol handler: " + t);
    		}
    	}
    	
    	initializeCommonHeaders();
    	
    	_notifyMgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    	
        // Display a notification about us starting.  We put an icon in the status bar.
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
        Log.i("GrowlListenerService.onStartCommand", "Received start id " + startId + ": " + intent);
        if (_socketAcceptor != null) {
        	Log.i("GrowlListenerService.onStartCommand", "Already started");
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
				Log.i("GrowlListenerService.onStartCommand", "Initialising ZeroConf...");
				initialiseZeroConf(deviceName, this);
			}
			
			// Start subscribing to notifications from other devices
            Log.i("GrowlListenerService.onStartCommand", "Renewing subscriptions...");
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
			
			setWasRunning(true);
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
		_zeroConf.registerService(
				Constants.GNTP_ZEROCONF_SERVICE_TYPE, serviceName,
				Constants.GNTP_PORT, Constants.GNTP_ZEROCONF_TEXT);
	}

    public Context getContext() {
    	return getBaseContext();
    }
    
    private void setWasRunning(boolean wasRunning) {
    	Log.i("ListenerServiceConnection.setWasRunning", "Was Running = " + wasRunning);
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
		prefs.edit().putBoolean(Preferences.WAS_RUNNING, wasRunning).commit();	
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
        // Cancel the persistent notification.
    	_notifyMgr.cancel(SERVICE_NOTIFICATION);

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
			_zeroConf.unregisterAllServices();
			_zeroConf.close();
			_zeroConf = null;
		}
    }
    
    protected void finalize() {
    	stop();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
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
        Notification notification = new Notification(R.drawable.statusbar_disabled,
        		getText(R.string.growl_on_status), System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

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
    
	public Drawable getIcon(URL icon) {
		// TODO Auto-generated method stub
		return null;
	}

	public void registerResource(GrowlResource resource) {
		_resources.put(resource);
	}

	public GrowlApplication getApplication(String name) {
		Cursor cursor = _database.getApplication(name);
		return getApplication(cursor);
	}
	
	public GrowlApplication getApplication(long id) {
		Cursor cursor = _database.getApplication(id);
		return getApplication(cursor);
	}
	
	private GrowlApplication getApplication(Cursor cursor) {
		GrowlApplication application = null;
		if (cursor.moveToFirst()) {
			try {
				int id = cursor.getInt(cursor.getColumnIndex(Database.KEY_ROWID));
				String name = cursor.getString(cursor.getColumnIndex(Database.KEY_NAME));
				boolean enabled = cursor.getInt(cursor.getColumnIndex(Database.KEY_ENABLED)) != 0;
				String icon = cursor.getString(cursor.getColumnIndex(Database.KEY_ICON_URL));
				URL iconUrl = icon == null ? null : new URL(icon);
				
				int displayColumn = cursor.getColumnIndex(Database.KEY_DISPLAY_ID);
				Integer displayId = null;
				if (!cursor.isNull(displayColumn))
					displayId = new Integer(cursor.getInt(displayColumn));
				
				application = new GrowlApplication(this, id, name, enabled, iconUrl, displayId);
				
				Log.i("GrowlListenerService.loadApplication", "Loaded application \"" + name + "\" with ID = " + id);
			} catch (MalformedURLException x) {
				Log.e("GrowlListenerService.getApplication", x.toString());
			}
		}
		cursor.close();
		return application;
	}
	
	public GrowlApplication registerApplication(String name, URL iconUrl) {
		// Create a new Application and store application in a dictionary
		GrowlApplication oldApp = getApplication(name);
		GrowlApplication result = oldApp;
		if (oldApp != null) {
			long id = oldApp.ID;
			Log.i("GrowlListenerService.registerApplication", "Re-registering application \"" + name + "\" with ID = " + id);
			_database.setApplicationIcon(id, iconUrl);
			result = getApplication(id);
		} else {
			Boolean enabled = true;
			int id = _database.insertApplication(name, enabled, iconUrl);
			GrowlApplication newApp = new GrowlApplication(this, id, name, enabled, iconUrl, null);
			Log.i("GrowlListenerService.registerApplication", "Registered new application \"" + name + "\" with ID = " + newApp.ID);
			result = newApp;
		}
		
		onApplicationRegistered(result);
		return result;
	}
	
	public void displayNotification(GrowlNotification notification) {
		NotificationType type = notification.getType();
		GrowlApplication app = type.Application;
		if (!type.isEnabled()) {
			Log.i("GrowlListenerService.displayNotification", "Not displaying notification from \"" + app.getName() + "\" " +
					"of type \"" + type.TypeName + "\" as notification type is disabled");
			return;
		}		
		if (!app.isEnabled()) {
			Log.i("GrowlListenerService.displayNotification", "Not displaying notification from \"" + app.getName() + "\" " +
					"of type \"" + type.TypeName + "\" as application is disabled");
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

		Log.i("GrowlListenerService.displayNotification", "Displaying notification from \"" + app.getName() + "\" " +
				"of type \"" + type.TypeName + "\" with title \"" + title + "\" and message \"" + message + "\" " +
				"using display profile " + displayProfile.getId());
		
		if (displayProfile.shouldLog()) {
			// Log the message in the history
			_database.insertNotificationHistory(type.ID, title, message, iconUrl, origin, receivedAtMS);
		}
		
		// Display the notification based on the preferences of the profile, launching MainActivity if it is clicked
		displayProfile.displayNotification(this, notification, new Intent(this, MainActivity.class));
        
        // Notify the status changed handlers
        onDisplayNotification(notification);
	}

	public NotificationType getNotificationType(GrowlApplication application, String typeName) {
		NotificationType type = null;
		Cursor cursor = _database.getNotificationType(application.ID, typeName);
		if (cursor.moveToFirst()) {
			int id = cursor.getInt(cursor.getColumnIndex(Database.KEY_ROWID));
			String displayName = cursor.getString(cursor.getColumnIndex(Database.KEY_DISPLAY_NAME));
			boolean enabled = cursor.getInt(cursor.getColumnIndex(Database.KEY_ENABLED)) != 0;
			String icon = cursor.getString(cursor.getColumnIndex(Database.KEY_ICON_URL));
			URL iconUrl = null;
			try {
				iconUrl = icon == null ? null : new URL(icon);
			} catch (Exception x) {
				x.printStackTrace();
			}
			
			int displayColumn = cursor.getColumnIndex(Database.KEY_DISPLAY_ID);
			Integer displayId = null;
			if (!cursor.isNull(displayColumn))
				displayId = new Integer(cursor.getInt(displayColumn));
			
			type = new NotificationType(id, application, typeName, displayName, enabled, iconUrl, displayId);
		}
		cursor.close();
		return type;
	}

	public NotificationType registerNotificationType(GrowlApplication application, String typeName, String displayName,	boolean enabled, URL iconUrl) {
		if (displayName == null)
			displayName = typeName;
		int id = _database.insertNotificationType(application.ID, typeName, displayName, enabled, iconUrl);
		NotificationType type = new NotificationType(id, application, typeName, displayName, enabled, iconUrl, null);
		onNotificationTypeRegistered(type);
		return type;
	}

	public boolean requiresPassword() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		return prefs.getBoolean("security_require_passwords", true);
	}

	public byte[] getMatchingKey(HashAlgorithm algorithm, String hash, String salt) {
		byte[] hashBytes = Utility.hexStringToByteArray(hash);
		byte[] saltBytes = Utility.hexStringToByteArray(salt);
		return getMatchingKey(algorithm, hashBytes, saltBytes);
	}
	
	public byte[] getMatchingKey(HashAlgorithm algorithm, byte[] hash, byte[] salt) {
		byte[] matchingKey = null;
		Cursor cursor = _database.getAllPasswordsAndNames(_subscriber.getId().toString());
		if (cursor.moveToFirst()) {
			final int nameColumn = cursor.getColumnIndex(Database.KEY_NAME);
			final int passwordColumn = cursor.getColumnIndex(Database.KEY_PASSWORD);
			do {
				String name = cursor.getString(nameColumn);
				String password = cursor.getString(passwordColumn);
				// Log.i("GrowlListenerService.getMatchingKey", "Name:      " + name);
				// Log.i("GrowlListenerService.getMatchingKey", "Password:  " + password);
				byte[] key = algorithm.calculateKey(password, salt);
				// Log.i("GrowlListenerService.getMatchingKey", "Key:       " + Utility.getHexStringFromByteArray(key));
				byte[] validHash = algorithm.calculateHash(key);
				// Log.i("GrowlListenerService.getMatchingKey", "Hash:      " + Utility.getHexStringFromByteArray(validHash));
				boolean isValid = Utility.compareArrays(validHash, hash);
				if (isValid) {
					Log.i("GrowlListenerService.getMatchingKey", "Found key match: " + name);
					matchingKey = key;
					break;
				}
			} while (cursor.moveToNext());
		}
		cursor.close();
		return matchingKey;
	}
	
	public void addStatusChangedHandler(StatusChangedHandler h) {
		WeakReference<StatusChangedHandler> reference = new WeakReference<StatusChangedHandler>(h);
		_statusChangedHandlers.add(reference);
	}
	
	public void removeStatusChangedHandler(StatusChangedHandler h) {
		for(WeakReference<StatusChangedHandler> reference : _statusChangedHandlers) {
			StatusChangedHandler handler = reference.get();
			if ((handler == null) || (handler == h)) {
				_statusChangedHandlers.remove(reference);
				break;
			}
		}
	}
	
	private void onApplicationRegistered(GrowlApplication application) {
		Log.i("GrowlListenerService.onApplicationRegistered", application.getName() + " has been registered");
		for(WeakReference<StatusChangedHandler> reference : _statusChangedHandlers) {
			StatusChangedHandler handler = reference.get();
			if (handler != null) {
				handler.onApplicationRegistered(application);
			} else {
				// This reference has expired
				_statusChangedHandlers.remove(reference);
			}
		}
	}
	
	private void onDisplayNotification(GrowlNotification notification) {
		Log.i("GrowlListenerService.onDisplayNotification", "Message: \"" + notification.getMessage() + "\"");
		for(WeakReference<StatusChangedHandler> reference : _statusChangedHandlers) {
			StatusChangedHandler handler = reference.get();
			if (handler != null) {
				handler.onDisplayNotification(notification);
			} else {
				// This reference has expired
				_statusChangedHandlers.remove(reference);
			}
		}
	}
	
	private void onNotificationTypeRegistered(NotificationType type) {
		Log.i("GrowlListenerService.onNotificationTypeRegistered", type.getDisplayName() + " has been registered");
		for(WeakReference<StatusChangedHandler> reference : _statusChangedHandlers) {
			StatusChangedHandler handler = reference.get();
			if (handler != null) {
				handler.onNotificationTypeRegistered(type);
			} else {
				// This reference has expired
				_statusChangedHandlers.remove(reference);
			}
		}
	}
	
	private void onSubscriptionStatusChanged(long id, String status) {
		Log.i("GrowlListenerService.onSubscriptionStatusChanged",
				"The status of subscription " + id + " has changed");
		for(WeakReference<StatusChangedHandler> reference : _statusChangedHandlers) {
			StatusChangedHandler handler = reference.get();
			if (handler != null) {
				handler.onSubscriptionStatusChanged(id, status);
			} else {
				// This reference has expired
				_statusChangedHandlers.remove(reference);
			}
		}
	}
	
	public interface StatusChangedHandler {
		void onIsRunningChanged(boolean isRunning);
		void onNotificationTypeRegistered(NotificationType type);
		void onApplicationRegistered(GrowlApplication app);
		void onDisplayNotification(GrowlNotification notification);
		void onSubscriptionStatusChanged(long id, String status);
	}
}









