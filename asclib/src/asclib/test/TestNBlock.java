package asclib.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TestNBlock {

	public static void main(String[] argv) throws IOException, InterruptedException {
		System.out.printf("Hello, World !!\n");
		// CoreNIO
		SocketChannel client = SocketChannel.open();
		client.configureBlocking(false);
		client.connect(new InetSocketAddress("127.0.0.1", 6000));
		while (true) {
			Thread.sleep(1);
			if (client.isConnectionPending()) {
				System.out.printf("pending, connected: %s\n", client.isConnected());
				System.out.printf("finish %s\n", client.finishConnect());
				System.out.printf("connected: %s\n", client.isConnected());
				break;
			}
		}
		while (true) {
			Thread.sleep(1);
			client.finishConnect();
			if (client.isConnected()) {
				System.out.printf("establish, connected: %s\n", client.isConnected());
				break;
			}
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Socket sock = client.socket();
		ByteBuffer b = ByteBuffer.allocate(0x1000);
		System.out.printf("wait ok\n");
		int hr = client.read(b);
		System.out.printf("channel.readed: %d\n", hr);
		OutputStream os = sock.getOutputStream();
		InputStream is = sock.getInputStream();
		//hr = is.read(new byte[100]);
		System.out.printf("InputStream.read: %d\n", hr);
		for (int i = 0; i < 100; i++) {
			b.rewind();
			try {
				hr = client.write(b);
				//os.write(b.array());
			}
			catch (java.io.IOException e) {
				System.out.printf("error: %s\n", e.getMessage());
				e.printStackTrace();
				break;
			}
			System.out.printf("[%d] write: %d\n", i, hr);
		}
		System.out.printf("END.\n");
	}
}


