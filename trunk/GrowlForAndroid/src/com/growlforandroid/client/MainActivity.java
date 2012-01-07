package com.growlforandroid.client;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.growlforandroid.common.Database;
import com.growlforandroid.common.GrowlApplication;
import com.growlforandroid.common.GrowlNotification;
import com.growlforandroid.common.GrowlRegistry;
import com.growlforandroid.common.IGrowlRegistry;
import com.growlforandroid.common.NotificationType;
import com.growlforandroid.common.Utility;

import android.app.*;
import android.content.*;
import android.content.res.Resources;
import android.os.*;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends Activity implements GrowlListenerService.StatusChangedHandler {

	private static final int MAX_HISTORY_ITEMS = 50;

	private static final int DIALOG_DELETE_PROMPT = 2;
	private static final int DIALOG_ABOUT = 3;

	private ListenerServiceConnection _service;
	private Database _database;
	private IGrowlRegistry _registry;
	private NotificationListAdapter _adapter;

	private ListView _lsvNotifications;
	private ToggleButton _tglServiceState;
	private TextView _txtServiceState;

	private MenuItem _mniApplications;
	private MenuItem _mniPreferences;
	private MenuItem _mniClearHistory;
	private MenuItem _mniAbout;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		_service = new ListenerServiceConnection(this, this);
		_database = new Database(this);
		_registry = new GrowlRegistry(this, _database);

		// Load the default preferences
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		setContentView(R.layout.main);
		setTitle(R.string.app_name);

		// List the recent notifications
		_lsvNotifications = (ListView) findViewById(R.id.lsvNotifications);
		_lsvNotifications.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapter, View item, int position, long id) {
				GrowlNotification notification = _registry.getNotificationFromHistory(id);
				Dialog dialog = createNotificationMenu(notification);
				dialog.show();
			}
		});

		// Watch for button clicks
		_tglServiceState = (ToggleButton) findViewById(R.id.tglServiceState);
		_tglServiceState.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (!_tglServiceState.isChecked()) {
					_service.stop();
				} else {
					try {
						_service.start();
					} catch (Exception e) {
						// Failed to start the service
						Log.e("tglServiceState.onClick", e.toString());
						_tglServiceState.setChecked(false);
					}
				}
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();

		_txtServiceState = (TextView) findViewById(R.id.txtServiceState);
		updateServiceState();

		// Update the notification history, then scroll to the top (most recent)
		refresh();
		_lsvNotifications.setSelection(0);
	}

	private void refresh() {
		if (_adapter == null) {
			IGrowlRegistry registry = new GrowlRegistry(this, _database);
			_adapter = new NotificationListAdapter(this, getLayoutInflater(), registry, MAX_HISTORY_ITEMS);
			_lsvNotifications.setAdapter(_adapter);
		} else {
			_adapter.refresh();
		}
	}

	@Override
	public void onDestroy() {
		if (_service != null) {
			_service.unbind();
			_service = null;
		}

		if (_database != null) {
			_database.close();
			_database = null;
		}
		super.onDestroy();
	}

	protected void finalize() throws Throwable {
		onDestroy();
		super.finalize();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		_mniApplications = menu.add(R.string.menu_applications);
		_mniApplications.setIcon(android.R.drawable.ic_menu_manage);

		_mniPreferences = menu.add(R.string.menu_preferences);
		_mniPreferences.setIcon(android.R.drawable.ic_menu_preferences);

		_mniClearHistory = menu.add(R.string.menu_clear_history);
		_mniClearHistory.setIcon(android.R.drawable.ic_menu_delete);

		_mniAbout = menu.add(R.string.menu_about);
		_mniAbout.setIcon(android.R.drawable.ic_menu_info_details);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item == _mniApplications) {
			startActivity(new Intent(this, Applications.class));
			return true;

		} else if (item == _mniPreferences) {
			startActivity(new Intent(this, Preferences.class));
			return true;

		} else if (item == _mniClearHistory) {
			showDialog(DIALOG_DELETE_PROMPT);
			return true;

		} else if (item == _mniAbout) {
			showDialog(DIALOG_ABOUT);
			return true;

		} else {
			Log.e("MainActivity.onOptionsItemSelected", "Unknown menu item: " + item.getTitle());
			return false;
		}
	}

	@Override
	public Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_DELETE_PROMPT:
			return new AlertDialog.Builder(this).setTitle(R.string.history_title)
					.setMessage(R.string.history_delete_prompt)
					.setPositiveButton(R.string.button_delete, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							_database.deleteNotificationHistory();
							refresh();
						}
					}).setNegativeButton(R.string.button_cancel, null).create();

		case DIALOG_ABOUT:
			LayoutInflater factory = LayoutInflater.from(this);
			View aboutDialog = factory.inflate(R.layout.about_dialog, null);

			String appName = getString(R.string.app_name);
			String appVersion = getString(R.string.app_version);
			String title = appName + " " + appVersion;

			TextView txtCopyright = (TextView) aboutDialog.findViewById(R.id.txtCopyright);
			String copyright = getString(R.string.app_copyright_message);
			SpannableString spanText = new SpannableString(copyright);
			Linkify.addLinks(spanText, Linkify.ALL);
			txtCopyright.setMovementMethod(LinkMovementMethod.getInstance());
			txtCopyright.setText(spanText);

			return new AlertDialog.Builder(this).setIcon(R.drawable.launcher).setTitle(title).setView(aboutDialog)
					.setPositiveButton(R.string.dialog_ok, null).create();

		}
		return null;
	}

	private Dialog createNotificationMenu(final GrowlNotification notification) {
		if (notification == null) {
			return null;
		}
		
		final NotificationType type = notification.getType();
		final GrowlApplication app = type.Application;
		final Context context = this;
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(app.getName());
		
		// Menu items
		Resources resources = getResources();
		ArrayList<String> items = new ArrayList<String>();
		String appPrefs = resources.getString(R.string.notification_menu_app_preferences);
		items.add(appPrefs.replace("%s", app.getName()));
		
		String typePrefs = resources.getString(R.string.notification_menu_type_preferences);
		items.add(typePrefs.replace("%s", type.getDisplayName()));
		
		items.add(resources.getString(R.string.notification_menu_copy_message));
		items.add(resources.getString(R.string.notification_menu_delete));
		
		// The callback URL, if specified
		URL callbackUrl = notification.getCallbackUrl();
		if (callbackUrl != null) {
			items.add(callbackUrl.toExternalForm());
		}
		
		// Any other links in the message
		List<URL> links = Utility.findUrls(notification.getMessage());
		for(URL link:links) {
			items.add(link.toExternalForm());
		}

		// Create the menu and add item click handlers
		final String[] menuItems = items.toArray(new String[items.size()]);
		builder.setItems(menuItems, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
					case 0:
						// Application preferences
						Intent appPrefs = Application.createIntent(context, app);
						startActivity(appPrefs);
						break;

					case 1:
						// Notification type preferences
						Intent typePrefs = TypePreferences.createIntent(context, type);
						startActivity(typePrefs);
						break;

					case 2:
						// Copy message
						ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); 
						clipboard.setText(notification.getMessage());
						break;
						
					case 3:
						// Delete notification from history
						_database.deleteNotificationFromHistory(notification.getId());
						refresh();
						break;
						
					default:
						// A link
						String url = menuItems[which];
						Intent intent = Utility.createLaunchBrowserIntent(url);
						startActivity(intent);
						break;
				}
			}
		});
		return builder.create();
	}
	
	protected void updateServiceState() {
		onIsRunningChanged(_service.isRunning());
	}

	public void onApplicationRegistered(GrowlApplication app) {
	}

	public void onNotificationTypeRegistered(NotificationType type) {
	}

	public void onIsRunningChanged(boolean isRunning) {
		if (_tglServiceState.isChecked() != isRunning) {
			_tglServiceState.setChecked(isRunning);
		}
		_txtServiceState.setText(isRunning ? R.string.growl_on_status : R.string.growl_off_status);
	}

	public void onDisplayNotification(GrowlNotification notification) {
		// Update the history view
		this.runOnUiThread(new Runnable() {
			public void run() {
				refresh();
			}
		});
	}

	public void onSubscriptionStatusChanged(long id, String status) {
	}
}