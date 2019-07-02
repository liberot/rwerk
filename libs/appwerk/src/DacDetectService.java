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

import android.media.MediaRecorder;
import android.media.AudioRecord;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.AudioManager;

import java.util.Arrays;
import java.lang.IllegalThreadStateException;

import de.fraunhofer.sit.watermarking.algorithmmanager.AlgorithmParameter;
import de.fraunhofer.sit.watermarking.algorithmmanager.WatermarkMessage;
import de.fraunhofer.sit.watermarking.algorithmmanager.detector.StreamWatermarkDetector;
import de.fraunhofer.sit.watermarking.algorithmmanager.exception.WatermarkException;
import de.fraunhofer.sit.watermarking.sitmark.audio.SITMarkAudioAnnotationDetector;

public class DacDetectService extends Service implements Runnable
{
	protected static final String AUDIOANN_MESSAGE_LENGTH_PARAM = "NetMessageLength";
	protected static final String AUDIOANN_FREQ_MIN_PARAM = "FreqMin";
	protected static final String AUDIOANN_FREQ_MAX_PARAM = "FreqMax";
	protected static final String AUDIOANN_ECC_TYPE_PARAM = "ErrorCorrectionType";
	protected static final String AUDIOANN_WM_REDUNDANCY_PARAM = "WMRedundancy";

	static public final String MARK_DETECTED = "de.appwerk.radio.DacDetectService.MARK_DETECTED";
	
	protected Thread t;
	protected AudioRecord mic;
	protected int source;
	protected int buffSize;
	protected int sampleRate;
	protected int format;
	protected int channelConfig;	
	protected boolean running = false;
	protected boolean rec = false;

	private final IBinder mBinder = new LocalBinder();

	protected byte[] buff;
	protected SITMarkAudioAnnotationDetector detector;
	protected List<WatermarkMessage> detectedMessages;
	protected WatermarkMessage detectedMessage;
	protected String prevDetectedMessage = new String("");
	protected Set<String> distinctFoundMessages = new TreeSet<String>();
	protected ByteArrayInputStream istream;
	protected boolean foundMessage;
	protected short numberOfFoundMessages = 0;
	protected double confidence;

	protected String detectMessageLength = "24";
	protected String detectMinFreq = "2000";
	protected String detectMaxFreq = "10000";
	protected String detectEccMode = "1";
	protected String detectRedundancy = "2";
	
	public class LocalBinder extends Binder
	{
		DacDetectService getService(){
			return DacDetectService.this;
		}
	}
	
