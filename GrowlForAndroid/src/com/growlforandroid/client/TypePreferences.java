package com.growlforandroid.client;

import com.growlforandroid.common.Database;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;

public class TypePreferences
	extends Activity {

	private long _typeId = -1;
	
	private Database _database;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setTitle(R.string.type_title);
        setContentView(R.layout.type_dialog);

        _typeId = this.getIntent().getLongExtra("ID", -1);
        _database = new Database(this);
        refresh();
    }

    private void refresh() {
    	Cursor typeCursor = _database.getApplication(_typeId);
    	if (typeCursor.moveToFirst()) {
    		String appName = typeCursor.getString(typeCursor.getColumnIndex(Database.KEY_DISPLAY_NAME));
    		boolean enabled = typeCursor.getInt(typeCursor.getColumnIndex(Database.KEY_ENABLED)) != 0;
    		
    		TextView txtTitle = (TextView)findViewById(R.id.txtAppName);
    		txtTitle.setText(appName);
    		
    		/* CheckBox chkEnabled = (CheckBox)findViewById(R.id.chkEnabled);
    		chkEnabled.setChecked(enabled);*/
    	}
    	typeCursor.close();
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

}
