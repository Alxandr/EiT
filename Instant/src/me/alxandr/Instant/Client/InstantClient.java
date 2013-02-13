package me.alxandr.Instant.Client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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
	
	public InstantClient (InetAddress hostAddress, int port) throws IOException
	{
		_hostAddress = hostAddress;
		_port = port;
		_selector = initSelector();
	}
	
	public void connect () throws IOException {
		if(_running)
			throw new IllegalStateException("Already accepting");
		_running = true;
		
		SocketChannel socketChannel = SocketChannel.open ();
		socketChannel.configureBlocking (false);
		
		socketChannel.connect (new InetSocketAddress (_hostAddress, _port));
		socketChannel.register (_selector, SelectionKey.OP_CONNECT);
		
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
	
	public void disconnect () throws IOException {
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
	
	private Selector initSelector () throws IOException {
		// Create a new selector
		return SelectorProvider.provider().openSelector();
	}
	
	private void bgThread () {
		try {
			while (_running) {
				if (_selectedKeys != null) {
					while (_selectedKeys.hasNext()) {
						SelectionKey key = (SelectionKey)_selectedKeys.next();
						_selectedKeys.remove();
						
						if (!key.isValid ()) {
							continue;
						}
						
						if (key.isConnectable ()) {
							connect(key);
						} else if (key.isReadable ()) {
							read(key);
						}
					}
				}
				if(_selector.select (1000) > 0) {
					_selectedKeys = _selector.selectedKeys ().iterator ();
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private final void connect(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		// Finish the connection. If the connection operation failed
		// this will raise an IOException.
		try {
			socketChannel.finishConnect();
		} catch (IOException e) {
			// Cancel the channel's registration with our selector
			System.out.println(e);
			key.cancel();
			return;
		}
		
		key.interestOps(SelectionKey.OP_READ);
		onConnect();
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
	      onDisconnect();
	      return;
	    }
		
		if (numRead == -1) {
	      // Remote entity shut the socket down cleanly. Do the
	      // same from our end and cancel the channel.
	      key.channel().close();
	      key.cancel();
	      onDisconnect();
	      return;
	    }
		
		byte[] data = new byte[numRead];
		_readBuffer.get(data, 0, numRead);
		onProcessData(data);
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
	
	private void onProcessData (final byte[] data) {
		_executor.execute(new Runnable () {
			@Override
			public void run () {
				processData(data);
			}
		});
	}
	
	protected abstract void connected ();
	protected abstract void disconnected ();
	protected abstract void processData (byte[] data);
}
