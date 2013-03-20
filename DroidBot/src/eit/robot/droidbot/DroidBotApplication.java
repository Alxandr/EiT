package eit.robot.droidbot;

import android.app.Application;
import android.os.StrictMode;

public final class DroidBotApplication extends Application {
	public DroidBotApplication() {
		super();
		
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
	        .detectDiskReads()
	        .detectDiskWrites()
	        .detectNetwork()
	        .penaltyLog()
	        .build());
		
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
	        .detectLeakedSqlLiteObjects()
	        .penaltyLog()
	        .build());
	}
}
