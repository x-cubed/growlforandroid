package com.growlforandroid.gntp;

import java.nio.channels.SocketChannel;

import com.growlforandroid.common.*;

public class GntpListenerThreadFactory implements ISocketThreadFactory {
	private final IGrowlRegistry _registry;
	
	public GntpListenerThreadFactory(IGrowlRegistry registry) {
		_registry = registry;
	}
	
	public Thread createThread(SocketAcceptor socketAcceptor, long connectionID, SocketChannel channel) {
		return new GntpListenerThread(socketAcceptor, connectionID, _registry, channel);
	}
}
