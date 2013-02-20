package me.alxandr.Instant.Client;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public abstract class MessageClient extends InstantClient {

	private final ByteBuffer _buffer = ByteBuffer.allocate(4096);
	private int _length;
	
	public MessageClient(InetAddress hostAddress, int port) throws IOException {
		super(hostAddress, port);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected final void processData(byte[] data, int length) {
		// TODO Auto-generated method stub
		_buffer.position (_length);
		_length += data.length;
		_buffer.put (data, 0, length);
		
		while(true) {
			if(_length < 2)
				return;
			
			_buffer.position(0);
			length = _buffer.getShort ();
			if(_length >= length + 2 && length > 0) {
				byte[] msg = new byte[length];
				_buffer.get(msg, 0, length);
				_buffer.position (length + 2);
				_buffer.compact ();
				_length -= length + 2;
				processMessage (msg);
			} else {
				return;
			}
		}
	}
	
	public void send(byte[] message) throws IOException {
		ByteBuffer buff = ByteBuffer.allocate(message.length + 2);
		buff.putShort((short) message.length);
		buff.put(message);
		send(buff);
	}
	
	protected abstract void processMessage(byte[] msg);

}
