package me.alxandr.Transport;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import com.google.protobuf.InvalidProtocolBufferException;

import me.alxandr.Instant.Server.MessageServer;

public class RobotServer extends MessageServer {
	private IRobot _robot;
	
	
	public RobotServer(IRobot robot) throws IOException {
		super(InetAddress.getByName("0.0.0.0"), 10009);
		// TODO Auto-generated constructor stub
		_robot = robot;
		if(robot == null) {
			throw new NullPointerException("robot is null");
		}
	}

	@Override
	protected void clientConnected(UUID clientId) {
		// TODO Auto-generated method stub
		System.out.println("connected");
	}

	@Override
	protected void clientDisconnected(UUID clientId) {
		// TODO Auto-generated method stub
		_robot.setEngineSpeed(0, 0);
	}

	@Override
	protected void processMessage(UUID clientId, byte[] message) {
		// TODO Auto-generated method stub
		
		try {
			Robot.EngineSpeed es = Robot.EngineSpeed.parseFrom(message);
			if(es != null) {
				_robot.setEngineSpeed(es.getX(), es.getY());
			}
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
