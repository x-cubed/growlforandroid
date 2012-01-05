package com.growlforandroid.common;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import com.growlforandroid.gntp.HashAlgorithm;

public class GrowlRegistry implements IGrowlRegistry {
	private static GrowlResources _resources;

	private final Database _database;

	private final Set<WeakReference<EventHandler>> _eventHandlers = Collections
			.synchronizedSet(new HashSet<WeakReference<EventHandler>>());

	public GrowlRegistry(Context context, Database database) {
		_database = database;

		if (_resources == null) {
			_resources = new GrowlResources(context);
		}
	}

	public GrowlApplication registerApplication(String name, URL iconUrl) {
		// Create a new Application and store application in a dictionary
		GrowlApplication oldApp = getApplication(name);
		GrowlApplication result = oldApp;
		if (oldApp != null) {
			long id = oldApp.getId();
			Log.i("GrowlRegistry.registerApplication", "Re-registering application \"" + name + "\" with ID = " + id);
			_database.setApplicationIcon(id, iconUrl);
			result = getApplication(id);
		} else {
			Boolean enabled = true;
			int id = _database.insertApplication(name, enabled, iconUrl);
			GrowlApplication newApp = new GrowlApplication(this, id, name, enabled, iconUrl, null);
			Log.i("GrowlRegistry.registerApplication", "Registered new application \"" + name + "\" with ID = "
					+ newApp.getId());
			result = newApp;
		}

		onApplicationRegistered(result);
		return result;
	}

	public GrowlApplication getApplication(String name) {
		Cursor cursor = _database.getApplication(name);
		GrowlApplication application = null;
		if (cursor.moveToFirst()) {
			application = loadApplication(cursor);
		}
		cursor.close();
		return application;
	}

	public GrowlApplication getApplication(long id) {
		Cursor cursor = _database.getApplication(id);
		GrowlApplication application = null;
		if (cursor.moveToFirst()) {
			application = loadApplication(cursor);
		}
		cursor.close();
		return application;
	}

	public List<GrowlApplication> getApplications() {
		ArrayList<GrowlApplication> applications = new ArrayList<GrowlApplication>();
		Cursor cursor = _database.getAllApplications();
		while (cursor.moveToNext()) {
			GrowlApplication application = loadApplication(cursor);
			applications.add(application);
		}
		cursor.close();
		return applications;
	}

	private GrowlApplication loadApplication(Cursor cursor) {
		int id = cursor.getInt(cursor.getColumnIndex(Database.KEY_ROWID));
		String name = cursor.getString(cursor.getColumnIndex(Database.KEY_NAME));
		boolean enabled = cursor.getInt(cursor.getColumnIndex(Database.KEY_ENABLED)) != 0;
		String icon = cursor.getString(cursor.getColumnIndex(Database.KEY_ICON_URL));
		URL iconUrl = null;
		try {
			iconUrl = icon == null ? null : new URL(icon);
		} catch (MalformedURLException x) {
			x.printStackTrace();
		}

		int displayColumn = cursor.getColumnIndex(Database.KEY_DISPLAY_ID);
		Integer displayId = null;
		if (!cursor.isNull(displayColumn)) {
			displayId = new Integer(cursor.getInt(displayColumn));
		}

		GrowlApplication application = new GrowlApplication(this, id, name, enabled, iconUrl, displayId);
		Log.i("GrowlRegistry.loadApplication", "Loaded application \"" + name + "\" with ID = " + id);

		return application;
	}

	public void registerResource(GrowlResource resource) {
		_resources.put(resource);
	}

	public Bitmap getIcon(URL source) {
		if (source == null) {
			return null;
		}

		final int maxIconSize = GrowlResources.MAX_ICON_SIZE;
		String name = source.toExternalForm();
		InputStream stream = null;
		Bitmap icon = null;
		try {
			stream = source.openStream();
			if (stream != null) {
				// Resource was found
				icon = new BitmapDrawable(stream).getBitmap();

				// Ensure that the icon isn't too big to display
				int width = icon.getWidth();
				int height = icon.getHeight();
				if ((width > maxIconSize) || (height > maxIconSize)) {
					// Reduce the size of the icon to something reasonable
					Bitmap scaledIcon = Bitmap.createScaledBitmap(icon, maxIconSize, maxIconSize, true);
					icon = scaledIcon;
				}
			}

		} catch (Exception x) {
			Log.e("GrowlRegistry.getIcon", "Unable to load source: " + name + "\n" + x.toString());
		}
		if (stream != null) {
			try {
				stream.close();
			} catch (Exception x) {
			}
		}
		return icon;

	}

	public NotificationType getNotificationType(GrowlApplication application, String typeName) {
		NotificationType type = null;
		Cursor cursor = _database.getNotificationType(application.getId(), typeName);
		if (cursor.moveToFirst()) {
			type = loadNotificationType(application, cursor);
		}
		cursor.close();
		return type;
	}

	public NotificationType getNotificationType(int id) {
		NotificationType type = null;
		Cursor cursor = _database.getNotificationType(id);
		if (cursor.moveToFirst()) {
			final int APP_ID_COLUMN = cursor.getColumnIndex(Database.KEY_APP_ID);
			int appId = cursor.getInt(APP_ID_COLUMN);
			GrowlApplication application = getApplication(appId);
			type = loadNotificationType(application, cursor);
		}
		return type;
	}
	
