//=====================================================================
//
// asclib.core.CoreEncode - Encoding, Decoding and Serializing
// 
// NOTE:
// for more information, please see the readme file
//
//=====================================================================
package asclib.core;

public class CoreEncode {
	
	public static void encode8i(byte[] buf, int pos, byte x) {
		buf[pos] = x;
	}
	
	public static byte decode8i(byte[] buf, int pos) {
		return buf[pos];
	}
	
	public static void encode8u(byte[] buf, int pos, int x) {
		buf[pos] = (byte)(x & 0xff);
	}
	
	public static int decode8u(byte[] buf, int pos) {
		return ((int)buf[pos]) & 0xff;
	}
	
	public static void encode16i_lsb(byte[] buf, int pos, short x) {
		buf[pos + 0] = (byte)(x & 0xff);
		buf[pos + 1] = (byte)((x >>> 8) & 0xff);
	}
	
	public static short decode16i_lsb(byte[] buf, int pos) {
		int x1 = ((int)buf[pos + 0]) & 0xff;
		int x2 = ((int)buf[pos + 1]) & 0xff;
		return (short)(((x2 << 8) | x1) & 0xffff);
	}
	
	public static void encode16i_msb(byte[] buf, int pos, short x) {
		buf[pos + 1] = (byte)(x & 0xff);
		buf[pos + 0] = (byte)((x >>> 8) & 0xff);		
	}
	
	public static short decode16i_msb(byte[] buf, int pos) {
		int x1 = ((int)buf[pos + 1]) & 0xff;
		int x2 = ((int)buf[pos + 0]) & 0xff;
		int x3 = (x2 << 8) | x1;
		return (short)(x3 & 0xffff);
	}
	
	public static void encode16u_lsb(byte[] buf, int pos, int x) {
		buf[pos + 0] = (byte)(x & 0xff);
		buf[pos + 1] = (byte)((x >>> 8) & 0xff);
	}
	
	public static int decode16u_lsb(byte[] buf, int pos) {
		int x1 = ((int)buf[pos + 0]) & 0xff;
		int x2 = ((int)buf[pos + 1]) & 0xff;
		return ((x2 << 8) | x1) & 0xffff;
	}
	
	public static void encode16u_msb(byte[] buf, int pos, int x) {
		buf[pos + 1] = (byte)(x & 0xff);
		buf[pos + 0] = (byte)((x >>> 8) & 0xff);
	}
	
	public static int decode16u_msb(byte[] buf, int pos) {
		int x1 = ((int)buf[pos + 1]) & 0xff;
		int x2 = ((int)buf[pos + 0]) & 0xff;
		return ((x2 << 8) | x1) & 0xffff;
	}
	
	public static void encode32i_lsb(byte[] buf, int pos, int x) {
		buf[pos + 0] = (byte)(x & 0xff);
		buf[pos + 1] = (byte)((x >>> 8) & 0xff);
		buf[pos + 2] = (byte)((x >>> 16) & 0xff);
		buf[pos + 3] = (byte)((x >>> 24) & 0xff);
	}
	
	public static int decode32i_lsb(byte[] buf, int pos) {
		int x1 = ((int)buf[pos + 0]) & 0xff;
		int x2 = ((int)buf[pos + 1]) & 0xff;
		int x3 = ((int)buf[pos + 2]) & 0xff;
		int x4 = ((int)buf[pos + 3]) & 0xff;
		return (x1) | (x2 << 8) | (x3 << 16) | (x4 << 24);
	}
	
	public static void encode32i_msb(byte[] buf, int pos, int x) {
		buf[pos + 3] = (byte)(x & 0xff);
		buf[pos + 2] = (byte)((x >>> 8) & 0xff);
		buf[pos + 1] = (byte)((x >>> 16) & 0xff);
		buf[pos + 0] = (byte)((x >>> 24) & 0xff);
	}
	
	public static int decode32i_msb(byte[] buf, int pos) {
		int x1 = ((int)buf[pos + 3]) & 0xff;
		int x2 = ((int)buf[pos + 2]) & 0xff;
		int x3 = ((int)buf[pos + 1]) & 0xff;
		int x4 = ((int)buf[pos + 0]) & 0xff;
		return (x1) | (x2 << 8) | (x3 << 16) | (x4 << 24);
	}
	
	public static void encode32u_lsb(byte[] buf, int pos, long x) {
		buf[pos + 0] = (byte)(x & 0xff);
		buf[pos + 1] = (byte)((x >>> 8) & 0xff);
		buf[pos + 2] = (byte)((x >>> 16) & 0xff);
		buf[pos + 3] = (byte)((x >>> 24) & 0xff);
	}
	
