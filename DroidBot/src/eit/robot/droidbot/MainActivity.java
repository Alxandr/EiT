package eit.robot.droidbot;

import java.io.IOException;

import me.alxandr.Transport.IRobot;
import me.alxandr.Transport.RobotServer;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.WindowManager;
import android.widget.TextView;

public class MainActivity extends Activity implements BotHandler, IRobot {
	private TextView _txt;
	private TextView _txt2;
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    //BOO
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true; 
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	
    	WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
    	int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
    	_txt = (TextView)findViewById(R.id.bolle);
    	_txt2 = (TextView)findViewById(R.id.bug);
    	final String formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
    	        (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    	setText(formatedIpAddress + "v2");
		// TODO: Init server
    	final IRobot r = this;
    	if(r == null) {
    		System.out.println("this is null???? 1");
    	}
		Thread t = new Thread() {
			@Override
			public void run() {
				RobotServer server;
				try {
					if(r == null) {
			    		System.out.println("this is null???? 1");
			    	}
					server = new RobotServer(r);
					server.open();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		t.setDaemon(true);
		t.start();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	
    	// TODO: Stop server
    }

	@Override
	public void setText(String text) {
		// TODO Auto-generated method stub
		_txt2.setText(text);
	}

	@Override
	public void setEngineSpeed(final float x, final float y) {
		// TODO Auto-generated method stub
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				_txt.setText("x: " + x + ", y: " + y);
			}
		});
	}
    
}
