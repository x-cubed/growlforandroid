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
import android.widget.*;

public class GrowlListenerService
	extends Service
	implements IGrowlRegistry {
	
	private static final int GNTP_PORT = 23053;
	
	private static final int LED_COLOUR = 0xffffff00;
	private static final int LED_OFF_MS = 100;
	private static final int LED_ON_MS = 900;
	private static final int SERVICE_NOTIFICATION = 0xDEADBEEF;
	
	private static Database _database;
	private static GrowlResources _resources;
	
	private final Set<WeakReference<StatusChangedHandler>> _statusChangedHandlers = new HashSet<WeakReference<StatusChangedHandler>>();
	
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
    	
    	_notifyMgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    	
        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("GrowlListenerService.onStartCommand", "Received start id " + startId + ": " + intent);
        if (_socketAcceptor != null) {
        	Log.i("GrowlListenerService.onStartCommand", "Already started");
        	return START_STICKY;
        }
        
    	// We can only get the Bluetooth adaptor name from a looper thread, so grab it now and save it for later
    	String deviceName = Utility.getDeviceFriendlyName();
    	Context context = getBaseContext();
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	prefs.edit().putString(GntpMessage.PREFERENCE_DEVICE_NAME, deviceName).commit();
        
        try {
        	_zeroConf = ZeroConf.getInstance(getContext());
        	
        	// Start listening on GNTP_PORT, on all interfaces
        	ISocketThreadFactory factory = new GntpListenerThreadFactory(this);
			_socketAcceptor = new SocketAcceptor(this, factory, new InetSocketAddress(GNTP_PORT));
			_socketAcceptor.start();
	        
			// Register the GNTP service with Bonjour/ZeroConf
			String appName = context.getText(R.string.app_name).toString();
			String format = context.getText(R.string.gntp_zeroconf_name_format).toString();
			final String serviceName = String.format(format, appName, deviceName);
			ZeroConf.getInstance(context).registerService(
					Constants.GNTP_ZEROCONF_SERVICE_TYPE, serviceName, GNTP_PORT, Constants.GNTP_ZEROCONF_TEXT);	
			
			// Start subscribing to notifications from other devices
            Log.i("GrowlListenerService.onStartCommand", "Renewing subscriptions...");
			final GrowlListenerService service = this;
			_subscriber = new Subscriber(service) {
				public void onSubscriptionStatusChanged(long id, String status) {
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
    	
        // Tell the user we stopped.
        Toast.makeText(this, R.string.service_stopped, Toast.LENGTH_SHORT).show();
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
	
		_database.close();
		_database = null;
		
		_zeroConf.close();
		_zeroConf = null;
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
        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.statusbar_enabled,
        		getText(R.string.growl_on_status), System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
        notification.setLatestEventInfo(this, getText(R.string.app_name),
        		getText(R.string.growl_on_status), contentIntent);

        // Send the notification.
        _notifyMgr.notify(SERVICE_NOTIFICATION, notification);
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
				int id = cursor.getInt(0);
				String name = cursor.getString(1);
				boolean enabled = cursor.getInt(2) != 0;
				String icon = cursor.getString(3);
				URL iconUrl = icon == null ? null : new URL(icon);
				
				application = new GrowlApplication(this, id, name, enabled, iconUrl);
				
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
			GrowlApplication newApp = new GrowlApplication(this, id, name, enabled, iconUrl);
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
		
		String title = notification.getTitle();
		String message = notification.getMessage();
		URL iconUrl = notification.getIconUrl();
		String origin = notification.getOrigin();
		
		_database.insertNotificationHistory(type.ID, title, message, iconUrl, origin);
		
		Log.i("GrowlListenerService.displayNotification", "Displaying notification from \"" + app.getName() + "\" " +
				"of type \"" + type.TypeName + "\" with title \"" + title + "\" and message \"" + message + "\"");
		Notification statusBarPanel = new Notification(R.drawable.statusbar_enabled, message, System.currentTimeMillis());
		
        // Set the info for the views that show in the notification panel.
        statusBarPanel.ledARGB = LED_COLOUR;
        statusBarPanel.ledOffMS = LED_OFF_MS;
        statusBarPanel.ledOnMS = LED_ON_MS;
        statusBarPanel.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS;
        
        // Use our custom layout for the notification panel, so that we can insert the application icon
        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification_list_item);
        contentView.setImageViewResource(R.id.imgNotificationIcon, R.drawable.launcher);
        contentView.setTextViewText(R.id.txtNotificationTitle, title);
        contentView.setTextViewText(R.id.txtNotificationMessage, message.replace("\n", " / "));
        contentView.setTextViewText(R.id.txtNotificationApp, app.getName());
        statusBarPanel.contentView = contentView;

        // The PendingIntent to launch our activity if the user selects this notification
        statusBarPanel.contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        // Send the notification to the status bar
        _notifyMgr.notify(app.ID, statusBarPanel);
        
        // FIXME: Find a way to enqueue the displaying of the Toast on the UI threads Looper
        /* final String toastText = "Growl from " + app.Name + "\n\n" + title + "\n" + message;
        final GrowlListenerService service = this;		
    	Toast toast = Toast.makeText(service, toastText, Toast.LENGTH_LONG);
        toast.show(); */	
        
        // Notify the status changed handlers
        onDisplayNotification(notification);
	}

	public NotificationType getNotificationType(GrowlApplication application, String typeName) {
		NotificationType type = null;
		Cursor cursor = _database.getNotificationType(application.ID, typeName);
		if (cursor.moveToFirst()) {
			int id = cursor.getInt(0);
			String displayName = cursor.getString(2);
			boolean enabled = cursor.getInt(3) != 0;
			String icon = cursor.getString(4);
			URL iconUrl = null;
			try {
				iconUrl = icon == null ? null : new URL(icon);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			
			type = new NotificationType(id, application, typeName, displayName, enabled, iconUrl);
		}
		cursor.close();
		return type;
	}

	public NotificationType registerNotificationType(GrowlApplication application, String typeName, String displayName,	boolean enabled, URL iconUrl) {
		if (displayName == null)
			displayName = typeName;
		int id = _database.insertNotificationType(application.ID, typeName, displayName, enabled, iconUrl);
		NotificationType type = new NotificationType(id, application, typeName, displayName, enabled, iconUrl);
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