	@Override
	public void onCreate()
	{
		super.onCreate();

		this.detectMessageLength = this.getString(R.string.detect_message_length);
		this.detectMinFreq = this.getString(R.string.detect_min_freq);
		this.detectMaxFreq = this.getString(R.string.detect_max_freq);
		this.detectEccMode = this.getString(R.string.detect_ecc_mode);
		this.detectRedundancy = this.getString(R.string.detect_redundancy);
		
		this.source = MediaRecorder.AudioSource.DEFAULT;
		this.sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM);
		this.sampleRate = 44100;
		this.channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
		this.format = AudioFormat.ENCODING_PCM_16BIT;
		this.buffSize = AudioRecord.getMinBufferSize(this.sampleRate, this.channelConfig, this.format) *10;
		this.buff = new byte[this.buffSize];
		this.istream = new ByteArrayInputStream(this.buff);
		
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
			Log.d(DacDetectService.class.getName(), "re: " +e.getMessage());
			e.printStackTrace();
		}
		catch(Exception e){
			Log.d(DacDetectService.class.getName(), "e: " +e.getMessage());
			e.printStackTrace();
		}

		if(null == this.mic){
			return;
		}
		
		try{
			this.mic.startRecording();
		}
		catch(IllegalStateException e){
			Log.d(DacDetectService.class.getName(), "" +e.getMessage());
			e.printStackTrace();
		}

		try{
			this.detector = new SITMarkAudioAnnotationDetector();
			this.detector.initWithoutAlgoman();

			AlgorithmParameter messageLength = this.detector.getParameter(DacDetectService.AUDIOANN_MESSAGE_LENGTH_PARAM);
			AlgorithmParameter minFreq= this.detector.getParameter(DacDetectService.AUDIOANN_FREQ_MIN_PARAM);
			AlgorithmParameter maxFreq= this.detector.getParameter(DacDetectService.AUDIOANN_FREQ_MAX_PARAM);
			AlgorithmParameter ECCMode= this.detector.getParameter(DacDetectService.AUDIOANN_ECC_TYPE_PARAM);
			AlgorithmParameter WatermarkRedundancy= this.detector.getParameter(DacDetectService.AUDIOANN_WM_REDUNDANCY_PARAM);

			messageLength.setValue(this.detectMessageLength);
			minFreq.setValue(this.detectMinFreq);
			maxFreq.setValue(this.detectMaxFreq);
			ECCMode.setValue(this.detectEccMode);
			WatermarkRedundancy.setValue(this.detectRedundancy);

			this.detector.setParameter(messageLength);
			this.detector.setParameter(minFreq);
			this.detector.setParameter(maxFreq);
			this.detector.setParameter(ECCMode);
			this.detector.setParameter(WatermarkRedundancy);
			this.detector.reinitialize();
		}
		catch (WatermarkException e){
			e.printStackTrace();
		}
		catch (Exception e){
			e.printStackTrace();
		}

		this.running = true;
		this.t = new Thread(this);
		try{
			this.t.start();
		}	
		catch(IllegalThreadStateException e){
			Log.d(DacDetectService.class.getName(), ""+e.getMessage());
			e.printStackTrace();
		}
	}

	@Override 
	public void onDestroy()
	{
		this.running = false;	
		if(null != this.mic){
			try{
				this.mic.stop();
				this.mic.release();
			}
			catch(IllegalStateException e){
				Log.d(DacDetectService.class.getName(), "" +e.getMessage());
				e.printStackTrace();
			}
		}
		super.onDestroy();
	}

	@Override 
	public boolean onUnbind(Intent intent)
	{
		return super.onUnbind(intent);
	}
	
	@Override
	public IBinder onBind(Intent intent)
	{
		return this.mBinder;
	}
	
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		return 1;
	}

	protected void detect()
	{
		this.mic.read(this.buff, 0, this.buff.length);
		this.istream = new ByteArrayInputStream(this.buff);
		this.foundMessage = false;
		try{ do {
		
			this.detectedMessages = this.detector.detect(this.istream);
			this.foundMessage = (this.detectedMessages.size() > 0);
		
			if (this.foundMessage) {
			
				this.detectedMessage = this.detectedMessages.get(0);
			
				this.confidence = Double.parseDouble(this.detectedMessage.getMetaDate(WatermarkMessage.SCORE_METADATA));
			
				if (this.confidence > 0.1) {
					this.distinctFoundMessages.add(this.detectedMessage.toString());
					this.numberOfFoundMessages++;
				}
				
				String msg = "Detected: " 
					+System.currentTimeMillis() +": "
					+this.numberOfFoundMessages +": "
					+this.detectedMessage.toString();
			
				// don't	
				// this.broadcast(msg);
		
				if(!this.detectedMessage.toString().equals(this.prevDetectedMessage)){
					String msg2 = "New message: " +this.detectedMessage.toString();
					// Log.d(DacDetectService.class.getName(), "New message: " +msg2);
					this.broadcast(msg2);
				}
				
				this.prevDetectedMessage = this.detectedMessage.toString();
			}
				
			Log.d(DacDetectService.class.getName(), 
				String.format("Found %d messages (%d distinct): %s", 
					this.numberOfFoundMessages, 
					this.distinctFoundMessages.size(), 
					this.distinctFoundMessages
				)
			);
		} 	while (this.foundMessage); 
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	@Override 
	public void run()
	{
		while(true == this.running){
			this.detect();
		}

		return;
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
				i.setAction(DacDetectService.MARK_DETECTED);
				i.putExtra("msg", bm);
				sendBroadcast(i);
			}
		};
		
		t.start();
	}
}
