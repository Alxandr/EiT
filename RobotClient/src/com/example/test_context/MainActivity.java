package com.example.test_context;

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
