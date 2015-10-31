package com.growlforandroid.client;

import com.growlforandroid.common.Database;
import com.growlforandroid.common.NotificationType;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class TypePreferences
	extends Activity {
	private static final String EXTRA_ID = "ID";
	private static final int DIALOG_CHOOSE_DISPLAY = 0;

	private int _typeId = -1;
	
	private Database _database;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setTitle(R.string.type_title);
        setContentView(R.layout.type_dialog);

        _typeId = this.getIntent().getIntExtra(EXTRA_ID, -1);
        _database = new Database(this);
        refresh();
    }

    private void refresh() {
    	Cursor typeCursor = _database.getNotificationType(_typeId);
    	if (typeCursor.moveToFirst()) {
    		int appId = typeCursor.getInt(typeCursor.getColumnIndexOrThrow(Database.KEY_APP_ID));
    		String appName = _database.getApplicationName(appId);
    		String typeName = typeCursor.getString(typeCursor.getColumnIndexOrThrow(Database.KEY_DISPLAY_NAME));
    		boolean enabled = typeCursor.getInt(typeCursor.getColumnIndexOrThrow(Database.KEY_ENABLED)) != 0;
    		int displayId = typeCursor.getInt(typeCursor.getColumnIndexOrThrow(Database.KEY_DISPLAY_ID));
    		String displayName = _database.getDisplayProfileName(displayId);
    		if (displayName == null) {
    			displayName = getText(R.string.type_display_default).toString();
    		}
    		
    		TextView txtTitle = (TextView)findViewById(R.id.txtAppName);
    		txtTitle.setText(appName);
    	
    		TextView lblOptions = (TextView)findViewById(R.id.lblOptions);
    		lblOptions.setText(typeName);
    		
    		PreferenceCheckBoxView chkEnabled = (PreferenceCheckBoxView)findViewById(R.id.chkEnabled);
    		chkEnabled.setChecked(enabled);
    		chkEnabled.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView,	boolean isChecked) {
					_database.setNotificationTypeEnabled(_typeId, isChecked);
				}
    		});
    		
    		PreferenceDropDownView drpDisplay = (PreferenceDropDownView)findViewById(R.id.drpDisplayProfile);
    		drpDisplay.setSummary(displayName);
    		drpDisplay.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					showDialog(DIALOG_CHOOSE_DISPLAY);
				}
    		});
    	}
    	typeCursor.close();
    }
    
    @Override
    public Dialog onCreateDialog(int id) {
    	switch (id) {
	    	case DIALOG_CHOOSE_DISPLAY:
	    		return (new ChooseDisplayDialog(this, _database, true) {
	    			public void onDisplayChosen(Integer displayId) {
	    				Log.i("TypePreferences.onDispl", "Type: " + _typeId + ", Display Id: " + displayId);
	    				_database.setNotificationTypeDisplay(_typeId, displayId);
	    				refresh();
	    			}
	    		}).create();
    	}
    	return null;
    }
    
    @Override
    public void onDestroy() {
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

    public static Intent createIntent(Context context, NotificationType type) {
    	return createIntent(context, type.getId());
    }
    
    public static Intent createIntent(Context context, int typeId) {
		Intent intent = new Intent(context, TypePreferences.class);
		intent.putExtra(EXTRA_ID, typeId);
		return intent;
	}
}
