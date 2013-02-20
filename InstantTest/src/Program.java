import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.UUID;

import me.alxandr.Instant.Client.MessageClient;
import me.alxandr.Instant.Server.MessageServer;
import me.alxandr.Transport.RobotClient;
import me.alxandr.Transport.RobotServer;


public class Program {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws UnknownHostException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
		RobotServer s = new RobotServer();
		s.open();
		
		RobotClient c = new RobotClient(InetAddress.getByName("127.0.0.1"));
		c.connect();
		
		c.setEngines(0.2f, 1f);
		
		Thread.sleep(100000);
	}

}
