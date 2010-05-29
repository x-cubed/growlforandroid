package com.growlforandroid.common;

import java.io.*;
import java.net.*;
import java.util.*;

import android.util.Log;

public class GrowlResources implements URLStreamHandlerFactory {
	private static String PROTOCOL = "x-growl-resource";
	private final ResourceHandler _handler = new ResourceHandler();
	private final Map<String, GrowlResource> _resources = new HashMap<String, GrowlResource>();
	
	public URLStreamHandler createURLStreamHandler(String protocol) {
		return (PROTOCOL.equals(protocol)) ? _handler : null;
	}
	
	public GrowlResource get(String identifier) {
		return _resources.get(identifier);
	}
	
	public void put(GrowlResource resource) {
		String identifier = resource.getIdentifier();
		Log.i("GrowlResources.put()", "Adding resource " + identifier);
		_resources.put(identifier, resource);
	}
	
	private class ResourceHandler extends URLStreamHandler {
		@Override
		protected URLConnection openConnection(URL u) throws IOException {
			return new ResourceURLConnection(u);
		}
	}
	
	private class ResourceURLConnection extends URLConnection {
		private String _identifier;
		private InputStream _inputStream;
		
		protected ResourceURLConnection(URL url) {
			super(url);
			_identifier = url.getHost();
		}

		@Override
		public void connect() throws IOException {
			Log.i("GrowlResourceURLConnection.connect()", "Connecting to resource " + _identifier);
			GrowlResource resource = get(_identifier);
		}
		
		synchronized public InputStream getInputStream() throws IOException {
			return _inputStream;
		}
	}
}
