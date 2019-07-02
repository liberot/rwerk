package de.appwerk.radioapp.lib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Arrays;
import java.lang.IllegalThreadStateException;

import android.app.Service;
import android.content.Intent;

import android.os.IBinder;
import android.os.Binder;

import android.util.Log;
import android.os.Process; 

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.IOException;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Arrays;

import android.os.Environment;
import java.io.*;

public class MicService extends Service implements Runnable
{

	static public final String STATE_CHANGED = "de.appwerk.radio.MicService.STATE_CHANGED";
	static public final int REC_TIMEOUT = 9000;
	static public final String RAW_REC_NAME = "reci.3gp";
		// Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)+"/reci.wav";
	
	protected Thread t;
	protected AudioRecord mic;
	protected int source;
	protected int buffSize;
	protected byte[] buff;
	protected int sampleRate;
	protected int format;
	protected int channelConfig;	

	protected FileOutputStream oFile;
	protected OutputStream os;
	protected BufferedOutputStream bos;
	protected DataOutputStream dos;
	protected Timer timer;
	protected MediaRecorder mrec;
	protected int bitsPerSample;
	protected boolean running;
	protected boolean rec;

	private final IBinder mBinder = new LocalBinder();
	
	public class LocalBinder extends Binder
	{
		MicService getService(){
			return MicService.this;
		}
	}
	
	@Override
	public void onCreate()
	{

		super.onCreate();
			
		/*
		try{
			this.oFile = this.openFileOutput(MicService.RAW_REC_NAME, MODE_PRIVATE);
			Log.d(MicService.class.getName(), "File created at: " +this.oFile);
			this.bos = new BufferedOutputStream(this.oFile);
			this.dos = new DataOutputStream(this.bos);
		}
		catch(Exception e){
			e.printStackTrace();
			return;
		}

		this.source = MediaRecorder.AudioSource.DEFAULT;
		this.source = MediaRecorder.AudioSource.MIC;
		this.sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM);
		this.sampleRate = 8000;
		this.bitsPerSample = 16;
		this.channelConfig = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
		this.format = AudioFormat.ENCODING_PCM_16BIT;
		this.buffSize = AudioRecord.getMinBufferSize(this.sampleRate, this.channelConfig, this.format);
		this.buff = new byte[this.buffSize];
		
		try{
			this.mic = new AudioRecord(
				
				this.source,
				this.sampleRate,
				this.channelConfig,
				this.format,
				this.buffSize
			);
		}
		catch(RuntimeException e){
			Log.d(MicService.class.getName(), e.getMessage());
		}
		catch(Exception e){
			Log.d(MicService.class.getName(), e.getMessage());
		}

		if(null == this.mic){
			return;
		}
		
		try{
			this.mic.startRecording();
			Log.d(MicService.class.getName(), "Mic started recording");
		}
		catch(IllegalStateException e){
			e.printStackTrace();
		}

		this.running = true;
		this.t = new Thread(this);
		
		try{
			this.t.start();
		}	
		catch(IllegalThreadStateException e){
			e.printStackTrace();
		}

		this.timer = new Timer();
		this.timer.schedule(
			new TimerTask(){
				@Override public void run(){
					
					broadcast("stop");
				}
			}, 
			MicService.REC_TIMEOUT
		);
		*/

		try{

			this.oFile = this.openFileOutput(MicService.RAW_REC_NAME, MODE_PRIVATE);
		}
		catch(Exception e){
	
			e.printStackTrace();
		}

		try{
	
			this.mrec = new MediaRecorder();
			this.mrec.setAudioSource(MediaRecorder.AudioSource.MIC); 
			this.mrec.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT); 
			this.mrec.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
			this.mrec.setOutputFile(this.oFile.getFD());
			this.mrec.prepare();
			this.mrec.start();
		}
		catch(Exception e){

			e.printStackTrace();
		}
		
