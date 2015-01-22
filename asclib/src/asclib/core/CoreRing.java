package asclib.core;

/**
 * Ring Buffer
 *
 */
public class CoreRing {
	private byte[] ring = null;
	private int head = 0;
	private int tail = 0;
	private int size = 0;
	private int rest = 0;
	private int capacity = 0;
	private boolean autoinc = false;
	
	public CoreRing() {
		destroy();
		update();
	}
	
	public void destroy() {
		ring = null;
		head = tail = size = capacity = 0;
	}	
	
	public int length() {
		return size;
	}
	
	private void update() {
		if (head >= tail) {
			size = head - tail;
		}	else {
			size = capacity - tail + head;
		}
		rest = capacity - size - 1;
		if (rest < 0) rest = 0;
	}
	
	public void resize(int newCapacity) {
		newCapacity += 1;
		if (newCapacity <= capacity) return;
		for (int x = 64; ; x <<= 1) {
			if (x >= newCapacity) {
				newCapacity = x;
				break;
			}
		}
		byte[] newring = new byte[newCapacity];
		read(newring, 0, newring.length);
		head = size;
		tail = 0;
		ring = newring;
		capacity = newCapacity;
		update();
	}
	
	public int write(byte[] buf, int offset, int length) {
		if (buf != null) {
			if (offset + length > buf.length) {
				length = (buf.length >= offset)? (buf.length - offset) : 0;
			}
		}
		if (autoinc && length > rest) {
			resize(capacity + length - rest);
		}
		int canwrite = rest;
		int half = capacity - head;
		if (canwrite == 0 || length == 0) return 0;
		if (length > canwrite) length = canwrite;
		if (buf != null) {
			if (half >= length) {
				System.arraycopy(buf, offset, ring, head, length);
			}	else {
				System.arraycopy(buf, offset, ring, head, half);
				System.arraycopy(buf, offset + half, ring, 0, length - half);
			}
		}
		head += length;
		if (head >= capacity) head -= capacity;
		update();
		return length;
	}
	
	private int fetch(byte[] buf, int offset, int length, Boolean peek) {
		if (buf != null) {
			if (offset + length > buf.length) {
				length = (buf.length >= offset)? (buf.length - offset) : 0;
			}
		}		
		int canread = (size < length)? size : length;
		if (canread == 0) return 0;
		if (buf == null && peek == true) return canread;
		if (head >= tail) {
			if (buf != null) {
				System.arraycopy(ring, tail, buf, offset, canread);
			}
		}	else {
			int t1 = capacity - tail;
			int t2 = canread - t1;
			if (buf != null) {
				System.arraycopy(ring, tail, buf, offset, t1);
				System.arraycopy(ring, 0, buf, offset + t1, t2);
			}
		}
		if (peek == false) {
			tail += canread;
			if (tail >= capacity) tail -= capacity;
			update();
		}		
		return 0;
	}
	
	public int read(byte[] buf, int offset, int length) {
		return fetch(buf, offset, length, false);
	}
	
	public int read(byte[] buf) {
		return read(buf, 0, (buf != null)? buf.length : 0);
	}
	
	public int peek(byte[] buf, int offset, int length) {
		return fetch(buf, offset, length, true);
	}
	
	public int peek(byte[] buf) {
		return peek(buf, 0, (buf != null)? buf.length : 0);
	}
	
	public int drop(int length) {
		return fetch(null, 0, length, false);
	}
	
	public byte[] buffer() {
		return ring;
	}
	
	public int position() {
		return tail;
	}
	
	public int pitch() {
		return (head >= tail)? (head - tail) : (capacity - tail);
	}
	
	public int getLength() {
		return size;
	}
	
	public int getRest() {
		return rest;
	}
	
	public int getCapacity() {
		return (capacity <= 0)? 0 : (capacity - 1);
	}
}


