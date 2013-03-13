package com.example.test_context;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

//Lap:bare brukt til å teste kommunikasjon mellom activity
public class Mush extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mush_layout);
	}

	public void onClickButton(View view){
		TextView textView = (TextView) findViewById(R.id.mushText);
		
		Intent intent = new Intent(this, MainActivity.class);
		String value = textView.getText().toString();
		intent.putExtra("mush", value);
		startActivity(intent);	
	}
}
