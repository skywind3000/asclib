package asclib.core;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;

/**
 * CoreNet - Async Socket Client
 *
 */
public class CoreNet {
	public static final int STATE_CLOSED = 0;
	public static final int STATE_CONNECTING = 1;
	public static final int STATE_ESTAB = 2;
	
	private SocketChannel channel = null;
	private int state = STATE_CLOSED;
	
	private synchronized void reset() {
		if (channel != null) {
			try {
				channel.close();
			} catch (IOException e) {
			}
			channel = null;
		}
		state = STATE_CLOSED;
	}
	
	protected void finalize() throws java.lang.Throwable {
		reset();
		super.finalize();
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}


