package asclib.test;
import java.nio.ByteBuffer;
public class TestByteBuffer {
	public static void main(String[] argv) {
		ByteBuffer b = ByteBuffer.allocate(1024);
		byte[] c = b.array();
		for (int i = 0; i < 100; i++) b.put((byte)i);
		for (int i = 0; i < 100; i++) System.out.printf("[%d] %d\n", i, c[i]);
		for (int i = 0; i < 100; i++) c[i] = (byte)(i / 2);
		b.rewind();
		for (int i = 0; i < 10; i++) System.out.printf("[%d] %d\n", i, b.get());
	}
}
