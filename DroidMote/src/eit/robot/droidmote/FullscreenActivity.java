package eit.robot.droidmote;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import eit.robot.joystick.JoystickMovedListener;
import eit.robot.joystick.JoystickView;
import eit.robot.mjpeg.MjpegInputStream;
import eit.robot.mjpeg.MjpegView;

import me.alxandr.Transport.RobotClient;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class FullscreenActivity extends Activity {
	private RobotClient _client;
	private Button _btnUp, _btnLeft, _btnDown, _btnRight;
	private float x, y;
	private JoystickView joystick;
	private TextView txtX, txtY;
	
	private MjpegView mv;
	private String URL = "http://yayayayayayyayy.no";
	JoystickFragment joystickFragment;

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
		RelativeLayout main = (RelativeLayout) findViewById(R.id.main_layout);
        mv = new MjpegView(this);
        main.addView(mv,0);
		txtX = (TextView)findViewById(R.id.TextViewX);
        txtY = (TextView)findViewById(R.id.TextViewY);
        joystick = (JoystickView)findViewById(R.id.joystickView);
        
        joystick.setOnJostickMovedListener(_listener);
      //  setContentView(mv);

 //        addContentView(joystick, null);
        
//        final MjpegInputStream stream = MjpegInputStream.read(ip);
//		mv.setSource(stream);
//		mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
//        mv.showFps(true);
        
        Thread cam = new Thread() {
        	@Override
        	public void run() {
        		final MjpegInputStream stream = MjpegInputStream.read(ip);
        		runOnUiThread(new Runnable() {
        			@Override
        			public void run() 
        			{
        				Log.d("DroidMote", "Nettverk done!");
        				mv.setSource(stream);
        				mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
        		        mv.showFps(true);
        			}
        		});
        	}
        };
        cam.start();
        
//        FragmentManager fragmentManager = getFragmentManager();
//        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
//        
//        joystickFragment = new JoystickFragment();
//    	fragmentTransaction.add(R.layout.joystick, joystickFragment);
//    	fragmentTransaction.commit();

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
	
    private JoystickMovedListener _listener = new JoystickMovedListener() {

		@Override
		public void OnMoved(int pan, int tilt) {
			tilt = tilt *-1; // y is reversed, this unreverse it
			txtX.setText(Integer.toString(pan));
			txtY.setText(Integer.toString(tilt));
			x = pan;
			y = tilt;
			updateValues();
		}

		@Override
		public void OnReleased() {
			txtX.setText("stopped");
			txtY.setText("stopped");
			x = 0;
			y = 0;
			updateValues();
		}
	}; 
}
