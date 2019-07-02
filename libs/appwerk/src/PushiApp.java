package de.appwerk.radioapp.lib;

import android.app.Application;
import android.util.Log;
import com.parse.*;

public class PushiApp extends Application
{
	@Override public void onCreate()
	{
		Parse.initialize(
			this.getApplicationContext(), 
			this.getString(R.string.parse_client_key),
		 	this.getString(R.string.parse_application_id)
		);
	}
}