	public List<NotificationType> getNotificationTypes(GrowlApplication application) {
		ArrayList<NotificationType> types = new ArrayList<NotificationType>();
		Cursor cursor = _database.getNotificationTypes(application.getId());
		while (cursor.moveToNext()) {
			NotificationType type = loadNotificationType(application, cursor);
			types.add(type);
		}
		cursor.close();
		return types;
	}

	private NotificationType loadNotificationType(GrowlApplication application, Cursor cursor) {
		final int ID_COLUMN = cursor.getColumnIndex(Database.KEY_ROWID);
		final int NAME_COLUMN = cursor.getColumnIndex(Database.KEY_NAME);
		final int DISPLAY_NAME_COLUMN = cursor.getColumnIndex(Database.KEY_DISPLAY_NAME);
		final int ENABLED_COLUMN = cursor.getColumnIndex(Database.KEY_ENABLED);
		final int ICON_URL_COLUMN = cursor.getColumnIndex(Database.KEY_ICON_URL);
		final int DISPLAY_PROFILE_COLUMN = cursor.getColumnIndex(Database.KEY_DISPLAY_ID);

		int id = cursor.getInt(ID_COLUMN);
		String typeName = cursor.getString(NAME_COLUMN);
		String displayName = cursor.getString(DISPLAY_NAME_COLUMN);
		boolean enabled = cursor.getInt(ENABLED_COLUMN) != 0;

		String icon = cursor.getString(ICON_URL_COLUMN);
		URL iconUrl = null;
		try {
			iconUrl = icon == null ? null : new URL(icon);
		} catch (Exception x) {
			x.printStackTrace();
		}

		Integer displayId = null;
		if (!cursor.isNull(DISPLAY_PROFILE_COLUMN)) {
			displayId = new Integer(cursor.getInt(DISPLAY_PROFILE_COLUMN));
		}

		return new NotificationType(id, application, typeName, displayName, enabled, iconUrl, displayId);
	}

	public NotificationType registerNotificationType(GrowlApplication application, String typeName, String displayName,
			boolean enabled, URL iconUrl) {
		if (displayName == null)
			displayName = typeName;
		int id = _database.insertNotificationType(application.getId(), typeName, displayName, enabled, iconUrl);
		NotificationType type = new NotificationType(id, application, typeName, displayName, enabled, iconUrl, null);
		onNotificationTypeRegistered(type);
		return type;
	}

	public byte[] getMatchingKey(String subscriberId, HashAlgorithm algorithm, String hash, String salt) {
		byte[] hashBytes = Utility.hexStringToByteArray(hash);
		byte[] saltBytes = Utility.hexStringToByteArray(salt);
		return getMatchingKey(subscriberId, algorithm, hashBytes, saltBytes);
	}

	public byte[] getMatchingKey(String subscriberId, HashAlgorithm algorithm, byte[] hash, byte[] salt) {
		byte[] matchingKey = null;
		Cursor cursor = _database.getAllPasswordsAndNames(subscriberId);
		if (cursor.moveToFirst()) {
			final int nameColumn = cursor.getColumnIndex(Database.KEY_NAME);
			final int passwordColumn = cursor.getColumnIndex(Database.KEY_PASSWORD);
			do {
				String name = cursor.getString(nameColumn);
				String password = cursor.getString(passwordColumn);
				byte[] key = algorithm.calculateKey(password, salt);
				byte[] validHash = algorithm.calculateHash(key);
				boolean isValid = Utility.compareArrays(validHash, hash);
				if (isValid) {
					Log.i("GrowlRegistry.getMatchingKey", "Found key match: " + name);
					matchingKey = key;
					break;
				}
			} while (cursor.moveToNext());
		}
		cursor.close();
		return matchingKey;
	}

	public File getResourcesDir() {
		return _resources.getResourcesDir();
	}

	public void addEventHandler(EventHandler h) {
		WeakReference<EventHandler> reference = new WeakReference<EventHandler>(h);
		_eventHandlers.add(reference);
	}

	public void removeEventHandler(EventHandler h) {
		for (WeakReference<EventHandler> reference : _eventHandlers) {
			EventHandler handler = reference.get();
			if ((handler == null) || (handler == h)) {
				_eventHandlers.remove(reference);
				break;
			}
		}
	}

	private void onApplicationRegistered(GrowlApplication application) {
		Log.i("GrowlRegistry.onApplicationRegistered", application.getName() + " has been registered");
		for (WeakReference<EventHandler> reference : _eventHandlers) {
			EventHandler handler = reference.get();
			if (handler != null) {
				handler.onApplicationRegistered(application);
			} else {
				// This reference has expired
				_eventHandlers.remove(reference);
			}
		}
	}

	private void onNotificationTypeRegistered(NotificationType type) {
		Log.i("GrowlRegistry.onNotificationTypeRegistered", type.getDisplayName() + " has been registered");
		for (WeakReference<EventHandler> reference : _eventHandlers) {
			EventHandler handler = reference.get();
			if (handler != null) {
				handler.onNotificationTypeRegistered(type);
			} else {
				// This reference has expired
				_eventHandlers.remove(reference);
			}
		}
	}
}
