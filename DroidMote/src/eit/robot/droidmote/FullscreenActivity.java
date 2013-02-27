package eit.robot.droidmote;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import me.alxandr.Transport.RobotClient;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class FullscreenActivity extends Activity {
	private RobotClient _client;
	private Button _btnUp, _btnLeft, _btnDown, _btnRight;
	float x, y;
	
	private void doFinish() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				finish();
			}
		});
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		Intent i = getIntent();
		final String ip = i.getExtras().getString("ip");
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					_client = new RobotClient(InetAddress.getByName(ip));
					_client.connect();
					_client.setEngines(0, 0);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					
					doFinish();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					doFinish();
				}
			}
		};
		t.setDaemon(true);
		t.start();
		

		setContentView(R.layout.activity_fullscreen);

		final View contentView = findViewById(R.id.fullscreen_content);
	}
	
	public void addLeft(View v) {
		x -= 0.05f;
		updateValues();
	}
	
	public void addRight(View v) {
		x += 0.05f;
		updateValues();
	}
	
	public void addUpp(View v) {
		y += 0.05f;
		updateValues();
	}
	
	public void addBottom(View v) {
		y -= 0.05f;
		updateValues();
	}
	
	private void updateValues() {
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					_client.setEngines(x, y);
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
	protected void onResume() {
		super.onResume();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
}
