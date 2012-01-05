package com.growlforandroid.common;

import java.net.URL;

import com.growlforandroid.client.R;

import android.app.Notification;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

public class Database {
	public static final String KEY_ROWID = "_id";
	public static final String KEY_APP_ID = "app_id";
	public static final String KEY_TYPE_ID = "type_id";
	public static final String KEY_NAME = "name";
	public static final String KEY_DISPLAY_NAME = "display_name";
	public static final String KEY_ENABLED = "enabled";
	public static final String KEY_ICON_URL = "icon_url";
	public static final String KEY_PASSWORD = "password";
	public static final String KEY_RECEIVED_AT = "received_at";
	public static final String KEY_TITLE = "title";
	public static final String KEY_MESSAGE = "message";
	public static final String KEY_ORIGIN = "origin";
	public static final String KEY_ADDRESS = "address";
	public static final String KEY_ZERO_CONF = "zero_conf";
	public static final String KEY_STATUS = "status";
	public static final String KEY_DISPLAY_ID = "display_id";
	public static final String KEY_LOG = "log";
	public static final String KEY_STATUS_BAR_DEFAULTS = "status_bar_defaults";
	public static final String KEY_STATUS_BAR_FLAGS = "status_bar_flags";
	public static final String KEY_TOAST_FLAGS = "toast_flags";
	public static final String KEY_ALERT_URL = "alert_url";
	
	public static final String KEY_APP_NAME = "app_name";
	public static final String KEY_TYPE_DISPLAY_NAME = "type_name";
	public static final String KEY_TYPE_LIST = "type_list";
	
	public static final String TABLE_APPLICATIONS = "applications";
	public static final String TABLE_NOTIFICATION_TYPES = "notification_types";
	public static final String TABLE_DISPLAYS = "displays";
	public static final String TABLE_PASSWORDS = "passwords";
	public static final String TABLE_SUBSCRIPTIONS = "subscriptions";
	public static final String TABLE_NOTIFICATION_HISTORY = "notification_history";
	
	public static final String PREFERENCE_DEFAULT_DISPLAY_ID = "display_default_id";
	
	private static final String DATABASE_NAME = "growl";
	private static final int DATABASE_VERSION = 3;
	
	private final Context _context;

	private Helper DBHelper;
	private SQLiteDatabase db;

	public Database(Context context) {
		_context = context;
		DBHelper = new Helper(_context);
		db = DBHelper.getWritableDatabase();
		
		if (!hasDisplayProfiles()) {
			createDisplayProfiles();
		}
	}

	private class Helper extends SQLiteOpenHelper {
		Helper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d("Database", "Creating new database");
			db.execSQL("CREATE TABLE applications ("
					+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ "name TEXT NOT NULL, "
					+ "enabled INTEGER NOT NULL, "
					+ "icon_url TEXT, "
					+ "display_id INT, "
					+ "FOREIGN KEY(display_id) REFERENCES displays(id) ON DELETE SET NULL);");

			db.execSQL("CREATE TABLE notification_types ("
					+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ "app_id INTEGER NOT NULL, "
					+ "name TEXT NOT NULL, "
					+ "display_name TEXT NOT NULL, "
					+ "enabled INTEGER NOT NULL, "
					+ "icon_url TEXT, "
					+ "display_id INT, "
					+ "FOREIGN KEY(app_id) REFERENCES applications(id), "
					+ "FOREIGN KEY(display_id) REFERENCES displays(id) ON DELETE SET NULL);");

			db.execSQL("CREATE TABLE displays ("
					+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ "name TEXT NOT NULL, "
					+ "log INTEGER NOT NULL, "
					+ "status_bar_defaults INTEGER, "
					+ "status_bar_flags INTEGER, "
					+ "toast_flags INTEGER, "
					+ "alert_url TEXT);");

			db.execSQL("CREATE TABLE passwords ("
					+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ "name TEXT NOT NULL, "
					+ "password TEXT);");
			
