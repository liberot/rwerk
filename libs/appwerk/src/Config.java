package de.appwerk.radioapp.lib;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;
import android.util.Log;

public class Config extends Activity 
{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		SharedPreferences spf = PreferenceManager.getDefaultSharedPreferences(this);
		int counter = spf.getInt("counter", 0);
		Log.d(Config.class.getName(), "App started: "+counter);
		SharedPreferences.Editor editor = spf.edit();
		editor.putInt("counter", ++counter);
		editor.commit();
	}
}
