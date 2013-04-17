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
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.util.Log;
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
import android.widget.Toast;

public class MainActivity extends Activity implements BotHandler, IRobot, SurfaceHolder.Callback, BTConnectable {
	private static final String TAG = MainActivity.class.getSimpleName();

    private static final String PREF_FLASH_LIGHT = "flash_light";
    private static final boolean PREF_FLASH_LIGHT_DEF = true;
    private static final String PREF_PORT = "port";
    private static final int PREF_PORT_DEF = 8080;
    private static final String PREF_JPEG_QUALITY = "jpeg_quality";
    private static final int PREF_JPEG_QUALITY_DEF = 40;
    
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    
    private boolean _running = false;
    private boolean _previewDisplayCreated = false;
    private SurfaceHolder _previewDisplay = null;
    private CameraStreamer _cameraStreamer = null;
    private RobotServer _server;
    
    private String _ipAddress = "";
    private boolean _useFlashLight = PREF_FLASH_LIGHT_DEF;
    private int _port = PREF_PORT_DEF;
    private int _quality = PREF_JPEG_QUALITY_DEF;
    
	private TextView _txt;
	private TextView _txt2;
	private SurfaceView _surface;
	
	private BTCommunicator _btComm;
	private Handler _btcHandler;
	private boolean _findingNxt = false;
	private boolean _pairing;

	private ProgressDialog _proggresModal;
	private int _position;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_main);
        
        _txt = (TextView)findViewById(R.id.bolle);
    	_txt2 = (TextView)findViewById(R.id.bug);
    	_surface = (SurfaceView)findViewById(R.id.mediaController1);
    	_previewDisplay = _surface.getHolder();
    	_previewDisplay.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    	_previewDisplay.addCallback(this);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch(requestCode) {
    	case REQUEST_CONNECT_DEVICE:
    		// When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address and start a new bt communicator thread
            	_pairing = data.getExtras().getBoolean(DeviceListActivity.PAIRING);
                String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                startBTCommunicator(address);
            }
            //_findingNxt = false;
            break;
    	}
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
					_server = server;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		t.setDaemon(true);
		if(_btComm != null)
			t.start();
		
		try {
			tryStartCameraStreamer();
			tryConnectToMindstorm();
		} catch(Exception e)
		{
			_running = false;
	    	if(_server != null) {
	    		try {
					_server.close();
				} catch (InterruptedException e2) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    		_server = null;
	    	}
	    	ensureCameraStreamerStopped();
	    	finish();
		}
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	
    	_running = false;
    	if(_server != null) {
    		try {
				_server.close();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		_server = null;
    	}
    	ensureCameraStreamerStopped();
    	destroyBTCommunicator();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	
    	_running = false;
    	if(_server != null) {
    		try {
				_server.close();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		_server = null;
    	}
    	ensureCameraStreamerStopped();
    	destroyBTCommunicator();
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
    
    private void tryConnectToMindstorm()
    {
    	if(_btComm != null) return;
    	
    	if(BluetoothAdapter.getDefaultAdapter() == null) {
    		Toast.makeText(this, "No bluetooth", Toast.LENGTH_LONG).show();
    		destroyBTCommunicator();
    		throw new IllegalStateException();
    	}
    	
    	if(!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
    		Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    		startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    	} else {
    		selectNXT();
    	}
    } // tryConnectToMindstorm()
    
    private void startBTCommunicator(String address) {
    	_proggresModal = ProgressDialog.show(this, "", getResources().getString(R.string.connecting_please_wait), true);

        if (_btComm != null) {
            try {
            	_btComm.destroyNXTconnection();
            }
            catch (IOException e) { }
        }
        _btComm = new BTCommunicator(this, _mindstormHandler, BluetoothAdapter.getDefaultAdapter(), getResources());
        _btcHandler = _btComm.getHandler();
        _btComm.setMACAddress(address);
        _btComm.start();
	}
    
    private synchronized void destroyBTCommunicator() {
		// TODO Auto-generated method stub
		if(_btComm != null) {
			sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.DISCONNECT, 0, 0);
			_btComm = null;
		}
	}

	private void sendBTCmessage(int delay, int message, int value1, int value2) {
		Bundle myBundle = new Bundle();
        myBundle.putInt("message", message);
        myBundle.putInt("value1", value1);
        myBundle.putInt("value2", value2);
        Message myMessage = _mindstormHandler.obtainMessage();
        myMessage.setData(myBundle);
        if (delay == 0)
            _btcHandler.sendMessage(myMessage);

        else
            _btcHandler.sendMessageDelayed(myMessage, delay);
	}

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

    
    int sentB = 0, sentC = 0;
    int wantB = 0, wantC = 0;
	@Override
	public void setEngineSpeed(final float x, final float y) {
		// TODO Auto-generated method stub
		// math
		Log.d("engine", "x: " + x + ", y: " + y);
		synchronized(this) {
			if(y == 0) {
				wantB = wantC = 0;
			} else {
				int base = (int)(y * 20);
				int diff = (int)((-x / 5) * (Math.abs(base)/2));
				if(diff > 0) {
					wantB = base;
					wantC = (Math.abs(base) / base) * (Math.abs(base) - Math.abs(diff));
				} else if(diff < 0) {
					wantB = (Math.abs(base) / base) * (Math.abs(base) - Math.abs(diff));
					wantC = base;
				} else {
					wantB = base;
					wantC = base;
				}
			}
			
			if(wantB != sentB || wantC != sentC) {
				if(wantB != sentB) {
					sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.MOTOR_B, wantB, 0);
					sentB = wantB;
				}
				
				if(wantC != sentC) {
					sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.MOTOR_C, wantC, 0);
					sentC = wantC;
				}
			}
		}
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
    
	synchronized void selectNXT() {
		if(_findingNxt) return;
		_findingNxt = true;
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }
	
	final Handler _mindstormHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.getData().getInt("message")) {
			case BTCommunicator.DISPLAY_TOAST:
				Toast.makeText(MainActivity.this, msg.getData().getString("toastText"), Toast.LENGTH_SHORT).show();
				break;
				
			case BTCommunicator.STATE_CONNECTED:
				_proggresModal.dismiss();
				break;
				
			case BTCommunicator.MOTOR_STATE:
				if(_btComm != null) {
					byte[] motorMessage = _btComm.getReturnMessage();
                    _position = byteToInt(motorMessage[21]) + (byteToInt(motorMessage[22]) << 8) + (byteToInt(motorMessage[23]) << 16)
                                   + (byteToInt(motorMessage[24]) << 24);
				}
				break;
				
			case BTCommunicator.STATE_CONNECTERROR_PAIRING:
			case BTCommunicator.STATE_CONNECTERROR:
				_proggresModal.dismiss();
				Toast.makeText(MainActivity.this, "Error connecting", Toast.LENGTH_LONG).show();
				new Thread() {
					@Override
					public void run() {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								finish();
							}
						});
					}
				}.start();
				break;
			}
		}
	};
	
	private int byteToInt(byte byteValue) {
        int intValue = (byteValue & (byte) 0x7f);

        if ((byteValue & (byte) 0x80) != 0)
            intValue |= 0x80;

        return intValue;
    }

	@Override
	public boolean isPairing() {
		// TODO Auto-generated method stub
		return _pairing;
	}
	
}
