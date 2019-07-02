package de.appwerk.radioapp.lib;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.UnsupportedEncodingException;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.NameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.BasicHttpParams;

import java.util.concurrent.ExecutionException;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Calendar;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import android.util.Log;
import android.os.AsyncTask;
import android.graphics.Bitmap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings.Secure;

import java.text.DateFormat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.http.entity.InputStreamEntity;
import org.apache.http.HttpEntity;
import org.apache.http.protocol.HTTP;

import java.net.URLEncoder;

import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import com.parse.Parse;
import com.parse.ParsePush;
import com.parse.ParseException;
import com.parse.SaveCallback;
import com.parse.ParseInstallation;
import com.parse.PushService;

public class AppwerkService
{
	static public final String UTF_8 = "UTF-8";
	static public final String SERVICE_COMPLETE = "AppwerkService.SERVICE_COMPLETE";
	static public final String ITUNES_QUERY_URL = "https://itunes.apple.com/search?term=";
	static public final String GOOGLE_QUERY_URL = "https://play.google.com/store/search?c=music&q=";
	static public final int MESSAGE_TIME_OUT = 2500;

	protected String newWatermarkDetectedURL; 
	protected String adURL; 
	protected String guessAdIsOverURL; 
	
	protected String uid;
	
	protected DefaultHttpClient httpClient; 
	protected Context context;
	
	protected String configURL;
	protected String recUploadURL;
	protected String snapshotUploadURL;
	protected String trackURL;
	protected String commentURL;
	protected String likeURL;
	
	protected IntentFilter intentFilter;
	
	protected BroadcastReceiver metaReceiver;
	protected BroadcastReceiver viewReceiver;
	protected BroadcastReceiver serviceReceiver;
	protected BroadcastReceiver snapshotReceiver;
	protected BroadcastReceiver bgDetectReceiver;
	protected BroadcastReceiver pushReceiver;
	
	protected ConnectivityManager cm;
	protected NetworkInfo nf;
	
	protected String rid;
	protected String filename;

	protected Timer timer = new Timer();
	protected String currentWatermark = "-1";
	
	protected String parseApplicationId;
	protected String parseClientKey;
	// protected ParsePush push;
		
	public AppwerkService(Context context)
	{
		this.context = context;
		this.setupParse();
		this.uid = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
		this.likeURL = this.context.getString(R.string.radio_like_url);
		this.configURL = this.context.getString(R.string.radio_config_url);
		this.recUploadURL = this.context.getString(R.string.rec_upload_url);
		this.snapshotUploadURL = this.context.getString(R.string.snapshot_upload_url);
		this.commentURL = this.context.getString(R.string.comment_url);
		this.trackURL = this.context.getString(R.string.track_url);
		this.newWatermarkDetectedURL = this.context.getString(R.string.new_watermark_detected_url);
		this.adURL = this.context.getString(R.string.ad_url);
		this.guessAdIsOverURL = this.context.getString(R.string.guess_ad_is_over_url);
		this.rid = this.context.getString(R.string.radio_config_rid);
		this.httpClient = new DefaultHttpClient();
		this.registerReceiver();

		if(this.clientIsConnectedToTheInternet()){
			this.loadConfig();
		}
		else{
			JSONObject json = new JSONObject();
			try{
				json.put("req", "noConnection");
			}
			catch(JSONException e){
				e.printStackTrace();
			}
			PostReq pr = new PostReq(this.context, this.httpClient);
			pr.broadcastResultMessage(json.toString());
		}
	}

