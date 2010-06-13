package com.growlforandroid.gntp;

public final class Constants {
	public static final String CHARSET = "UTF-8";
	
	public static final String END_OF_LINE = "\r\n";
	public static final String FIELD_DELIMITER = " ";
	
	public static final String SUPPORTED_PROTOCOL = "GNTP";
	public static final String SUPPORTED_PROTOCOL_VERSION = "1.0";
	
	public static final String GNTP_PROTOCOL_VERSION = SUPPORTED_PROTOCOL + "/" + SUPPORTED_PROTOCOL_VERSION;
	
	public static final String HEADER_APPLICATION_NAME = "Application-Name";
	public static final String HEADER_APPLICATION_ICON = "Application-Icon";
	public static final String HEADER_NOTIFICATIONS_COUNT = "Notifications-Count";
	
	public static final String HEADER_NOTIFICATION_ID = "Notification-ID";
	public static final String HEADER_NOTIFICATION_NAME = "Notification-Name";
	public static final String HEADER_NOTIFICATION_DISPLAY_NAME = "Notification-Display-Name";
	public static final String HEADER_NOTIFICATION_ICON = "Notification-Icon";
	public static final String HEADER_NOTIFICATION_TITLE = "Notification-Title";
	public static final String HEADER_NOTIFICATION_TEXT = "Notification-Text";
	public static final String HEADER_NOTIFICATION_ENABLED = "Notification-Enabled";
	public static final String HEADER_NOTIFICATION_ORIGIN = "Origin-Machine-Name";
	
	public static final String HEADER_RESOURCE_IDENTIFIER = "Identifier";
	public static final String HEADER_RESOURCE_LENGTH = "Length";

	public static final String HEADER_SUBSCRIPTION_ID = "Subscriber-ID";
	public static final String HEADER_SUBSCRIPTION_NAME = "Subscriber-Name";
	public static final String HEADER_SUBSCRIPTION_TTL = "Subscription-TTL";
	public static final int SUBSCRIPTION_TTL = 300; // seconds
	
	public static final String HEADER_RESPONSE_ACTION = "Response-Action";
		
	public static final String HEADER_ERROR_CODE = "Error-Code";
	public static final String HEADER_ERROR_DESCRIPTION = "Error-Description";
	
	public static final String HEADER_ORIGIN_MACHINE_NAME = "Origin-Machine-Name";
	public static final String HEADER_ORIGIN_PLATFORM_NAME = "Origin-Platform-Name";
	public static final String HEADER_ORIGIN_PLATFORM_VERSION = "Origin-Platform-Version";
	public static final String HEADER_ORIGIN_SOFTWARE_NAME = "Origin-Software-Name";
	public static final String HEADER_ORIGIN_SOFTWARE_VERSION = "Origin-Software-Version";
	
	public static final String RESOURCE_URI_PROTOCOL = "x-growl-resource";
	public static final String RESOURCE_URI_PREFIX = RESOURCE_URI_PROTOCOL + "://";
}
