package asclib.net;

import asclib.core.CoreSocket;
import asclib.core.CoreEncode;

public class ChannelSocket {

	private CoreSocket sock = new CoreSocket();	
	private int headmod = 0;
	private int channel = 0;
	private byte xormask = 0;
	private boolean needflush = true;
	private boolean dirty = false;
	private byte[] sndbuf = new byte[1024 * 1024 + 12];
	private byte[] rcvbuf = new byte[1024 * 1024 + 12];
	private byte[] caster = new byte[1024 * 1024 + 12];
	private byte[] sndhdr = new byte[10];	
	private static final byte[] EMPTY = new byte[0];
	private byte[][] vector = { EMPTY, EMPTY, EMPTY, EMPTY };
	private int[] offs = { 0, 0, 0 };
	private int[] lens = { 0, 0, 0 };
	
	public ChannelSocket() {
	}
	
	private synchronized void exit() {
		sock.close();
		channel = -1;
		dirty = false;
	}
	
	private static void XOR(byte[] b, int off, int len, byte mask) {
		for (int i = off; len > 0; i++, len--) {
			b[i] ^= mask;
		}
	}
	
	public synchronized boolean write(int event, int wparam, int lparam, byte[] data, int off, int len, boolean flush) {
		if (data == null) data = EMPTY;
		boolean hr = false;
		CoreEncode.encode16i_lsb(sndhdr, 0, (short)(event & 0xffff));
		CoreEncode.encode32i_lsb(sndhdr, 2, wparam);
		CoreEncode.encode32i_lsb(sndhdr, 6, lparam);
		if (xormask == 0) {
			vector[0] = sndhdr;
			vector[1] = (data == null)? EMPTY : data;
			offs[0] = 0;
			offs[1] = off;
			lens[0] = 10;
			lens[1] = len;
			hr = sock.write(vector, offs, lens, 2, 0);
		}	else {
			byte[] cache = sndbuf;
			if (len > sndbuf.length) {
				cache = new byte[len];
			}
			vector[0] = sndhdr;
			vector[1] = EMPTY;
			offs[0] = 0;
			offs[1] = off;
			lens[0] = 10;
			lens[1] = len;
			if (data != null && len > 0) {
				System.arraycopy(data, 0, cache, 0, len);
				if (xormask != 0) {
					if (event == ChannelInst.ITMC_DATA) {
						XOR(cache, 0, len, xormask);
					}	
					else if (event == ChannelInst.ITMC_BROADCAST) {
						int size = wparam * 4;
						if (size < data.length) {
							XOR(cache, 0, len - size, xormask);
						}
					}
				}
				vector[1] = cache;
			}
			hr = sock.write(vector, offs, lens, 2, 0);
			cache = null;
		}
		if (hr) {
			dirty = true;
			if (flush) {
				this.flush();
			}
		}
		return hr;
	}
	
	public boolean write(int event, int wparam, int lparam, byte[] data, boolean flush) {
		if (data == null) data = EMPTY;
		return write(event, wparam, lparam, data, 0, data.length, flush);
	}
	
	public boolean write(int event, int wparam, int lparam, byte[] data) {
		if (data == null) data = EMPTY;
		return write(event, wparam, lparam, data, 0, data.length, needflush);
	}
	
	public boolean write(ChannelMsg msg) {
		return write(msg.event, msg.wparam, msg.lparam, msg.data, needflush);
	}
	
	public synchronized void flush() {
		sock.flush();
		dirty = false;
	}
	
	public synchronized ChannelMsg read() {
		if (dirty) {
			sock.flush();
			dirty = false;
		}
		int needed = sock.read(null);
		int length = needed - 10;
		byte[] cache = rcvbuf;
		if (needed < 0) return null;
		if (needed > rcvbuf.length) {
			cache = new byte[needed];
		}
		int readed = sock.read(cache, 0, needed);
		if (readed < 0) {
			return null;
		}
		ChannelMsg msg = new ChannelMsg();
		msg.event = CoreEncode.decode16u_lsb(cache, 0);
		msg.wparam = CoreEncode.decode32i_lsb(cache, 2);
		msg.lparam = CoreEncode.decode32i_lsb(cache, 6);
		if (length > 0) {
			msg.data = new byte[length];
			System.arraycopy(cache, 10, msg.data, 0, length);
			if (msg.wparam == ChannelInst.ITMT_DATA) {
				XOR(msg.data, 0, length, xormask);
			}
		}
		cache = null;
		return msg;
	}
	
