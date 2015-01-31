package asclib.net;

import asclib.core.CoreNet;

public abstract class ClientNet {

	/**
	 * Will be invoked when connection established
	 */
	public abstract void OnConnected();
	
	/**
	 * Will be invoked when connection broken
	 * @param code reason
	 * @param msg message for the reason
	 */
	public abstract void OnDisconnect(int code, String msg);
	
	/**
	 * Will be invoked when new packet arrived  
	 * @param packet binary data of new packet 
	 */
	public abstract void OnData(byte[] packet);
	
	/**
	 * Will be invoked every second during the connection lifetime
	 * use interval(NewPeriod) to change the period
	 */
	public abstract void OnUpdate();
	
	/**
	 * send data to remote endpoint
	 * @param buf buffer to send
	 * @param pos offset of buffer
	 * @param len how many bytes to be sent 
	 * @param mask subscribe mask
	 * @return true for success false for error
	 */
	public boolean send(byte[] buf, int pos, int len, int mask) {
		return _net.send(buf, pos, len, mask);
	}
	
	/**
	 * send data to remote endpoint
	 * @param buf buffer to send
	 * @param pos offset of buffer
	 * @param len how many bytes to be sent
	 * @return true for success false for error
	 */
	public boolean send(byte[] buf, int pos, int len) {
		return _net.send(buf, pos, len, 0);
	}
	
	/**
	 * send data to remote endpoint
	 * @param buf
	 * @return ture for success false for error
	 */
	public boolean send(byte[] buf) {
		return _net.send(buf, 0, buf.length, 0);
	}
	
	/**
	 * Send string in utf-8 format
	 * @param text
	 * @return
	 */
	public boolean send(String text) {
		return send(text.getBytes());
	}
	
	/**
	 * close socket connection
	 * @param code reason
	 */
	public void close(int code) {
		_net.close(code);
	}
	
	/**
	 * Connect the remote server
	 * @param ip address of server
	 * @param port port of server
	 * @param head message headers see {@link asclib.core.CoreNet.Header}
	 * @return true for success false for error
	 */
	public boolean connect(String ip, int port, int head) {
		return _net.connect(ip, port, head);
	}
	
	private CoreNet _net = new CoreNet();
	private int _state = CoreNet.STATE_CLOSED;
	private byte[] _buffer = new byte[8];
	private long _timeslap = 0;
	private int _period = 1000;
	
	/**
	 * Call it every interval (eg.100ms)
	 */
	public void update() {
		long current = System.currentTimeMillis();
		_net.update();
		int newstate = _net.state();
		
		if (newstate == CoreNet.STATE_ESTAB) {
			if (_state != CoreNet.STATE_ESTAB) {
				OnConnected();
				_state = CoreNet.STATE_ESTAB;
			}
		}
		
		while (true) {
			int hr = _net.recv(_buffer, 0, _buffer.length);
			if (hr == -2) {	// out of capacity
				int need = _net.recv(null, 0, 0);
				for (int newsize = 8; ; newsize <<= 1) {
					if (newsize >= need) {
						_buffer = new byte[newsize];
						break;
					}
				}
			}
			else if (hr < 0) {	// empty packet queue
				break;
			}
			else {
				byte[] packet = new byte[hr];
				System.arraycopy(_buffer, 0, packet, 0, hr);
				OnData(packet);
			}
		}
		
		if (newstate == CoreNet.STATE_CLOSED) {
			if (_state != CoreNet.STATE_CLOSED) {
				OnDisconnect(_net.code(), _net.message());
				_timeslap = System.currentTimeMillis();
				_state = CoreNet.STATE_CLOSED;
			}
		}

		_state = newstate;
		
		// invoke OnUpdate
		if (_state == CoreNet.STATE_ESTAB && _period > 0) {
			if (current >= _timeslap) {
				if (current - _timeslap > _period * 10) {
					_timeslap = current;
				}
				while (_timeslap < current) {
					_timeslap += _period;
				}
				OnUpdate();
			}
		}
	}
	
	/**
	 * Set interval of OnUpdate in millisec
	 * @param period how many millisecs between two OnUpdate
	 */
	public void interval(int period) {
		_period = period;
	}
	
	/**
	 * connection state: 0-closed, 1-connecting, 2-established see asclib.core.CoreNet
	 * @return current state
	 */
	public int state() {
		return _net.state();
	}
	
	/**
	 * flush
	 */
	public void flush() {
		_net.flush();
	}
	
	public boolean setNodelay(boolean on) {
		return _net.setNodelay(on);
	}
	
	public void limit(int bufferLimit) {
		_net.limit(bufferLimit);
	}
	
	/**
	 * Testing Case
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		ClientNet cnet = new ClientNet() {
			int index = 0;

			@Override
			public void OnConnected() {
				// TODO Auto-generated method stub
				System.out.println("connected");
			}

			@Override
			public void OnDisconnect(int code, String msg) {
				// TODO Auto-generated method stub
				System.out.printf("closed(%d): %s\n", code, msg);
			}

			@Override
			public void OnData(byte[] packet) {
				// TODO Auto-generated method stub
				String text = new String(packet);
				System.out.println("[RECV] " + text);
			}

			@Override
			public void OnUpdate() {
				// TODO Auto-generated method stub
				String text = "HELLO NETWORK PACKET: " + (index++);
				send(text.getBytes());
			}
			
		};
		
		cnet.connect("192.168.0.21", 6000, 0);
		while (cnet.state() != CoreNet.STATE_CLOSED) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			cnet.update();
		}
	}
}


