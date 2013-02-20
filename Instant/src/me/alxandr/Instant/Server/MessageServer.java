package me.alxandr.Instant.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.UUID;

public abstract class MessageServer extends InstantServer {

	final HashMap<Socket, MessageClient> _clients;
	final HashMap<UUID, MessageClient> _clientIds;
	
	public MessageServer(InetAddress hostAddress, int port) throws IOException {
		super(hostAddress, port);
		// TODO Auto-generated constructor stub
		_clients = new HashMap<Socket, MessageClient>();
		_clientIds = new HashMap<UUID, MessageServer.MessageClient>();
	}
	
	public final void send(UUID clientId, byte[] message) throws IOException
	{
		MessageClient client = _clientIds.get(clientId);
		ByteBuffer buffer = ByteBuffer.allocate(message.length + 2);
		buffer.putShort ((short) message.length);
		buffer.put (message, 0, message.length);
		send (client.channel (), buffer);
	}

	@Override
	protected final void clientConnected(Socket socketChannel)
	{
		MessageClient client = new MessageClient (socketChannel);
		synchronized(_clients) {
			_clients.put (socketChannel, client);
			_clientIds.put (client.id (), client);
		}
		clientConnected (client.id ());
	}
	
	@Override
	protected final void clientDisconnected (Socket socketChannel)
	{
		MessageClient client;
		synchronized (_clients) {
			client = _clients.get (socketChannel);
			_clients.remove (socketChannel);
			_clientIds.remove (client.id());
		}
		clientDisconnected (client.id ());
	}
	
	@Override
	protected final void processData (Socket socketChannel, byte[] array, int length)
	{
		MessageClient client;
		synchronized(_clients) {
			client = _clients.get(socketChannel);
		}
		
		client.append(array, 0, length);
		
		byte[] msg;
		while((msg = client.message ()) != null) {
			processMessage (client.id (), msg);
		}
	}
	
	protected abstract void clientConnected (UUID clientId);
	protected abstract void clientDisconnected (UUID clientId);
	protected abstract void processMessage (UUID clientId, byte[] message);
	
	private class MessageClient
	{
		private final ByteBuffer _buffer = ByteBuffer.allocate(4096);
		private final Socket _channel;
		private final UUID _id;
		private int _length;
		
		public MessageClient (Socket socketChannel) {
			_channel = socketChannel;
			_id = UUID.randomUUID ();
			_length = 0;
		}
		
		public UUID id() {
			return _id;
		}
		
		public Socket channel () {
			return _channel;
		}

		public synchronized void append (byte[] data, int start, int length) {
			_buffer.position (_length);
			_length += length;
			_buffer.put (data, start, length);
		}
		
		public synchronized byte[] message () {
			if(_length < 2)
				return null;
			
			_buffer.position(0);
			short length = _buffer.getShort ();
			if(_length >= length + 2) {
				byte[] msg = new byte[length];
				_buffer.get(msg, 0, length);
				_buffer.position(length + 2);
				_buffer.compact();
				_length -= length + 2;
				return msg;
			}
			return null;
		}
	}
}
