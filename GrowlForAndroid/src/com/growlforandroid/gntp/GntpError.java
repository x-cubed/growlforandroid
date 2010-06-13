package com.growlforandroid.gntp;

import java.util.EnumSet;

import com.growlforandroid.common.EncryptedChannelReader.DecryptionException;

public enum GntpError {
	Reserved (100, "Reserved for future use"),
	TimedOut (200, "The server timed out waiting for the request to complete"),
	NetworkFailure (201, "The server was unavailable or the client could not reach the server for any reason"),
	InvalidRequest (300, "The request contained an unsupported directive, invalid headers or values, or was otherwise malformed"),
	UnknownProtocol (301, "The request was not a GNTP request"),
	UnknownProtocolVersion (302, "The request specified an unknown or unsupported GNTP version"),
	RequiredHeaderMissing (303, "The request was missing required information"),
	NotAuthorized (400, "The request supplied a missing or wrong password/key or was otherwise not authorized"),
	UnknownApplication (401, "Application is not registered to send notifications"),
	UnknownNotification (402, "Notification type is not registered by the application"),
	InternalServerError (500, "Internal server error"),
	;
	
	public final int ErrorCode;
	public String Description;
	
	GntpError(int errorCode, String initialDescription) {
		ErrorCode = errorCode;
		Description = initialDescription;
	}
	
	public void setDescription(String description) {
		Description = description;
	}

	public static GntpError fromErrorCode(int errorCode) {
		for (GntpError error : EnumSet.allOf(GntpError.class)) {
			if (error.ErrorCode == errorCode)
				return error;
		}
		return null;
	}
	
	public static GntpError getErrorFrom(Exception x) {
		if (x instanceof GntpException) {
			return ((GntpException)x).Error;
		}
		else if (x instanceof DecryptionException) {
			return GntpError.NotAuthorized;
		}
		else {
			return GntpError.InternalServerError;
		}
	}
}
