package de.appwerk.radioapp.lib;

import android.os.AsyncTask;
import android.app.Service;
import android.content.Intent;
import java.io.IOException;
import android.os.IBinder;
import android.os.Binder;
import android.util.Log;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaMetadataRetriever;

public class MediaPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener
{
	static public final String PLAYER_STATE_CHANGED = "MediaPlayerService.PLAYER_STATE_CHANGED";

	protected MediaPlayer mp;
	protected String streamURL;
	protected final IBinder mBinder = new LocalBinder();

	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if(null == intent.getExtras()){
			return 0;
		}
	
		try{	
			this.streamURL = intent.getExtras().getString("streamURL");
		}
		catch(Exception e){
			return 0;
		}	

		if(null == this.streamURL){
			return 0;
		}

		if("" == this.streamURL){
			return 0;
		}	
	
		this.mp = new MediaPlayer();
		this.mp.reset();
		this.mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
		// this.mp.setAudioStreamType(AudioManager.STREAM_ALARM);
		this.mp.setLooping(true);
		this.mp.setOnErrorListener(this);
		this.mp.setOnBufferingUpdateListener(this);
		this.mp.setOnPreparedListener(this);
		
		try{
			this.mp.setDataSource(this.streamURL);
			this.mp.prepareAsync();
		}
		catch(IOException e){
			e.printStackTrace();
		}
	
		return 1;	
	}

	@Override public void onPrepared(MediaPlayer mp)
	{
		Log.d("MediaPlayerService:", "onPrepared: ");
		this.broadcast("prepared");
		this.mp.start();	
	}

	@Override public void onBufferingUpdate(MediaPlayer mp, int percent)
	{
		Log.d("MediaPlayerService:", "onBufferingUpdate: " +percent);
		this.broadcast("bufferingUpdate");
	}

	public boolean onError(MediaPlayer mp, int idx, int extra)
	{
		Log.d("MediaPlayerService:", "onError: " +idx);
		this.broadcast("error");
		return true;
	}
	
	@Override public boolean onUnbind(Intent intent)
	{
		Log.d("MediaPlayerService:", "onUnbind: ");
		if(null != this.mp){
			if(this.mp.isPlaying()){
				this.mp.stop();
			}
			this.mp.reset();
			this.mp.release();
			this.mp = null;
		}
		return super.onUnbind(intent);
	}
	
	@Override public IBinder onBind(Intent intent)
	{
		Log.d("MediaPlayerService:", "onBind: ");
		return this.mBinder; 
	}

	@Override public void onDestroy()
	{
		Log.d("MediaPlayerService:", "onDestroy: ");
		this.mp.stop();
		this.mp.release();
	}
	
	public class LocalBinder extends Binder
	{
		MediaPlayerService getService(){
			return MediaPlayerService.this;
		}
	}

	protected String bm;
	public void broadcast(String m)
	{
		this.bm = m;
		Thread t = new Thread()
		{
			@Override
			public void run()
			{
				Intent i = new Intent();
				i.setAction(MediaPlayerService.PLAYER_STATE_CHANGED);
				i.putExtra("state", bm);
				sendBroadcast(i);
			}
		};
		t.start();
	}
}
