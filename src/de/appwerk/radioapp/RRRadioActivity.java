package de.appwerk.radioapp;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;

import de.appwerk.radioapp.lib.AppwerkService;
import de.appwerk.radioapp.lib.DacDetectService;
import de.appwerk.radioapp.lib.MediaPlayerService;
import de.appwerk.radioapp.lib.AACPlayerService;
import de.appwerk.radioapp.lib.MicService;
import de.appwerk.radioapp.lib.ViewBuilder;
import de.appwerk.radioapp.lib.Prefs;
import de.appwerk.radioapp.lib.IcyStreamMetaService;
import de.appwerk.radioapp.lib.PushReceiver;

import java.lang.String;

import android.widget.TextView;
import android.widget.Button;
import android.widget.LinearLayout;

import android.util.Log;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.net.Uri;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentActivity;
import android.app.ActionBar;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;

import java.net.URL;
import java.net.MalformedURLException;

import android.provider.MediaStore;
import android.provider.MediaStore.Files.FileColumns;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.*;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.ByteArrayOutputStream;
import android.content.res.Configuration;

import com.parse.*;

public class RRRadioActivity extends FragmentActivity
{
	static protected final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 9999;

	protected Intent dacDetectService;
	protected Intent mediaPlayerService;
	protected Intent micService;
	protected Intent aacPlayerService;
	protected IcyStreamMetaService isms;

	protected Menu menu;
	protected IntentFilter intentFilter;

	protected BroadcastReceiver webServiceReceiver;
	protected BroadcastReceiver viewReceiver;
	protected BroadcastReceiver mplayerReceiver;
	protected BroadcastReceiver aacPlayerReceiver;
	protected BroadcastReceiver micServiceReceiver;
	protected BroadcastReceiver pushReceiver;

	protected SharedPreferences.Editor editor;
	protected SharedPreferences spf;
	protected String config;

	protected ViewBuilder viewBuilder;
	protected AppwerkService appwerkService;
	
	protected Prefs prefs;

	protected String streamURL; 

	@Override public void onCreate(Bundle savedInstanceState)
	{
		String parseClientKey = this.getString(R.string.parse_client_key);
		String parseApplicationId = this.getString(R.string.parse_application_id);
		Parse.initialize(this.getApplicationContext(), parseApplicationId, parseClientKey);

		// GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(Plus.API).build();
		// Log.d(AppwerkService.class.getName(), "" +GooglePlayServicesUtil.isGooglePlayServicesAvailable(this));
		super.onCreate(savedInstanceState);
		
		this.prefs = new Prefs(this.getApplicationContext());	
		this.initViews();
		this.setupReceiver();
		this.setupServices();
	}

	protected void initViews()
	{
		this.viewBuilder = new ViewBuilder(
			(Activity)this, 
			(FragmentManager)getSupportFragmentManager()
		);
	}

	protected void setupReceiver()
	{
		this.intentFilter = new IntentFilter();
		this.intentFilter.addAction(PushReceiver.PUSH_RECEIVED);
		this.pushReceiver = new BroadcastReceiver(){
			@Override public void onReceive(Context context, Intent i){
				String res = i.getStringExtra("push");
			}
		};
		this.registerReceiver(this.pushReceiver, this.intentFilter);

		this.intentFilter = new IntentFilter();
		this.intentFilter.addAction(AppwerkService.SERVICE_COMPLETE);
		this.webServiceReceiver = new BroadcastReceiver(){
			@Override public void onReceive(Context context, Intent i){
				String res = i.getStringExtra("res");
			}
		};
		this.registerReceiver(this.webServiceReceiver, this.intentFilter);
	
		this.intentFilter = new IntentFilter();
		this.intentFilter.addAction(ViewBuilder.ACTION_REQUESTED);
		this.viewReceiver = new BroadcastReceiver(){
			@Override public void onReceive(Context context, Intent i){
				String act = i.getStringExtra("act");
				String arg = i.getStringExtra("arg");
				routeViewActionCall(act, arg);	
			}
		};	
		this.registerReceiver(this.viewReceiver, this.intentFilter);
	
		this.intentFilter = new IntentFilter();
		this.intentFilter.addAction(MediaPlayerService.PLAYER_STATE_CHANGED);
		this.mplayerReceiver = new BroadcastReceiver(){
			@Override public void onReceive(Context context, Intent i){
				String state = i.getStringExtra("state");
				mplayerStateChanged(state);	
			}
		};
		this.registerReceiver(this.mplayerReceiver, this.intentFilter);
	
		this.intentFilter = new IntentFilter();
		this.intentFilter.addAction(AACPlayerService.PLAYER_STATE_CHANGED);
		this.aacPlayerReceiver = new BroadcastReceiver(){
			@Override public void onReceive(Context context, Intent i){
				String state = i.getStringExtra("state");
				aacPlayerStateChanged(state);	
			}
		};
		this.registerReceiver(this.aacPlayerReceiver, this.intentFilter);
	
		this.intentFilter = new IntentFilter();
		this.intentFilter.addAction(MicService.STATE_CHANGED);
		this.micServiceReceiver = new BroadcastReceiver(){
			@Override public void onReceive(Context context, Intent i){
				String state = i.getStringExtra("state");
				micStateChanged(state);	
			}
		};
		this.registerReceiver(this.micServiceReceiver, this.intentFilter);
	}	

