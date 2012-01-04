package com.growlforandroid.common;

import java.io.*;
import java.net.*;
import java.util.*;

import com.growlforandroid.gntp.HashAlgorithm;

import android.util.Log;

public class GrowlResources implements URLStreamHandlerFactory {
	public final static int MAX_ICON_SIZE = 100;
	private final static String PROTOCOL = "x-growl-resource";

	private final File _cacheDir;
	private final ResourceHandler _handler = new ResourceHandler();
	private final Map<String, GrowlResource> _resources = new HashMap<String, GrowlResource>();

	public GrowlResources(File cacheDir) {
		_cacheDir = cacheDir;

		// Register a protocol handler for x-growl-resource:// URLs
		try {
			Log.d("GrowlRegistry", "Registering protocol handler");
			URL.setURLStreamHandlerFactory(this);
		} catch (Throwable t) {
			Log.e("GrowlRegistry", "Failed to register protocol handler: " + t);
		}
	}

	public URLStreamHandler createURLStreamHandler(String protocol) {
		Log.d("GrowlResources.createURLStreamHandler", "Protocol: " + protocol);
		return (PROTOCOL.equals(protocol)) ? _handler : null;
	}

	public GrowlResource get(String identifier) {
		return _resources.get(identifier);
	}

	public GrowlResource getOrCreate(String identifier) {
		GrowlResource resource = get(identifier);
		if (resource == null) {
			byte[] idHash = HashAlgorithm.MD5.calculateHash(identifier.getBytes());
			String fileName = Utility.getHexStringFromByteArray(idHash);
			File cacheFile = new File(_cacheDir, fileName);
			if (cacheFile.exists()) {
				Log.d("GrowlResources.getOrCreate", "Creating resource from cache file " + fileName);
				resource = new GrowlResource(identifier, cacheFile.length());
				resource.setSourceFile(cacheFile);
			} else {
				Log.d("GrowlResources.getOrCreate", "No such cache file " + fileName);
			}
		}
		return resource;
	}

	public InputStream get(URL url) throws IOException {
		URLConnection connection = url.openConnection();
		connection.connect();
		return connection.getInputStream();
	}

	public void put(GrowlResource resource) {
		String identifier = resource.getIdentifier();
		Log.i("GrowlResources.put", "Adding resource " + identifier);
		_resources.put(identifier, resource);
	}

	private class ResourceHandler extends URLStreamHandler {
		@Override
		protected URLConnection openConnection(URL u) throws IOException {
			Log.i("ResourceHandler.openConnection", u.toExternalForm());
			return new ResourceURLConnection(u);
		}
	}

	private class ResourceURLConnection extends URLConnection {
		private String _identifier;
		private GrowlResource _resource;

		protected ResourceURLConnection(URL url) {
			super(url);

			_identifier = url.getHost();
			Log.i("ResourceURLConnection", "Connecting to resource " + _identifier);
			_resource = getOrCreate(_identifier);
		}

		@Override
		public void connect() throws IOException {
		}

		synchronized public InputStream getInputStream() throws IOException {
			if (_resource == null) {
				return null;
			}
			
			Log.i("ResourceURLConnection", "Retrieving " + _identifier + "...");
			try {
				File source = _resource.getSourceFile();
				FileInputStream inputStream = new FileInputStream(source);
				return inputStream;
			} catch (Exception x) {
				Log.e("ResourceURLConnection", "Failed to retrieve " + _identifier + "\n" + x.toString());
				return null;
			}
		}
	}
}
