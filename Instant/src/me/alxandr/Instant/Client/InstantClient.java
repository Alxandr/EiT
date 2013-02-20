package me.alxandr.Instant.Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public abstract class InstantClient {
	// The host:port combination to listen on
	private InetAddress _hostAddress;
	private int _port;
	
	private volatile boolean _running = false;
	
	private Thread _listener;
	private ExecutorService _executor;
	
	private Socket _socket = null;
	private DataOutputStream _oStream;
	
	public InstantClient(InetAddress address, int port) {
		_hostAddress = address;
		_port = port;
		_executor = Executors.newCachedThreadPool(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				return t;
			}
		});
	}
	
	public void connect() throws IOException {
		_socket = new Socket(_hostAddress, _port);
		final DataInputStream iStream = new DataInputStream(_socket.getInputStream());
		_oStream = new DataOutputStream(_socket.getOutputStream());
		_running = true;
		
		Thread reader = new Thread() {
			@Override
			public void run() {
				while(_running) {
					try {
						_socket.setSoTimeout(1000);
						try {
							if(iStream.available() > 0) {
								byte[] buffer = new byte[4096];
								int length = iStream.read(buffer);
								if(length == -1) {
									// Remote end shut down clean
									onDisconnect();
									return;
								} else {
									onProcessData(buffer, length);
								}
							} else {
								try {
									Thread.sleep(200);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						} catch (SocketTimeoutException te) {
							
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		reader.setDaemon(true);
		onConnect();
		reader.start();
	}
	
	public void send(ByteBuffer buffer) throws IOException {
		_oStream.write(buffer.array(), buffer.arrayOffset(), buffer.limit());
	}
	
	private void onConnect () {
		_executor.execute(new Runnable () {
			@Override
			public void run () {
				connected ();
			}
		});
	}
	
	private void onDisconnect () {
		_executor.execute(new Runnable () {
			@Override
			public void run () {
				disconnected ();
			}
		});
	}
	
	private void onProcessData (final byte[] data, final int length) {
		_executor.execute(new Runnable () {
			@Override
			public void run () {
				processData(data, length);
			}
		});
	}
	
	protected abstract void connected ();
	protected abstract void disconnected ();
	protected abstract void processData (byte[] data, int length);
}
