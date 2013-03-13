package com.example.test_context;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

//Lap:bare brukt til å teste kommunikasjon mellom activity
public class Yay extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.yay_layout);
	}
	
	public void backToMain(View view){
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);	
	}
	
}
