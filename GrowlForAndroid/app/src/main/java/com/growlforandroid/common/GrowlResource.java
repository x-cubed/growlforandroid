package com.growlforandroid.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import com.growlforandroid.gntp.Constants;

public class GrowlResource {
	private final static int MAX_ICON_SIZE = 100;
	private final static int ICON_QUALITY = 100;
	
	public final Map<String, String> Headers = new HashMap<String, String>();
	private File _cacheFile = null;
	
	private GrowlResource(File cacheFile) {
		_cacheFile = cacheFile;
	}
	
	public GrowlResource(Map<String, String> headers, File cacheFile) {
		this(cacheFile);
		Headers.putAll(headers);
	}
	
	public GrowlResource(String identifier, long length, File cacheFile) {
		this(cacheFile);
		Headers.put(Constants.HEADER_RESOURCE_IDENTIFIER, identifier);
		Headers.put(Constants.HEADER_RESOURCE_LENGTH, Long.toString(length));
	}
	
	public long getLength() {
		return Long.parseLong(Headers.get(Constants.HEADER_RESOURCE_LENGTH));
	}
	
	public String getIdentifier() {
		return Headers.get(Constants.HEADER_RESOURCE_IDENTIFIER);
	}
	
	public File getCacheFile() {
		return _cacheFile;
	}

	public void tryResizeBitmap() {
		Bitmap original;
		try {
			InputStream inputStream = new FileInputStream(_cacheFile);
			original = new BitmapDrawable(inputStream).getBitmap();
			inputStream.close();
		} catch (Exception x) {
			Log.e("GrowlResource.resizeBitmap", "Unable to load source: " + _cacheFile.getAbsolutePath() + "\n" + x.toString());
			return;
		}

		// Ensure that the icon isn't too big to display
		int width = original.getWidth();
		int height = original.getHeight();
		if ((width < MAX_ICON_SIZE) || (height < MAX_ICON_SIZE)) {
			// Image size is okay
			return;
		}

		// Reduce the size of the icon to something reasonable
		Bitmap scaledIcon = Bitmap.createScaledBitmap(original, MAX_ICON_SIZE, MAX_ICON_SIZE, true);
		try {
			FileOutputStream outputStream = new FileOutputStream(_cacheFile);
			scaledIcon.compress(CompressFormat.PNG, ICON_QUALITY, outputStream);
			outputStream.close();
		} catch (Exception x) {
			Log.e("GrowlResource.resizeBitmap", "Unable to save image: " + _cacheFile.getAbsolutePath() + "\n" + x.toString());
			return;
		}
	}
}
