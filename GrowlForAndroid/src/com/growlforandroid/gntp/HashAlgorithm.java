package com.growlforandroid.gntp;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;

import android.util.Log;

import com.growlforandroid.common.Utility;

public enum HashAlgorithm {
	NONE (null),
	MD5 ("MD5"),
	SHA1 ("SHA1"),
	SHA256 ("SHA256"),
	SHA512 ("SHA512");
	
	private static final Charset _charset = Charset.forName(Constants.CHARSET);
	public final String Type;
	private final MessageDigest Digest;
	
	private HashAlgorithm(String type) {
		this(type, getDigest(type));
	}
	
	private HashAlgorithm(String type, MessageDigest digest) {
		Type = type;
		Digest = digest;
	}
	
	public String toString() {
		return Type;
	}
	
	public byte[] calculateHash(String password, byte[] salt) {
		ByteBuffer pswdBytes = _charset.encode(password);
		ByteBuffer keyBytes = ByteBuffer.allocate(pswdBytes.limit() + salt.length);
		keyBytes.put(pswdBytes);
		keyBytes.put(salt);
		
		byte[] keyBasis = keyBytes.array();
		Log.i("calculateHash", "Key Basis: " + Utility.getHexStringFromByteArray(keyBasis));
		byte[] key = calculateHash(keyBasis);	
		byte[] hash = calculateHash(key);
		Log.i("calculateHash", name() + " Hash: " + Utility.getHexStringFromByteArray(hash));
		return hash;
	}
	
	public byte[] calculateHash(byte[] data) {
		if ((Digest == null) || (data == null))
			return null;
		
		byte[] hash = Digest.digest(data);
		Digest.reset();
		return hash;
	}
	
	private static MessageDigest getDigest(String name) {
		if (name == null)
			return null;
		
		try {
			return MessageDigest.getInstance(name);
		} catch (NoSuchAlgorithmException x) {
			x.printStackTrace();
			return null;
		}
	}
	
	public static HashAlgorithm fromString(String hashAlgorithm) {
		for (HashAlgorithm type : EnumSet.allOf(HashAlgorithm.class)) {
			if (type.Type == null) {
				if (hashAlgorithm == null)
					return type;
			} else {
				if (type.Type.equals(hashAlgorithm))
					return type;
			}
		}
		return null;
	}
}