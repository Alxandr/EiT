package com.example.test_context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			 String value = extras.getString("mush");
			 setText(value);
		}
		
    	WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
    	int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
    	final String formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
    	        (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    	setText(formatedIpAddress + "v2");

	}

	public void doContextChange(View view){
		System.out.println("yay");
	//	Intent intent = new Intent(this, Mush.class);
	//	Intent intent = new Intent(this, Yay.class);
		Intent intent = new Intent(this, MINDdroid.class);
		startActivity(intent);
	}
	
	public void setText(String txt){
		TextView text = (TextView) findViewById(R.id.ip);
		text.setText(txt);
	}

//	
//	final Handler myHandler = new Handler(){
//		@Override
//		public void handleMessage(Message myMessage) {
//			String txt = myMessage.getData().getString("yaySays");
//			setText(txt);
//		}
//	};
}
