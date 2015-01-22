package asclib.serialize.msgpack;

import java.io.IOException;

/**
 * Thrown when we try to pack something we can't pack.
 * @author jon
 */
public class UnpackableItemException extends IOException {
	static final long serialVersionUID = 1234;
	public UnpackableItemException() {
		super();
	}
	public UnpackableItemException(String message) {
		super(message);
	}
	public UnpackableItemException(String message, Throwable cause) {
		super(message, cause);
	}
	public UnpackableItemException(Throwable cause) {
		super(cause);
	}
}


