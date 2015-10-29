package com.growlforandroid.gntp;

import java.util.EnumSet;

public enum ResponseType {
	OK ("-OK"),
	Error ("-ERROR");
	
	public final String MessageType;
	
	private ResponseType(String messageType) {
		MessageType = messageType;
	}
	
	public String toString() {
		return MessageType;
	}
	
	public static ResponseType fromString(String messageType) {
		for (ResponseType type : EnumSet.allOf(ResponseType.class)) {
			if (type.MessageType.equals(messageType))
				return type;
		}
		return null;
	}
}
