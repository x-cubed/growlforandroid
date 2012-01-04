package com.growlforandroid.common;

import java.io.File;

import com.growlforandroid.gntp.HashAlgorithm;

public interface IGrowlService extends IGrowlRegistry {

	void connectionClosed(Thread thread);

	File getCacheDir();

	void registerResource(GrowlResource _currentResource);

	byte[] getMatchingKey(HashAlgorithm algorithm, String hash, String salt);

	void displayNotification(GrowlNotification notification);

}