	protected void setupParse()
	{
		this.parseApplicationId = this.context.getString(R.string.parse_application_id);
		this.parseClientKey = this.context.getString(R.string.parse_client_key);
		
		// Parse.initialize(this.context, this.parseApplicationId, this.parseClientKey);
		ParsePush.subscribeInBackground("", new SaveCallback(){
			@Override public void done(ParseException e){
				if(e == null){
					Log.d(AppwerkService.class.getName(), "successfully subscribed to the broadcast channel.");
				}
				else{
					Log.e(AppwerkService.class.getName(), "failed to subscribe for push", e);
				}
			}
		});
		
		/*
		this.push = new ParsePush();
		this.push.setChannel("TestChannel");
		this.push.setMessage("FyFFa! As in F**Ü yo**r f**** f***ce");
		this.push.sendInBackground();
		*/
	}

	public void unregister()
	{
		if(null != this.pushReceiver){
			this.context.unregisterReceiver(this.pushReceiver);
		}
		if(null != this.bgDetectReceiver){
			this.context.unregisterReceiver(this.bgDetectReceiver);
		}
		if(null != this.metaReceiver){
			this.context.unregisterReceiver(this.metaReceiver);
		}
		if(null != this.serviceReceiver){
			this.context.unregisterReceiver(this.serviceReceiver);
		}
		if(null != this.viewReceiver){
			this.context.unregisterReceiver(this.viewReceiver);
		}
	}	
	
	protected void registerReceiver()
	{
		this.intentFilter = new IntentFilter();
		this.intentFilter.addAction(PushReceiver.PUSH_RECEIVED);
		this.pushReceiver = new BroadcastReceiver(){
			@Override public void onReceive(Context context, Intent i){
				String res = i.getStringExtra("push");
			}
		};
		this.context.registerReceiver(this.pushReceiver, this.intentFilter);

		this.intentFilter = new IntentFilter();
		this.intentFilter.addAction(DacDetectService.MARK_DETECTED);
		this.bgDetectReceiver = new BroadcastReceiver(){
			@Override public void onReceive(Context context, Intent i){
				String res = i.getStringExtra("msg");
				showDetectResult(res);	
			}
		};
		this.context.registerReceiver(this.bgDetectReceiver, this.intentFilter);

		this.intentFilter = new IntentFilter();
		this.intentFilter.addAction(IcyStreamMetaService.META_RECEIVED);
		
		this.metaReceiver = new BroadcastReceiver(){
			@Override public void onReceive(Context context, Intent i){
				String res = i.getStringExtra("meta");
				dLoadiTunesRelated(res);
				// dLoadAndroidMusikRelated(res);
			}
		};
		this.context.registerReceiver(this.metaReceiver, this.intentFilter);
	
		this.intentFilter = new IntentFilter();
		this.intentFilter.addAction(AppwerkService.SERVICE_COMPLETE);
		this.serviceReceiver = new BroadcastReceiver(){
			@Override public void onReceive(Context context, Intent i){
				// String res = i.getStringExtra("res");
				// parseResult(res);
			}
		};
		this.context.registerReceiver(this.serviceReceiver, this.intentFilter);
	
		this.intentFilter = new IntentFilter();
		this.intentFilter.addAction(ViewBuilder.ACTION_REQUESTED);
		this.viewReceiver = new BroadcastReceiver(){
			@Override public void onReceive(Context context, Intent i){
				String act = i.getStringExtra("act");
				String arg = i.getStringExtra("arg");
				
				if(null == arg){
					return;
				}	
				if(null == act){
					return;
				}
				/*
				if(act.equals("loadRSSFeed")){
					loadRSSFeed(arg);
				}
				*/
				else if(act.equals("sendSnapshot")){
					sendSnapshot(arg);
				}
				else if(act.equals("sendComment")){
					sendComment(arg);
				}
				else if(act.equals("sendTrack")){
					sendTrack(arg);
				}
				else if(act.equals("loadAdHTML")){
					loadAdHTML(arg);
				}
			}
		};
		this.context.registerReceiver(this.viewReceiver, this.intentFilter);
	}

	// Parses resulting JSON 
	// .... 
	protected JSONObject res;
	protected JSONArray channels;
	protected JSONArray features;
	protected JSONObject allFeatures;
	
