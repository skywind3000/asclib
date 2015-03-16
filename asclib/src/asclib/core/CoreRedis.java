package asclib.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CoreRedis {
	
	public static class RedisError extends Exception {
		private static final long serialVersionUID = -8527064024116882268L;
		public RedisError(String what) { super(what); }
	}
	
	public static class Element { 
		public char cmd = 0; 
		public byte[] data = null; 
		public int value = 0; 
		public Element[] child = null;
	}
	
	private CoreReader _reader = new CoreReader();
		
	private int _state = 0;
	private int _mode = 0;
	private int _need = 0;
	private int _position = 0;
	private Iterator<Element> _it = null;
	private ArrayDeque<Element> _input = new ArrayDeque<Element>();
	private ArrayDeque<Element> _ready = new ArrayDeque<Element>();
	
	public void destroy() {
		_it = null;		
		if (_input != null) _input.clear();
		if (_ready != null) _ready.clear();	
		if (_reader != null) _reader.destroy();
		_input = null;
		_ready = null;
		_reader = null;
	}
	
	protected void finalize() throws java.lang.Throwable {
		destroy();
		super.finalize();
	}
	
	public void clear() {
		if (_input != null) _input.clear();
		if (_ready != null) _ready.clear();	
		if (_reader != null) _reader.clear();
		_state = 0;
		_mode = 0;
	}
	
	private byte[] trim(byte[] input) {
		int length = input.length;
		byte r = (byte)'\r';
		byte n = (byte)'\n';
		if (length == 0) return input;
		while (length > 0) {
			if (input[length - 1] != r && input[length - 1] != n) break;
			length--;
		}
		if (length == input.length) return input;
		byte[] t = new byte[length];
		System.arraycopy(input, 0, t, 0, length);
		return t;
	}
	
	private int bytes2int(byte[] m) {
		byte zero = (byte)'0';
		byte nine = (byte)'9';
		byte minus = (byte)'-';
		int value = 0;
		int length = m.length;
		for (int i = 0; i < length; i++) {
			byte cc = m[i];
			if (cc == minus) return -1;
			if (cc >= zero && cc <= nine) {
				value = value * 10 + (cc - zero);
			}
		}
		return value;
	}
	
	/**
	 * feed data into CoreRedis
	 * @param buf buffer
	 * @param offset position
	 * @param length size
	 */
	public void feed(byte[] buf, int offset, int length) {
		_reader.feed(buf, offset, length);
		while (true) {
			if (_state == 0) {
				_reader.mode(CoreReader.READ_BYTE, 0);
				byte[] m = _reader.read();
				if (m == null) break;
				assert m.length == 1: "Fatal error in RedisReader.feed";
				_mode = m[0];
				_state = 1;
			}
			if (_state == 1) {
				_reader.mode(CoreReader.READ_LINE, (int)'\n');
				byte[] m = _reader.read();
				if (m == null) break;
				m = trim(m);
				if (_mode != (int)'$') {
					Element e = new Element();
					e.cmd = (char)(_mode & 0xff);
					e.data = m;
					e.value = 0;
					if (_mode == (int)'*' || _mode == (int)':') {
						e.value = bytes2int(m);
						if (e.value < 0 && _mode == (int)'*') {
							e.data = null;
						}
					}
					_input.add(e);
					_state = 0;
				}	else {
					_need = bytes2int(m);
					if (_need < 0) {
						Element e = new Element();
						e.cmd = (int)'$';
						e.data = null;
						e.value = -1;
						_input.add(e);
						_state = 0;
					}	else {
						_state = 2;
					}
				}
			}
			if (_state == 2) {
				_reader.mode(CoreReader.READ_BLOCK, _need);
				byte[] m = _reader.read();
				if (m == null) break;
				Element e = new Element();
				e.cmd = '$';
				e.data = m;
				e.value = 0;
				_input.add(e);
				_state = 3;
			}
			if (_state == 3) {
				_reader.mode(CoreReader.READ_LINE, (int)'\n');
				byte[] m = _reader.read();
				if (m == null) break;
				_state = 0;
			}
		}
		while (true) {
			_position = 0;
			_it = _input.iterator();
			Element e = parseToken();
			if (e == null) break;
			_it = null;
			_ready.add(e);
			for (int i = 0; i < _position; i++) {
				_input.remove();
			}
		}
	}
	
	private Element nextElement() {
		if (_it.hasNext() == false) return null;
		Element e = _it.next();
		_position++;
		return e;
	}
	
	private Element parseToken() {
		Element e = nextElement();
		if (e == null) return null;
		switch (e.cmd) {
		case '+':
		case '-':
		case ':':			
		case '$':
			return e;
		case '*':
			if (e.value < 0) {
				e.child = null;
				return e;
			}
			e.child = new Element[e.value];
			for (int i = 0; i < e.value; i++) {
				Element x = parseToken();
				if (x == null) return null;
				e.child[i] = x;
			}
			return e;
		}
		return e;
	}
	
	public void feed(byte[] buf) {
		feed(buf, 0, buf.length);
	}
	
	public void feed(String s) {
		byte[] b = s.getBytes(CoreReader.UTF8_CHARSET);
		feed(b, 0, b.length);
	}
	
	/**
	 * Translate Element to Object
	 * @param e element
	 * @param convert true to convert byte[] to string false to keep byte[]
	 * @return
	 */
	public Object translate(Element e, boolean convert) {
		switch (e.cmd) {
		case '+':
		case '$':
			if (e.data == null) return null;
			if (convert) {
				return new String(e.data, CoreReader.UTF8_CHARSET);
			}
			return e.data;
		case '-':
			return new RedisError(new String(e.data, CoreReader.UTF8_CHARSET));
		case ':':
			return new Integer(e.value);
		case '*':
			if (e.child == null) return null;
			else {
				ArrayList<Object> array = new ArrayList<Object>();
				for (int i = 0; i < e.child.length; i++) {
					Object o = translate(e.child[i], convert);
					array.add(o);
				}
				return array;
			}
		}
		return new RedisError("unknow command: '" + e.cmd + "'");
	}
	
	/**
	 * Retrieves and removes the first element, or returns null if not enough data.
	 * @return element, or null if not enough data
	 */
	public Element poll() {
		return _ready.pollFirst();
	}
	
	private static final byte[] T_CRLF = "\r\n".getBytes(CoreReader.UTF8_CHARSET);
	private static final byte[] T_NULL = "$-1\r\n".getBytes(CoreReader.UTF8_CHARSET);
	
	private void write(ArrayList<byte[]> output, byte[] o) {
		output.add(o);
	}
	
	private void write(ArrayList<byte[]> output, String s) {
		byte[] b = s.getBytes(CoreReader.UTF8_CHARSET);
		output.add(b);
	}
	
	public void encode(ArrayList<byte[]> output, Object o) {
		if (o == null) {
			write(output, T_NULL);
		}
		else if (o instanceof String) {
			String s = ((String) o);
			byte[] b = s.getBytes(CoreReader.UTF8_CHARSET);					
			write(output, "$" + String.valueOf(b.length) + "\r\n");
			write(output, b);
			write(output, T_CRLF);
		}
		else if (o instanceof byte[]) {
			byte[] b = ((byte[]) o);
			write(output, "$" + String.valueOf(b.length) + "\r\n");
			write(output, b);
			write(output, T_CRLF);
		}
		else if (o instanceof Number) {
			String s = String.valueOf((Number)o);
			byte[] x = s.getBytes(CoreReader.UTF8_CHARSET);
			write(output, "$" + String.valueOf(x.length) + "\r\n");
			write(output, x);
			write(output, T_CRLF);
		}
		else if (o instanceof List) {
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>)o;
			int size = list.size();
			write(output, "*" + String.valueOf(size) + "\r\n");
			for (Object e : list) {
				encode(output, e);
			}
		}
		else if (o instanceof Object[]) {
			Object[] array = (Object[])o;
			int size = array.length;
			write(output, "*" + String.valueOf(size) + "\r\n");
			for (int i = 0; i < size; i++) {
				Object e = array[i];
				encode(output, e);
			}
		}
	}
	
	public byte[] serialize(Object o) {
		ArrayList<byte[]> output = new ArrayList<byte[]>();
		encode(output, o);
		int size = 0;
		int count = output.size();
		int position = 0;
		for (int i = 0; i < count; i++) size += output.get(i).length;
		byte[] buf = new byte[size];
		for (int i = 0; i < count; i++) {
			byte[] s = output.get(i);
			int length = s.length;
			if (length > 0) {
				System.arraycopy(s, 0, buf, position, length);
				position += length;
			}
		}
		output.clear();
		output = null;
		return buf;
	}
	
	public byte[] marshal(Object ... args) {
		return serialize(args);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CoreRedis r = new CoreRedis();
		String x = "*3\r\n$3\r\nSET\r\n$1\r\nX\r\n$10\r\n012\t4\r\n789\r\n+PING\r\n-girl\r\n:23\r\n$-1\r\n*-1\r\n";
		String y = "*4\r\n$3\r\nHAH\r\n*2\r\n$4\r\nFUCK\r\n$4\r\nSUCK\r\n$5\r\nHELLO\r\n*0\r\n*1\r\n$0\r\n\r\n";
		r.feed(x.substring(0, 40));
		r.feed(x.substring(40));
		r.feed(y);
		byte[] b = r.marshal("set", "x", "100");
		System.out.println(CoreEncode.repr(b));
		r.feed(b);
		b = r.marshal("set", "y", null, new Object[] {1,2,3,null,""});
		System.out.println(CoreEncode.repr(b));
		r.feed(b);
		for (int i = 0; ; i++) {
			Element e = r.poll();
			if (e == null) break;
			System.out.printf("[%d] %s\n", i, r.translate(e, true));
		}
		
		System.out.println("");
	}
}


