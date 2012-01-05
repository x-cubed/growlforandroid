package com.growlforandroid.common;

import com.growlforandroid.gntp.HashAlgorithm;

public interface IGrowlService extends IGrowlRegistry {
	void connectionClosed(Thread thread);

	void registerResource(GrowlResource currentResource);

	byte[] getMatchingKey(HashAlgorithm algorithm, String hash, String salt);

	void displayNotification(GrowlNotification notification);
}
