package com.growlforandroid.client;

import java.nio.channels.ServerSocketChannel;

import com.growlforandroid.common.IGrowlRegistry;

import android.util.Log;

/*
 * Listens for incoming connections on the specified server socket,
 * then spawns a new GNTPListenerThread to handle each one.
 */
public class SocketAcceptor extends Thread {
	private final IGrowlRegistry _registry;
	private final ServerSocketChannel _channel;
	private boolean _listening = true;
	
	public SocketAcceptor(IGrowlRegistry registry, ServerSocketChannel channel) {
		_registry = registry;
		_channel = channel;
	}
	
	public void run() {
		Log.i("SocketAcceptor.run", "Started listening for incoming connections...");
		while (_listening) {
			try {
				// Start a new thread for this connection
				new GntpListenerThread(_registry, _channel.accept()).start();				
			} catch (Exception x) {
				Log.e("SocketAcceptor.run", x.toString());
				_listening = false;
			}
		}
		Log.i("SocketAcceptor.run", "Stopped listening for incoming connections");
	}
}
