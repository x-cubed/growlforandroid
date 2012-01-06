package com.growlforandroid.common;

import java.io.*;
import java.net.*;
import java.util.*;

import com.growlforandroid.gntp.HashAlgorithm;

import android.content.Context;
import android.util.Log;

public class GrowlResources implements URLStreamHandlerFactory {
	public final static int MAX_ICON_SIZE = 100;
	private final static String PROTOCOL = "x-growl-resource";
	private final static String NO_MEDIA = ".nomedia";

	private final File _resourcesDir;
	private final ResourceHandler _handler = new ResourceHandler();
	private final Map<String, GrowlResource> _resources = new HashMap<String, GrowlResource>();

	public GrowlResources(Context context) {
		_resourcesDir = getOrCreateResourcesDir(context);

		// Register a protocol handler for x-growl-resource:// URLs
		try {
			Log.d("GrowlResources", "Registering protocol handler");
			URL.setURLStreamHandlerFactory(this);
		} catch (Throwable t) {
			Log.e("GrowlResources", "Failed to register protocol handler: " + t);
		}
	}

	private static File getOrCreateResourcesDir(Context context) {
		File resourcesDir = context.getCacheDir();
		Log.i("GrowlResources.getOrCreateResourcesDir", "Using resource directory: " + resourcesDir);

		// Create a marker to suggest to media scanners that they should ignore these files
		File noMedia = new File(resourcesDir, NO_MEDIA);
		if (!noMedia.exists()) {
			try {
				noMedia.createNewFile();
			} catch (Exception x) {
			}
		}

		return resourcesDir;
	}

	public File getResourcesDir() {
		return _resourcesDir;
	}

	public URLStreamHandler createURLStreamHandler(String protocol) {
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
			File cacheFile = new File(_resourcesDir, fileName);
			if (cacheFile.exists()) {
				// We happen to have an existing cache file for this resource
				resource = new GrowlResource(identifier, cacheFile.length());
				resource.setSourceFile(cacheFile);
				put(resource);
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
			return new ResourceURLConnection(u);
		}
	}

	private class ResourceURLConnection extends URLConnection {
		private String _identifier;
		private GrowlResource _resource;

		protected ResourceURLConnection(URL url) {
			super(url);

			_identifier = url.getHost();
			_resource = getOrCreate(_identifier);
		}

		@Override
		public void connect() throws IOException {
		}

		synchronized public InputStream getInputStream() throws IOException {
			if (_resource == null) {
				return null;
			}

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
