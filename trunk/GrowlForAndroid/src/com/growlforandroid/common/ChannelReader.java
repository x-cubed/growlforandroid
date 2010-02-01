package com.growlforandroid.common;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import android.util.Log;

public class ChannelReader {
	private static final int CAPACITY = 1024;
	
	private final CharsetDecoder _decoder;
	
	private final SocketChannel _channel;
	private final ByteBuffer _buffer;
	private CharBuffer _charBuffer;
	
	public ChannelReader(SocketChannel channel, String charsetName) throws IllegalCharsetNameException, UnsupportedCharsetException, CharacterCodingException { 
		this(channel, Charset.forName(charsetName));
	}
	
	public ChannelReader(SocketChannel channel, Charset charset) throws CharacterCodingException {
		_channel = channel;
		_decoder = charset.newDecoder();
		
		_buffer = ByteBuffer.allocateDirect(CAPACITY);
	}
	
	private void fillBuffer() throws IOException {
		_buffer.rewind();
		_channel.read(_buffer);
		_buffer.rewind();
		
		_charBuffer = _decoder.decode(_buffer);
		_charBuffer.rewind();
		Log.i("ChannelReader.fillBuffer", "Read " + _buffer.limit() + " bytes into buffer");
	}
	
	public char readChar() throws IOException {
		if (_charBuffer == null) {
			Log.i("ChannelReader.readChar", "Filling the buffer for the first time");
			fillBuffer();
		} else if (_charBuffer.limit() <= _charBuffer.position()) {
			Log.i("ChannelReader.readChar", "Position " + _charBuffer.position() + ", limit " + _charBuffer.limit() + ", so we need to fill the buffer");
			fillBuffer();
		}
		char result = _charBuffer.get();
		return result;
	}
	
	private String readCharsUntil(char delimiter) throws IOException {
		if (_charBuffer == null) {
			Log.i("ChannelReader.readCharsUntil", "Filling the buffer for the first time");
			fillBuffer();
		}
		int startPosition = _charBuffer.position();
		while (_charBuffer.get() != delimiter);
		int endPosition = _charBuffer.position();

		// Start position and end position for subSequence are relative, so we need to move to the start of the buffer
		_charBuffer.rewind();
		String line = _charBuffer.subSequence(startPosition, endPosition).toString();
		_charBuffer.position(endPosition);
		
		//Log.i("ChannelReader.readCharsUntil", "Read \"" + line + "\" from " + startPosition + " to " + endPosition);
		return line;
	}
	
	public String readCharsUntil(String delimiter) throws IOException {
		String line = "";
		
		// Grab the last character of the delimiter
		char lastChar = delimiter.charAt(delimiter.length() - 1);
		while (!line.endsWith(delimiter)) {
			line += readCharsUntil(lastChar);
		}

		return line;
	}
}
