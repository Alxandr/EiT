package eit.robot.droidbot;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;

import android.os.Handler;

public class BotServer extends NanoHTTPD{
	private Handler _handler;
	
	
	public BotServer() throws IOException {
		super(10007, null);
		
		_handler = new Handler();
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public Response serve(
			String uri,
			String method,
			Properties headers,
			Properties params,
			Properties files) {
		final StringBuilder buf = new StringBuilder();
	      for (Entry<Object, Object> kv : headers.entrySet())
	        buf.append(kv.getKey() + " : " + kv.getValue() + "\n");
	      _handler.post(new Runnable() {
	        @Override
	        public void run() {
	          //hello.setText(buf);
	        }
	      });
	 
	      final String html = "<html><head><head><body><h1>Hello, World</h1></body></html>";
	      return new NanoHTTPD.Response(HTTP_OK, MIME_HTML, html);
	}
	 
}
