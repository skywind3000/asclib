//=====================================================================
//
// asclib.core.CoreRC4 - RC4 encryption
//
// NOTE:
// for more information, please see the readme file.
//
//=====================================================================
package asclib.core;

public class CoreRC4 {
	private int[] box = new int[256];
	private int X = -1;
	private int Y = -1;
	
	public CoreRC4(byte[] key) {
		if (key == null) {
			X = -1;
			Y = -1;
		}	else {
			int i = 0, j = 0, k = 0, a = 0;
			for (i = 0; i < 256; i++) {
				box[i] = i;
			}
			for (i = 0; i < 256; i++) {
				a = box[i];
				j = (j + a + (((int)key[k]) & 0xff)) & 255;
				box[i] = box[j];
				box[j] = a;
				if (++k >= key.length) k = 0;
			}
			X = 0;
			Y = 0;
		}
	}
	
	public void crypt(byte[] src, int srcPos, byte[] dst, int dstPos, int len) {
		if (X < 0 || Y < 0) {
			if (src != dst || srcPos != dstPos) {
				System.arraycopy(src, srcPos, dst, dstPos, len);
			}
		}	else {
			int a, b;
			for (; len > 0; srcPos++, dstPos++, len--) {
				X = (X + 1) & 255;
				a = box[X];
				Y = (Y + a) & 255;
				box[X] = box[Y];
				b = box[Y];
				box[Y] = a;
				dst[dstPos] = (byte) (src[srcPos] ^ box[(a + b) & 255]);
			}
		}
	}
}


