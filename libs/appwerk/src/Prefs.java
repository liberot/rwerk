package de.appwerk.radioapp.lib;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;
import android.content.Context;
import android.content.Intent;

public class Prefs
{

	protected String config;
	protected SharedPreferences.Editor editor;
	protected SharedPreferences spf;
	protected Context context;
	protected JSONObject json;
	
	static public final String SETTINGS_LOADED = "Prefs.SETTINGS_LOADED";

	public Prefs(Context context)
	{
		
		this.context = context;
		this.readPrefs();
	}
	
	protected JSONObject readPrefs()
	{
	
		this.spf = context.getSharedPreferences("pref.json", Context.MODE_PRIVATE);
		this.config = this.spf.getString("config", this.getDefaultConfig());
		
		this.json = new JSONObject();

		try{
			
			this.json = new JSONObject(this.config);
		}
		catch(JSONException e){

			e.printStackTrace();
		}
		
		Log.d(Prefs.class.getName(), "Starting with configuration: " +this.config);

		return this.json;
	}

	public void readPreferences()
	{
		if(null == this.json){

			this.json = this.readPrefs();	
		}

		String json = "";
		
		try{

			json = this.json.toString(8);
		}
		catch(JSONException e){
		
			e.printStackTrace();
		}
			
		this.broadcast(json);
	}

	protected String getDefaultConfig()
	{

		return "{'autostart':'false','wlan':'false','mobile':'false'}";
	}

	protected void updateConfig(String key, String value)
	{

		this.editor = this.spf.edit();
		this.editor.putString("config", this.json.toString());
		this.editor.commit();
	}

	protected String m;
	protected void broadcast(String prefs)
	{
		this.m = prefs;
		
		Thread t = new Thread()
		{
			@Override public void run()
			{
				Intent i = new Intent();
				i.setAction(Prefs.SETTINGS_LOADED);
				i.putExtra("prefs", m);
				context.sendBroadcast(i);
			}
		};
		
		t.start();
	}
}	
