package asclib.core;

import java.nio.ByteBuffer;

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
	private ByteBuffer bf = null;
	
	public CoreRing() {
		destroy();
		update();
	}
	
	public void destroy() {
		ring = null;
		head = tail = size = capacity = 0;
		if (bf != null) {
			bf.clear();
		}
		bf = null;
	}	
	
	public int length() {
		return size;
	}
	
	private void update() {
		if (head >= tail) {
			size = head - tail;
			if (bf != null) bf.limit(head);
		}	else {
			size = capacity - tail + head;
			if (bf != null) bf.limit(capacity);
		}
		if (bf != null) bf.position(tail);
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
		int saved = size;
		read(newring, 0, newring.length);
		head = saved;
		tail = 0;
		ring = newring;
		capacity = newCapacity;
		bf = ByteBuffer.wrap(ring);
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
		int canread = size;
		length = (length < canread)? length : canread;
		if (length == 0) return 0;
		if (buf == null && peek == true) return length;
		int half = capacity - tail;
		if (half >= length) {
			if (buf != null) {
				System.arraycopy(ring, tail, buf, offset, length);
			}
		}	else {
			if (buf != null) {
				System.arraycopy(ring, tail, buf, offset, half);
				System.arraycopy(ring, 0, buf, offset + half, length - half);
			}	
		}
		if (peek == false) {
			tail += length;
			if (tail >= capacity) tail -= capacity;
			update();
		}		
		return length;
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
		if (length < 0) {
			update();
			return 0;
		}
		return fetch(null, 0, length, false);
	}
	
	public byte[] array() {
		return ring;
	}
	
	public ByteBuffer buffer() {
		return bf;
	}	
	
	public int position() {
		return tail;
	}
	
	public int pitch() {
		return (head >= tail)? (head - tail) : (capacity - tail);
	}
	
	public int getRest() {
		return rest;
	}
	
	public int getCapacity() {
		return (capacity <= 0)? 0 : (capacity - 1);
	}
	
	public boolean auto() {
		return autoinc;
	}
	
	public void auto(boolean enable) {
		autoinc = enable;
	}
	
	public void clear() {
		head = 0;
		tail = 0;
		update();
	}
}


