package com.growlforandroid.common;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.*;

import javax.crypto.*;

import android.util.Log;

import com.growlforandroid.gntp.*;

public class EncryptedChannelReader
	extends ChannelReader {

	private static final byte[] END_OF_BLOCK = new byte[] { 0x0D, 0x0A, 0x0D, 0x0A };

	public EncryptedChannelReader(SocketChannel channel) {
		super(channel);
	}
	
	private byte[] decrypt(byte[] encrypted, EncryptionType type, byte[] iv, byte[] key)
		throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
		InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		
		Cipher decryptor = type.createDecryptor(iv, key);
		byte[] decrypted = decryptor.doFinal(encrypted);
		return decrypted;
	}
	
	/**
	 * Reads all the data up to the next CRLF, CRLF sequence, then decrypts it using the
	 * specified parameters.
	 * @param type		The encryption algorithm to use
	 * @param iv		The initialisation vector
	 * @param key		The decryption key
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidAlgorithmParameterException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public void decryptNextBlock(EncryptionType type, byte[] iv, byte[] key)
		throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
		InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		
		if ((type == EncryptionType.None) || (iv == null) || (key == null)) {
			// Nothing to decrypt
			return;
		}
		
		// Read the encrypted data until we find two CRLFs, then strip them from the end of the data
		ByteBuffer encryptedBuffer = readBytesUntil(END_OF_BLOCK);
		int encryptedLength = encryptedBuffer.position() - END_OF_BLOCK.length;		
		Log.i("EncryptedChannelReader.decryptNextBlock", "Encrypted data (" + encryptedLength + " bytes):");
		encryptedBuffer.rewind();
		byte[] encrypted = new byte[encryptedLength];
		encryptedBuffer.get(encrypted, 0, encrypted.length);
		Utility.logByteArrayAsHex("EncryptedChannelReader.decryptNextBlock", encrypted);
		
		// Do the actual decryption
		byte[] decrypted = decrypt(encrypted, type, iv, key);
		Log.i("EncryptedChannelReader.decryptNextBlock", "Decrypted data:");
		Utility.logByteArrayAsHex("EncryptedChannelReader.decryptNextBlock", decrypted);
		
		// Grab any remaining data out of the buffer
		byte[] buffered = new byte[_availableBytes - _buffer.position()];
		_buffer.get(buffered, 0, buffered.length);
		Log.i("EncryptedChannelReader.decryptNextBlock", "Buffered data:");
		Utility.logByteArrayAsHex("EncryptedChannelReader.decryptNextBlock", buffered);

		// Create a new buffer containing the decrypted data followed by what was left in the old buffer
		ByteBuffer newBuffer = ByteBuffer.allocate(decrypted.length + buffered.length);
		newBuffer.put(decrypted);
		newBuffer.put(buffered);
		
		// Replace the buffer of the underlying ChannelReader so that the other methods work seamlessly
		_buffer = newBuffer;
		_availableBytes = newBuffer.capacity();
	}
	
	/**
	 * Reads a fixed number of bytes from the channel then decrypts them
	 * @param length	The number of bytes to be read
	 * @param type		The encryption type to use when decrypting
	 * @param iv		The initialisation vector
	 * @param key		The decryption key
	 * @return
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidAlgorithmParameterException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public byte[] readAndDecryptBytes(int length, EncryptionType type, byte[] iv, byte[] key)
		throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
		InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		
		byte[] encrypted = readBytes(length);
		return decrypt(encrypted, type, iv, key);
	}
}
