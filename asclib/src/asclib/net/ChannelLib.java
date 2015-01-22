package asclib.net;

import asclib.net.ChannelInst;
import asclib.net.ChannelMsg;
import asclib.net.ChannelSocket;


public class ChannelLib {
	private ChannelSocket sock = new ChannelSocket();
	private int[] users = new int[0x10000];
	private int[] tags = new int[0x10000];
	private boolean readFilter = true;
	
	public ChannelLib() {
		reset();
	}
	
	protected void finalize() throws java.lang.Throwable {
		reset();
		super.finalize();
	}
	
	private synchronized void reset() {
		userReset();
		sock.quit();
	}
	
	
	private boolean userContains(int hid) {
		if (hid < 0) return false;
		return (users[hid & 0xffff] == hid)? true : false; 
	}
	
	private void userAdd(int hid) {
		if (hid >= 0) {
			users[hid & 0xffff] = hid; 
			tags[hid & 0xffff] = -1;
		}
	}
	
	private void userDel(int hid) {
		if (hid >= 0) {
			users[hid & 0xffff] = -1;
			tags[hid & 0xffff] = -1;
		}
	}
	
	private void userReset() {
		for (int i = 0; i < 0x10000; i++) {
			users[i] = -1;
			tags[i] = -1;
		}
	}
	
	/**
	 * Login transmod and register as a channel
	 * @param ip transmod ip
	 * @param port transmod port
	 * @param channel which channel do you want to register, 0xffff to let transmod allocate 
	 * @param header header format (0-14)
	 * @param timeout connection time out (in seconds)
	 * @return true for successful, false for error
	 */
	public boolean attach(String ip, int port, int channel, int header, int timeout) {
		return sock.attach(ip, port, channel, header, timeout);
	}
	
	/**
	 * Login transmod and register as a channel
	 * @param ip transmod ip
	 * @param port transmod port
	 * @param channel which channel do you want to register, 0xffff to let transmod allocate 
	 * @param header header format (0-14)
	 * @return true for successful, false for error
	 */
	public boolean attach(String ip, int port, int channel, int header) {
		return sock.attach(ip, port, channel, header, 15);
	}
	
	/**
	 * quit transmod
	 */
	public void quit() {
		reset();
	}
	
	/**
	 * Read event from transmod. invoking this method may block if there is no event.
	 * See ITMT_XX in {@link auxlib.net.ChannelInst ChannelInst} 
	 * @return a {@link auxlib.net.ChannelMsg ChannelMsg} object consisted of (event, wp, lp, data) 
	 */
	public synchronized ChannelMsg read() {
		ChannelMsg msg = null;
		if (readFilter == false) {
			return sock.read();
		}
		while (true) {
			boolean discard = false;
			msg = sock.read();
			if (msg == null) break;
			switch (msg.event) {
			case ChannelInst.ITMT_NEW:
				userAdd(msg.wparam);
				tags[msg.wparam & 0xffff] = msg.lparam;
				break;
			case ChannelInst.ITMT_LEAVE:
				if (userContains(msg.wparam) == false) {
					discard = true;
				}	else {
					msg.lparam = tags[msg.wparam & 0xffff];
				}
				userDel(msg.wparam);
				break;
			case ChannelInst.ITMT_DATA:
				if (userContains(msg.wparam) == false) {
					discard = true;
				}	else {
					msg.lparam = tags[msg.wparam & 0xffff];
				}
				break;
			case ChannelInst.ITMT_UNRDAT:
				if (userContains(msg.wparam) == false) {
					discard = true;
				}	else {
					msg.lparam = tags[msg.wparam & 0xffff];
				}
				break;
			}
			if (discard == false) {
				break;
			}
		}
		return msg;
	}
	
	/**
	 * write command to transmod.
	 * @param event ITMC_XX in {@link auxlib.net.ChannelInst ChannelInst} 
	 * @param wparam first parameter
	 * @param lparam second parameter
	 * @param data data
	 * @param off data offset 
	 * @param len data size
	 * @param flush wheather need flush when writing finished
	 * @return true for successful, false for error
	 */
	public boolean write(int event, int wparam, int lparam, byte[] data, int off, int len, boolean flush) {
		return sock.write(event, wparam, lparam, data, off, len, flush);
	}
	
	/**
	 * write command to transmod.
	 * @param event ITMC_XX in {@link auxlib.net.ChannelInst ChannelInst} 
	 * @param wparam first parameter
	 * @param lparam second parameter
	 * @param data data
	 * @return true for successful, false for error
	 */
	public boolean write(int event, int wparam, int lparam, byte[] data) {
		return sock.write(event, wparam, lparam, data);
	}
	
	/**
	 * write command to transmod.
	 * @param msg event to send to transmod
	 * @return true for successful, false for error
	 */
	public boolean write(ChannelMsg msg) {
		return sock.write(msg);
	}
	
	/**
	 * send data to user
	 * @param hid user hyper identity
	 * @param data data to sent
	 * @param off offset
	 * @param len length
	 * @param limit buffer limit (data will be discard if buffer size exceed that value)
	 * @param useudp wheather to use udp
	 * @return true for successful, false for error
	 */
	public boolean send(int hid, byte[] data, int off, int len, int limit, boolean useudp) {
		return sock.send(hid, data, off, len, limit, useudp);
	}
	