		this.timer = new Timer();
		this.timer.schedule(
			new TimerTask(){
				@Override public void run(){
					broadcast("stop");
				}
			}, 
			MicService.REC_TIMEOUT
		);
	}

	protected void stopMic()
	{
		/*
		this.running = false;	
		
		if(null == this.mic){
			
			return;
		}
		
		try{
		
			this.mic.stop();
			this.mic.release();
		}
		catch(IllegalStateException e){
			
			e.printStackTrace();
		}

		this.writeWaveFormat();
		*/

		try{

			this.mrec.stop();
			this.mrec.reset();
			this.mrec.release();
		}
		catch(Exception e){

			e.printStackTrace();
		}

		if(null == this.oFile){

			return;
		}
		
		try{
			
			this.oFile.close();
		}
		catch(Exception e){

			e.printStackTrace();
		}
	}

	@Override public void onDestroy()
	{
		this.stopMic();
		super.onDestroy();
	}

	@Override public boolean onUnbind(Intent intent)
	{
		return super.onUnbind(intent);
	}
	
	@Override public IBinder onBind(Intent intent)
	{
		return this.mBinder;
	}
	
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		return 1;
	}

	@Override public void run()
	{
		/*
		int len = 0;
		while(true == this.running){
			len = this.mic.read(this.buff, 0, this.buff.length);
			try{
				for(int i = 0; i < len; i++){
					
					// Log.d(MicService.class.getName(), "" +this.buff[i]);
					this.dos.writeShort(this.buff[i]);
				}
			}
			catch(IOException e){
				
				e.printStackTrace();
			}
		}
		return;
		*/
	}

	protected String bm;
	protected void broadcast(String m)
	{
		this.bm = m;
		
		Thread t = new Thread()
		{
			@Override
			public void run()
			{
				Intent i = new Intent();
				i.setAction(MicService.STATE_CHANGED);
				i.putExtra("state", bm);
				sendBroadcast(i);
			}
		};
		
	
		t.start();
	}

	/*
	protected void writeWaveFormat()
	{
		FileInputStream in = null;
		FileOutputStream out = null;
		
		int channels = 2;
		byte bps = (byte)this.bitsPerSample;
		
		long audiolen = 0;
		long datalen = audiolen +44;
		long byterate = this.bitsPerSample *this.sampleRate *channels/8;
		long samplerate = this.sampleRate;
		
		byte[] data = new byte[this.buffSize];
		
		try{
			audiolen = this.buffSize;
			datalen = audiolen +44;
				
			byte[] header = new byte[44];
			
			header[0]  = 'R';
			header[1]  = 'I';
			header[2]  = 'F';
			header[3]  = 'F';
			header[4]  = (byte)((datalen & 0xff));
			header[5]  = (byte)((datalen >> 8) &0xff);
			header[6]  = (byte)((datalen >> 16) &0xff);
			header[7]  = (byte)((datalen >> 24) &0xff);
			header[8]  = 'W';
			header[9]  = 'A';
			header[10] = 'V';
			header[11] = 'E';
			header[12] = 'f';
			header[13] = 'm';
			header[14] = 't';
			header[15] = ' ';
			header[16] =  16;
			header[17] =   0;
			header[18] =   0;
			header[19] =   0;
			header[20] =   1;
			header[21] =   0;
			header[22] = (byte)channels;
			header[23] =   0;
			header[24] = (byte)((samplerate & 0xff));
			header[25] = (byte)((samplerate >> 8) & 0xff);
			header[26] = (byte)((samplerate >> 16) & 0xff);
			header[27] = (byte)((samplerate >> 24) & 0xff);
			header[28] = (byte)((byterate & 0xff));
			header[29] = (byte)((byterate >> 8) & 0xff);
			header[30] = (byte)((byterate >> 16) & 0xff);
			header[31] = (byte)((byterate >> 24) & 0xff);
			header[32] = (byte)(2 *16 /8);
			header[33] =   0;
			header[34] = bps;
			header[35] =   0;
			header[36] = 'd';
			header[37] = 'a';
			header[38] = 't';
			header[39] = 'a';
			header[40] = (byte)((audiolen & 0xff));
			header[41] = (byte)((audiolen >> 8) & 0xff);
			header[42] = (byte)((audiolen >> 16) & 0xff);
			header[43] = (byte)((audiolen >> 24) & 0xff);

			out = this.openFileOutput(MicService.WAV_REC_NAME, MODE_PRIVATE);
			out.write(header, 0, 44);
	
			in = this.openFileInput(MicService.RAW_REC_NAME);
			short rs;
			while(-1 != (rs = (short)in.read())){

				Log.d(MicService.class.getName(), "" +rs);
				out.write(rs);	
			}
			
			in.close();
			out.close();
		}
		catch(FileNotFoundException e){
			
			e.printStackTrace();
		}
		catch(IOException e){
		
			e.printStackTrace();
		}
	}
	*/
}
