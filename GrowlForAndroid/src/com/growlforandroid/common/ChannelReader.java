package com.growlforandroid.common;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.growlforandroid.gntp.Constants;

import android.util.Log;

/**
 * Reads byte and UTF-8 character data from a channel
 * @author Carey Bishop
  */
public class ChannelReader {
	private static final int CAPACITY = 8192;
	
	private static final byte UTF8_MULTI_START = (byte) 0x80;
	private static final byte UTF8_MULTI_MASK = (byte) 0xC0;
	
	private static final byte UTF8_MULTI_START_2 = (byte) 0xC0;
	private static final byte UTF8_MULTI_START_2_MASK = (byte) 0xE0;
	
	private static final byte UTF8_MULTI_START_3 = (byte) 0xE0;
	private static final byte UTF8_MULTI_START_3_MASK = (byte) 0xF0;
	
	private static final byte UTF8_MULTI_START_4 = (byte) 0xF0;
	private static final byte UTF8_MULTI_START_4_MASK = (byte) 0xF8;
	
	private final SocketChannel _channel;
	protected ByteBuffer _buffer;
	protected int _availableBytes = 0;

	public ChannelReader(SocketChannel channel) {
		_channel = channel;
		_buffer = ByteBuffer.allocateDirect(CAPACITY);
	}
	
	/**
	 * Fills the byte buffer with any available data from the channel
	 * and updates _availableBytes
	 * @throws IOException
	 */
	private void fillBuffer() throws IOException {
		_buffer.rewind();
		Log.i("ChannelReader.fillBuffer", "Filling the buffer...");
		while (_availableBytes == 0) {
			_availableBytes =_channel.read(_buffer);
		}
		Log.i("ChannelReader.fillBuffer", "Read " + _availableBytes + " bytes into the buffer");
		_buffer.rewind();
	}
	
	/**
	 * Reads an array of bytes from the channel
	 * @param length	The number of bytes to be read
	 * @return A new array of byte data
	 * @throws IOException
	 */
	public byte[] readBytes(int length) throws IOException {
		byte[] data = new byte[length];
		for(int i=0; i < data.length; i++) {
			data[i] = readByte();
		}
		return data;
	}
	
	/**
	 * Reads an array of bytes from the channel and discards the result
	 * @param length	The number of bytes to be skipped
	 * @throws IOException
	 */
	public void skipBytes(int length) throws IOException {
		for(int i=0; i < length; i++) {
			readByte();
		}
	}
	
	/**
	 * Reads a single byte from the channel
	 * @return
	 * @throws IOException
	 */
	public byte readByte() throws IOException {
		if (_availableBytes == 0) {
			fillBuffer();
		}
		byte data = _buffer.get();
		_availableBytes--;
		return data;
	}
	
	public ByteBuffer readBytesUntil(byte[] delimiter) throws IOException {
		byte endOfDelimiter = delimiter[delimiter.length - 1];
		ByteBuffer buffer = ByteBuffer.allocate(CAPACITY);
		
		while (true) {
			// Read a byte into the buffer
			byte data = readByte();
			buffer.put(data);
			
			// If this byte is the last byte of the delimiter
			if (data == endOfDelimiter) {
				// Check to see if the preceding bytes also match the delimiter
				boolean found = true;
				int startOfDelimiter = buffer.position() - delimiter.length;
				for (int i = 0; i < delimiter.length; i++) {
					data = buffer.get(startOfDelimiter + i);
					if (data != delimiter[i]) {
						found = false;
						break;
					}
				}
				
				if (found) {
					return buffer;
				}
			}
		}
	}
	
	/**
	 * Reads a byte from the channel and checks that it is a valid UTF-8 suffix byte
	 * @return
	 * @throws IOException
	 */
	private byte readUTF8MultiByte() throws IOException {
		byte data = readByte();
		if ((data & UTF8_MULTI_MASK) != UTF8_MULTI_START) {
			throw new IOException("Invalid UTF-8 data");
		}
		return data;
	}
	
	/**
	 * Reads a single UTF-8 character from the channel
	 * @return
	 * @throws IOException
	 */
	public char readChar() throws IOException {
		byte data = readByte();
		if ((data & UTF8_MULTI_START) == UTF8_MULTI_START) {
			if ((data & UTF8_MULTI_START_2_MASK) == UTF8_MULTI_START_2) {
				return readTwoByteChar(data);
			} else if ((data & UTF8_MULTI_START_3_MASK) == UTF8_MULTI_START_3) {
				return readThreeByteChar(data);
			} else if ((data & UTF8_MULTI_START_4_MASK) == UTF8_MULTI_START_4) {
				return readFourByteChar(data);
			}
			throw new IOException("Invalid UTF-8 data");
			
		} else {
			return (char)data;
		}
	}
	
	/**
	 * Reads a two-byte UTF-8 character from the channel
	 * @param firstByte		The contents of the first byte of the character byte sequence
	 * @return
	 * @throws IOException
	 */
	private char readTwoByteChar(byte firstByte) throws IOException {
		byte secondByte = readUTF8MultiByte();
		int data = ((firstByte & 0x1C) << 8) | (secondByte & 0x3F);
		return (char)data;
	}
	
	/**
	 * Reads a three-byte UTF-8 character from the channel
	 * @param firstByte		The contents of the first byte of the character byte sequence
	 * @return
	 * @throws IOException
	 */
	private char readThreeByteChar(byte firstByte) throws IOException {
		byte secondByte = readUTF8MultiByte();
		byte thirdByte = readUTF8MultiByte();
		int data =
			((firstByte & 0x0F) << 12) |
			((secondByte & 0x3F) << 6) |
			(thirdByte & 0x3F);
		return (char)data;
	}
	
	/**
	 * Reads a four-byte UTF-8 character from the channel
	 * @param firstByte		The contents of the first byte of the character byte sequence
	 * @return
	 * @throws IOException
	 */
	private char readFourByteChar(byte firstByte) throws IOException {
		byte secondByte = readUTF8MultiByte();
		byte thirdByte = readUTF8MultiByte();
		byte fourthByte = readUTF8MultiByte();
		int data =
			((firstByte & 0x07) << 18) |
			((secondByte & 0x3F) << 12) |
			((thirdByte & 0x3F) << 6) |
			(fourthByte & 0x3F);
		return (char)data;	
	}
	
	/**
	 * Reads characters from the channel until delimiter is found.
	 * The delimiter is included in the output. 
	 * @param delimiter		The character to stop reading at when found
	 * @return
	 * @throws IOException
	 */
	private String readCharsUntil(char delimiter) throws IOException {
		StringBuffer buffer = new StringBuffer(255);
		char next;
		do {
			next = readChar();
			buffer.append(next);
		} while (next != delimiter);
		
		String result = buffer.toString();
		return result;
	}
	
	/**
	 * Reads characters from the channel until delimiter is found.
	 * The delimiter is included in the output.
	 * @param delimiter		The string that delimits the end of the data
	 * @return
	 * @throws IOException
	 */
	public String readCharsUntil(String delimiter) throws IOException {
		String line = "";
		
		// Grab the last character of the delimiter
		char lastChar = delimiter.charAt(delimiter.length() - 1);
		while (!line.endsWith(delimiter)) {
			line += readCharsUntil(lastChar);
		}

		return line;
	}
	
	public String readLine() throws IOException {
		String line = readCharsUntil(Constants.END_OF_LINE);
		int withoutDelimiter = line.length() - Constants.END_OF_LINE.length();
		if ((line != null) && (withoutDelimiter >= 0))
			line = line.substring(0, withoutDelimiter);
		return line;
	}
}