	public synchronized boolean attach(String ip, int port, int channel, int header, int timeout) {
		exit();
		headmod = (header >= 12)? 2 : header;
		if (sock.open(ip, port, headmod, timeout) == false) {
			return false;
		}
		byte[] login = new byte[2];
		if (channel < 0) {
			channel = 0xffff;
		}
		CoreEncode.encode16u_lsb(login, 0, channel);
		sock.write(login);
		write(ChannelInst.ITMC_SYSCD, ChannelInst.ITMS_CHID, 0, null, false);
		write(ChannelInst.ITMC_NOOP, 0, 0, null, false);
		sock.flush();
		ChannelMsg msg = read();
		if (msg == null) {
			sock.close();
			return false;
		}
		this.channel = msg.lparam;
		msg = read();
		if (msg.event != ChannelInst.ITMT_NOOP) {
			sock.close();
			return false;
		}
		sock.setKeepAlive(true);		
		return true;
	}
	
	public void quit() {
		exit();
	}
	
	public int getChannelId() {
		return channel;
	}
	
	public synchronized boolean setFlushMode(boolean newNeedFlush) {
		boolean old = needflush;
		needflush = newNeedFlush;
		return old;
	}
	
	public boolean send(int hid, byte[] data, int off, int len, int limit, boolean useudp) {
		if (useudp == true) {
			return write(ChannelInst.ITMC_UNRDAT, hid, 0, data, off, len, needflush);
		}	else {
			int lparam = (limit > 0)? (0x40000000 | limit) : 0;
			return write(ChannelInst.ITMC_DATA, hid, lparam, data, off, len, needflush);
		}
	}
	
	public boolean send(int hid, byte[] data, int limit) {
		if (data == null) data = EMPTY;
		return send(hid, data, 0, data.length, limit, false);
	}
	
	public boolean send(int hid, byte[] data) {
		if (data == null) data = EMPTY;
		return send(hid, data, 0, data.length, 0, false);
	}
	
	public boolean close(int hid, int code) {
		return write(ChannelInst.ITMC_CLOSE, hid, code, null);
	}
	
	public synchronized boolean groupcast(int[] hids, int count, byte[] data, int off, int len, int limit) {
		int needed = count * 4 + len;
		byte[] cache = caster;
		if (needed > cache.length) {
			cache = new byte[needed];
		}
		/*
		for (int i = 0; i < count; i++) {
			CoreEncode.encode32i_lsb(cache, i * 4, hids[i]);
		}
		if (data != null) {
			System.arraycopy(data, off, cache, count * 4, len);
		}
		*/
		if (data != null && len > 0) {
			System.arraycopy(data, off, cache, 0, len);
		}
		for (int i = 0; i < count; i++) {
			CoreEncode.encode32i_lsb(cache, len + i * 4, hids[i]);
		}
		int lparam = (limit <= 0)? 0 : (0x40000000 | limit);
		boolean hr = write(ChannelInst.ITMC_BROADCAST, count, lparam, cache, 0, needed, needflush);
		cache = null;
		return hr;
	}
	
	public boolean groupcast(int[] hids, int count, byte[] data) {
		if (data == null) data = EMPTY;
		return groupcast(hids, count, data, 0, data.length, 0);
	}
	
	public boolean channel(int ch, byte[] data, int off, int len) {
		return write(ChannelInst.ITMC_CHANNEL, ch, 0, data, off, len, needflush);
	}
	
	public boolean channel(int ch, byte[] data) {
		if (data == null) data = EMPTY;
		return channel(ch, data, 0, data.length);
	}
	
	//public boolean broadcast()
	
	public void setXorMask(int mask) {
		xormask = (byte)(mask & 0xff);
	}
	
	protected void finalize() throws java.lang.Throwable {
		exit();
		super.finalize();
	}
}


