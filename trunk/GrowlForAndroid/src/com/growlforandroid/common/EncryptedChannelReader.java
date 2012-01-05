package com.growlforandroid.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.*;

import javax.crypto.*;

import com.growlforandroid.gntp.*;

public class EncryptedChannelReader extends ChannelReader {
	private static final byte[] END_OF_LINE = new byte[] { 0x0D, 0x0A };
	private static final byte[] END_OF_BLOCK = new byte[] { 0x0D, 0x0A, 0x0D, 0x0A };

	public EncryptedChannelReader(SocketChannel channel) {
		super(channel);
	}

	private byte[] decrypt(byte[] encrypted, EncryptionType type, byte[] iv, byte[] key) throws DecryptionException {
		try {
			Cipher decryptor = type.createDecryptor(iv, key);
			byte[] decrypted = decryptor.doFinal(encrypted);
			return decrypted;
		} catch (Exception x) {
			throw new DecryptionException(x);
		}
	}

	/**
	 * Reads all the data up to the next CRLF, CRLF sequence, then decrypts it
	 * using the specified parameters.
	 * 
	 * @param type
	 *            The encryption algorithm to use
	 * @param iv
	 *            The initialization vector
	 * @param key
	 *            The decryption key
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidAlgorithmParameterException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public void decryptNextBlock(EncryptionType type, byte[] iv, byte[] key) throws IOException, DecryptionException {

		// If we aren't dealing with encrypted content, we're already decrypted
		if (type == EncryptionType.None)
			return;

		// If we're missing anything, we should give up now
		if (iv == null)
			throw new DecryptionException("Missing initialisation vector");
		if (key == null)
			throw new DecryptionException("Missing decryption key");

		// Read the encrypted data until we find two CRLFs, then strip them from
		// the end of the data
		ByteBuffer encryptedBuffer = readBytesUntil(END_OF_BLOCK);
		int encryptedLength = encryptedBuffer.position() - END_OF_BLOCK.length;
		encryptedBuffer.rewind();
		byte[] encrypted = new byte[encryptedLength];
		encryptedBuffer.get(encrypted, 0, encrypted.length);

		// Do the actual decryption
		byte[] decrypted = decrypt(encrypted, type, iv, key);

		// Grab any remaining data out of the buffer
		byte[] buffered = new byte[_availableBytes];
		if (_availableBytes != 0) {
			_buffer.get(buffered, 0, buffered.length);
		}

		// Create a new buffer containing the decrypted data followed by what
		// was left in the old buffer
		int newBufferSize = decrypted.length + END_OF_LINE.length + buffered.length;
		ByteBuffer newBuffer = ByteBuffer.allocate(newBufferSize);
		newBuffer.put(decrypted);
		newBuffer.put(END_OF_LINE);
		newBuffer.put(buffered);
		newBuffer.rewind();

		// Replace the buffer of the underlying ChannelReader so that the other
		// methods work seamlessly
		_buffer = newBuffer;
		_availableBytes = newBufferSize;
	}

	/**
	 * Reads a fixed number of bytes from the channel then decrypts them
	 * 
	 * @param length
	 *            The number of bytes to be read
	 * @param type
	 *            The encryption type to use when decrypting
	 * @param iv
	 *            The initialization vector
	 * @param key
	 *            The decryption key
	 * @return
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidAlgorithmParameterException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public byte[] readAndDecryptBytes(int length, EncryptionType type, byte[] iv, byte[] key) throws IOException,
			DecryptionException {

		byte[] encrypted = readBytes(length);
		return decrypt(encrypted, type, iv, key);
	}

	public File readAndDecryptBytesToCacheFile(long length, EncryptionType type, byte[] iv, byte[] key, File folder,
			String fileName) throws IOException, DecryptionException {

		File cacheFile = null;
		FileOutputStream output = null;
		try {
			// Create a new file and marker it for deletion when we exit
			cacheFile = new File(folder, fileName);
			output = new FileOutputStream(cacheFile);

			boolean isFinalBlock = false;
			int blockLength = BUFFER_SIZE;
			Cipher decryptor = type.createDecryptor(iv, key);
			for (long offset = 0; offset < length; offset += BUFFER_SIZE) {
				long remaining = length - offset;
				if (remaining <= BUFFER_SIZE) {
					isFinalBlock = true;
					blockLength = (int)remaining;
				}
				byte[] raw = readBytes(blockLength);
				if (decryptor == null) {
					// Source is not encrypted, just save it
					output.write(raw);
				} else {
					// Source is encrypted, decrypt it first
					byte[] decrypted = (isFinalBlock) ? decryptor.doFinal(raw) : decryptor.update(raw);
					output.write(decrypted);
				}
			}

		} catch (InvalidKeyException x) {
			throw new DecryptionException(x);
		} catch (NoSuchAlgorithmException x) {
			throw new DecryptionException(x);
		} catch (NoSuchPaddingException x) {
			throw new DecryptionException(x);
		} catch (InvalidAlgorithmParameterException x) {
			throw new DecryptionException(x);
		} catch (IllegalBlockSizeException x) {
			throw new DecryptionException(x);
		} catch (BadPaddingException x) {
			throw new DecryptionException(x);
		} finally {
			if (output != null) {
				output.flush();
				output.close();
			}
		}
		return cacheFile;
	}

	public class DecryptionException extends Exception {
		private static final long serialVersionUID = 7004842321539940877L;

		public DecryptionException(String message) {
			super(message);
		}

		public DecryptionException(Exception innerException) {
			super("Decryption failed: " + innerException.getMessage(), innerException);
		}
	}
}
