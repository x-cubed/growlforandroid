package com.growlforandroid.common;

import java.util.HashMap;
import java.util.Map;

import com.growlforandroid.gntp.Constants;

public class GrowlResource {
	public final Map<String, String> Headers = new HashMap<String, String>();
	
	public GrowlResource() {
	}
	
	public int getLength() {
		return Integer.parseInt(Headers.get(Constants.HEADER_RESOURCE_LENGTH));
	}
	
	public String getIdentifier() {
		return Headers.get(Constants.HEADER_RESOURCE_IDENTIFIER);
	}
	
	public void setData(byte[] data) {
		// TODO: Implement me
	}
	
	public byte[] getData() {
		// TODO: Implement me
		return null;
	}
}