			db.execSQL("CREATE TABLE subscriptions ("
					+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ "name TEXT NOT NULL, "
					+ "address TEXT NOT NULL, "
					+ "password TEXT NOT NULL, "
					+ "zero_conf INTEGER NOT NULL DEFAULT 0, "
					+ "status TEXT NOT NULL);");
			
			db.execSQL("CREATE TABLE notification_history ("
					+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ "received_at TIMESTAMP NOT NULL,"
					+ "type_id INTEGER NOT NULL, "
					+ "title TEXT NOT NULL, "
					+ "message TEXT NOT NULL, "
					+ "icon_url TEXT, "
					+ "origin TEXT, "
					+ "FOREIGN KEY(type_id) REFERENCES notification_types(id));");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w("Database.onUpgrade", "Upgrading database from version " + oldVersion + " to " + newVersion);
			if (oldVersion <= 1) {
				Log.i("Database.onUpgrade", "Upgrading display profiles from version 1");
				upgradeDisplayProfilesFrom1To2(db);
			}
			
			if (oldVersion <= 2) {
				Log.i("Database.onUpgrade", "Upgrading subscriptions from version 2");
				updateSubscriptionsFrom2To3(db);
			}
		}

		/***
		 * Status bar defaults and flags were mistakenly stored in the same column in DB version 1,
		 * when their values can collide. These are split in version 2.
		 * @param db
		 */
		private void upgradeDisplayProfilesFrom1To2(SQLiteDatabase db) {
			// Delete the existing profiles, they'll be replaced in the Database constructor
			db.execSQL("DELETE FROM " + TABLE_DISPLAYS);
			db.execSQL("ALTER TABLE " + TABLE_DISPLAYS + " " +
					"ADD COLUMN " + KEY_STATUS_BAR_DEFAULTS + " INTEGER;");
		}
		