	/*
	protected void parseResult(String res)
	{
		this.res = new JSONObject();
		try{
			this.res = new JSONObject(res);
			this.channels = this.res.getJSONArray("formats");
		}	
		catch(JSONException e){
			e.printStackTrace();
		}
	}
	*/
	
	protected String mSong;
	protected String mArtist;

	// Google Service
	protected void dLoadAndroidMusikRelated(String res)
	{
		Log.d(AppwerkService.class.getName(), res);
		
		this.mSong = "";
		this.mArtist = "";
		
		String[] songAndArtist = res.split("-");	
		
		for(int i = 0; i < songAndArtist.length; i++){
			String tmp = songAndArtist[i];
			if(0 == i){
				this.mSong = tmp.trim();
			}
			else if(1 == i){
				this.mArtist = tmp.trim();
			}
		}
		
		if(this.mArtist.equals("")){
			return;
		}
		
		String i = this.mSong;
		String s = this.mArtist;
				
		try{
			i = URLEncoder.encode(i, AppwerkService.UTF_8);
			s = URLEncoder.encode(s, AppwerkService.UTF_8);
		}
		catch(UnsupportedEncodingException e){
			e.printStackTrace();
		}
		
		String qURL = AppwerkService.GOOGLE_QUERY_URL +i +"+" +s;
		// String qURL = R.string.gtunes_url +i +"+" +s;
		String d = "";
		PostReq pr = new PostReq(this.context, this.httpClient);
		pr.send(qURL, d);
	}

	// iTunes Service...	
	protected void dLoadiTunesRelated(String res)
	{
		this.mSong = "";
		this.mArtist = "";
		String[] songAndArtist = res.split("-");
		
		for(int i = 0; i < songAndArtist.length; i++){
			String tmp = songAndArtist[i];
			if(0 == i){
				this.mSong = tmp.trim();
			}
			else if(1 == i){
				this.mArtist = tmp.trim();
			}
		}		

		if(this.mArtist.equals("")){
			return;
		}
		
		String i = this.mSong;
		String s = this.mArtist;

		// Removes non ASCII
		i = i.replaceAll("[^\\x00-\\x7F]", "");
		s = s.replaceAll("[^\\x00-\\x7F]", "");
		
		try{
			i = URLEncoder.encode(i, AppwerkService.UTF_8);
			s = URLEncoder.encode(s, AppwerkService.UTF_8);
		}
		catch(UnsupportedEncodingException e){
			e.printStackTrace();
		}
		
		String qURL = AppwerkService.ITUNES_QUERY_URL +i +"+" +s;
		// String qURL = R.string.itunes_url +i +"+" +s;
		String d = "";
		
		Log.d(AppwerkService.class.getName(), qURL);

		PostReq pr = new PostReq(this.context, this.httpClient);
		pr.send(qURL, d);
	}

	// BGDetect		
	/*
	protected void showBGDetectResult(String res)
	{
		Log.d(AppwerkService.class.getName(), "showBGDetectResult:" +res);
	}
	*/
	
	protected boolean clientIsConnectedToTheInternet()
	{
		boolean res = false;

		this.cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		this.nf = cm.getActiveNetworkInfo();
		
		if(null != nf && nf.isConnectedOrConnecting()){
			res = true;
		}
		
		return res;
	}

	public void sendLikeReq(String val)
	{
		if(false == this.clientIsConnectedToTheInternet()){
			return;
		}

		String channel = "";
		String song = "";
		String[] tmp;
	
		try{
			tmp = val.split(";");
			channel = tmp[0];
			song = tmp[1];
		}
		catch(Exception e){
			// e.printStackTrace();
			return;
		}		

		// String stmp = DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
		long stmp = System.currentTimeMillis() /1000L;			

		JSONObject json = new JSONObject();
		
		if(this.clientIsConnectedToTheInternet()){
		
			try{
				json.put("req", "sendLikeReq");
				json.put("rid", this.rid);
				json.put("song", song);
				json.put("channel", channel);
				json.put("stmp", stmp);
			}
			catch(JSONException e){
			
				e.printStackTrace();
			}
	
			PostReq pr = new PostReq(this.context, this.httpClient);
			pr.send(this.likeURL, json.toString());
		}
		else{
	
			// .. No Internet Connection
		}
	}

