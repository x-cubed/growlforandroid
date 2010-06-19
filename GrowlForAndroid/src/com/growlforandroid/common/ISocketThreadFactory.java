package com.growlforandroid.common;

import java.nio.channels.SocketChannel;

public interface ISocketThreadFactory {
	public Thread createThread(SocketAcceptor socketAcceptor, long connectionID, SocketChannel channel);
}
