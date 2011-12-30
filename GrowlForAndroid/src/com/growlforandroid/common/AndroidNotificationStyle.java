package com.growlforandroid.common;

import android.app.Notification;
import android.content.Context;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/***
 * Grabs the text color and size from status bar notifications. Needed on Android versions before Gingerbread
 * when they added public styles for the status bar notifications.
 * 
 * http://stackoverflow.com/questions/4867338/custom-notification-layouts-and-text-colors
 */
public class AndroidNotificationStyle {
	private static Integer _titleTextColor = null;
	private static Integer _messageTextColor = null;
	private static Typeface _titleTypeface = null;
	private static Typeface _messageTypeface = null;
	private static float _titleTextSize = 11;
	private static float _messageTextSize = 11;
	private static final String TITLE_TEXT = "TITLE";
	private static final String MESSAGE_TEXT = "MESSAGE";
	
	public static Integer getTitleTextColor() {
		return _titleTextColor;
	}
	
	public static float getTitleTextSize() {
		return _titleTextSize;
	}
	
	public static Typeface getTitleTypeface() {
		return _titleTypeface;
	}
	
	public static Integer getMessageTextColor() {
		return _messageTextColor;
	}
	
	public static float getMessageTextSize() {
		return _messageTextSize;
	}
	
	public static Typeface getMessageTypeface() {
		return _messageTypeface;
	}
	
	private static void recurseGroup(Context context, ViewGroup gp) {
	    final int count = gp.getChildCount();
	    for (int i = 0; i < count; ++i) {
	        if (gp.getChildAt(i) instanceof TextView) {
	            final TextView text = (TextView) gp.getChildAt(i);
	            final String szText = text.getText().toString();
	            if (TITLE_TEXT.equals(szText)) {
	            	_titleTextColor = text.getTextColors().getDefaultColor();
	            	_titleTypeface = text.getTypeface();
	            	_titleTextSize = getScaledTextSize(context, text);
	            	
	            } else if (MESSAGE_TEXT.equals(szText)) {
	            	_messageTextColor = text.getTextColors().getDefaultColor();
	            	_messageTypeface = text.getTypeface();
	            	_messageTextSize = getScaledTextSize(context, text);
	            }
	            
	        } else if (gp.getChildAt(i) instanceof ViewGroup) {
	            recurseGroup(context, (ViewGroup) gp.getChildAt(i));
	        }
	    }
	}
	
	private static float getScaledTextSize(Context context, TextView text) {
		DisplayMetrics metrics = new DisplayMetrics();
        WindowManager systemWM = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        systemWM.getDefaultDisplay().getMetrics(metrics);
        return (text.getTextSize() / metrics.scaledDensity);
	}

	public static void grabNotificationStyle(Context context) {
	    if (_titleTextColor != null)
	        return;

	    try {
	        Notification notification = new Notification();
	        notification.setLatestEventInfo(context, TITLE_TEXT, MESSAGE_TEXT, null);
	        LinearLayout group = new LinearLayout(context);
	        ViewGroup event = (ViewGroup) notification.contentView.apply(context, group);
	        recurseGroup(context, event);
	        group.removeAllViews();
	        
	    } catch (Exception e) {
	    	Log.e("AndroidNotificationStyle.grabNotificationStyle", e.toString());
	    	_titleTextColor = android.R.color.black;
	    	_messageTextColor = android.R.color.black;
	    }
	}
}