	protected void showPushResult(String res)
	{
		Log.d(AppwerkService.class.getName(), "showPushResult():" +res);
	}
	
	protected void micStateChanged(String state)
	{
		Log.d("micStateChanged():", state);
		if(state.equals("stop")){
			this.stopService(this.micService);	
		}
	}
	
	protected void aacPlayerStateChanged(String state)
	{
		Log.d("aacPlayerStateChanged():", state);
	}
	
	protected void mplayerStateChanged(String state)
	{
		if(state.equals("prepared")){
			this.readStreamMetadata();		
		}
	}

	protected void readStreamMetadata()
	{
		try{
			URL url = new URL(this.streamURL);
			this.isms.read(url);
		}
		catch(MalformedURLException e){
			e.printStackTrace();
		}
	}

	protected void takeSnapshot()
	{
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if(null != intent.resolveActivity(this.getPackageManager())){
			startActivityForResult(intent, RRRadioActivity.CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
		}
	}

	@Override protected void onActivityResult(int reqCode, int resCode, Intent data)
	{
		if(RRRadioActivity.CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE == reqCode && RESULT_OK == resCode){
			
			Bundle extras = data.getExtras();
			
			Bitmap bmp = (Bitmap)extras.get("data");
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
			
			byte[] ary = stream.toByteArray();

			String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
			String filename = "snapshot" +"." +stamp +".jpg";
			
			try{
				FileOutputStream oFile = this.openFileOutput(filename, MODE_PRIVATE);
				oFile.write(ary);
				oFile.close();
			}
			catch(IOException e){
				e.printStackTrace();
			}
			
			this.appwerkService.sendSnapshot(filename);
		}	
	}

	protected void routeViewActionCall(String act, String arg)
	{
		
		if(act.equals("startStream")){
			Log.d(RRRadioActivity.class.getName(), "" +act +":" +arg);
			this.stopService(this.mediaPlayerService);
			this.mediaPlayerService.putExtra("streamURL", arg);
			this.startService(this.mediaPlayerService);
			this.streamURL = arg;
		}
		
		else if(act.equals("stopStream")){
			Log.d(RRRadioActivity.class.getName(), "" +act);
			this.stopService(this.mediaPlayerService);
		}

		else if(act.equals("startSitmark")){
			Log.d(RRRadioActivity.class.getName(), "" +act);
			this.stopService(this.dacDetectService);
			this.startService(this.dacDetectService);
		}

		else if(act.equals("stopSitmark")){
			Log.d(RRRadioActivity.class.getName(), "" +act);
			this.stopService(this.dacDetectService);
		}

		else if(act.equals("readMetadata")){
			Log.d(RRRadioActivity.class.getName(), ""+act);
			this.readStreamMetadata();
		}

		else if(act.equals("readPreferences")){
			Log.d(RRRadioActivity.class.getName(), "" +act);
			this.prefs.readPreferences();
		}

		else if(act.equals("sendLikeReq")){
			Log.d(RRRadioActivity.class.getName(), "" +act +":" +arg);
			this.appwerkService.sendLikeReq(arg);
		}

		else if(act.equals("takeSnapshot")){
			Log.d(RRRadioActivity.class.getName(), "" +act);
			this.takeSnapshot();
		}

		else if(act.equals("startAACPlayer")){
			Log.d(RRRadioActivity.class.getName(), "" +act);
			this.stopService(this.aacPlayerService);
			this.aacPlayerService.putExtra("streamURL", "" +act);
			this.startService(this.aacPlayerService);
		}

		else if(act.equals("stopAACPlayer")){
			Log.d(RRRadioActivity.class.getName(), "" +act);
			this.stopService(this.aacPlayerService);
		}

		else if(act.equals("startMic")){
			Log.d(RRRadioActivity.class.getName(), "" +act);
			this.stopService(this.micService);
			this.startService(this.micService);
		}

		else if(act.equals("stopMic")){
			Log.d(RRRadioActivity.class.getName(), "" +act);
			this.stopService(this.micService);
		}

		else if(act.equals("sendRec")){
			Log.d(RRRadioActivity.class.getName(), "" +act);
			this.appwerkService.sendRec(arg);
		}

		else if(act.equals("loadWebLinkReq")){
			Log.d(RRRadioActivity.class.getName(), "" +act);
			this.loadWebLink(arg);
		}

		else if(act.equals("loadRSSFeed")){
			Log.d(RRRadioActivity.class.getName(), "" +act);
			this.loadRSSFeed(arg);
		}
	}

	protected void loadWebLink(String arg)
	{
		Log.d(RRRadioActivity.class.getName(), "" +arg);
		Intent bi = new Intent(Intent.ACTION_VIEW, Uri.parse(arg));
		startActivity(bi);
	}

	protected void loadRSSFeed(String arg)
	{
		Log.d(RRRadioActivity.class.getName(), "" +arg);
		Uri uri = Uri.parse(this.getString(R.string.rss_url) +"?loc=" +arg);
		Intent bi = new Intent(Intent.ACTION_VIEW, uri);
		startActivity(bi);
	}
	
	protected void setupServices()
	{
		this.dacDetectService = new Intent(this, DacDetectService.class);
		this.mediaPlayerService = new Intent(this, MediaPlayerService.class);
		this.aacPlayerService = new Intent(this, AACPlayerService.class);
		this.micService = new Intent(this, MicService.class);
		this.appwerkService = new AppwerkService(this.getApplicationContext());
		this.isms = new IcyStreamMetaService(this.getApplicationContext());
	}

	@Override public void onStop()
	{
		super.onStop();
	}

	@Override public void onDestroy()
	{	
		this.viewBuilder.unregister();
		this.appwerkService.unregister();
		this.isms.unregister();
		
		this.stopService(this.mediaPlayerService);
		this.stopService(this.dacDetectService);
		this.stopService(this.aacPlayerService);
		this.stopService(this.micService);
		
		this.unregisterReceiver(this.pushReceiver);
		this.unregisterReceiver(this.webServiceReceiver);
		this.unregisterReceiver(this.viewReceiver);
		this.unregisterReceiver(this.mplayerReceiver);
		this.unregisterReceiver(this.aacPlayerReceiver);	
		this.unregisterReceiver(this.micServiceReceiver);	
	
		super.onDestroy();
	}

	@Override public void onPause()
	{
		super.onPause();
	}
	
	@Override public void onResume()
	{
		super.onResume();
	}

	@Override public boolean onCreateOptionsMenu(Menu menu)
	{
		this.menu = menu;
		this.getMenuInflater().inflate(R.menu.main, menu);
		
		return super.onCreateOptionsMenu(menu);
	}

	public Menu getMenu()
	{
		return this.menu;
	}

	@Override public boolean onOptionsItemSelected(MenuItem item)
	{
		String title = (String)item.getTitle();
		
		if(title.equals("Quit")){
			this.finish();
		}
		
		return super.onOptionsItemSelected(item);
	}

	@Override public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
			this.viewBuilder.orientationChanged(Configuration.ORIENTATION_LANDSCAPE);
		}
		else if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
			this.viewBuilder.orientationChanged(Configuration.ORIENTATION_PORTRAIT);
		}
	}
}
