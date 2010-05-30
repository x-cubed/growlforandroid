package com.growlforandroid.gntp;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;

import javax.crypto.*;
import javax.crypto.spec.*;

public enum EncryptionType {
	None ("NONE", "", 0),
	AES ("AES", "AES/CBC/PKCS5Padding", 24),
	DES ("DES", "DES/CBC/PKCS5Padding", 8),
	TripleDES ("3DES", "DESede/CBC/PKCS5Padding", 24);
	
	public final String Type;
	public final String CipherName;
	public final int KeyLength;
	
	private EncryptionType(String type, String cipherName, int keyLength) {
		Type = type;
		CipherName = cipherName;
		KeyLength = keyLength;
	}
	
	public String toString() {
		return Type;
	}
	
	public Cipher createDecryptor(byte[] iv, byte[] key)
		throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		
		if (this == EncryptionType.None) {
			return null;
		}
		
		if (key.length < KeyLength) {
			// Key is too short
			throw new InvalidKeyException("Key must be at least " + KeyLength + " bytes");
		}
		if (key.length > KeyLength) {
			// Key is too long, just take the first KeyLength bytes
			byte[] newKey = new byte[KeyLength];
			for(int i = 0; i<newKey.length; i++) {
				newKey[i] = key[i];
			}
			key = newKey;
		}

		IvParameterSpec ivSpec = new IvParameterSpec(iv);
		SecretKeySpec secretKey = new SecretKeySpec(key, Type);

		Cipher cipher = Cipher.getInstance(CipherName);
		cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
		
		return cipher;
	}
	
	public static EncryptionType fromString(String encryptionType) {
		for (EncryptionType type : EnumSet.allOf(EncryptionType.class)) {
			if (type.Type.equals(encryptionType))
				return type;
		}
		return null;
	}
}
