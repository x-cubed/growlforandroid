package com.growlforandroid.common;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

import com.growlforandroid.gntp.Constants;

public class GrowlResource {
	public final Map<String, String> Headers = new HashMap<String, String>();
	private File _sourceFile = null;
	
	public GrowlResource() {
	}
	
	public GrowlResource(String identifier, long length) {
		Headers.put(Constants.HEADER_RESOURCE_IDENTIFIER, identifier);
		Headers.put(Constants.HEADER_RESOURCE_LENGTH, Long.toString(length));
	}
	
	public long getLength() {
		return Long.parseLong(Headers.get(Constants.HEADER_RESOURCE_LENGTH));
	}
	
	public String getIdentifier() {
		return Headers.get(Constants.HEADER_RESOURCE_IDENTIFIER);
	}

	public void setSourceFile(File tempResource) {
		_sourceFile = tempResource;
	}
	
	public File getSourceFile() {
		return _sourceFile;
	}
	
	public void deleteSourceFile() {
		Log.i("GrowlResource.finalize", "Deleting source file " + _sourceFile.getAbsolutePath());
		_sourceFile.delete();
		_sourceFile = null;
	}
	
	protected void finalize() {
		if (_sourceFile != null) {
			deleteSourceFile();
		}
	}
}
