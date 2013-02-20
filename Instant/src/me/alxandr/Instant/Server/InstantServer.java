package me.alxandr.Instant.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
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
import java.util.concurrent.ThreadPoolExecutor;

public abstract class InstantServer {
	// The host:port combination to listen on
	private InetAddress _hostAddress;
	private int _port;
	
	// The channel on which we'll accept connections
	private ServerSocketChannel _serverChannel;
	
	// The selector we'll be monitoring
	private Selector _selector;
	private Iterator _selectedKeys;
	
	// Incomming buffer
	private ByteBuffer _readBuffer = ByteBuffer.allocate(8192);
	
	private volatile boolean _running = false;
	
	private Thread _runner;
	private ExecutorService _executor;
	
	public InstantServer(InetAddress hostAddress, int port) throws IOException
	{
		_hostAddress = hostAddress;
		_port = port;
		_selector = initSelector();
	}
	
	private Selector initSelector() throws IOException
	{
		// Create a new selector
		Selector socketSelector = SelectorProvider.provider().openSelector();
		
		// Create a new non-blocking server socket channel
		_serverChannel = ServerSocketChannel.open();
		_serverChannel.configureBlocking(false);
		
		// Bind the socket to the specified address and port
		InetSocketAddress isa = new InetSocketAddress(_hostAddress, _port);
		_serverChannel.socket().bind(isa);
		
		// Register the server socket channel, indicating an interest in
		// accepting new connections
		_serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);
		
		return socketSelector;
	}
	
	public final synchronized void start () {
		if(_running)
			throw new IllegalStateException("Already accepting");
		_running = true;
		
		_executor = Executors.newCachedThreadPool(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable arg0) {
				Thread t = new Thread(arg0);
				t.setDaemon(true);
				return t;
			}
		});
		_runner = new Thread() {
			@Override
			public void run() {
				bgThread();
			}
		};
		_runner.setDaemon(true);
		_runner.start();
	}
	
	private void bgThread () {
		try {
			while(_running) {
				if(_selectedKeys != null) {
					while(_selectedKeys.hasNext()) {
						SelectionKey key = (SelectionKey)_selectedKeys.next();
						_selectedKeys.remove();
						
						if(!key.isValid()) {
							continue;
						}
						
						if(key.isAcceptable()) {
							accept(key);
						} else if(key.isReadable()) {
							read(key);
						}
					}
				}
				if(_selector.select(1000) > 0) {
					_selectedKeys = _selector.selectedKeys().iterator();
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public final synchronized void send(SocketChannel channel, byte[] data) throws IOException
	{
		send(channel, ByteBuffer.wrap(data.clone()));
	}
	
	public final synchronized void send(SocketChannel channel, ByteBuffer buffer) throws IOException
	{
		channel.write(buffer);
	}
	
	public final synchronized void stop()
	{
		if(!_running)
			throw new IllegalStateException("Not accepting");
		
		_running = false;
		try {
			_runner.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		_runner = null;
		_executor.shutdown();
		_executor = null;
	}
	
	private final void accept(SelectionKey key) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		
		// Accept the connection and make it non-blocking
		final SocketChannel channel = serverSocketChannel.accept();
		Socket socket = channel.socket();
		channel.configureBlocking(false);
		
		// Register the new SocketChannel with our Selector, indicating
		// we'd like to be notified when there's data waiting to be read
		channel.register(_selector, SelectionKey.OP_READ);
		_executor.execute(new Runnable() {
			@Override
			public void run() {
				clientConnected(channel);
			}
		});
	}
	
	private final void read(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		// Clear out our read buffer so it's ready for new data
		_readBuffer.clear();
		
		// Attempt to read off the channel
		int numRead;
		try {
	      numRead = socketChannel.read(_readBuffer);
	    } catch (IOException e) {
	      // The remote forcibly closed the connection, cancel
	      // the selection key and close the channel.
	      key.cancel();
	      socketChannel.close();
	      onClientDisconnect(socketChannel);
	      return;
	    }
		
		if (numRead == -1) {
	      // Remote entity shut the socket down cleanly. Do the
	      // same from our end and cancel the channel.
	      key.channel().close();
	      key.cancel();
	      onClientDisconnect(socketChannel);
	      return;
	    }
		
		final SocketChannel channel = socketChannel;
		_readBuffer.position(0);
		final byte[] data = new byte[numRead];
		_readBuffer.get(data, 0, data.length);
		_executor.execute(new Runnable() {
			@Override
			public void run() {
				processData(channel, data);
			}
		});
	}
	
	private final void onClientDisconnect(final SocketChannel channel)
	{
		_executor.execute(new Runnable() {
			@Override
			public void run() {
				clientDisconnected(channel);
			}
		});
	}

	protected abstract void processData(SocketChannel socketChannel, byte[] array);
	protected abstract void clientConnected(SocketChannel socketChannel);
	protected abstract void clientDisconnected(SocketChannel socketChannel);
}
