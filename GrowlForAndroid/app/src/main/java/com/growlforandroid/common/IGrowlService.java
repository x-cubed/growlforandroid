package com.growlforandroid.common;

import com.growlforandroid.gntp.HashAlgorithm;

public interface IGrowlService {
	void connectionClosed(Thread thread);

	byte[] getMatchingKey(HashAlgorithm algorithm, String hash, String salt);

	boolean requiresPassword();

	void displayNotification(GrowlNotification notification);
	
	IGrowlRegistry getRegistry();
}
