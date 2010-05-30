package com.growlforandroid.gntp;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;

import javax.crypto.*;
import javax.crypto.spec.*;

public enum EncryptionType {
	None ("NONE"),
	AES ("AES"),
	DES ("DES"),
	TripleDES ("3DES");
	
	public final String Type;
	
	private EncryptionType(String type) {
		Type = type;
	}
	
	public String toString() {
		return Type;
	}
	
	public Cipher createDecryptor(byte[] iv, byte[] key)
		throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		
		if (this == EncryptionType.None) {
			return null;
		}

		IvParameterSpec ivSpec = new IvParameterSpec(iv);
		SecretKeySpec secretKey = new SecretKeySpec(key, Type);
		
		String algorithmAndOptions = Type + "/CBC/PKCS5Padding";
		Cipher cipher = Cipher.getInstance(algorithmAndOptions);
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
