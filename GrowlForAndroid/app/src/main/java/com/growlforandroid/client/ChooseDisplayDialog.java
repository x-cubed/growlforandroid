package com.growlforandroid.client;

import java.util.Hashtable;

import com.growlforandroid.common.Database;

import android.app.AlertDialog;
import android.content.*;
import android.database.Cursor;
import android.util.Log;

public class ChooseDisplayDialog {
	private final Context _context;
	
	private final Integer[] _displayIds;
	private final String[] _displayItems;
	
	public ChooseDisplayDialog(Context context, Database database, boolean showDefault) {
		_context = context;

		Hashtable<Integer, String> displayProfiles = new Hashtable<Integer, String>();
		Cursor cursor = database.getDisplayProfiles();
		while (cursor.moveToNext()) {
			int id = cursor.getInt(cursor.getColumnIndexOrThrow(Database.KEY_ROWID));
			String name = cursor.getString(cursor.getColumnIndexOrThrow(Database.KEY_NAME));
			displayProfiles.put(id, name);
		}
		cursor.close();
		
		int size = displayProfiles.size();
		if (showDefault) size++;
		_displayIds = new Integer[size];
		_displayItems = new String[size];
		int index = 0;
		if (showDefault) {
			_displayIds[index] = null;
			_displayItems[index] = context.getText(R.string.display_profile_default).toString();
			index++;
		}
		for(int id : displayProfiles.keySet()) {
			_displayIds[index] = id;
			_displayItems[index] = displayProfiles.get(id);
			index++;
		}
	}
	
	public AlertDialog create() {
		return new AlertDialog.Builder(_context)
	    	.setTitle(R.string.type_display_title)
	        .setItems(_displayItems, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int which) {
	            	Integer displayId = _displayIds[which];        		
	            	onDisplayChosen(displayId);
	            }
	        })
	        .create();
	}
	
	public void onDisplayChosen(Integer displayId) {
		Log.e("ChooseDisplay.onDisplayChosen", "No handler attached (displayId = " + displayId + ")");
	}
}
