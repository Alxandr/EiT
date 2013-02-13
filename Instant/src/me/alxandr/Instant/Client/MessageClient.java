package me.alxandr.Instant.Client;

import java.io.IOException;
import java.net.InetAddress;

public abstract class MessageClient extends InstantClient {

	public MessageClient(InetAddress hostAddress, int port) throws IOException {
		super(hostAddress, port);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected final void processData(byte[] data) {
		// TODO Auto-generated method stub

	}

}
