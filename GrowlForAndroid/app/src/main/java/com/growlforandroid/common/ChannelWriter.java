package com.growlforandroid.common;

import java.io.IOException;
import java.nio.*;
import java.nio.channels.SocketChannel;
import java.nio.charset.*;

public class ChannelWriter {
	private final SocketChannel _channel;
	private final CharsetEncoder _encoder;
	
	public ChannelWriter(SocketChannel channel, String charsetName) {
		this(channel, Charset.forName(charsetName));
	}
	
	public ChannelWriter(SocketChannel channel, Charset charset) {
		_channel = channel;
		_encoder = charset.newEncoder();
	}
		
	public void write(String text) throws IOException {
		CharBuffer charBuffer = CharBuffer.wrap(text);
		// Log.d("ChannelWriter.write", text);
		ByteBuffer byteBuffer = _encoder.encode(charBuffer);
		_channel.write(byteBuffer);
	}
}
