package de.appwerk.radioapp.lib;

import android.util.Log;
import android.os.AsyncTask;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import android.webkit.URLUtil;

public class IcyStreamMetaService
{
	static public final String META_RECEIVED = "IcyStreamMetaService.META_RECEIVED";
	static public final String STREAM_TITLE = "StreamTitle";
	static public final int META_GET_INTERVAL = 60 *1000 *5;
	
	protected IcyStreamMeta res;
	protected Context context;
	protected String bm;
	protected TimerTask tt;
	protected Timer t;
	protected URL metadataURL;
	protected MetadataTask mt;
	protected Handler h;
	protected HandlerThread ht;

	public IcyStreamMetaService(Context context)
	{
		this.context = context;
		
		this.ht = new HandlerThread("ht");
		this.ht.start();
		
		this.h = new Handler(this.ht.getLooper());
		this.h.postDelayed(this.task, IcyStreamMetaService.META_GET_INTERVAL);	
	}

	Runnable task = new Runnable()
	{
		@Override public void run(){
			read(metadataURL);	
			h.postDelayed(this, IcyStreamMetaService.META_GET_INTERVAL);
		}
	};

	public void unregister()
	{
		if(null == this.ht){
			return;
		}	
		
		this.ht.quit();	
	}	
	
	public void read(URL url)
	{	

		if(null == url){
			return;
		}
		
		if(false == URLUtil.isValidUrl(url.toString())){
			return;
		}	

		this.mt = new MetadataTask();
		this.metadataURL = url;
		
		try{
			this.res = (IcyStreamMeta)this.mt.execute(url).get();
			this.broadcast(res.getMeta());
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	protected void broadcast(String m)
	{
		this.bm = m;
		Thread t = new Thread()
		{
			@Override public void run()
			{
				Intent i = new Intent();
				i.setAction(IcyStreamMetaService.META_RECEIVED);
				i.putExtra("meta", bm);
				context.sendBroadcast(i);
			}
		};
		
		t.start();
	}
}

class IcyStreamMeta
{
	protected Map<String, String> metadata;
	protected URL streamUrl;
	protected boolean isError;
 
	public IcyStreamMeta(URL streamUrl)
	{
		setStreamUrl(streamUrl);
		isError = false;
	}
 
	/**
	 * Get artist using stream's title
	 *
	 * @return String
	 * @throws IOException
	 */
	protected String getMeta() throws IOException
	{
		String res = "";	
		Map<String, String> data = getMetadata();
		
		if(data.containsKey(IcyStreamMetaService.STREAM_TITLE)){
			String streamTitle = data.get(IcyStreamMetaService.STREAM_TITLE);
			res = streamTitle.trim();
		}

		return res;
	}
 
	protected Map<String, String> getMetadata() throws IOException
	{
		if(this.metadata == null){
			this.refreshMeta();
		}
 
		return this.metadata;
	}
 
	protected void refreshMeta() throws IOException
	{
		this.retreiveMetadata();
	}
 
	protected void retreiveMetadata() throws IOException
	{
		URLConnection con = streamUrl.openConnection();

		con.setRequestProperty("Icy-MetaData", "1");
		con.setRequestProperty("Connection", "close");
		con.setRequestProperty("Accept", null);
		con.connect();
 
		int metaDataOffset = 0;

		Map<String, List<String>> headers = con.getHeaderFields();
		InputStream stream = con.getInputStream();

		if (headers.containsKey("icy-metaint")) {
			metaDataOffset = Integer.parseInt(headers.get("icy-metaint").get(0));
		} 
		else {
		
			StringBuilder strHeaders = new StringBuilder();
			char c;
			
			while ((c = (char)stream.read()) != -1) {
				strHeaders.append(c);
				if (strHeaders.length() > 5 && (strHeaders.substring((strHeaders.length() - 4), strHeaders.length()).equals("\r\n\r\n"))) {
					break;
				}
			}
		}
 
		int b;
		int count = 0;
		int metaDataLength = 4080; // 4080 is the max length
		
		boolean inData = false;
		
		StringBuilder metaData = new StringBuilder();
		
		// Stream position should be either at the beginning or right after headers
		while ((b = stream.read()) != -1){
			
			count++;
 
			// Length of the metadata
			if(count == metaDataOffset +1){
				metaDataLength = b *16;
			}
 
			if(count >metaDataOffset +1 && count <(metaDataOffset +metaDataLength)){
				inData = true;
			}
			else{ 				
				inData = false; 			
			} 	 			
			
			if(inData){ 				
				if (b != 0){ 					
					metaData.append((char)b); 				
				} 
			} 	 			
			
			if(count > (metaDataOffset + metaDataLength)){
				break;
			}
		}
 
		stream.close();
		this.metadata = this.parseMetadata(metaData.toString());
	}
 
	protected boolean isError()
	{
		return isError;
	}
 
	protected URL getStreamUrl()
	{
		return streamUrl;
	}
 
	protected void setStreamUrl(URL streamUrl)
	{
		this.streamUrl = streamUrl;
		this.isError = false;
		this.metadata = null;
	}

	protected Map<String, String> parseMetadata(String metaString)
	{
		this.metadata = new HashMap();
		
		String[] metaParts = metaString.split(";");
		Pattern p = Pattern.compile("^([a-zA-Z]+)=\\'([^\\']*)\\'$");
		Matcher m;
		
		for (int i = 0; i < metaParts.length; i++) {

			m = p.matcher(metaParts[i]);
			
			if (m.find()) {
				this.metadata.put((String)m.group(1), (String)m.group(2));
			}
		}

		/*
		String[] songAndInterpret = metaString.split("-");
		
		this.mSong = "";
		this.mInterpret = "";
		for(int i = 0; i < songAndInterpret.length; i++){

			String res = songAndInterpret[i];
			
			if(0 == i){

				this.mSong = res.trim();
			}
			else if(1 == i){

				this.mInterpret = res.trim();
			}
		}		

		Log.d(">>>>>>>>>>>>>>>>> mSong:", this.mSong);
		Log.d(">>>>>>>>>>>>>>>>> mInterpret: ", this.mInterpret);
 		*/

		return this.metadata;
	}
}

class MetadataTask extends AsyncTask<URL, IcyStreamMeta, IcyStreamMeta>
{

	protected IcyStreamMeta streamMeta;
 
	@Override protected IcyStreamMeta doInBackground(URL... urls)	
	{
		this.streamMeta = new IcyStreamMeta(urls[0]);
		
		try {
			this.streamMeta.refreshMeta();
		}
		catch (IOException e) {
			Log.e(MetadataTask.class.toString(), e.getMessage());
		}
		
		return streamMeta;
	}
 
	@Override protected void onPostExecute(IcyStreamMeta result)
	{
		
		try {
			Log.d(IcyStreamMetaService.class.getName(), "Metadata: " +this.streamMeta.getMeta());
		} 
		catch (IOException e) {
			Log.e(MetadataTask.class.toString(), e.getMessage());
		}
	}
}
