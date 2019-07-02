package de.appwerk.radioapp.lib;

import android.os.AsyncTask;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Binder;
import android.util.Log;
import java.io.IOException;

import com.spoledge.aacdecoder.AACPlayer;
import com.spoledge.aacdecoder.MultiPlayer;
import com.spoledge.aacdecoder.PlayerCallback;
import android.media.AudioTrack;

public class AACPlayerService extends Service
{

	static public final String PLAYER_STATE_CHANGED = "AACPlayer.PLAYER_STATE_CHANGED";
	
	protected final IBinder mBinder = new LocalBinder();

	protected String streamURL;	
	protected AACPlayer aacPlayer;
	protected MultiPlayer multiPlayer;
	protected PlayerCallback cb;

	public int onStartCommand(Intent intent, int flags, int startId)
	{

		this.streamURL = intent.getExtras().getString("streamURL");
		
		this.cb = new PlayerCallback()
		{
			public void playerStarted()
			{
				Log.d("PlayerCallback", "playerStarted()");
			}

			public void playerPCMFeedBuffer(boolean bb, int ia, int ib)
			{
				Log.d("PlayerCallback", "playerPCMFeedBuffer()");
			}
	
			public void playerStopped()
			{
				Log.d("PlayerCallback", "playerStopped()");
			}

			public void playerException(Throwable t)
			{
				Log.d("PlayerCallback", "playerException()");
			}

			public void playerMetadata(String key, String value)
			{
				Log.d("PlayerCallback", "playerMetadata()");
			}

			public void playerAudioTrackCreated(AudioTrack at)
			{
			}

			public void playerStopped(int p)
			{
			}
		};

		Log.d("AACPlayerService::MultiPlayer", "playAsync():" +this.streamURL);

		this.multiPlayer = new MultiPlayer(this.cb);
		this.multiPlayer.playAsync(this.streamURL);
		
		return 1;	
	}

	@Override public boolean onUnbind(Intent intent)
	{
		Log.d("AACPlayerService:", "onUnbind");
		return super.onUnbind(intent);
	}
	
	@Override public IBinder onBind(Intent intent)
	{
		Log.d("AACPlayerService:", "onBind");
		return this.mBinder; 
	}

	@Override public void onDestroy()
	{
		Log.d("MediaPlayerService:", "onDestroy");
		this.multiPlayer.stop();
	}
	
	public class LocalBinder extends Binder
	{
		AACPlayerService getService(){
			return AACPlayerService.this;
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
				i.setAction(AACPlayerService.PLAYER_STATE_CHANGED);
				i.putExtra("state", bm);
				sendBroadcast(i);
			}
		};
		
		t.start();
	}
}
