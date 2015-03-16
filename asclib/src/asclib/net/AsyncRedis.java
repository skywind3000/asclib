package asclib.net;

import asclib.core.CoreNet;
import asclib.core.CoreRedis;
import asclib.core.CoreRedis.RedisError;

public class AsyncRedis {

	/**
	 * Callback for Redis 
	 */
	public static abstract class Callback {
		/**
		 * invoked when response received
		 * @param response response from redis
		 */
		public abstract void OnResponse(Object response);
		
		/**
		 * invoked when error received
		 * @param error error from redis
		 */
		public abstract void OnError(CoreRedis.RedisError error);
		
		/**
		 * invoked when start to connect
		 */
		public abstract void OnStart();
		
		/**
		 * invoked when connection established
		 */
		public abstract void OnConnected();
		
		/**
		 * invoked when connection closed
		 */
		public abstract void OnDisconnect();
	}
	
	private Callback _callback = null;
	private CoreRedis _redis = new CoreRedis();
	private CoreNet _net = new CoreNet();
	private int _state = CoreNet.STATE_CLOSED;
	private byte[] _buffer = new byte[8];
	
	private boolean _convert_string = true;
	
	private long _reconnect_ts = -1; 
	private int _reconnect_time = -1;
	private int _connect_timeout = -1;
	
	private String _host = "";
	private int _port = -1;
	
	public void destroy() {
		if (_redis != null) _redis.destroy();
		if (_net != null) _net.close(0);
		_redis = null;
		_net = null;
	}
	
	public Callback callback(Callback cb) {
		Callback o = _callback;
		_callback = cb;
		return o;
	}
	
	public void close() {
		if (_redis != null) {
			_redis.clear();
		}
		if (_net != null) {
			_net.close(0);
		}
		if (_state != CoreNet.STATE_CLOSED) {
			_state = CoreNet.STATE_CLOSED;
			if (_callback != null)
				_callback.OnDisconnect();
		}
		_reconnect_time = -1;
	}	
	
	/**
	 * connect to remote redis server
	 * @param ip redis address
	 * @param port redis port (6379 ?)
	 * @param timeout connection timeout (in seconds)
	 * @param reconnect reconnect timeout (in seconds)
	 */
	public void connect(String ip, int port, int timeout, int reconnect) {
		close();
		if (timeout <= 0) timeout = 10;
		_connect_timeout = (timeout > 0)? timeout * 1000 : -1;
		_reconnect_time = (reconnect > 0)? reconnect * 1000 : -1;		
		_net.timeout(_connect_timeout);
		_net.connect(ip, port, CoreNet.Header.RAWDATA);
		_state = _net.state();
		_host = ip;
		_port = port;
		_reconnect_ts = -1;
		if (_callback != null) 
			_callback.OnStart();
	}
	
	public void connect(String ip, int port) {
		connect(ip, port, 10, 5);
	}
	
	/**
	 * Call it every interval (eg.100ms)
	 */
	public void update() {
		if (_net == null || _redis == null) return;
		long current = System.currentTimeMillis();
		_net.update();
		int newstate = _net.state();
		int oldstate = _state;
		if (oldstate != CoreNet.STATE_ESTAB && newstate == CoreNet.STATE_ESTAB) {
			_state = CoreNet.STATE_ESTAB;
			if (_callback != null)
				_callback.OnConnected();
			_reconnect_ts = -1;
		}
		
		// receive network message and pass to CoreRedis
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
				_redis.feed(_buffer, 0, hr);
			}
		}
		
		// poll message from CoreRedis and dispatch
		while (true) {
			CoreRedis.Element element = _redis.poll();
			if (element == null) break;
			Object o = _redis.translate(element, _convert_string);
			if (_callback != null) {
				if (o == null) {
					_callback.OnResponse(null);
				}
				else if (o instanceof CoreRedis.RedisError) {
					_callback.OnError((CoreRedis.RedisError)o);
				}	
				else {
					_callback.OnResponse(o);
				}
			}
		}
		
		// update state
		oldstate = _state;
		if (oldstate != CoreNet.STATE_CONNECTING && newstate == CoreNet.STATE_CONNECTING) {
			_state = CoreNet.STATE_CONNECTING;
		}
		oldstate = _state;
		if (oldstate != CoreNet.STATE_CLOSED && newstate == CoreNet.STATE_CLOSED) {
			_state = CoreNet.STATE_CLOSED;
			if (_callback != null) 
				_callback.OnDisconnect();
		}
		// calculate reconnect
		if (_state == CoreNet.STATE_CLOSED) {
			if (_reconnect_ts < 0 && _reconnect_time > 0) {
				_reconnect_ts = current + _reconnect_time;
			}
			else if (_reconnect_ts > 0) {
				if (current >= _reconnect_ts) {
					connect(_host, _port, _connect_timeout / 1000, _reconnect_time / 1000);
				}
			}
		}
	}
	
	// send packet to redis server
	public void send(Object o) {
		if (_redis == null) return;
		byte[] b = _redis.serialize(o);
		_net.send(b, 0, b.length, 0);
	}
	
	// send request in vector
	public void request(Object ... args) {
		send(args);
	}
	
	// convert elements of response from byte[] to string
	public void convert(boolean on) {
		_convert_string = on;
	}
	
	// whether or not to convert byte[] to string in response
	public boolean convert() {
		return _convert_string;
	}
	
	/**
	 * testing case
	 * @param args ignore
	 */
	public static void main(String[] args) {
		AsyncRedis r = new AsyncRedis();
		
		r.callback(new Callback() {
			int connected_times = 0;
			
			@Override
			public void OnResponse(Object response) {
				// TODO Auto-generated method stub
				System.out.printf("OnResponse(%s)\n", response);
			}

			@Override
			public void OnError(RedisError error) {
				// TODO Auto-generated method stub
				System.out.printf("OnError(%s)\n", error.getMessage());
			}

			@Override
			public void OnStart() {
				// TODO Auto-generated method stub
				System.out.println("OnStart()");
			}

			@Override
			public void OnConnected() {
				// TODO Auto-generated method stub
				System.out.println("OnConnected()");
				connected_times++;
				if (connected_times == 2) {
					System.out.println("quit");
					System.exit(0);
				}
			}

			@Override
			public void OnDisconnect() {
				// TODO Auto-generated method stub
				System.out.println("OnDisconnect() - reconnect in 5 seconds");
			}});
		
		//r.connect("192.168.0.21", 6379);
		r.connect("xnode2.ddns.net", 6379);
		
		r.request("set", "x", 100);
		r.request("get", "x");
		r.request("del", "x");
		r.request("get", "x");
		r.request("set", "uid", 1024);
		r.request("fuck", "error");
		
		r.request("hset", "myhash", "key1", "v1");
		r.request("hset", "myhash", "key2", "v2");
		r.request("hgetall", "myhash");
		
		r.request("MULTI");
		r.request("echo", "TRANSACTION");
		r.request("echo", "CONTEXT:myctx1");
		r.request("get", "uid");
		r.request("hgetall", "myhash");
		r.request("EXEC");
		
		r.request("ECHO", "QUIT");
		r.request("QUIT");
		
		while (true) {
			r.update();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}



