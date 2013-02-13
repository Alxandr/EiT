import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.UUID;

import me.alxandr.Instant.Server.MessageServer;


public class Program {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) throws UnknownHostException, IOException {
		// TODO Auto-generated method stub
		MessageServer server = new MessageServer(InetAddress.getByName("0.0.0.0"), 10007) {
			
			@Override
			protected void processMessage(UUID clientId, byte[] message) {
				// TODO Auto-generated method stub
				StringBuilder sb = new StringBuilder();
				sb.append("Client sent message: ");
				sb.append(clientId);
				sb.append(" [");
				for(int i = 0; i < message.length; i++) {
					sb.append(message[i]);
					if(i < message.length - 1)
						sb.append(", ");
				}
				sb.append("]");
				System.out.println(sb.toString());
			}
			
			@Override
			protected void clientDisconnected(UUID clientId) {
				// TODO Auto-generated method stub
				System.out.println("Client disconnected: " + clientId);
			}
			
			@Override
			protected void clientConnected(UUID clientId) {
				// TODO Auto-generated method stub
				System.out.println("Client connected: " + clientId);
			}
		};
		server.start();
		System.out.println("Listening");
		
		
	}

}
