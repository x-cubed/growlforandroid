package com.growlforandroid.gntp;

import java.nio.channels.SocketChannel;

import com.growlforandroid.common.*;

public class GntpListenerThreadFactory implements ISocketThreadFactory {
	private final IGrowlService _service;
	
	public GntpListenerThreadFactory(IGrowlService service) {
		_service = service;
	}
	
	public Thread createThread(SocketAcceptor socketAcceptor, long connectionID, SocketChannel channel) {
		return new GntpListenerThread(socketAcceptor, _service, connectionID, channel);
	}
}
