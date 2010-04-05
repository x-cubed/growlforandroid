package com.growlforandroid.gntp;

import java.util.EnumSet;

public enum RequestType {
	Register ("REGISTER"),
	Notify ("NOTIFY"),
	Subscribe ("SUBSCRIBE"),
	
	Ignore (null);

	public final String MessageType;
	
	private RequestType(String messageType) {
		MessageType = messageType;
	}
	
	public String toString() {
		return MessageType;
	}
	
	public static RequestType fromString(String messageType) {
		for (RequestType type : EnumSet.allOf(RequestType.class)) {
			if (type.MessageType == null) {
				if (messageType == null)
					return type;
			} else {
				if (type.MessageType.equals(messageType))
					return type;
			}
		}
		return null;
	}
}

