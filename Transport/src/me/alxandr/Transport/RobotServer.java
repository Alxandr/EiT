package me.alxandr.Transport;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import com.google.protobuf.InvalidProtocolBufferException;

import me.alxandr.Instant.Server.MessageServer;

public class RobotServer extends MessageServer {

	public RobotServer() throws IOException {
		super(InetAddress.getByName("0.0.0.0"), 10007);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void clientConnected(UUID clientId) {
		// TODO Auto-generated method stub
		System.out.println("connected");
	}

	@Override
	protected void clientDisconnected(UUID clientId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void processMessage(UUID clientId, byte[] message) {
		// TODO Auto-generated method stub
		
		try {
			Robot.EngineSpeed es = Robot.EngineSpeed.parseFrom(message);
			
			System.out.println("New x: " + es.getX());
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
