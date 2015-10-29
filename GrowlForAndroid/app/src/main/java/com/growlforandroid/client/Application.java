package com.growlforandroid.client;

import com.growlforandroid.common.Database;
import com.growlforandroid.common.GrowlApplication;
import com.growlforandroid.common.GrowlRegistry;
import com.growlforandroid.common.IGrowlRegistry;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class Application extends Activity {
	private static final String EXTRA_ID = "ID";
	private static final int DIALOG_CHOOSE_DISPLAY = 0;

	private Database _database;
	private Cursor _cursor;
	private IGrowlRegistry _registry;
	private GrowlApplication _application;
	private TypeListAdapter _adapter;
	private int _typeId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle(R.string.application_title);
		setContentView(R.layout.application_dialog);

		_database = new Database(this);
		_registry = new GrowlRegistry(this, _database);

		final long APP_ID = getIntent().getIntExtra(EXTRA_ID, -1);
		_application = _registry.getApplication(APP_ID);
		if (_application == null) {
			return;
		}

		_adapter = new TypeListAdapter(this, getLayoutInflater(), _registry, _application);
		ListView lsvNotificationTypes = (ListView) findViewById(R.id.lsvNotificationTypes);
		lsvNotificationTypes.setAdapter(_adapter);
		lsvNotificationTypes.setTextFilterEnabled(true);

		final Application app = this;
		lsvNotificationTypes.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				_typeId = (int)id;
				Intent typePrefs = TypePreferences.createIntent(app, _typeId);
				startActivity(typePrefs);
			}
		});

		refresh();
	}

	@Override
	public void onResume() {
		super.onResume();
		refresh();
	}
	
	private void refresh() {
		String displayName = _database.getDisplayProfileName(_application.getDisplayId());
		if (displayName == null) {
			displayName = getText(R.string.application_option_display_default).toString();
		}

		ImageView imgAppIcon = (ImageView) findViewById(R.id.imgAppIcon);
		Bitmap icon = _application.getIcon();
		if (icon == null) {
			// Default icon
			icon = BitmapFactory.decodeResource(getResources(), R.drawable.launcher);
		}
		imgAppIcon.setImageBitmap(icon);

		TextView txtTitle = (TextView) findViewById(R.id.txtAppName);
		txtTitle.setText(_application.getName());

		PreferenceCheckBoxView chkEnabled = (PreferenceCheckBoxView) findViewById(R.id.chkEnabled);
		chkEnabled.setChecked(_application.isEnabled());
		chkEnabled.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				_database.setApplicationEnabled(_application.getId(), isChecked);
			}
		});

		PreferenceDropDownView drpDisplay = (PreferenceDropDownView) findViewById(R.id.drpDisplayProfile);
		drpDisplay.setSummary(displayName);
		drpDisplay.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showDialog(DIALOG_CHOOSE_DISPLAY);
			}
		});

		_adapter.refresh();
	}

	@Override
	public Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_CHOOSE_DISPLAY:
			return (new ChooseDisplayDialog(this, _database, true) {
				public void onDisplayChosen(Integer displayId) {
					_database.setApplicationDisplay(_application.getId(), displayId);
					refresh();
				}
			}).create();
		}
		return null;
	}

	@Override
	public void onDestroy() {
		if (_cursor != null) {
			_cursor.close();
			_cursor = null;
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
	
	public static Intent createIntent(Context context, GrowlApplication application) {
		return createIntent(context, application.getId());
	}
	
	public static Intent createIntent(Context context, int appId) {
		Intent intent = new Intent(context, Application.class);
		intent.putExtra(EXTRA_ID, appId);
		return intent;
	}
}
