package eit.robot.droidbot;

import java.io.IOException;
import java.util.List;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;

/*package*/ final class CameraStreamer {
	private static final String TAG = CameraStreamer.class.getSimpleName();

    private static final int MESSAGE_TRY_START_STREAMING = 0;
    private static final int MESSAGE_SEND_PREVIEW_FRAME = 1;

    private static final long OPEN_CAMERA_POLL_INTERVAL_MS = 500L;
    
    private final Object _lock = new Object();
    private final MovingAverage _averageSpf = new MovingAverage(50);
    
    
    private final boolean _useFlashLight;
    private final int _port;
    private final int _quality;
    private final SurfaceHolder _previewDisplay;
    
    private boolean _running = false;
    private Looper _looper = null;
    private Handler _workHandler = null;
    private Camera _camera = null;
    private int _previewFormat = Integer.MIN_VALUE;
    private int _previewWidth = Integer.MIN_VALUE;
    private int _previewHeight = Integer.MIN_VALUE;
    private Rect _previewRect = null;
    private int _previewBufferSize = Integer.MIN_VALUE;
    private MemoryOutputStream _outputStream = null;
    private MJpegHttpStreamer _streamer = null;
    
    private long _numFrames = 0L;
    private long _lastTimestamp = Long.MIN_VALUE;
    
    /* package */ CameraStreamer(final boolean useFlashLight, final int port, final int quality,
    		final SurfaceHolder previewDisplay) {
    	super();
    	
    	if(previewDisplay == null) {
    		throw new IllegalArgumentException("previewDisplay must not be null");
    	}
    	
    	_useFlashLight = useFlashLight;
    	_port = port;
    	_quality = quality;
    	_previewDisplay = previewDisplay;
    }
    
    private final class WorkHandler extends Handler {
    	private WorkHandler(final Looper looper) {
            super(looper);
        } // constructor(Looper)
    	
    	@Override
    	public void handleMessage(final Message message) {
    		switch(message.what) {
    		case MESSAGE_TRY_START_STREAMING:
                tryStartStreaming();
                break;
            case MESSAGE_SEND_PREVIEW_FRAME:
                final Object[] args = (Object[]) message.obj;
                sendPreviewFrame((byte[]) args[0], (Camera) args[1], (Long) args[2]);
                break;
            default:
                throw new IllegalArgumentException("cannot handle message");
    		}
    	}
    }
    
    /* package */ void start() {
    	synchronized(_lock) {
    		if(_running) {
    			throw new IllegalStateException("CameraStreamer is already running");
    		}
    		
    		_running = true;
    	}
    	
    	final HandlerThread worker = new HandlerThread(TAG, Process.THREAD_PRIORITY_MORE_FAVORABLE);
        worker.setDaemon(true);
        worker.start();
        _looper = worker.getLooper();
        _workHandler = new WorkHandler(_looper);
        _workHandler.obtainMessage(MESSAGE_TRY_START_STREAMING).sendToTarget();
    }
    
    /* package */ void stop() {
    	synchronized(_lock) {
    		_running = false;
    		
    		if(_streamer != null) {
    			_streamer.stop();
    		}
    		if(_camera != null) {
    			_camera.release();
    			_camera = null;
    		}
    	} // synchronized
    	_looper.quit();
    }
    
    private void tryStartStreaming() {
    	try {
    		while(true) {
    			try {
    				startStreamingIfRunning();
    			} catch(final RuntimeException openCameraFailed) {
    				Log.d(TAG, "Open camera failed, retying in " + OPEN_CAMERA_POLL_INTERVAL_MS
                            + "ms", openCameraFailed);
    				Thread.sleep(OPEN_CAMERA_POLL_INTERVAL_MS);
    				continue;
    			}
    			break;
    		}
    	} catch(final Exception startPreviewFailed) {
    		// Captures the IOException from startStreamingIfRunning and
            // the InterruptException from Thread.sleep.
            Log.w(TAG, "Failed to start camera preview", startPreviewFailed);
    	}
    }
    
    private void startStreamingIfRunning() throws IOException {
    	// Throws RuntimeException if the camera is currently opened
        // by another application.
        final Camera camera = Camera.open();
        final Camera.Parameters params = camera.getParameters();
        
        if(_useFlashLight) {
        	params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        }
        List<Camera.Size> sizes = params.getSupportedPictureSizes();
        Camera.Size min = null;
        for(Size s : sizes) {
        	if(min == null)
        		min = s;
        	if(s.height * s.height < min.width * min.height)
        		min = s;
        }
        params.setPreviewSize(min.width, min.height);
    //    params.setPreviewSize(480,320);
        List <int[]> test = params.getSupportedPreviewFpsRange();
        final int[] range = test.get(test.size() - 1);
   
  
        params.setPreviewFpsRange(range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
               range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
        camera.setParameters(params);
        
        _previewFormat = params.getPreviewFormat();
        final Camera.Size previewSize = params.getPreviewSize();
        _previewWidth = previewSize.width;
        _previewHeight = previewSize.height;
        final int BITS_PER_BYTE = 8;
        final int bytesPerPixel = ImageFormat.getBitsPerPixel(_previewFormat) / BITS_PER_BYTE;
        // XXX: According to the documentation the buffer size can be
        // calculated by width * height * bytesPerPixel. However, this
        // returned an error saying it was too small. It always needed
        // to be exactly 1.5 times larger.
        _previewBufferSize = _previewWidth * _previewHeight * bytesPerPixel * 3 / 2 + 1;
        camera.addCallbackBuffer(new byte[_previewBufferSize]);
        _previewRect = new Rect(0, 0, _previewWidth, _previewHeight);
        camera.setPreviewCallbackWithBuffer(_previewCallback);
        
        // We assumed that the compressed image will be no bigger than
        // the uncompressed image.
        _outputStream = new MemoryOutputStream(_previewBufferSize);
        
        final MJpegHttpStreamer streamer = new MJpegHttpStreamer(_port, _previewBufferSize);
        streamer.start();
        
        synchronized(_lock) {
        	if(!_running) {
        		streamer.stop();
        		camera.release();
        		return;
        	}
        	
        	try {
        		camera.setPreviewDisplay(_previewDisplay);
        	} catch (final IOException e) {
                streamer.stop();
                camera.release();
                throw e;
            } // catch
        	
        	_streamer = streamer;
        	camera.startPreview();
        	_camera = camera;
        }
    }
    
    private final Camera.PreviewCallback _previewCallback = new Camera.PreviewCallback() {
		
		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
			// TODO Auto-generated method stub
			final Long timestamp = SystemClock.elapsedRealtime();
            final Message message = _workHandler.obtainMessage();
            message.what = MESSAGE_SEND_PREVIEW_FRAME;
            message.obj = new Object[]{ data, camera, timestamp };
            message.sendToTarget();
		}
	};
	
	private void sendPreviewFrame(final byte[] data, final Camera camera, final long timestamp) {
		// Calcalute the timestamp
        final long MILLI_PER_SECOND = 1000L;
        final long timestampSeconds = timestamp / MILLI_PER_SECOND;
        
        // Update and log the frame rate
        final long LOGS_PER_FRAME = 10L;
        _numFrames++;
        if (_lastTimestamp != Long.MIN_VALUE)
        {
            _averageSpf.update(timestampSeconds - _lastTimestamp);
            if (_numFrames % LOGS_PER_FRAME == LOGS_PER_FRAME - 1)
            {
                Log.d(TAG, "FPS: " + 1.0 / _averageSpf.getAverage());
            } // if
        } // else
        
        _lastTimestamp = timestampSeconds;
        
        // Create JPEG
        final YuvImage image = new YuvImage(data, _previewFormat, _previewWidth, _previewHeight, null);
        image.compressToJpeg(_previewRect, _quality, _outputStream);
        _streamer.streamJpeg(_outputStream.getBuffer(), _outputStream.getLength(), timestamp);
        
        // Clean up
        _outputStream.seek(0);
        // XXX: I believe that this is thread-safe because we're not
        // calling methods in other threads. I might be wrong, the
        // documentation is not clear.
        camera.addCallbackBuffer(data);
	}
}
