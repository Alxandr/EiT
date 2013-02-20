package me.alxandr.Transport;

import java.io.IOException;
import java.net.InetAddress;

import me.alxandr.Instant.Client.MessageClient;

public class RobotClient extends MessageClient {

	public RobotClient(InetAddress hostAddress) throws IOException {
		super(hostAddress, 10007);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void processMessage(byte[] msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void connected() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void disconnected() {
		// TODO Auto-generated method stub
		
	}
	
	public void setEngines(float x, float y) throws IOException {
		Robot.EngineSpeed es = Robot.EngineSpeed.newBuilder()
			.setX(x)
			.setY(y)
			.build();
		
		send(es.toByteArray());
	}

}
