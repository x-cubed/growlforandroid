package com.growlforandroid.common;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;

import com.growlforandroid.client.GrowlListenerService;
import android.content.Context;
import android.util.Log;

/**
 * Listens for incoming connections on the specified server socket,
 * then spawns a new thread to handle each one.
 */
public class SocketAcceptor extends Thread {
	private final Context _context;
	private final InetSocketAddress _listenAddress;
	private final ISocketThreadFactory _threadFactory;
	private ServerSocketChannel _channel;
	
	private boolean _listening = true;
	private Set<Thread> _activeConnections = new HashSet<Thread>();
	
	public SocketAcceptor(Context context, ISocketThreadFactory threadFactory, InetSocketAddress listenAddress) throws IOException {
		_context = context;
		_threadFactory = threadFactory;
		_listenAddress = listenAddress;
		
		_channel = ServerSocketChannel.open();
		_channel.socket().bind(_listenAddress);
	}
	
	public SocketAcceptor(GrowlListenerService context,	ISocketThreadFactory factory, int port) throws IOException {
		this(context, factory, new InetSocketAddress(port));
	}

	public Context getContext() {
		return _context;
	}
	
	public void run() {
		_listening = true;
		while (_listening) {
			try {
				// Wait for an incoming connection
				SocketChannel incoming = _channel.accept();
				
				// Start a new thread for this connection
				int connectionID = _activeConnections.size();
				Thread connection = _threadFactory.createThread(this, connectionID, incoming);
				connection.start();
				
				_activeConnections.add(connection);
				
			} catch (ClosedByInterruptException x) {
				// stopListening was called
				_listening = false;
				
			} catch (Exception x) {
				Log.e("SocketAcceptor.run", x.toString());
				_listening = false;
			}
		}
	}
	
	public void stopListening() {
		_listening = false;
		interrupt();
	}
	
	protected void finalize() {
		stopListening();
		closeConnections();
		
		try {
			if (_channel != null) {
				_channel.close();
				_channel = null;
			}
		} catch (IOException ioe) {
			Log.e("SocketAcceptor.finalize", ioe.toString());
		}
	}
	
	public void closeConnections() {
		// Interrupt all the current connections with an InterruptedException
		for(Thread thread : _activeConnections) {
			thread.interrupt();
		}
		
		// Wait for all the active connections to close
		int lastSize = -1;
		int size = _activeConnections.size();
		while (size > 0) {
			if (lastSize != size) {
				lastSize = size;
			}
			try {
				wait(100);
			} catch (InterruptedException ie) {
			}
			size = _activeConnections.size();
		}
	}

	public void connectionClosed(Thread connection) {
		_activeConnections.remove(connection);
	}
}
