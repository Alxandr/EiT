package me.alxandr.Instant.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;

public abstract class InstantServer {
	// The host:port combination to listen on
	private InetAddress _hostAddress;
	private int _port;
	
	private volatile boolean _running = false;
	
	private Thread _listener;
	private ExecutorService _executor;
	
	private ServerSocket _serverSocket = null;
	
	private HashMap<Socket, DataOutputStream> _outStreams;
	
	public InstantServer(InetAddress address, int port) {
		_port = port;
		_hostAddress = address;
		_outStreams = new HashMap<Socket, DataOutputStream>();
		_executor = Executors.newCachedThreadPool(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				return t;
			}
		});
	}
	
	public synchronized void open() throws IOException {
		if(_running)
			throw new IllegalStateException("Already opened");
		
		_running = true;
		
		_serverSocket = new ServerSocket(10007, 5, _hostAddress);
		_listener = new Thread() {
			@Override
			public void run() {
				while(_running) {
					try {
						_serverSocket.setSoTimeout(1000);
						try {
							Socket socket = _serverSocket.accept();
							onClientConnected(socket);
						} catch (SocketTimeoutException te) {
							
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		_listener.setDaemon(true);
		_listener.start();
	}
	
	private final void onClientConnected(final Socket socket) throws IOException {
		final DataInputStream iStream = new DataInputStream(socket.getInputStream());
		final DataOutputStream oStream = new DataOutputStream(socket.getOutputStream());
		
		synchronized(_outStreams) {
			_outStreams.put(socket, oStream);
		}
		
		Thread reader = new Thread() {
			@Override
			public void run() {
				while(_running) {
					try {
						socket.setSoTimeout(1000);
						try {
							if(iStream.available() > 0) {
								byte[] buffer = new byte[4096];
								int length = iStream.read(buffer);
								if(length == -1) {
									// Remote end shut down clean
									onClientDisconnect(socket);
									return;
								} else {
									onProcessData(socket, buffer, length);
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
		reader.start();
		_executor.execute(new Runnable() {
			@Override
			public void run() {
				clientConnected(socket);
			}
		});
	}
	
	public void send(Socket socket, ByteBuffer buffer) throws IOException {
		synchronized(_outStreams) {
			DataOutputStream oStream = _outStreams.get(socket);
			oStream.write(buffer.array(), buffer.arrayOffset(), buffer.limit());
		}
	}
	
	private final void onClientDisconnect(final Socket socket) {
		_executor.execute(new Runnable() {
			@Override
			public void run() {
				clientDisconnected(socket);
			}
		});
	}
	
	private final void onProcessData(final Socket socket, final byte[] array, final int length) {
		_executor.execute(new Runnable() {
			@Override
			public void run() {
				processData(socket, array, length);
			}
		});
	}

	protected abstract void processData(Socket socketChannel, byte[] array, int length);
	protected abstract void clientConnected(Socket socketChannel);
	protected abstract void clientDisconnected(Socket socketChannel);
}
