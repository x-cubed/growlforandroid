package com.growlforandroid.common;

import java.net.URL;
import java.util.HashSet;

import com.growlforandroid.client.R;

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

	public static final String KEY_APP_NAME = "appName";
	public static final String KEY_TYPE_DISPLAY_NAME = "typeName";
	public static final String KEY_TYPE_LIST = "typeList";
	
	public static final String TABLE_APPLICATIONS = "applications";
	public static final String TABLE_NOTIFICATION_TYPES = "notification_types";
	public static final String TABLE_PASSWORDS = "passwords";
	public static final String TABLE_SUBSCRIPTIONS = "subscriptions";
	public static final String TABLE_NOTIFICATION_HISTORY = "notification_history";
	
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
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_APPLICATIONS + ";");
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTIFICATION_TYPES + ";");
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_PASSWORDS + ";");
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_SUBSCRIPTIONS + ";");
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTIFICATION_HISTORY + ";");
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
		return (int) db.insert(TABLE_PASSWORDS, null, initialValues);
	}

	public boolean deletePassword(long rowId) {
		Log.i("Database.deletePassword", "Deleting password " + rowId);
		return db.delete(TABLE_PASSWORDS, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public Cursor getAllPasswordsAndNames() {
		return db.query(TABLE_PASSWORDS, new String[] { KEY_ROWID, KEY_NAME,
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
				KEY_ROWID, KEY_NAME, KEY_ENABLED, KEY_ICON_URL },
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

	public Cursor getApplication(long itemId) throws SQLException {
		Cursor cursor = db.query(true, TABLE_APPLICATIONS, new String[] {
				KEY_ROWID, KEY_NAME, KEY_ENABLED, KEY_ICON_URL }, KEY_ROWID
				+ "=" + itemId, null, null, null, null, null);
		return cursor;
	}

	public boolean updateApplication(long id, String name, String iconUrl) {
		ContentValues args = new ContentValues();
		args.put(KEY_NAME, name);
		return db.update(TABLE_APPLICATIONS, args, KEY_ROWID + "=" + id, null) > 0;
	}

	public Cursor getNotificationType(long appId, String typeName) {
		Cursor cursor = db.query(true, TABLE_NOTIFICATION_TYPES, new String[] {
				KEY_ROWID, KEY_NAME, KEY_DISPLAY_NAME, KEY_ENABLED,
				KEY_ICON_URL }, KEY_APP_ID + "=" + appId + " AND " + KEY_NAME
				+ "=?", new String[] { typeName }, null, null, null, null);
		return cursor;
	}

	public Cursor getNotificationTypes(long appId) {
		Cursor cursor = db.query(true, TABLE_NOTIFICATION_TYPES, new String[] {
				KEY_ROWID, KEY_NAME, KEY_DISPLAY_NAME, KEY_ENABLED,
				KEY_ICON_URL }, KEY_APP_ID + "=" + appId,
				null, null, null, null, null);
		return cursor;
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

	public int insertNotificationHistory(long typeID, String title, String message, URL iconUrl, String origin) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_TYPE_ID, typeID);
		initialValues.put(KEY_RECEIVED_AT, System.currentTimeMillis());
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
}
