//=====================================================================
//
// asclib.core.CoreNet - Async Network Client
//
// NOTE:
// for more information, please see the readme file.
//
//=====================================================================
package asclib.core;

import java.io.IOException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketException;

/**
 * CoreNet - Async Socket Client
 *
 */
public class CoreNet {
	
	public static final int STATE_CLOSED = 0;		// connection closed
	public static final int STATE_CONNECTING = 1;	// connection connecting
	public static final int STATE_ESTAB = 2;		// connection established
	
	/**
	 * Message Headers represent split messages int the tcp data stream.
	 * With a 2-bytes or 4-bytes before the message body to indicate message size ? 
	 * or just split by '\n' ? Header mode must be compatible with remote server.
	 */
	public static class Header {
		public static final int WORDLSB = 0;	// 2-bytes header (lsb) include header size
		public static final int WORDMSB = 1;	// 2-bytes header (msb) include header size
		public static final int DWORDLSB = 2;	// 4-bytes header (lsb) include header size
		public static final int DWORDMSB = 3;	// 4-bytes header (msb) include header size
		public static final int BYTELSB = 4;	// 1-byte header (lsb) include header size
		public static final int BYTEMSB = 5;	// 1-byte header (msb) include header size
		public static final int EWORDLSB = 6;	// 2-bytes header (lsb) only body size
		public static final int EWORDMSB = 7;	// 2-bytes header (msb) only body size
		public static final int EDWORDLSB = 8;	// 4-bytes header (lsb) only body size
		public static final int EDWORDMSB = 9;	// 4-bytes header (msb) only body size
		public static final int EBYTELSB = 10;	// 1-bytes header (lsb) only body size
		public static final int EBYTEMSB = 11;	// 1-bytes header (msb) only body size
		public static final int DWORDMASK = 12;	// 4-bytes header (lsb) with mask
		public static final int RAWDATA = 13;	// raw data without header
		public static final int LINESPLIT = 14;		// split by '\n'
	}
	
	private static final int[] HEAD_LEN = { 2, 2, 4, 4, 1, 1, 2, 2, 4, 4, 1, 1, 4, 0, 4 };
	private static final int[] HEAD_INC = { 0, 0, 0, 0, 0, 0, 2, 2, 4, 4, 1, 1, 0, 0, 0 };
	private int head_mod = 0;
	private int head_int = 0;
	private int head_len = 0;
	private int head_inc = 0;
	private byte[] head_data = null;
	
	private SocketChannel channel = null;
	private int _state = STATE_CLOSED;
	private int _code = 0;
	
	private CoreRing sndbuf = null;
	private CoreRing rcvbuf = null;
	private CoreRing rcline = null;
	private ByteBuffer buffer = null;
	private String _message = null;
	
	private CoreRC4 sndrc4 = null;
	private CoreRC4 rcvrc4 = null;
	
	private int _connect_count = 0;
	private int _connect_limit = 5;
	private long _connect_timeout = 20000;
	private long _connect_start = 0;
	
	private int _limit_send = -1;

	
	private synchronized void destroy() {
		if (channel != null) {
			try {
				channel.close();
			} catch (IOException e) {
			}
			channel = null;
		}
		if (sndbuf != null) {
			sndbuf.destroy();
			sndbuf = null;
		}
		if (rcvbuf != null) {
			rcvbuf.destroy();
			rcvbuf = null;
		}
		if (rcline != null) {
			rcline.destroy();
			rcline = null;
		}
		if (buffer != null) {
			buffer.clear();
			buffer = null;
		}
		_message = null;
		head_data = null;
		sndrc4 = null;
		rcvrc4 = null;
		_code = 0;
		_state = STATE_CLOSED;
	}
	
	protected void finalize() throws java.lang.Throwable {
		destroy();
		super.finalize();
	}
	
	public int state() {
		return _state;
	}
	
	/**
	 * Connect to remote server
	 * @param ip address of remote server
	 * @param port port
	 * @param head message header mode see {@link Header}
	 * @return true for success, false for error
	 */
	public synchronized boolean connect(String ip, int port, int head) {
		destroy();
		try {
			channel = SocketChannel.open();
			channel.configureBlocking(false);
			channel.connect(new InetSocketAddress(ip, port));
		}
		catch (IOException e) {
			_message = e.getMessage();
			return false;
		}
		head_mod = (head < 0)? 0 : ((head > 14)? 14 : head);
		head_len = HEAD_LEN[head_mod];
		head_inc = HEAD_INC[head_mod];
		
		if (head_mod < 6) {
			head_int = head_mod;
		}
		else if (head_mod < 12) {
			head_int = head_mod - 6;
		}
		else {
			head_int = 2;
		}
		
		sndbuf = new CoreRing();
		rcvbuf = new CoreRing();
		rcline = new CoreRing();
		sndbuf.resize(8);
		rcvbuf.resize(8);
		rcline.resize(8);
		sndbuf.auto(true);
		rcvbuf.auto(true);
		rcline.auto(true);
		//rcvbuf.debug = 1;
		
		buffer = ByteBuffer.allocate(0x10000);
		head_data = new byte[8];
		
		_connect_count = 0;
		_state = STATE_CONNECTING;
		_code = 0;
		_limit_send = 1024 * 1024;
		
		return true;
	}
	
