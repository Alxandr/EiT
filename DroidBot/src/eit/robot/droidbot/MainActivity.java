package eit.robot.droidbot;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;

import me.alxandr.Transport.IRobot;
import me.alxandr.Transport.RobotServer;

import android.hardware.Camera;
import android.media.MediaRecorder;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.view.Menu;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.Window;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements BotHandler, IRobot, Callback {
	private TextView _txt;
	private TextView _txt2;
	private MediaRecorder _mediaRecorder;
	private SurfaceView _surface;
	private SurfaceHolder _holder;
	private Camera _camera;
	private boolean _running;
	private boolean _init;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        setContentView(R.layout.activity_main);
        
        
        
        _txt = (TextView)findViewById(R.id.bolle);
    	_txt2 = (TextView)findViewById(R.id.bug);
    	_surface = (SurfaceView)findViewById(R.id.mediaController1);
    	_holder = _surface.getHolder();
    	_holder.addCallback(this);
    	
    	Button b = (Button)findViewById(R.id.button1);
    	b.setOnClickListener(new View.OnClickListener() {
    		@Override
    		public void onClick(View v) {
    			if(!_running) {
    				_mediaRecorder.start();
    				_running = true;
    			} else {
    				_mediaRecorder.stop();
    				_mediaRecorder.reset();
    				try {
    					initRecorder(_holder.getSurface());
    				} catch(IOException e) {
    					e.printStackTrace();
    				}
    				_running = false;
    			}
    		}
    	});
    }
    
    private void initRecorder(Surface surface) throws IOException {
    	if(_camera == null) {
    		_camera = Camera.open();
    		_camera.unlock();
    	}
    	
    	if(_mediaRecorder == null)
    		_mediaRecorder = new MediaRecorder();
    	
    	_mediaRecorder.setPreviewDisplay(surface);
    	_mediaRecorder.setCamera(_camera);
    	
    	_mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
    	_mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
    	
    	File file = new File(Environment.getExternalStorageDirectory() + "/EiT.avi");
    	if(file.exists())
    		file.delete();
    	
    	DatagramSocket socket = new DatagramSocket(10008);
    	socket.setBroadcast(true);
    	
    	_mediaRecorder.setOutputFile(ParcelFileDescriptor.fromDatagramSocket(socket).getFileDescriptor());
    	
    	_mediaRecorder.setMaxDuration(0);
    	_mediaRecorder.setCaptureRate(15);
    	
    	_mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
    	
    	try {
    		_mediaRecorder.prepare();
    	} catch(IllegalStateException e) {
    		e.printStackTrace();
    	}
    	
    	_init = true;
    }
    
    private void shutdown() {
    	if(_mediaRecorder != null) {
	    	_mediaRecorder.reset();
	    	_mediaRecorder.release();
    	}
    	if(_camera != null) {
    		_camera.release();
    	}
    	
    	_mediaRecorder = null;
    	_camera = null;
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
    	
    	
    	
    	
    	_holder = _surface.getHolder();
    	_holder.addCallback(this);
    	
    	
    	
    	final String formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
    	        (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    	setText(formatedIpAddress + "v2");
		// TODO: Init server
    	final IRobot r = this;
		Thread t = new Thread() {
			@Override
			public void run() {
				RobotServer server;
				try {
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
		
		
		
		helloLog("Testing this log thing xD");
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	
    	// TODO: Stop server
    	shutdown();
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
				String str = "x: " + x + ", y: " + y;
				_txt.setText(str);
			}
		});
	}
	
	static {
	    System.loadLibrary("ndk1");
	}
	private native void helloLog(String logThis);

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		// TODO Auto-generated method stub
		shutdown();
	}
	
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    	try {
    		if(!_init) {
    			initRecorder(_holder.getSurface());
    		}
    	} catch(IOException e) {
    		e.printStackTrace();
    	}
    }
    
}
