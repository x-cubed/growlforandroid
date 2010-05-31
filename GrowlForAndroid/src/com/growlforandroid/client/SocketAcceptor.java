package com.growlforandroid.client;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;

import com.growlforandroid.common.IGrowlRegistry;

import android.util.Log;

/**
 * Listens for incoming connections on the specified server socket,
 * then spawns a new GNTPListenerThread to handle each one.
 */
public class SocketAcceptor extends Thread {
	private final IGrowlRegistry _registry;
	private final ServerSocketChannel _channel;
	private boolean _listening = true;
	private Set<GntpListenerThread> _activeConnections = new HashSet<GntpListenerThread>();
	
	public SocketAcceptor(IGrowlRegistry registry, ServerSocketChannel channel) {
		_registry = registry;
		_channel = channel;
	}
	
	public void run() {
		_listening = true;
		Log.i("SocketAcceptor.run", "Started listening for incoming connections...");
		while (_listening) {
			try {
				// Wait for an incoming connection
				SocketChannel incoming = _channel.accept();
				
				// Start a new thread for this connection
				int connectionID = _activeConnections.size();
				GntpListenerThread connection = new GntpListenerThread(this, connectionID, _registry, incoming);
				connection.start();
				
				_activeConnections.add(connection);
			} catch (Exception x) {
				Log.e("SocketAcceptor.run", x.toString());
				_listening = false;
			}
		}
		Log.i("SocketAcceptor.run", "Stopped listening for incoming connections");
	}
	
	public void stopListening() {
		_listening = false;
		interrupt();
	}
	
	public void closeConnections() {
		// Interrupt all the current connections with an InterruptedException
		for(GntpListenerThread thread : _activeConnections) {
			thread.interrupt();
		}
		
		// Wait for all the active connections to close
		int lastSize = -1;
		int size = _activeConnections.size();
		while (size > 0) {
			if (lastSize != size) {
				Log.i("SocketAcceptor.closeConnections", "Waiting for " + size + " connections to close...");
				lastSize = size;
			}
			try {
				wait(100);
			} catch (InterruptedException ie) {
			}
			size = _activeConnections.size();
		}
		Log.i("SocketAcceptor.closeConnections", "All connections closed");
	}

	public void connectionClosed(GntpListenerThread connection) {
		_activeConnections.remove(connection);
	}
}
