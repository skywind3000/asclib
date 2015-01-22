package asclib.core;

import java.net.Socket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;


public class CoreSocket {
	private static final int[] HEAD_LEN = { 2, 2, 4, 4, 1, 1, 2, 2, 4, 4, 1, 1, 4, 0, 0 };
	private static final int[] HEAD_INC = { 0, 0, 0, 0, 0, 0, 2, 2, 4, 4, 1, 1, 0, 0, 0 };
	private int head_mod = 0;
	private int head_int = 0;
	private int head_len = 0;
	private int head_inc = 0;
	
	private Socket sock = null;
	private BufferedInputStream istream = null;
	private BufferedOutputStream ostream = null;
	
	private byte[] head_data = null;
	private int head_size = -1;
	
	private InetSocketAddress remote = null;
	private InetSocketAddress local = null;
	private String errorMsg = "";
	private int errorCode = 0;
	
	public CoreSocket() {
		head_data = new byte[8];
	}
	
	private synchronized void reset() {
		if (sock != null) {
			try { if (istream != null) istream.close(); }
			catch (IOException e) {}
			try { if (ostream != null) ostream.close(); }
			catch (IOException e) {}
			try { sock.close(); }
			catch (IOException e) { }
			sock = null;
			istream = null;
			ostream = null;
			remote = null;
			local = null;
			head_size = -1;
		}
	}
	
	protected void finalize() throws java.lang.Throwable {
		reset();
		super.finalize();
	}
	
	private void except(String text, int code) {
		reset();
		errorMsg = text;
		errorCode = code;
	}
	
	public boolean open(String ip, int port, int head, int timeout) {
		reset();
		if (head < 0 || head >= 14) {
			except("error head mode", 1000);
			return false;
		}
		
		remote = new InetSocketAddress(ip, port);
		sock = new Socket();
		try {
			sock.connect(remote, timeout * 1000);
		}	
		catch (IOException e) {
			except("can not connect to " + remote.toString(), 1001);
			return false;
		}
		
		head_mod = head;
		head_len = HEAD_LEN[head];
		head_inc = HEAD_INC[head];
		
		if (head < 6) {
			head_int = head;
		}
		else if (head < 12) {
			head_int = head - 6;
		}
		else if (head == 12) {
			head_int = 2;
		}
		else if (head == 13) {
			head_int = 2;
		}
		else if (head == 14) {
			head_int = 2;
		}
		
		try {
			istream = new BufferedInputStream(sock.getInputStream(), 1024 * 1024 * 2);
			ostream = new BufferedOutputStream(sock.getOutputStream(), 1024 * 1024 * 2);
		}
		catch (IOException e) {
			reset();
			except("get socket stream error: " + e.toString(), 1002);
			return false;
		}
		
		try {
			sock.setKeepAlive(true);
			sock.setReceiveBufferSize(1024 * 1024 * 8);
			sock.setSendBufferSize(1024 * 1024 * 8);
			sock.setSoLinger(true, 5);
		}
		catch (IOException e) {
		}
		
		local = (InetSocketAddress)sock.getLocalSocketAddress();
		
		return true;
	}
	
	public boolean open(String ip, int port, int head) {
		return open(ip, port, head, 15);
	}
	
	public void close() {
		reset();
	}	
	
	private int readall(byte[] b, int off, int len) {
		int total = 0;
		try {
			while (len > 0) {
				int readed = istream.read(b, off, len);
				if (readed < 0) {
					except("read from socket stream eof", 1003);
					return -1;
				}
				off += readed;
				len -= readed;
				total += readed;
			}
		}
		catch (IOException e) {
			except("read from socket stream error: " + e.toString(), 1004);
			return -1;
		}
		return total;
	}
	
	public synchronized int read(byte[] b, int off, int len) {
		int readed = -1;
		if (sock == null) return -1;
		if (head_mod == 13) {
			if (b == null) {
				return 1024;
			}
			try {
				readed = istream.read(b, off, len);
			}	catch (IOException e) {
				reset();
				except("read from socket stream error: " + e.toString(), 1005);
				return -1;
			}
			return readed;
		}
		if (head_size < 0) {
			readed = readall(head_data, 0, head_len);
			if (readed < 0) return -1;
			if (readed < head_len) {
				reset();
				except("error: not enough head size", 1006);
				return -1;
			}
			switch (head_int) {
			case 0: head_size = CoreEncode.decode16u_lsb(head_data, 0); break;
			case 1: head_size = CoreEncode.decode16u_msb(head_data, 0); break;
			case 2: head_size = CoreEncode.decode32i_lsb(head_data, 0); break;
			case 3: head_size = CoreEncode.decode32i_msb(head_data, 0); break;
			case 4: head_size = CoreEncode.decode8u(head_data, 0); break;
			case 5: head_size = CoreEncode.decode8u(head_data, 0); break;
			}
			head_size = head_size & 0x7fffffff;
			if (head_mod == 12) head_size &= 0xffffff;
			head_size = (head_size + head_inc) - head_len;
		}
		if (b == null) {
			return head_size;
		}
		if (b.length - off < head_size || len < head_size) {
			return -2;
		}
		readed = readall(b, off, len);
		head_size = -1;
		if (readed < 0) return -1;
		return readed;
	}
	
