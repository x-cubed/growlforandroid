package com.growlforandroid.gntp;


public class GntpException extends Exception {
	private static final long serialVersionUID = 1L;
	public final GntpError Error;
	
	public GntpException(GntpError error) {		
		super(error.Description);
		Error = error;
	}
	
	public GntpException(GntpError error, String description) {
		this(error);
		Error.setDescription(description);
	}
}
