package eit.robot.droidbot;

import java.io.IOException;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.WindowManager;
import android.widget.TextView;

public class MainActivity extends Activity implements BotHandler {
	private TextView _txt;
	
	
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
    	final String formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
    	        (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    	setText(formatedIpAddress + "v2");
    	try {
    		// TODO: Init server
    	} catch(/*IO*/Exception e) {
    		e.printStackTrace();
    	}
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	
    	// TODO: Stop server
    }

	@Override
	public void setText(String text) {
		// TODO Auto-generated method stub
		_txt.setText(text);
	}
    
}