		private void updateSubscriptionsFrom2To3(SQLiteDatabase db) {
			db.execSQL("ALTER TABLE " + TABLE_SUBSCRIPTIONS + " " +
					"ADD COLUMN " + KEY_ZERO_CONF + " INTEGER NOT NULL DEFAULT 0;");
		}
	}

	public void close() {
		DBHelper.close();
	}

	private void createDisplayProfiles() {
		Log.i("Database.createDisplayProfiles", "Creating default display profiles...");
		String ignore = _context.getString(R.string.display_profile_ignore);
		String silent = _context.getString(R.string.display_profile_silent);
		String vibrate = _context.getString(R.string.display_profile_vibrate);
		String growl = _context.getString(R.string.display_profile_growl);
		String growlVibrate = _context.getString(R.string.display_profile_growl_vibrate);
		
		insertDisplayProfile(ignore, false);
		insertDisplayProfile(silent, true);
		insertDisplayProfile(vibrate, Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE,
				Notification.FLAG_AUTO_CANCEL, null);
		int defaultProfile = insertDisplayProfile(growl, Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND,
				Notification.FLAG_AUTO_CANCEL, null);
		insertDisplayProfile(growlVibrate,
				Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE,
				Notification.FLAG_AUTO_CANCEL, null);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_context);
		prefs.edit().putInt(PREFERENCE_DEFAULT_DISPLAY_ID, defaultProfile).commit();
	}

	public int insertDisplayProfile(String name, boolean log) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NAME, name);
		initialValues.put(KEY_LOG, log);
		return (int) db.insert(TABLE_DISPLAYS, null, initialValues);
	}
	
	public int insertDisplayProfile(String name, Integer statusBarDefaults, Integer statusBarFlags, Integer toastFlags) {
		return insertDisplayProfile(name, true, statusBarDefaults, statusBarFlags, toastFlags, null);
	}
	
	public int insertDisplayProfile(String name, boolean log, Integer statusBarDefaults, Integer statusBarFlags, Integer toastFlags, Uri alert) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NAME, name);
		initialValues.put(KEY_LOG, log);
		initialValues.put(KEY_STATUS_BAR_DEFAULTS, statusBarDefaults);
		initialValues.put(KEY_STATUS_BAR_FLAGS, statusBarFlags);
		initialValues.put(KEY_TOAST_FLAGS, toastFlags);
		initialValues.put(KEY_ALERT_URL, (alert == null) ? null : alert.toString());
		return (int) db.insert(TABLE_DISPLAYS, null, initialValues);
	}
	
	public Cursor getDisplayProfile(int profile) {
		return db.query(TABLE_DISPLAYS, new String[] {
				KEY_ROWID, KEY_NAME, KEY_LOG, KEY_STATUS_BAR_DEFAULTS, KEY_STATUS_BAR_FLAGS, KEY_TOAST_FLAGS, KEY_ALERT_URL
				}, KEY_ROWID + "=" + profile, null, null, null, null);
	}
	
	public String getDisplayProfileName(Integer profile) {
		if (profile == null) {
			return null;
		}
		
		String result = null;
		Cursor cursor = getDisplayProfile(profile);
		if (cursor.moveToFirst()) {
			final int nameColumn = cursor.getColumnIndexOrThrow(KEY_NAME);
			result = cursor.getString(nameColumn);
		}
		cursor.close();
		
		Log.i("Database.getDisplayProfileName", "Profile " + profile + " is called " + result);
		return result;
	}
	
	public Cursor getDisplayProfiles() {
		return db.query(TABLE_DISPLAYS, new String[] {
				KEY_ROWID, KEY_NAME, KEY_LOG, KEY_STATUS_BAR_FLAGS, KEY_TOAST_FLAGS, KEY_ALERT_URL
				}, null, null, null, null, null);
	}
	
	public boolean hasDisplayProfiles() {
		Cursor cursor = getDisplayProfiles();
		boolean hasProfiles = cursor.moveToFirst();
		cursor.close();
		return hasProfiles;
	}
	
	public int insertPassword(String name, String password) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NAME, name);
		initialValues.put(KEY_PASSWORD, password);
		return (int) db.insert(TABLE_PASSWORDS, null, initialValues);
	}

	public boolean deletePassword(long rowId) {
		Log.i("Database.deletePassword", "Deleting password " + rowId);
		return db.delete(TABLE_PASSWORDS, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public Cursor getPasswordsAndNames() {
		return db.query(TABLE_PASSWORDS, new String[] { KEY_ROWID, KEY_NAME,
				KEY_PASSWORD }, null, null, null, null, null);
	}

	public Cursor getAllPasswordsAndNames(String subscriberId) {
		return db.rawQuery(
				"SELECT " + KEY_NAME + ", " + KEY_PASSWORD + " || ? AS " + KEY_PASSWORD + " " +
				"FROM " + TABLE_SUBSCRIPTIONS + " " +
				"UNION " +
				"SELECT " + KEY_NAME + ", " + KEY_PASSWORD + " " +
				"FROM " + TABLE_PASSWORDS, new String[] { subscriberId });
	}

	public int insertApplication(String name, Boolean enabled, URL iconUrl) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NAME, name);
		initialValues.put(KEY_ENABLED, enabled ? 1 : 0);

		String url = iconUrl == null ? null : iconUrl.toString();
		initialValues.put(KEY_ICON_URL, url);
		return (int) db.insert(TABLE_APPLICATIONS, null, initialValues);
	}

	public boolean deleteApplication(long rowId) {
		return deleteApplication(rowId, false);
	}
	
	public boolean deleteApplication(long rowId, boolean includeTypesAndHistory) {
		if (includeTypesAndHistory) {
			boolean success = false;
			Object[] params = new Object[] { rowId };
			db.beginTransaction();
			try {
				db.execSQL("DELETE FROM " + TABLE_NOTIFICATION_HISTORY + " WHERE " + KEY_TYPE_ID + " IN " +
					"(SELECT " + KEY_ROWID + " FROM " + TABLE_NOTIFICATION_TYPES + " WHERE " + KEY_APP_ID + "=?);", params);
				db.execSQL("DELETE FROM " + TABLE_NOTIFICATION_TYPES + " WHERE " + KEY_APP_ID + "=?;", params);
				db.execSQL("DELETE FROM " + TABLE_APPLICATIONS + " WHERE " + KEY_ROWID + "=?;", params);
				db.setTransactionSuccessful();
				success = true;
			} finally {
				db.endTransaction();
			}
			return success;
		} else {
			return db.delete(TABLE_APPLICATIONS, KEY_ROWID + "=" + rowId, null) > 0;
		}
	}

	public Cursor getAllApplications() {
		return db.query(TABLE_APPLICATIONS, new String[] {
				KEY_ROWID, KEY_NAME, KEY_ENABLED, KEY_ICON_URL, KEY_DISPLAY_ID },
				null, null, null, null, null);
	}
	
	public Cursor getAllApplicationsAndTypes() {
		return db.rawQuery(
				"SELECT " + KEY_ROWID + ", " + KEY_NAME + ", " + KEY_ICON_URL + ", " + KEY_TYPE_LIST + " " +
				"FROM " + TABLE_APPLICATIONS + " " +
					"LEFT JOIN (SELECT " + KEY_APP_ID + ", group_concat(" + KEY_DISPLAY_NAME + ", ', ') AS " + KEY_TYPE_LIST + " " +
							"FROM " + TABLE_NOTIFICATION_TYPES + " GROUP BY + " + KEY_APP_ID + ") AS " + TABLE_NOTIFICATION_TYPES + " " +
						"ON " + TABLE_NOTIFICATION_TYPES + "." + KEY_APP_ID + " = " + TABLE_APPLICATIONS + "." + KEY_ROWID + " " +
				"ORDER BY lower(" + KEY_NAME + ")", null);
	}

	public Cursor getApplication(long id) throws SQLException {
		Cursor cursor = db.query(true, TABLE_APPLICATIONS, new String[] {
				KEY_ROWID, KEY_NAME, KEY_ENABLED, KEY_ICON_URL, KEY_DISPLAY_ID },
				KEY_ROWID + "=" + id, null, null, null, null, null);
		return cursor;
	}

	public Cursor getApplication(String name) throws SQLException {
		Cursor cursor = db.query(true, TABLE_APPLICATIONS, new String[] {
				KEY_ROWID, KEY_NAME, KEY_ENABLED, KEY_ICON_URL, KEY_DISPLAY_ID },
				KEY_NAME + "=?", new String[] { name }, null, null, null, null);
		return cursor;
	}
	
	public String getApplicationName(long id) {
		String name = null;
		Cursor cursor = getApplication(id);
		if (cursor.moveToFirst()) {
			name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME));
		}
		cursor.close();
		return name;
	}
	
	public boolean setApplicationEnabled(long id, boolean isEnabled) {
		ContentValues args = new ContentValues();
		args.put(KEY_ENABLED, isEnabled);
		return db.update(TABLE_APPLICATIONS, args, KEY_ROWID + "=" + id, null) > 0;		
	}

	public boolean setApplicationDisplay(long id, Integer displayId) {
		ContentValues args = new ContentValues();
		args.put(KEY_DISPLAY_ID, displayId);
		return db.update(TABLE_APPLICATIONS, args, KEY_ROWID + "=" + id, null) > 0;
	}
	
	public boolean setApplicationIcon(long id, URL iconUrl) {
		ContentValues args = new ContentValues();
		args.put(KEY_ICON_URL, (iconUrl != null) ? iconUrl.toString() : null);
		return db.update(TABLE_APPLICATIONS, args, KEY_ROWID + "=" + id, null) > 0;
	}
	
	public Cursor getNotificationType(long id) {
		Cursor cursor = db.query(true, TABLE_NOTIFICATION_TYPES, new String[] {
				KEY_ROWID, KEY_APP_ID, KEY_NAME, KEY_DISPLAY_NAME, KEY_ENABLED,	KEY_ICON_URL, KEY_DISPLAY_ID
				}, KEY_ROWID + "=" + id, null, null, null, null, null);
		return cursor;
	}
	
	public Cursor getNotificationType(long appId, String typeName) {
		Cursor cursor = db.query(true, TABLE_NOTIFICATION_TYPES, new String[] {
				KEY_ROWID, KEY_NAME, KEY_DISPLAY_NAME, KEY_ENABLED,	KEY_ICON_URL, KEY_DISPLAY_ID
				}, KEY_APP_ID + "=" + appId + " AND " + KEY_NAME
				+ "=?", new String[] { typeName }, null, null, null, null);
		return cursor;
	}

	public Cursor getNotificationTypes(long appId) {
		Cursor cursor = db.query(true, TABLE_NOTIFICATION_TYPES, new String[] {
				KEY_ROWID, KEY_NAME, KEY_DISPLAY_NAME, KEY_ENABLED,	KEY_ICON_URL, KEY_DISPLAY_ID
				}, KEY_APP_ID + "=" + appId,
				null, null, null, null, null);
		return cursor;
	}
	
	public boolean setNotificationTypeEnabled(long id, boolean isEnabled) {
		ContentValues args = new ContentValues();
		args.put(KEY_ENABLED, isEnabled);
		return db.update(TABLE_NOTIFICATION_TYPES, args, KEY_ROWID + "=" + id, null) > 0;
	}
	
	public boolean setNotificationTypeDisplay(long id, Integer displayId) {
		ContentValues args = new ContentValues();
		args.put(KEY_DISPLAY_ID, displayId);
		return db.update(TABLE_NOTIFICATION_TYPES, args, KEY_ROWID + "=" + id, null) > 0;
	}
	
	public int insertNotificationType(long appId, String typeName,
			String displayName, boolean enabled, URL iconUrl) {

		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_APP_ID, appId);
		initialValues.put(KEY_NAME, typeName);
		initialValues.put(KEY_DISPLAY_NAME, displayName);
		initialValues.put(KEY_ENABLED, enabled ? 1 : 0);

		String url = iconUrl == null ? null : iconUrl.toString();
		initialValues.put(KEY_ICON_URL, url);
		return (int) db.insert(TABLE_NOTIFICATION_TYPES, null, initialValues);
	}
	
	public boolean deleteNotificationType(long rowId) {
		boolean history = db.delete(TABLE_NOTIFICATION_HISTORY, KEY_TYPE_ID + "=" + rowId, null) > 0;
		boolean type = db.delete(TABLE_NOTIFICATION_TYPES, KEY_ROWID + "=" + rowId, null) > 0;
		return history && type;
	}

	public int insertNotificationHistory(long typeID, String title, String message, URL iconUrl, String origin, long receivedAt) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_TYPE_ID, typeID);
		initialValues.put(KEY_RECEIVED_AT, receivedAt);
		initialValues.put(KEY_TITLE, title);
		initialValues.put(KEY_MESSAGE, message);

		String url = iconUrl == null ? null : iconUrl.toString();
		initialValues.put(KEY_ICON_URL, url);
		
		initialValues.put(KEY_ORIGIN, origin);
		return (int) db.insert(TABLE_NOTIFICATION_HISTORY, null, initialValues);
	}
	
	/**
	 * Returns all columns and rows from the notification_history table, as well as the notification type name and the application type name
	 * @return
	 */
	public Cursor getNotificationHistory(int limit) {
		return db.rawQuery(
				"SELECT " + TABLE_NOTIFICATION_HISTORY + ".*, " +
					TABLE_NOTIFICATION_TYPES + "." + KEY_DISPLAY_NAME + " AS " + KEY_TYPE_DISPLAY_NAME + ", " +
					TABLE_APPLICATIONS + "." + KEY_NAME + " AS " + KEY_APP_NAME + " " +
				"FROM " + TABLE_NOTIFICATION_HISTORY + " " +
					"INNER JOIN " + TABLE_NOTIFICATION_TYPES + " " +
						"ON " + TABLE_NOTIFICATION_TYPES + "." + KEY_ROWID + " = " + TABLE_NOTIFICATION_HISTORY + "." + KEY_TYPE_ID + " " +
					"INNER JOIN " + TABLE_APPLICATIONS + " " +
						"ON " + TABLE_APPLICATIONS + "." + KEY_ROWID + " = " + TABLE_NOTIFICATION_TYPES + "." + KEY_APP_ID + " " +
				"ORDER BY " + KEY_RECEIVED_AT + " DESC " +
				"LIMIT ?", new String[] { Integer.toString(limit) });
	}

	public void deleteNotificationHistory() {
		db.delete(TABLE_NOTIFICATION_HISTORY, null, null);
	}
	
	public long insertSubscription(String name, String address, String password, boolean isZeroConf, String status) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NAME, name);
		initialValues.put(KEY_ADDRESS, address);
		initialValues.put(KEY_PASSWORD, password);
		initialValues.put(KEY_ZERO_CONF, isZeroConf);
		initialValues.put(KEY_STATUS, status);
		return db.insert(TABLE_SUBSCRIPTIONS, null, initialValues);
	}

	public boolean updateSubscription(long id, String name, String address, String password, String status) {		
		ContentValues args = new ContentValues();
		args.put(KEY_NAME, name);
		args.put(KEY_ADDRESS, address);
		args.put(KEY_PASSWORD, password);		
		args.put(KEY_STATUS, status);
		return db.update(TABLE_SUBSCRIPTIONS, args, KEY_ROWID + "=" + id, null) > 0;
	}
	
	public boolean updateSubscription(long id, String status) {		
		ContentValues args = new ContentValues();
		args.put(KEY_STATUS, status);
		return db.update(TABLE_SUBSCRIPTIONS, args, KEY_ROWID + "=" + id, null) > 0;
	}
	
	public boolean deleteSubscription(long rowId) {
		Log.i("Database.deleteSubscription", "Deleting subscription " + rowId);
		return db.delete(TABLE_SUBSCRIPTIONS, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public Cursor getSubscriptions() {
		return db.query(TABLE_SUBSCRIPTIONS, new String[] {
				KEY_ROWID, KEY_NAME, KEY_ADDRESS, KEY_PASSWORD, KEY_ZERO_CONF, KEY_STATUS },
				null, null, null, null, null);
	}

	public Cursor getManualSubscriptions() {
		return db.query(TABLE_SUBSCRIPTIONS, new String[] {
				KEY_ROWID, KEY_NAME, KEY_ADDRESS, KEY_PASSWORD, KEY_ZERO_CONF, KEY_STATUS },
				KEY_ZERO_CONF + "=0", null, null, null, null);
	}
	
	public Cursor getSubscription(long id) {
		Cursor cursor = db.query(true, TABLE_SUBSCRIPTIONS, new String[] {
				KEY_ROWID, KEY_NAME, KEY_ADDRESS, KEY_PASSWORD, KEY_ZERO_CONF, KEY_STATUS },
				KEY_ROWID + "=" + id, null, null, null, null, null);
		return cursor;
	}
	
	public Cursor getZeroConfSubscription(String name) {
		Cursor cursor = db.query(true, TABLE_SUBSCRIPTIONS, new String[] {
				KEY_ROWID, KEY_NAME, KEY_ADDRESS, KEY_PASSWORD, KEY_ZERO_CONF, KEY_STATUS },
				KEY_NAME + "=? AND " + KEY_ZERO_CONF + "!=0",
				new String[] { name }, null, null, null, null);
		return cursor;
	}
}
