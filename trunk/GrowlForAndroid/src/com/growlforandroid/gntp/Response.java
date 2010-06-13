package com.growlforandroid.gntp;

import java.io.IOException;
import com.growlforandroid.common.ChannelWriter;
import com.growlforandroid.common.EncryptedChannelReader;

/*
 * Builds a GNTP Response in preparation for writing to a socket
 */
public class Response
	extends GntpMessage {
	
	private final ResponseType _type;
	private final EncryptionType _encryptionType = EncryptionType.None;
	
	public Response(ResponseType messageType) {
		_type = messageType;
	}
	
	public Response(ResponseType messageType, GntpError error) throws Exception {
		this(messageType, error, null);
	}
	
	public Response(ResponseType messageType, GntpError error, String errorDescription) throws Exception {
		this(messageType);
		
		switch(messageType) {
			case OK:
				if (error != null)
					throw new Exception("Type is OK, but error details were provided");
				addHeader(Constants.HEADER_RESPONSE_ACTION, _type.toString());
				break;
				
			case Error:
				if (error == null)
					throw new Exception("Type is Error, but no error details were provided");
				errorDescription = (errorDescription == null) ? error.Description : errorDescription;
				addHeader(Constants.HEADER_ERROR_CODE, String.valueOf(error.ErrorCode));
				addHeader(Constants.HEADER_ERROR_DESCRIPTION, errorDescription);
				break;
			
			default:
				throw new Exception("Invalid message type provided");		
		}		
	}
	
	public ResponseType getType() {
		return _type;
	}
	
	protected void writeLeaderLine(ChannelWriter writer) throws IOException {
		writeResponseLine(writer, _type, _encryptionType);
	}
	
	private static void writeResponseLine(ChannelWriter out, ResponseType messageType, EncryptionType encryptionType) throws IOException {
		out.write(Constants.GNTP_PROTOCOL_VERSION + " " + messageType + " " +
			encryptionType.toString() + Constants.END_OF_LINE);
	}

	public static Response read(EncryptedChannelReader reader) throws GntpException, IOException {
		String leader = reader.readLine();
		String[] leaderFields = leader.split(Constants.FIELD_DELIMITER);
		
		if (!Constants.GNTP_PROTOCOL_VERSION.equals(leaderFields[0]))
			throw new GntpException(GntpError.UnknownProtocolVersion);
		
		if (leaderFields.length != 3)
			throw new GntpException(GntpError.InvalidRequest,
					"Expected \"protocol/version type encryptionTypeAndIV\"");
		
		ResponseType type = ResponseType.fromString(leaderFields[1]);

		String[] encryptionFields = leaderFields[2].split(":");
		EncryptionType encryptionType = EncryptionType.fromString(encryptionFields[0]);
		if (encryptionType != EncryptionType.None)
			throw new GntpException(GntpError.InternalServerError, "Response encryption is not yet supported");
		
		Response response = new Response(type);
		response.readHeaders(reader);
		
		return response;
	}

	public GntpException getError() {
		if (_type == ResponseType.OK)
			return null;
		
		int errorCode = getHeaderInt(Constants.HEADER_ERROR_CODE, GntpError.InternalServerError.ErrorCode);
		GntpError error = GntpError.fromErrorCode(errorCode);
		String description = getHeaderString(Constants.HEADER_ERROR_DESCRIPTION);
		return new GntpException(error, description);
	}
}