	/**
	 * send data to user
	 * @param hid user hyper identity
	 * @param data data to send
	 * @param limit buffer limit (data will be discard if buffer size exceed that value)
	 * @return true for successful, false for error
	 */
	public boolean send(int hid, byte[] data, int limit) {
		return sock.send(hid, data, limit);
	}
	
	/**
	 * send data to user
	 * @param hid user hyper identity
	 * @param data data to send
	 * @return true for successful, false for error
	 */
	public boolean send(int hid, byte[] data) {
		return sock.send(hid,  data);
	}
	
	/**
	 * close user connection
	 * @param hid user hyper identity
	 * @param code reason code
	 * @return true for successful, false for error
	 */
	public boolean close(int hid, int code) {
		return sock.close(hid, code);
	}
	
	/**
	 * close user connection
	 * @param hid user hyper identity
	 * @return true for successful, false for error
	 */
	public boolean close(int hid) {
		return close(hid, 8123);
	}
	
	public boolean groupcast(int[] hids, int count, byte[] data, int off, int len, int limit) {
		return sock.groupcast(hids, count, data, off, len, limit);
	}
	
	public boolean groupcast(int[] hids, int count, byte[] data) {
		return sock.groupcast(hids, count, data);
	}
	
	public boolean channel(int ch, byte[] data, int off, int len) {
		return sock.channel(ch, data, off, len);
	}
	
	public boolean channel(int ch, byte[] data) {
		return sock.channel(ch, data);
	}
	
	public void setXorMask(int mask) {
		sock.setXorMask(mask);
	}
	
	public boolean setTag(int hid, int tag) {
		if (userContains(hid)) {
			tags[hid & 0xffff] = tag;
		}
		return sock.write(ChannelInst.ITMC_TAG, hid, tag, null);
	}
	
	public int getTag(int hid) {
		if (userContains(hid)) {
			return tags[hid & 0xffff];
		}
		return -1;
	}
	
	public boolean setReadFilter(boolean newFilter) {
		boolean old = readFilter;
		readFilter = newFilter;
		return old;
	}
	
	public boolean movec(int channel, int hid, byte[] data) {
		return sock.write(ChannelInst.ITMC_MOVEC, channel, hid, data);
	}
	
	public boolean setTimer(int millisec) {
		return sock.write(ChannelInst.ITMC_SYSCD, ChannelInst.ITMS_TIMER, millisec, null);
	}
	
	public boolean bookAdd(int catagory) {
		return sock.write(ChannelInst.ITMC_SYSCD, ChannelInst.ITMS_BOOKADD, catagory, null);
	}
	
	public boolean bookDel(int catagory) {
		return sock.write(ChannelInst.ITMC_SYSCD, ChannelInst.ITMS_BOOKDEL, catagory, null);
	}
	
	public boolean bookReset() {
		return sock.write(ChannelInst.ITMC_SYSCD, ChannelInst.ITMS_BOOKRST, 0, null);
	}
	
	public boolean control(int option, int value) {
		return sock.write(ChannelInst.ITMC_SYSCD, option, value, null);
	}
	
	public boolean setRC4SendKey(int hid, byte[] key) {
		return sock.write(ChannelInst.ITMC_SYSCD, ChannelInst.ITMS_RC4SKEY, hid, key);
	}
	
	public boolean setRC4RecvKey(int hid, byte[] key) {
		return sock.write(ChannelInst.ITMC_SYSCD, ChannelInst.ITMS_RC4RKEY, hid, key);
	}
	
	public void flush() {
		sock.flush();
	}
	
	public void setFlushMode(boolean needflush) {
		sock.setFlushMode(needflush);
	}
	
	public int getChannelId() {
		return sock.getChannelId();
	}
	
	public boolean syscd(int option, int value, byte[] data) {
		return sock.write(ChannelInst.ITMC_SYSCD, option, value, data);
	}
	
	public boolean syscd(int option, int value) {
		return syscd(option, value, null);
	}
	
	public boolean ioctl(int hid, int mode, int value) {
		int lparam = (value << 4) | (mode & 0xf);
		return sock.write(ChannelInst.ITMC_IOCTL, hid, lparam, null);
	}
	
	public boolean message(String command) {
		byte[] bytes = null;
		try {
			bytes = command.getBytes("UTF-8");
		}
		catch (java.io.UnsupportedEncodingException e) {
			return false;
		}
		return sock.write(ChannelInst.ITMC_SYSCD, ChannelInst.ITMS_MESSAGE, 0, bytes);
	}
	
	public boolean setNoDelay(int hid, boolean enable) {
		return ioctl(hid, ChannelInst.ITMS_NODELAY, enable? 1 : 0);
	}
	
	public boolean setPriority(int hid, int priority) {
		return ioctl(hid, ChannelInst.ITMS_PRIORITY, priority);
	}
	
	public boolean setTos(int hid, int tos) {
		return ioctl(hid, ChannelInst.ITMS_TOS, tos);
	}
	
	public boolean setEnviron(String item, String value) {
		return message("SET " + item + "=" + value);
	}
	
	public boolean setFastMode(boolean enable) {
		return syscd(ChannelInst.ITMS_FASTMODE, enable? 1 : 0);
	}
}


