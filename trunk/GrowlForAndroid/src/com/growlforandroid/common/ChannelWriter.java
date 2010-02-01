package com.growlforandroid.common;

import java.io.IOException;
import java.nio.*;
import java.nio.channels.SocketChannel;
import java.nio.charset.*;

public class ChannelWriter {
	private static final int CAPACITY = 1024;
	
	public final CharsetEncoder _encoder;
	
	private final SocketChannel _channel;
	private final CharBuffer _charBuffer;
	
	public ChannelWriter(SocketChannel channel, String charsetName) {
		this(channel, Charset.forName(charsetName));
	}
	
	public ChannelWriter(SocketChannel channel, Charset charset) {
		_channel = channel;
		_encoder = charset.newEncoder();
		
		_charBuffer = CharBuffer.allocate(CAPACITY);
	}
	
	public void flush() throws IOException {
		ByteBuffer byteBuffer = _encoder.encode(_charBuffer);
		_channel.write(byteBuffer);
		_charBuffer.clear();
	}
	
	public void write(String text) throws IOException {
		_charBuffer.append(text);
	}
}