	protected void sendWatermarkStoppedMessage(String res)
	{
		if(null == this.currentWatermark){
			return;
		}

		this.stopMessageOffTimer();	
	
		String url = this.guessAdIsOverURL;
	
		int adId = -1;
		try{
			adId = Integer.parseInt(this.currentWatermark, 2);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		url += "&adID=" +adId;
		url += "&senderID=" +this.rid;
		GetReq gr = new GetReq(this.context, this.httpClient);
		gr.send(url, "");
		
		// saves a copy
		this.currentWatermark = res;
		
		Log.d(AppwerkService.class.getName(), "sendWatermarkStoppedMessage(): " +url);
	}

	protected void sendWatermarkStartedMessage(String res)
	{
		String url = this.newWatermarkDetectedURL;
	
		// push notif req	
		int adId = -1;
		try{
			adId = Integer.parseInt(res, 2);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		url += "&adID=" +adId;
		url += "&senderID=" +this.rid;
		Log.d(AppwerkService.class.getName(), url);
		GetReq gr = new GetReq(this.context, this.httpClient);
		gr.send(url, "");
		
		Log.d(AppwerkService.class.getName(), "sendWatermarkStartedMessage(): " +url);

		// lazy load of ad
		// ...............
		// ...............
		// ...............
		String adURL = this.adURL +"/" +adId +"/index.html";
		PostReq pr = new PostReq(this.context, this.httpClient);
		pr.broadcastResultMessage("ad requested: " +adURL);
		// ...............
	}

	protected void stopMessageOffTimer()
	{
		try{
			this.timer.cancel();
			this.timer = new Timer();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
		
	protected void startMessageOffTimer(String res)
	{
		this.stopMessageOffTimer();

		this.timer.schedule(
			new TimerTask(){
				@Override public void run(){
					
					sendWatermarkStoppedMessage(currentWatermark);
				}
			}, 
			AppwerkService.MESSAGE_TIME_OUT
		);
	}	

	protected void loadAdHTML(String url)
	{
		GetReq gr = new GetReq(this.context, this.httpClient);
		gr.send(url, "");
	}

	protected void showPushResult(String res)
	{
		Log.d(AppwerkService.class.getName(), "showPushResult():" +res);
	}

	protected String prevMessage = "";	
	protected void showDetectResult(String res)
	{
		// Log.d(AppwerkService.class.getName(), "showDetectResult(): >>>>>>>>>>>>>>>>" +res);
		
		if(res.startsWith("New message: ")){
			res = res.replace("New message: ", "");
			this.sendWatermarkStoppedMessage(res);
			this.sendWatermarkStartedMessage(res);		
			this.startMessageOffTimer(res);
			this.prevMessage = res;
		}
		
		if(res.startsWith("Detect: ")){
			res = res.replace("Detect: ", "");
			this.startMessageOffTimer(res);	
		}	
	}

	protected int binaryToInteger(String binary)
	{
		char[] numbers = binary.toCharArray();
		
		int res = 0;
	
		int count = 0;	
		for(int i = numbers.length; i == 0; i--){
			if(numbers[i] == '1'){
				// res += (numbers.length -i +1) *2;
				res += (int)Math.pow(2, count);
				count++;
			}	
		}
		
		return res;
	}
	
	public void sendRec(String filename)
	{
		Log.d(AppwerkService.class.getName(), "sendRec(): " +filename +" " +this.recUploadURL);
		
		if("" != filename){
			this.filename = filename;
		}
		
		if(false == this.clientIsConnectedToTheInternet()){
			return;
		}
		
		new FileUpload(this.context, this.recUploadURL, filename);
	}

	public void sendSnapshot(String filename)
	{
		Log.d(AppwerkService.class.getName(), "sendSnapshot(): " +filename);
	
		if("" != filename){
			this.filename = filename;
		}	
	
		if(false == this.clientIsConnectedToTheInternet()){
			return;
		}
		
		new FileUpload(this.context, this.snapshotUploadURL, this.filename);
	}

	public void sendTrack(String arg)
	{
		StringTokenizer tks = new StringTokenizer(arg, "|");
		
		// String userId = tks.nextToken();
		// String phoneId = tks.nextToken();
		String event = tks.nextToken();
		String target = tks.nextToken();
		String description = tks.nextToken();	
	
		JSONObject json = new JSONObject();

		CookieStore cookieStore = this.httpClient.getCookieStore();
		List<Cookie> cookies = cookieStore.getCookies();
		Iterator<Cookie> ic = cookies.iterator();
		String ticket = new String();
		Cookie cookie;
		while(ic.hasNext()){
			cookie = ic.next();
			ticket = cookie.getValue();
			// Log.d(AppwerkService.class.getName(), ">>>>>>" +ticket);
		}
		
		try{
			json.put("req", "sendTrack");
			json.put("user_id", "1");
			json.put("phone_id", "1");
			json.put("ticket", ticket);
			json.put("event", event);
			json.put("target", target);
			json.put("description", description);
		}
		catch(JSONException e){

			e.printStackTrace();
		}
		
		Log.d(AppwerkService.class.getName(), "sendTrack():" +json.toString());
		
		PostReq pr = new PostReq(this.context, this.httpClient);
		pr.send(this.trackURL, json.toString());
	}
	
	public void sendComment(String arg)
	{
		JSONObject json = new JSONObject();

		String comment = "";
		
		try{
			
			comment = new String(arg.getBytes(), "UTF-8");
		}
		catch(Exception e){
			
			e.printStackTrace();
		}

		try{
			json.put("req", "sendComment");
			json.put("comment", comment);
		}
		catch(JSONException e){
			
			e.printStackTrace();
		}
		
		Log.d(AppwerkService.class.getName(), "sendComment():" +json.toString());
	
		PostReq pr = new PostReq(this.context, this.httpClient);
		pr.send(this.commentURL, json.toString());
	}

	public void loadConfig()
	{
		Log.d(AppwerkService.class.getName(), "loadConfig(): " +this.configURL);
		
		if(false == this.clientIsConnectedToTheInternet()){
			return;
		}

		JSONObject json = new JSONObject();
		
		try{
			json.put("req", "loadRadioConfig");
			json.put("rid", this.rid);
		}
		catch(JSONException e){

			e.printStackTrace();
		}
		
		PostReq pr = new PostReq(this.context, this.httpClient);
		pr.send(this.configURL, json.toString());
	}

	public void loadRSSFeed(String url)
	{
		if(null == url){
			return;
		}

		if("".equals(url)){
			return;
		}
	
		JSONObject json = new JSONObject();
		try{
			json.put("req", "loadRSSFeed");
			json.put("url", url);
		}	
		catch(JSONException e){
			e.printStackTrace();
		}
	
		PostReq pr = new PostReq(this.context, this.httpClient);
		pr.send(url, json.toString());
	} 

	public DefaultHttpClient getHttpClient()
	{
		return this.httpClient;
	}

	public void stop()
	{
	}
}

class GetReq extends AsyncTask<String, String, String>
{
	protected DefaultHttpClient httpClient;
	protected String resultString;
	protected Context context;
	
	public GetReq(Context context, DefaultHttpClient httpClient)
	{
		super();
		this.context = context;
		this.httpClient = httpClient;
	}
	
	@Override protected String doInBackground(String ...p)
	{
		String url = p[0];
		
		HttpGet req = new HttpGet(url);
		
		String result = "";	
		HttpResponse res; 
		
		try{
			res = httpClient.execute(req);
				
			InputStream is = res.getEntity().getContent();
			BufferedReader br = new BufferedReader(
				new InputStreamReader(is, "UTF-8")
			);
			String line;
			while(null != (line = br.readLine())){
				
				result += line;
			}
		}
		catch(ClientProtocolException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		return result;
	}

	@Override
	protected void onPostExecute(String res)
	{
		this.broadcastResultMessage(res);
	}
	
	protected String bm;
	public void broadcastResultMessage(String m)
	{
		this.bm = m;
		
		Thread t = new Thread()
		{
			@Override
			public void run()
			{
				Intent i = new Intent();
				i.setAction(AppwerkService.SERVICE_COMPLETE);
				i.putExtra("res", bm);
				context.sendBroadcast(i);
			}
		};
		
		t.start();
	}

	protected void send(String... p)
	{
		try{
			this.execute(p).get();	
		}
		catch(InterruptedException e){
			e.printStackTrace();
			Log.d(GetReq.class.getName(), e.getMessage());
		}	
		catch(ExecutionException e){
			e.printStackTrace();
			Log.d(GetReq.class.getName(), e.getMessage());
		}
	}
}

class PostReq extends AsyncTask<String, String, String>
{

	protected DefaultHttpClient httpClient;
	protected Context context;
	protected String bm;

	public PostReq(Context context, DefaultHttpClient httpClient)
	{
		super();
		this.context = context;
		this.httpClient = httpClient;
		HttpParams hp = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(hp, 1000);
		HttpConnectionParams.setConnectionTimeout(hp, 1000);
	}
	
	@Override protected String doInBackground(String ...p)
	{
		String res = null;
		HttpPost httpPost = new HttpPost(p[0]);
			
		try{
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
			nameValuePairs.add(new BasicNameValuePair("json", p[1]));
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
			httpPost.setHeader("Accept", "text/html,application/xml,application/xhtml+xml,text/html");
			httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
			// String e = new StringEntity(p[1], HTTP.UTF_8);
			// e.setContentType("application/json");
			// httpPost.setEntity(e);
		}
		catch(UnsupportedEncodingException e){
			e.printStackTrace();
			Log.d(PostReq.class.getName(), e.getMessage());
		}
		
		try{
			HttpResponse response = this.httpClient.execute(httpPost);
			StatusLine statusLine = response.getStatusLine();
			if(statusLine.getStatusCode() == HttpStatus.SC_OK){
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				response.getEntity().writeTo(out);
				res = out.toString();
				out.close();
			}
			else{
				response.getEntity().getContent().close();
				res = statusLine.getReasonPhrase();
			}
		}
		catch (ClientProtocolException e) {
			e.printStackTrace();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}

		return res;
	}

	@Override protected void onPostExecute(String res)
	{
		Log.d(AppwerkService.class.getName(), "onPostExecute():" +res);
		super.onPostExecute(res);
		this.broadcastResultMessage(res);
	}

	public void broadcastResultMessage(String m)
	{
		this.bm = m;
		
		Thread t = new Thread()
		{
			@Override
			public void run()
			{
				Intent i = new Intent();
				i.setAction(AppwerkService.SERVICE_COMPLETE);
				i.putExtra("res", bm);
				context.sendBroadcast(i);
			}
		};
		
		t.start();
	}

	protected void send(String... p)
	{
		try{
			this.execute(p).get();	
		}
		catch(InterruptedException e){
			e.printStackTrace();
			Log.d(PostReq.class.getName(), e.getMessage());
		}	
		catch(ExecutionException e){
			e.printStackTrace();
			Log.d(PostReq.class.getName(), e.getMessage());
		}
	}
}
