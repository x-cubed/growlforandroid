package com.growlforandroid.common;

import java.net.URL;
import java.util.HashSet;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
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

	private static final String DATABASE_NAME = "growl";
	private static final int DATABASE_VERSION = 1;

	private final Context context;

	private Helper DBHelper;
	private SQLiteDatabase db;

	public Database(Context ctx) {
		this.context = ctx;
		DBHelper = new Helper(context);
		db = DBHelper.getWritableDatabase();
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
					+ "icon_url TEXT);");

			db.execSQL("CREATE TABLE notification_types ("
					+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ "app_id INTEGER NOT NULL, "
					+ "name TEXT NOT NULL, "
					+ "display_name TEXT NOT NULL, "
					+ "enabled INTEGER NOT NULL, "
					+ "icon_url TEXT, "
					+ "FOREIGN KEY(app_id) REFERENCES applications(id));");

			db.execSQL("CREATE TABLE passwords ("
					+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ "name TEXT NOT NULL, "
					+ "password TEXT);");
			
			db.execSQL("CREATE TABLE subscriptions ("
					+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ "name TEXT NOT NULL, "
					+ "address TEXT NOT NULL, "
					+ "password TEXT);");
			
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
			Log.w("Database", "Upgrading database from version "
							+ oldVersion + " to " + newVersion
							+ ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS applications;");
			db.execSQL("DROP TABLE IF EXISTS notification_types;");
			db.execSQL("DROP TABLE IF EXISTS passwords;");
			db.execSQL("DROP TABLE IF EXISTS subscriptions;");
			db.execSQL("DROP TABLE IF EXISTS notification_history;");
			onCreate(db);
		}
	}

	public void close() {
		DBHelper.close();
	}

	public int insertPassword(String name, String password) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NAME, name);
		initialValues.put(KEY_PASSWORD, password);
		return (int) db.insert("passwords", null, initialValues);
	}

	public boolean deletePassword(long rowId) {
		Log.i("Database.deletePassword", "Deleting password " + rowId);
		return db.delete("passwords", KEY_ROWID + "=" + rowId, null) > 0;
	}

	public Cursor getAllPasswordsAndNames() {
		return db.query("passwords", new String[] { KEY_ROWID, KEY_NAME,
				KEY_PASSWORD }, null, null, null, null, null);
	}

	public String[] getAllPasswords() {
		HashSet<String> passwords = new HashSet<String>();
		Cursor cursor = getAllPasswordsAndNames();
		if (cursor.moveToFirst()) {
			final int passwordColumn = cursor.getColumnIndex(KEY_PASSWORD);
			do {
				String password = cursor.getString(passwordColumn);
				Log.i("Database.getAllPasswords()", "Adding password "
						+ password);
				passwords.add(password);
			} while (cursor.moveToNext());
		}
		cursor.close();
		return passwords.toArray(new String[0]);
	}

	public int insertApplication(String name, Boolean enabled, URL iconUrl) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NAME, name);
		initialValues.put(KEY_ENABLED, enabled ? 1 : 0);

		String url = iconUrl == null ? null : iconUrl.toString();
		initialValues.put(KEY_ICON_URL, url);
		return (int) db.insert("applications", null, initialValues);
	}

	public boolean deleteApplication(long rowId) {
		return db.delete("applications", KEY_ROWID + "=" + rowId, null) > 0;
	}

	public Cursor getAllApplications() {
		return db.query("applications", new String[] {
				KEY_ROWID, KEY_NAME, KEY_ENABLED, KEY_ICON_URL },
				null, null, null, null, null);
	}

	public Cursor getApplication(int id) throws SQLException {
		Cursor cursor = db.query(true, "applications", new String[] {
				KEY_ROWID, KEY_NAME, KEY_ENABLED, KEY_ICON_URL }, KEY_ROWID
				+ "=" + id, null, null, null, null, null);
		return cursor;
	}

	public boolean updateApplication(int id, String name, String iconUrl) {
		ContentValues args = new ContentValues();
		args.put(KEY_NAME, name);
		return db.update("applications", args, KEY_ROWID + "=" + id, null) > 0;
	}

	public Cursor getNotificationType(int appId, String typeName) {
		Cursor cursor = db.query(true, "notification_types", new String[] {
				KEY_ROWID, KEY_NAME, KEY_DISPLAY_NAME, KEY_ENABLED,
				KEY_ICON_URL }, KEY_APP_ID + "=" + appId + " AND " + KEY_NAME
				+ "=?", new String[] { typeName }, null, null, null, null);
		return cursor;
	}

	public int insertNotificationType(int appId, String typeName,
			String displayName, boolean enabled, URL iconUrl) {

		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_APP_ID, appId);
		initialValues.put(KEY_NAME, typeName);
		initialValues.put(KEY_DISPLAY_NAME, displayName);
		initialValues.put(KEY_ENABLED, enabled ? 1 : 0);

		String url = iconUrl == null ? null : iconUrl.toString();
		initialValues.put(KEY_ICON_URL, url);
		return (int) db.insert("notification_types", null, initialValues);
	}
	
	public boolean deleteNotificationType(long rowId) {
		boolean history = db.delete("notification_history", KEY_TYPE_ID + "=" + rowId, null) > 0;
		boolean type = db.delete("notification_types", KEY_ROWID + "=" + rowId, null) > 0;
		return history && type;
	}

	public int insertNotificationHistory(int typeID, String title, String message, URL iconUrl, String origin) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_TYPE_ID, typeID);
		initialValues.put(KEY_RECEIVED_AT, System.currentTimeMillis());
		initialValues.put(KEY_TITLE, title);
		initialValues.put(KEY_MESSAGE, message);

		String url = iconUrl == null ? null : iconUrl.toString();
		initialValues.put(KEY_ICON_URL, url);
		
		initialValues.put(KEY_ORIGIN, origin);
		return (int) db.insert("notification_history", null, initialValues);
	}
	
	public Cursor getNotificationHistory() {
		return db.query("notification_history", new String[] {
				KEY_ROWID, KEY_TYPE_ID, KEY_RECEIVED_AT, KEY_TITLE, KEY_MESSAGE, KEY_ICON_URL, KEY_ORIGIN },
				null, null, null, null, KEY_RECEIVED_AT + " DESC");
	}
}
