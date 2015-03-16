package asclib.core;

public class CoreKit {
	public static byte[] realloc(byte[] src, int newsize) {
		byte[] dst = new byte[newsize];
		int size = src.length;
		System.arraycopy(src, 0, dst, 0, (newsize < size)? newsize : size);
		return dst;
	}
	
	public static short[] realloc(short[] src, int newsize) {
		short[] dst = new short[newsize];
		int size = src.length;
		System.arraycopy(src, 0, dst, 0, (newsize < size)? newsize : size);
		return dst;
	}
	
	public static int[] realloc(int[] src, int newsize) {
		int[] dst = new int[newsize];
		int size = src.length;
		System.arraycopy(src, 0, dst, 0, (newsize < size)? newsize : size);
		return dst;
	}
	
	public static long[] realloc(long[] src, int newsize) {
		long[] dst = new long[newsize];
		int size = src.length;
		System.arraycopy(src, 0, dst, 0, (newsize < size)? newsize : size);
		return dst;
	}
}