	public synchronized void close(int code) {
		if (channel != null) {
			try {
				channel.close();
			} catch (IOException e) {
			}
			channel = null;
		}
		sndrc4 = null;
		rcvrc4 = null;
		_code = code;
		_state = STATE_CLOSED;
	}
	
	private void tryConnect() {
		if (_state != STATE_CONNECTING) return;
		if (channel.isConnectionPending()) {
			try {
				channel.finishConnect();
			} catch (IOException e) {
				return;
			}
			if (channel.isConnected()) {
				Socket sock = channel.socket();
				try {
					sock.setKeepAlive(true);
					sock.setReceiveBufferSize(64 * 1024);
					sock.setSendBufferSize(64 * 1024);
					sock.setSoLinger(true, 5);
				}
				catch (IOException e) {
				}
				_state = STATE_ESTAB;				
			}	else {
				if (_connect_count == 0) {
					_connect_start = System.currentTimeMillis();
				}	
				else if (_connect_count >= _connect_limit){
					long current = System.currentTimeMillis();
					if (current - _connect_start >= _connect_timeout) {
						_message = "connection time out";
						close(1001);
					}
				}
				_connect_count++;
			}
		}
	}
	
	private void tryReceive() {
		if (_state != STATE_ESTAB) return;
		if (channel == null) {
			_message = "channel must not be null"; 
			close(1002);
			return;
		}
		while (true) {
			int hr = 0;			
			buffer.limit(buffer.capacity());
			buffer.position(0);
			try {
				hr = channel.read(buffer);
			} catch (IOException e) {
				_message = e.getMessage();
				close(1003);
				return;
			} catch (NotYetConnectedException e) {
				return;
			}
			if (hr == 0) return;
			if (hr < 0) {
				_message = "remote disconnected";
				close(0);
				return;
			}
			byte[] ptr = buffer.array();
			if (rcvrc4 != null && hr > 0) {
				rcvrc4.crypt(ptr, 0, ptr, 0, hr);
			}
			if (hr == 0) break;
			if (head_mod != Header.LINESPLIT) {
				rcvbuf.write(ptr, 0, hr);
			}	else {
				int pos, start;
				for (start = 0, pos = 0; pos < hr; pos++) {
					if (ptr[pos] == 10) {
						int x = pos - start + 1;
						int y = rcline.length();
						CoreEncode.encode32i_lsb(head_data, 0, x + y + 4);
						rcvbuf.write(head_data, 0, 4);
						while (rcline.length() > 0) {
							ByteBuffer bb = rcline.buffer();
							rcvbuf.write(bb.array(), bb.position(), bb.remaining());
							rcline.drop(bb.remaining());
						}
						rcvbuf.write(ptr, start, x);
						start = pos + 1;
					}
				}
				if (pos > start) {
					rcline.write(ptr, start, pos - start);
				}
			}
		}
	}
	
	private void trySend() {
		if (_state != STATE_ESTAB) return;
		if (sndbuf.length() == 0) return;
		while (true) {
			ByteBuffer bb = sndbuf.buffer();
			int hr = 0;
			int remain = bb.remaining();
			try {
				hr = channel.write(bb);
			} catch (IOException e) {
				_message = e.getMessage();
				close(1004);
				return;
			} catch (NotYetConnectedException e) {
				return;
			}
			if (hr == 0) return;
			if (hr < 0) {
				_message = "unknow size error";
				close(1005);
				return;
			}
			sndbuf.drop(hr);
			if (hr < remain) break;
		}
	}
	
	/**
	 * Call it every interval (eg.100ms)
	 */
	public synchronized void update() {
		if (_state == STATE_CLOSED) return;
		if (_state == STATE_CONNECTING) tryConnect();
		if (_state == STATE_ESTAB) tryReceive();
		if (_state == STATE_ESTAB) trySend();
	}
	
