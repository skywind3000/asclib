package asclib.serialize.msgpack;

import java.io.IOException;

/**
 * Thrown when we can't unpack the given bytes to normal data.
 * @author jon
 */
public class InvalidMsgPackDataException extends IOException {
	static final long serialVersionUID = 1235;
	public InvalidMsgPackDataException() {
		super();
	}
	public InvalidMsgPackDataException(String message) {
		super(message);
	}
	public InvalidMsgPackDataException(String message, Throwable cause) {
		super(message, cause);
	}
	public InvalidMsgPackDataException(Throwable cause) {
		super(cause);
	}
}
