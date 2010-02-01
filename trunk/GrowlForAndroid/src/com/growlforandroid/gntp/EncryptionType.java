package com.growlforandroid.gntp;

import java.util.EnumSet;

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
	
	public static EncryptionType fromString(String encryptionType) {
		for (EncryptionType type : EnumSet.allOf(EncryptionType.class)) {
			if (type.Type.equals(encryptionType))
				return type;
		}
		return null;
	}
}
