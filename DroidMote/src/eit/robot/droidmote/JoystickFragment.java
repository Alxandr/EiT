package eit.robot.droidmote;

import eit.robot.joystick.JoystickMovedListener;
import eit.robot.joystick.JoystickView;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class JoystickFragment extends Fragment {

	TextView txtX, txtY;
	JoystickView joystick;
		
//	@Override
//	protected void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		setContentView(R.layout.joystick);
//
//		txtX = (TextView)findViewById(R.id.TextViewX);
//        txtY = (TextView)findViewById(R.id.TextViewY);
//        joystick = (JoystickView)findViewById(R.id.joystickView);
//        
//        joystick.setOnJostickMovedListener(_listener);
//	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
    	 View v = inflater.inflate(R.layout.joystick, container, false);
    	 txtX = (TextView)v.findViewById(R.id.TextViewX);
    	 txtY = (TextView)v.findViewById(R.id.TextViewY);
    	 joystick.setOnJostickMovedListener(_listener);
        return inflater.inflate(R.layout.joystick, container, false);
    }


    private JoystickMovedListener _listener = new JoystickMovedListener() {

		@Override
		public void OnMoved(int pan, int tilt) {
			txtX.setText(Integer.toString(pan));
			txtY.setText(Integer.toString(tilt));
		}

		@Override
		public void OnReleased() {
			txtX.setText("stopped");
			txtY.setText("stopped");
		}
	}; 

}

