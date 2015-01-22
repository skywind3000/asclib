package asclib.net;

public class ChannelMsg {
	private final static byte[] EMPTY = new byte[0];
	public int event = 0;
	public int wparam = 0;
	public int lparam = 0;
	public byte[] data = EMPTY;
	
	public ChannelMsg() {
		event = 0;
		wparam = 0;
		lparam = 0;
		data = EMPTY;
	}
	
	public ChannelMsg(int event, int wparam, int lparam, byte[] data) {
		this.event = event;
		this.wparam = wparam;
		this.lparam = lparam;
		this.data = data;
	}
	
	public ChannelMsg(int event, int wparam, int lparam, int size) {
		this.event = event;
		this.wparam = wparam;
		this.lparam = lparam;
		this.data = EMPTY;
		if (size > 0) {
			this.data = new byte[size];
		}
	}
	
	public ChannelMsg(int event, int wparam, int lparam) {
		this.event = event;
		this.wparam = wparam;
		this.lparam = lparam;
		this.data = EMPTY;
	}
	
	public String toString() {
		return String.format("ChannelMsg(event=%d, wparam=%xh, lparam=%xh, size=%d)", 
				event, wparam, lparam, data == null? 0 : data.length);
	}
}

