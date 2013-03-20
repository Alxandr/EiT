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
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
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

public class MainActivity extends Activity implements BotHandler, IRobot, SurfaceHolder.Callback {
	private static final String TAG = MainActivity.class.getSimpleName();

    private static final String PREF_FLASH_LIGHT = "flash_light";
    private static final boolean PREF_FLASH_LIGHT_DEF = false;
    private static final String PREF_PORT = "port";
    private static final int PREF_PORT_DEF = 8080;
    private static final String PREF_JPEG_QUALITY = "jpeg_quality";
    private static final int PREF_JPEG_QUALITY_DEF = 40;
    
    private boolean _running = false;
    private boolean _previewDisplayCreated = false;
    private SurfaceHolder _previewDisplay = null;
    private CameraStreamer _cameraStreamer = null;
    
    private String _ipAddress = "";
    private boolean _useFlashLight = PREF_FLASH_LIGHT_DEF;
    private int _port = PREF_PORT_DEF;
    private int _quality = PREF_JPEG_QUALITY_DEF;
    
	private TextView _txt;
	private TextView _txt2;
	private SurfaceView _surface;
	
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
    	_previewDisplay = _surface.getHolder();
    	_previewDisplay.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    	_previewDisplay.addCallback(this);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	_running = true;
    	
    	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	
    	WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
    	int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
    	
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
		
		tryStartCameraStreamer();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	
    	_running = false;
    	ensureCameraStreamerStopped();
    }
    
    @Override
    public void surfaceChanged(final SurfaceHolder holder, final int format, final int width,
            final int height)
    {
        // Ingored
    } // surfaceChanged(SurfaceHolder, int, int, int)
    
    @Override
    public void surfaceCreated(final SurfaceHolder holder)
    {
        _previewDisplayCreated = true;
        tryStartCameraStreamer();
    } // surfaceCreated(SurfaceHolder)
    
    @Override
    public void surfaceDestroyed(final SurfaceHolder holder)
    {
        _previewDisplayCreated = false;
        ensureCameraStreamerStopped();
    } // surfaceDestroyed(SurfaceHolder)
    
    private void tryStartCameraStreamer()
    {
        if (_running && _previewDisplayCreated)
        {
            _cameraStreamer = new CameraStreamer(_useFlashLight && hasFlashLight(), _port, _quality,
                    _previewDisplay);
            _cameraStreamer.start();
        } // if
    } // tryStartCameraStreamer()
    
    private void ensureCameraStreamerStopped()
    {
        if (_cameraStreamer != null)
        {
            _cameraStreamer.stop();
            _cameraStreamer = null;
        } // if
    } // stopCameraStreamer()
    
    private boolean hasFlashLight()
    {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    } // hasFlashLight()

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

	@Override
	public void setText(final String text) {
		// TODO Auto-generated method stub
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				_txt2.setText(text);
			}
		});
	}
    
}