	public int read(byte[] b) {
		if (b == null) {
			return read(null, 0, 0);
		}
		return read(b, 0, b.length);
	}
	
	public synchronized boolean write(byte[] b, int off, int len, int mask) {
		long size = len;
		if (sock == null) return false;
		if (head_mod >= 13) {
			try {
				ostream.write(b, off, len);
			}
			catch (IOException e) {
				except("error: failed to write socket buffer", 1007);
				return false;
			}
			return true;
		}
		size = ((size + head_len - head_inc) & 0xffffffffl);
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
		try {
			ostream.write(head_data, 4, head_len);
			ostream.write(b, off, len);
		}	catch (IOException e) {
			reset();
			except("error: failed to write socket buffer", 1008);
			return false;
		}
		return true;
	}
	
	public synchronized boolean write(byte[][] vector, int[] off, int[] len, int count, int mask) {
		long size = 0;
		long length = 0;
		if (sock == null) return false;
		if (vector == null) return false;
		if (count < 0) count = vector.length;
		if (vector.length < count) return false;
		if (off != null) { if (off.length < count) return false; }
		if (len != null) { if (len.length < count) return false; }
		for (int i = 0; i < count; i++) {
			int offset = (off == null)? 0 : off[i];
			if (len != null) {
				length += len[i];
			}	else {
				length += vector[i].length - offset;
			}
		}
		if (head_mod >= 13) {
			try {
				for (int i = 0; i < count; i++) {
					int offset = (off == null)? 0 : off[i];
					if (len != null) {
						ostream.write(vector[i], offset, len[i]);
					}	else {
						ostream.write(vector[i], offset, vector[i].length - offset);
					}
				}
			}
			catch (IOException e) {
				except("error: failed to write socket buffer", 1009);
				return false;
			}
			return true;
		}
		size = ((length + head_len - head_inc) & 0xffffffffl);
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
		try {
			ostream.write(head_data, 4, head_len);
			for (int i = 0; i < count; i++) {
				int offset = (off == null)? 0 : off[i];
				if (len == null) {
					ostream.write(vector[i], offset, vector[i].length - offset);
				}	else {
					ostream.write(vector[i], offset, len[i]);
				}
			}
		}	catch (IOException e) {
			except("error: failed to write socket buffer", 1010);
			return false;
		}
		return true;
	}
	
	public boolean write(byte[] b) {
		return write(b, 0, b.length, 0);
	}
	
	public synchronized boolean flush() {
		if (sock != null && ostream != null) {
			try {
				ostream.flush();
			}	catch (IOException e) {
				reset();
				except("error: failed to flush socket buffer", 1011);
				return false;
			}
		}
		return true;
	}
	
	public int getErrorCode() {
		return errorCode;
	}
	
	public String getErrorMsg() {
		return errorMsg;
	}
	
	public InetSocketAddress localAddress() {
		return local;
	}
	
	public InetSocketAddress remoteAddress() {
		return remote;
	}
	
	public synchronized boolean setReceiveBufferSize(int size) {
		if (sock == null) return false;
		try {
			sock.setReceiveBufferSize(size);
		}
		catch (IOException e) {
			return false;
		}
		return true;
	}
	
	public synchronized boolean setSendBufferSize(int size) {
		if (sock == null) return false;
		try {
			sock.setSendBufferSize(size);
		}
		catch (IOException e) {
			return false;
		}
		return true;
	}	
	
	public synchronized boolean setSoLinger(int seconds) {
		if (sock == null) return false;
		try {
			if (seconds <= 0) {
				sock.setSoLinger(false, 0);
			}	else {
				sock.setSoLinger(true, seconds);
			}
		}
		catch (IOException e) {
			return false;
		}
		return true;
	}
	
	public synchronized boolean setKeepAlive(boolean on) {
		if (sock == null) return false;
		try {
			sock.setKeepAlive(on);
		}
		catch (IOException e) {
			return false;
		}
		return true;
	}
	
	public static void main(String[] args) throws java.lang.Throwable {
		System.out.println("address: " + InetAddress.getLocalHost());
		System.out.println("address: " + (new InetSocketAddress("192.168.11.42", 3000)));
	}
}