	public static long decode32u_lsb(byte[] buf, int pos) {
		int x1 = ((int)buf[pos + 0]) & 0xff;
		int x2 = ((int)buf[pos + 1]) & 0xff;
		int x3 = ((int)buf[pos + 2]) & 0xff;
		int x4 = ((int)buf[pos + 3]) & 0xff;
		int x5 = (x1) | (x2 << 8) | (x3 << 16) | (x4 << 24);
		return ((long)x5) & 0xffffffff;
	}
	
	public static void encode32u_msb(byte[] buf, int pos, long x) {
		buf[pos + 3] = (byte)(x & 0xff);
		buf[pos + 2] = (byte)((x >>> 8) & 0xff);
		buf[pos + 1] = (byte)((x >>> 16) & 0xff);
		buf[pos + 0] = (byte)((x >>> 24) & 0xff);
	}
	
	public static long decode32u_msb(byte[] buf, int pos) {
		int x1 = ((int)buf[pos + 3]) & 0xff;
		int x2 = ((int)buf[pos + 2]) & 0xff;
		int x3 = ((int)buf[pos + 1]) & 0xff;
		int x4 = ((int)buf[pos + 0]) & 0xff;
		int x5 = (x1) | (x2 << 8) | (x3 << 16) | (x4 << 24);
		return ((long)x5) & 0xffffffff;
	}
	
	public static void encode64i_lsb(byte[] buf, int pos, long x) {
		buf[pos + 0] = (byte)(x & 0xff);
		buf[pos + 1] = (byte)((x >>> 8) & 0xff);
		buf[pos + 2] = (byte)((x >>> 16) & 0xff);
		buf[pos + 3] = (byte)((x >>> 24) & 0xff);
		buf[pos + 4] = (byte)((x >>> 32) & 0xff);
		buf[pos + 5] = (byte)((x >>> 40) & 0xff);
		buf[pos + 6] = (byte)((x >>> 48) & 0xff);
		buf[pos + 7] = (byte)((x >>> 56) & 0xff);	
	}
	
	public static long decode64i_lsb(byte[] buf, int pos) {
		long x1 = decode32u_lsb(buf, pos);
		long x2 = decode32u_lsb(buf, pos + 4);
		return (x1) | (x2 << 32);
	}
	
	public static void encode64i_msb(byte[] buf, int pos, long x) {
		buf[pos + 7] = (byte)(x & 0xff);
		buf[pos + 6] = (byte)((x >>> 8) & 0xff);
		buf[pos + 5] = (byte)((x >>> 16) & 0xff);
		buf[pos + 4] = (byte)((x >>> 24) & 0xff);
		buf[pos + 3] = (byte)((x >>> 32) & 0xff);
		buf[pos + 2] = (byte)((x >>> 40) & 0xff);
		buf[pos + 1] = (byte)((x >>> 48) & 0xff);
		buf[pos + 0] = (byte)((x >>> 56) & 0xff);	
	}
	
	public static long decode64i_msb(byte[] buf, int pos) {
		long x2 = decode32u_lsb(buf, pos);
		long x1 = decode32u_lsb(buf, pos + 4);
		return (x1) | (x2 << 32);
	}
	
	public static String bin2txt(byte[] buf, int pos, int len) {
		String txt = "";
		for (int i = 0; i < len; i++) {
			int ch = ((int)buf[pos + i]) & 0xff;
			txt += Integer.toHexString(ch) + ((i < len - 1)? " " : "");
		}
		return txt;
	}
	
	public static String repr(byte[] buf, int pos, int len) {
		String txt = "\"";
		String hex = "0123456789ABCDEF";
		for (int i = 0; i < len; i++) {
			int ch = ((int)buf[pos + i]) & 0xff;
			if (ch == (int)'\r') {
				txt += "\\r";
			}
			else if (ch == (int)'\n') {
				txt += "\\n";
			}
			else if (ch == (int)'\t') {
				txt += "\\t";
			}
			else if (ch >= 0x20 && ch <= 0x7e && ch != (int)'"') {
				txt += (char)ch;
			}	
			else {
				txt += "\\x" + hex.charAt((ch >> 4)) + hex.charAt(ch & 0xf);
			}
		}
		return txt + "\"";
	}
	
	public static String repr(byte[] buf) {
		return repr(buf, 0, buf.length);
	}
	
	public static byte[] txt2bin(String text) {
		
		return null;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}



