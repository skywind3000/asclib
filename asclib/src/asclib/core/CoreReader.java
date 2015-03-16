//=====================================================================
//
// asclib.core.CoreReader - Async Protocol Reader
//
// NOTE:
// for more information, please see the readme file.
//
//=====================================================================
package asclib.core;

import java.nio.charset.Charset;

public class CoreReader {
	public static final int READ_BYTE	= 0;
	public static final int READ_LINE	= 1;
	public static final int READ_BLOCK	= 2;
	
	private CoreRing _input = new CoreRing();
	private CoreRing _cache = new CoreRing();
	private int _mode = READ_BYTE;
	private int _need = 0;
	private byte _spliter = 0;
	
	public final static Charset UTF8_CHARSET = Charset.forName("UTF-8");
	
	public CoreReader() {
		_input.auto(true);
		_cache.auto(true);
		_mode = READ_BYTE;
	}
	
	public void destroy() {
		if (_input != null) _input.destroy();
		if (_cache != null) _cache.destroy();
		_input = null;
		_cache = null;
	}
	
	protected void finalize() throws java.lang.Throwable {
		destroy();
		super.finalize();
	}	
	
	private void reset() {
		if (_cache.length() > 0) {
			CoreRing tmp = new CoreRing();
			tmp.auto(true);
			tmp.transfer(_input);
			_input.transfer(_cache);
			_input.transfer(tmp);
			tmp.destroy();
		}
	}
	
	/**
	 * change mode
	 * @param newmode READ_BYTE, READ_LINE or READ_BLOCK
	 * @param what line split (READ_LINE) or block size (READ_BLOCK)
	 */
	public void mode(int mode, int what) {
		if (mode == READ_LINE) {
			if (_mode == mode && _spliter == (byte)(what & 0xff)) 
				return;
			_mode = READ_LINE;
			if (what < 0) what = (int)'\n';
			_spliter = (byte)(what & 0xff);
		}
		else if (mode == READ_BLOCK) {
			_need = what;			
			if (_mode == READ_BLOCK) 
				return;
			_mode = READ_BLOCK;
		}
		else {
			assert mode == READ_BYTE : "Error Mode";
			_mode = READ_BYTE;
		}
		reset();
	}
	
	/**
	 * get a BYTE, LINE or BLOCK, use mode to set
	 * @return data if available, null for not enouth data.
	 */
	public byte[] read() {
		if (_mode == READ_BYTE) {
			if (_cache.length() > 0) {
				byte[] b = new byte[1];
				_cache.read(b, 0, 1);
				return b;
			}
			if (_input.length() > 0) {
				byte[] b = new byte[1];
				_input.read(b, 0, 1);
				return b;
			}
			return null;
		}
		else if (_mode == READ_LINE) {
			byte spliter = _spliter;
			while (_input.length() > 0) {
				byte[] ring = _input.array();
				int position = _input.position();
				int pitch = _input.pitch();
				int i = 0;
				for (i = 0; i < pitch; i++) {
					byte ch = ring[position + i];
					if (ch == spliter) break;
				}
				if (i >= pitch) {
					_cache.write(ring, position, pitch);
					_input.drop(pitch);
				}	else {
					_cache.write(ring, position, i + 1);
					_input.drop(i + 1);
					int size = _cache.length();
					byte[] b = new byte[size];
					_cache.read(b, 0, size);
					return b;
				}
			}
		}
		else if (_mode == READ_BLOCK) {
			int length = _input.length();
			if (length < _need) return null;
			byte[] b = new byte[_need];
			_input.read(b, 0, _need);
			return b;
		}
		return null;
	}
	
	public void feed(byte[] buf, int offset, int length) {
		_input.write(buf, offset, length);
	}
	
	public void feed(byte[] buf) {
		_input.write(buf, 0, buf.length);
	}
	
	public void feed(String text) {
		byte[] buf = text.getBytes(UTF8_CHARSET);
		_input.write(buf, 0, buf.length);
	}
	
	public void clear() {
		_input.clear();
		_cache.clear();
		_mode = READ_BYTE;
	}
	
	/**
	 * testing case
	 * @param args ignore
	 */
	public static void main(String[] args) {
		CoreReader reader = new CoreReader();
		reader.feed("*3\r\n$3\r\nSET\r\n$1\r\nX\r\n$10\r\n012\t4\r\n789\r\n+PING\r\n");
		reader.mode(CoreReader.READ_LINE, 10);
		while (true) {
			byte[] b = reader.read();
			if (b == null) break;
			System.out.println(new String(b));
		}
	}
}