	public synchronized boolean send(byte[] buf, int off, int len, int mask) {
		if (buf == null || channel == null) return false;
		int size = ((len + head_len - head_inc) & 0x7fffffff);
		if (_limit_send > 0 && sndbuf.length() > _limit_send) {
			trySend();
			if (sndbuf.length() > _limit_send) {
				_message = "buffer size exceed limit";
				close(1006);
				return false;
			}
		}
		if (head_mod == 12) {
			size = (size & 0xffffff) | (mask << 24);
		}
		switch (head_int) {
		case 0: CoreEncode.encode16u_lsb(head_data, 4, (int)(size)); break;
		case 1: CoreEncode.encode16u_msb(head_data, 4, (int)(size)); break;
		case 2: CoreEncode.encode32u_lsb(head_data, 4, (long)(size)); break;
		case 3: CoreEncode.encode32u_msb(head_data, 4, (long)(size)); break;
		case 4: CoreEncode.encode8u(head_data, 4, (int)(size)); break;
		case 5: CoreEncode.encode8u(head_data, 4, (int)(size)); break;
		}
		if (head_len > 0 && head_mod < Header.RAWDATA) {
			if (sndrc4 != null) {
				sndrc4.crypt(head_data, 4, head_data, 4, head_len);
			}
			sndbuf.write(head_data, 4, head_len);
		}
		if (sndrc4 != null) {
			byte[] cache = buffer.array();
			while (len > 0) {
				int block = (len < cache.length)? len : cache.length;
				sndrc4.crypt(buf, off, cache, 0, block);
				sndbuf.write(cache, 0, block);
				off += block;
				len -= block;
			}
		}	else {
			sndbuf.write(buf, off, len);
		}
		return true;
	}
	
	/**
	 * receive message from CoreNet
	 * @param buf byte array, returns message size when buf is null
	 * @param off where to receive data
	 * @param len how many bytes can you receive
	 * @return message size, -1 for block, -2 for buffer size too small
	 */
	public synchronized int recv(byte[] buf, int off, int len) {
		int size = rcvbuf.length();
		if (size <= 0) return -1;
		if (head_mod == Header.RAWDATA) {
			len = (len < size)? len : size;
			if (buf == null) {
				return (size < 0x10000)? size : 0x10000;
			}
			if (size == 0) return -1;
			rcvbuf.read(buf, off, len);
			return len;
		}
		if (size < head_len) return -1;
		rcvbuf.peek(head_data, 0, head_len);
		int head_size = 0;
		switch (head_int) {
		case 0: head_size = CoreEncode.decode16u_lsb(head_data, 0); break;
		case 1: head_size = CoreEncode.decode16u_msb(head_data, 0); break;
		case 2: head_size = CoreEncode.decode32i_lsb(head_data, 0); break;
		case 3: head_size = CoreEncode.decode32i_msb(head_data, 0); break;
		case 4: head_size = CoreEncode.decode8u(head_data, 0); break;
		case 5: head_size = CoreEncode.decode8u(head_data, 0); break;
		}
		int length = head_size + head_inc - head_len;
		if (size < length + head_len) return -1;
		if (buf == null) return length;
		if (len < length) return -2;
		rcvbuf.drop(head_len);
		rcvbuf.read(buf, off, length);
		return length;
	}
	
	/**
	 * get error message
	 * @return message
	 */
	public String message() {
		return _message;
	}
	
	/**
	 * get error code
	 * @return code
	 */
	public int code() {
		return _code;
	}
	
	/**
	 * set rc4 key for output data
	 * @param key key
	 */
	public void setSendKey(byte[] key) {
		if (key == null) {
			sndrc4 = null;
		}	else {
			sndrc4 = new CoreRC4(key);
		}
	}
	
	/**
	 * set rc4 key for input data
	 * @param key key
	 */
	public void setRecvKey(byte[] key) {
		if (key == null) {
			rcvrc4 = null;
		}	else {
			rcvrc4 = new CoreRC4(key);
		}
	}
	
	public boolean setNodelay(boolean enable) {
		if (channel == null) return false;
		try {
			channel.socket().setTcpNoDelay(enable);
		} catch (SocketException e) {
			return false;
		}
		return true;
	}
	
	/**
	 * flush output data immediately. By default, messages will be sent out
	 * by next update() invoking. 
	 */
	public void flush() {
		if (_state == STATE_ESTAB) {
			trySend();
		}
	}
	
	/**
	 * Get buffer limit
	 * @return how many bytes of can output buffer hold.
	 */
	public int limit() {
		return _limit_send;
	}
	
	/**
	 * Set output buffer limit. If you are continue sending message to 
	 * remote, but remote don't receive, or network is week, messages
	 * will accumulate in the output buffer. If the size of output buffer
	 * exceed the limit, the connection will be close.
	 * @param bufferLimit how many bytes can output buffer hold
	 */
	public void limit(int bufferLimit) {
		_limit_send = bufferLimit;
	}
	
	/**
	 * get timeout of connecting
	 * @return milliseconds
	 */
	public int timeout() {
		return (int)this._connect_timeout;
	}
	
	/**
	 * set timeout of connecting
	 * @param timeout milliseconds
	 */
	public void timeout(int timeout) {
		this._connect_timeout = timeout;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}


