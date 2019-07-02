/* ****
''''
	ViewBuilder *builds dynamic view comps from ( mostly JSON.... ) service results
*/

package de.appwerk.radioapp.lib;

import android.app.AlertDialog;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.Toast;
import android.content.DialogInterface;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.widget.EditText;
import android.view.ViewTreeObserver.OnScrollChangedListener;

import android.support.v4.app.DialogFragment;
import android.util.AttributeSet;

import android.text.InputType;

import java.nio.ByteBuffer;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;

import android.view.View;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;

import android.widget.ScrollView;
import android.widget.Button;
import android.widget.ToggleButton;
import android.widget.LinearLayout;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import android.app.Service;
import android.app.Activity;

import android.view.Menu;
import android.view.LayoutInflater;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.TranslateAnimation;
import android.view.Gravity;

import android.support.v4.view.ViewPager;

import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import java.util.Map;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

import android.widget.ImageButton;
import android.widget.ImageView;

import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.util.Log;

import java.net.URL;
import java.net.HttpURLConnection;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Matrix;

import java.util.Date;
import java.util.Locale;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Document;

import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;

import android.webkit.URLUtil;

import android.util.TypedValue;
import android.view.Gravity;

import android.view.Display;
import android.view.WindowManager;

import android.graphics.Shader.TileMode;
import android.graphics.Rect;
import android.graphics.Color;
import android.graphics.drawable.ScaleDrawable;

import android.content.res.Configuration;

import android.net.Uri;

import java.net.MalformedURLException;
import java.util.ArrayList;
import android.support.v4.view.ViewPager.OnPageChangeListener;

import android.graphics.Bitmap.Config;
import android.graphics.Bitmap;

import android.view.MotionEvent;
import android.text.TextUtils;
import android.webkit.WebSettings;

import android.view.animation.AlphaAnimation;
import android.app.Dialog;
import android.widget.LinearLayout;

import de.appwerk.radioapp.lib.MarioBlur;

public class ViewBuilder 
{
	protected IntentFilter intentFilter;
	protected Activity activity;
	protected Context context;
	protected LinearLayout layout;
	protected LinearLayout ll;

	protected ViewPager pager;
	protected WebView adView;

	protected FragmentManager fragmentManager;
	protected MPageAdapter pageAdapter;

	protected BroadcastReceiver serviceReceiver;
	protected BroadcastReceiver micReceiver;
	protected BroadcastReceiver metaReceiver;
	protected BroadcastReceiver prefsReceiver;
	protected BroadcastReceiver uploadReceiver;
	protected BroadcastReceiver bgDetectReceiver;
	protected BroadcastReceiver pushReceiver;

	protected TextView detectOut;
	protected TextView voiceOut;
	protected TextView metaOut;
	protected TextView prefsOut;
	protected TextView likeOut;

	protected JSONObject formats;
	protected AttributeSet ats;
	protected ToggleButton wlanToggleButton;
	
	protected boolean sitmarkStarted;
	protected boolean micStarted;
	
	protected String songTitle = "";;
	protected String selectedChannelTitle = "";

	protected MarioBlur blur;

	protected int selectedRSS = 0;

	static public final String ACTION_REQUESTED = "ViewBuilder.ACTION_REQUESTED";

	public ViewBuilder(Activity activity, FragmentManager fragmentManager)
	{
		this.activity = activity;
		this.context = this.activity.getApplicationContext();
		this.formats = new JSONObject();
		this.activity.setContentView(R.layout.main);
		this.blur = new MarioBlur();
		this.fragmentManager = fragmentManager;
		this.pager = (ViewPager)this.activity.findViewById(R.id.pager);
		this.pager.setOnPageChangeListener(new OnPageChangeListener(){
			public void onPageScrollStateChanged(int state){
			}
			public void onPageScrolled(int pos, float off, int pix){
			}
			public void onPageSelected(int pos){
				selectPage(pos);
			}	
		});

		
		this.adView = (WebView)this.activity.findViewById(R.id.ad);
		this.adView.getSettings().setJavaScriptEnabled(true);
		this.adView.setBackgroundColor(0x00000000);
		/*
		this.loadAdHTML("http://radioscreen.org/ads/12/index.html");
		this.adView.setWebViewClient(new WebViewClient(){
			public void onPageFinished(WebView view, String url){
			}
		});
		*/	
		this.registerReceiver();
	}

	public void unregister()
	{
		if(null != this.pushReceiver){
			this.context.unregisterReceiver(this.pushReceiver);
		}
		if(null != this.serviceReceiver){
			this.context.unregisterReceiver(this.serviceReceiver);
		}
		if(null != this.bgDetectReceiver){
			this.context.unregisterReceiver(this.bgDetectReceiver);
		}
		if(null != this.metaReceiver){
			this.context.unregisterReceiver(this.metaReceiver);
		}
		if(null != this.prefsReceiver){
			this.context.unregisterReceiver(this.prefsReceiver);
		}
		if(null != this.micReceiver){
			this.context.unregisterReceiver(this.micReceiver);
		}
		if(null != this.uploadReceiver){
			this.context.unregisterReceiver(this.uploadReceiver);
		}
	}
	
	protected void registerReceiver()
	{	
		this.intentFilter = new IntentFilter();
		this.intentFilter.addAction(PushReceiver.PUSH_RECEIVED);
		this.pushReceiver = new BroadcastReceiver(){
			@Override public void onReceive(Context context, Intent i){
				String res = i.getStringExtra("push");
				showPushResult(res);
			}
		};
		this.context.registerReceiver(this.pushReceiver, this.intentFilter);

		this.intentFilter = new IntentFilter();
		this.intentFilter.addAction(AppwerkService.SERVICE_COMPLETE);
		
		this.serviceReceiver = new BroadcastReceiver(){
			@Override public void onReceive(Context context, Intent i){
				routeAppwerkServiceResult(i.getStringExtra("res"));
			}
		};
		
		this.context.registerReceiver(this.serviceReceiver, this.intentFilter);
	
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
				showMetaResult(res);
			}
		};
		
		this.context.registerReceiver(this.metaReceiver, this.intentFilter);

		this.intentFilter = new IntentFilter();
		this.intentFilter.addAction(Prefs.SETTINGS_LOADED);
		this.prefsReceiver = new BroadcastReceiver(){
			@Override public void onReceive(Context context, Intent i){
				String res = i.getStringExtra("prefs");
				showPrefs(res);
			}
		};
		
		this.context.registerReceiver(this.prefsReceiver, this.intentFilter);
	
		this.intentFilter = new IntentFilter();
		this.intentFilter.addAction(MicService.STATE_CHANGED);
		this.micReceiver = new BroadcastReceiver(){
			@Override public void onReceive(Context context, Intent i){
				String state = i.getStringExtra("state");
				micStateChanged(state);	
			}
		};
		
		this.context.registerReceiver(this.micReceiver, this.intentFilter);

		this.intentFilter = new IntentFilter();
		this.intentFilter.addAction(FileUpload.STATUS);
		this.uploadReceiver = new BroadcastReceiver(){
			@Override public void onReceive(Context context, Intent i){
				String res = i.getStringExtra("res");
				fileUploadStateChanged(res);	
			}
		};
		
		this.context.registerReceiver(this.uploadReceiver, this.intentFilter);
	}	
	
	protected void showPushResult(String res)
	{
		Log.d(ViewBuilder.class.getName(), "showPushResult():" +res);
		
		if(null == res){
			return;
		}
		
		if(res.equals("")){
			return;
		}

		if(!res.startsWith("http://")){
			return;
		}
	
		res = res.trim();
		if(!URLUtil.isValidUrl(res)){
			return;
		}		
	
		// loads ad for now... (push always loads ads...)
		this.loadAdHTML(res);
	}
	
	protected void micStateChanged(String state)
	{
		Log.d(ViewBuilder.class.getName(), "micStateChanged():" +state);
		
		if(state.equals("stop")){
			this.stopMic();
		}
	}

	protected int selectedPage = 0;
	protected void selectPage(int pos)
	{
		Log.d(ViewBuilder.class.getName(), "selectPage():" +pos);
		this.selectedPage = pos;
		try{
			this.selectedChannelTitle = this.titles.get(pos);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	protected void fileUploadStateChanged(String res)
	{
		Log.d(ViewBuilder.class.getName(), "fileUploadStateChanged():" +res);
	}

	protected ArrayList<View> rollovers;
	protected void rollover(View view)
	{
		if(null == this.rollovers){
			this.rollovers = new ArrayList<View>();
		}

		if(false == this.rollovers.contains(view)){
			this.rollovers.add(view);
			Log.d(ViewBuilder.class.getName(), "rollover(): adding: " +view);
		}
		
		for(View v : this.rollovers){
			this.rollout(v);
		}	
		
		AlphaAnimation disco;
	
		disco = new AlphaAnimation(0.7f, 0.7f);
		disco.setDuration(1200);
		disco.setFillAfter(true);
		
		view.startAnimation(disco);
	}
	
	protected void rolloutAll()
	{
		AlphaAnimation disco;
	
		disco = new AlphaAnimation(1.0f, 1.0f);
		disco.setDuration(1200);
		disco.setFillAfter(true);
		
		if(null != this.rollovers){
			for(View v : this.rollovers){
				v.startAnimation(disco);
			}	
		}
	}	
	
	protected void rollout(View view)
	{
		this.rolloutAll();
		
		AlphaAnimation disco;
		
		disco = new AlphaAnimation(1.0f, 1.0f);
		disco.setDuration(1200);
		disco.setFillAfter(true);
		
		view.startAnimation(disco);
	}	
	
	protected void routeAppwerkServiceResult(String ires)
	{
		
		if(null == ires){
			return;
		}
		
		if(ires.equals("")){
			return;	
		}

		Log.d("routeAppwerkServiceResult(): ", ires);

		// ad request 
		if(ires.startsWith("ad requested: ")){
			ires = ires.replace("ad requested: ", "");		
			this.loadAdHTML(ires);
			return;
		}

		// ad HTML
		if(ires.indexOf("viewport") != -1){
			this.loadAd(ires);	
			return;
		}

		//--> iTunes 
		/* **
		iTunes search returns:
		------------------------------------------
		{
			resultCount: 10,
			results: [
				{wrapperType: track, kind: song, artistId: 0, collectionId: 0,
					trackId: 0, artistName: "", collectionName= "", trackName: "", 
					collectionName: "", trackCensoredName: "", artistViewUrl: "", 
					collectionViewUrl: "", trackViewUrl: "", previewUrl: "",
					artworkUrl130: "", artworkUrl160: "", artworkUrl1100: "",
					collectionPrice: "", trackPrice: "", releaseDate: "", 
					collectionExplicitness: "", trackExplicitness: "", 
					discount: 1, discNumber: 0, trackCount: "", trackNumber: "";
					trackTimeMillis: 1, country: "", currency: "", primaryGenreName: "",
					radioStationUrl: "", radioStationUrl: "", 	  
				}
			]
		}	
		
		------------------------------------------
		*/

		// SuperGoogleService returns HTML
		if(ires.startsWith("<!doctype html>")){
			this.buildGoogleSearchResultView(ires);	
			return;
		}
		
		// Some services (RSS FEEDs) return XML
		try{
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			InputStream is = new ByteArrayInputStream(ires.getBytes());
			Document doc = db.parse(is);
			Log.d(ViewBuilder.class.getName(), "" +ires);
			NodeList channel = (NodeList)doc.getElementsByTagName("channel");
			// XML Duck-Type RSS Feed (Something with a Channel) 
			if(1 == channel.getLength()){
				this.renderRSSFeedResult(doc);	
				return;
			}
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
		catch(SAXException sae){
			sae.printStackTrace();
		}
		catch(ParserConfigurationException pce){
			pce.printStackTrace();
		}

		// Parses incoming JSON obj in general
		JSONObject jsonObj = new JSONObject();
		try{
			jsonObj = new JSONObject(ires);
		}
		catch(JSONException e){
			this.buildFromDefaultJSON();			
			e.printStackTrace();
			return;
		}
	
		// Parses consumed itunes result
		JSONArray iTunesResult = new JSONArray();
		try{
			iTunesResult = jsonObj.getJSONArray("results");
			this.renderiTunesResult(iTunesResult);
			return;
		}
		catch(JSONException e){
			e.printStackTrace();
		}
		
		// Parses incoming appwerk results	

		//--> Appwerk
		/* **
		ApppwerkService result is a JSON document: 
		------------------------------------------
		{
			'req': 'theRequestName',
			'res': 'theResult'
			'err': 'theError'
		}
		------------------------------------------
		*/
		
		String req = new String("noSuchReq");
		String err = new String("");
		
		try{
			req = jsonObj.getString("req");
		}
		catch(JSONException e){
			e.printStackTrace();
			return;
		}

		// Routes incoming (asynchron) results by their "req" fields 
		if(req.equals("loadRadioConfig")){
			try{
				// JSONObject res = jsonObj.getJSONObject("res");
				this.buildAppView(this.formats = jsonObj.getJSONObject("res"));
			}
			catch(JSONException e){
				this.buildFromDefaultJSON();			
				e.printStackTrace();
			}
		}
		
		else if(req.equals("iLikeThisSong")){
			String res = jsonObj.toString();
			this.showLikes(res); 
		}
		
		else if(req.equals("noConnection")){
			this.buildFromDefaultJSON();			
		}
				
		else{
		
		}
	}

	protected void loadAdHTML(String url)
	{
		// Log.d(ViewBuilder.class.getName(), "loadAd(): " +url);
		/*
		LayoutParams p = this.adView.getLayoutParams();
		p.height = this.adView.getHeight() *2;
		this.adView.setLayoutParams(p);
		*/
		
		this.adView.loadUrl(url);
		this.broadcast("loadAdHTML", url);
	}

	protected void loadAd(String html)
	{
		String needle1 = "height=";
		int from = html.indexOf(needle1);
		from += "height=".length();
		// Log.d(ViewBuilder.class.getName(), "from:" +from);
		
		String tmp = "";	
		try{
			tmp = html.substring(from, html.length());
		}
		catch(Exception e){
			e.printStackTrace();
		}

		String needle2 = ">";
		int to = tmp.indexOf(needle2) -1;
		// Log.d(ViewBuilder.class.getName(), "to:" +to);
		try{
			tmp = tmp.substring(0, to);
		}
		catch(Exception e){
			e.printStackTrace();
		}
			
		int height = Integer.parseInt(tmp);		

		// height adjust
		LayoutParams p = this.adView.getLayoutParams();
		Random r = new Random();
		int r1 = r.nextInt(24);
		p.height = height +r1;
		this.adView.setLayoutParams(p);
		// this.adView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
		// Log.d(ViewBuilder.class.getName(), "loadAd():" +height);
	}

	protected String extractGooglePlayServiceValues(String buf, String from, String to)
	{
		int pos = buf.indexOf(from);
		int nlen = from.length();
		int len = buf.length();
	
		String res = "";	
		res = buf.substring(pos +nlen, len);

		if(null != to){
		}
		
		return res;
	}

	protected void buildGoogleSearchResultView(String ires)
	{
		Log.d(ViewBuilder.class.getName(), "buildGoogleSearchResultView:" +ires);
		Log.d(ViewBuilder.class.getName(), "buildGoogleSearchResultView:" +ires);
		Log.d(ViewBuilder.class.getName(), "buildGoogleSearchResultView:" +ires);
		Log.d(ViewBuilder.class.getName(), "buildGoogleSearchResultView:" +ires);
		Log.d(ViewBuilder.class.getName(), "buildGoogleSearchResultView:" +ires);
		Log.d(ViewBuilder.class.getName(), "buildGoogleSearchResultView:" +ires);
		Log.d(ViewBuilder.class.getName(), "buildGoogleSearchResultView:" +ires);
		Log.d(ViewBuilder.class.getName(), "buildGoogleSearchResultView:" +ires);

		String needle = "<title id=\"main-title\">";
		
		int pos = ires.indexOf(needle);
		int nlen = needle.length();
		int strlen = ires.length();

		String songTitle = ires.substring(pos +nlen, strlen);
		int posu = songTitle.indexOf("-");
		songTitle = songTitle.substring(0, posu);		

		needle = "class=\"subtitle\"";
		pos = ires.indexOf(needle);
		nlen = needle.length();
		strlen = ires.length();
		String artist = ires.substring(pos +nlen, strlen);

		ires = ires.substring((int)nlen +pos, strlen);
	
		needle = "<img class=\"cover-image\"";
		pos = ires.indexOf(needle);
		nlen = needle.length();
		strlen = ires.length();
		ires = ires.substring((int)nlen +pos, strlen);

		int posa = 0;
		needle = "data-cover-large=\"";
		posa = ires.indexOf(needle);
		nlen = needle.length();
		int pose = ires.indexOf("\""); 
		strlen = ires.length();
		ires = ires.substring((int)nlen +posa, strlen);

		pose = ires.indexOf("\"");
		ires = ires.substring(0, pose);

		String imageURL = ires;
	}
	
	protected int parseColorHexString(String hex)
	{
		Log.d(ViewBuilder.class.getName(), "parseColorHexString():" +hex);
		
		String tmp = "#" +hex.substring(0, 6);
		int res = Color.parseColor(tmp);
		return res;
	}

	protected int parseAlphaHexString(String hex)
	{
		Log.d(ViewBuilder.class.getName(), "parseAlphaHexString():" +hex);
				
		String tmp = hex.substring(6, 8);
		int res = Integer.parseInt(tmp, 16);
		float rres = (float)res;
		float ffyf = (float)(rres /255 *100);	
		return (int)ffyf;
	}

	protected void loadRSSFeed()
	{
		this.selectedRSS = this.selectedPage;
		
		String loc = "";
		try{ 
			loc = this.rssFeedURLs.get(this.selectedPage);
		}	
		catch(Exception e){
			e.printStackTrace();
		}
		// Log.d(ViewBuilder.class.getName(), "loadRSSFeed():" +loc);
		this.broadcast("loadRSSFeed", loc);
	}

	protected ArrayList<String> rssFeedURLs = new ArrayList<String>();
	protected void renderRSSFeedResult(Document doc)
	{
		Log.d(ViewBuilder.class.getName(), "buildRSSFeedView():" +doc);
		
		NodeList channel = (NodeList)doc.getElementsByTagName("channel");
		
		long d;

		WebView target = new WebView(this.context);

		DynView vv = this.RWRSSCells.get(this.selectedRSS);
	
		if(null == vv){
			return;
		}

		for(int j = 0; j < vv.getChildCount(); j++){
			View currentChild = vv.getChildAt(j);
			if(currentChild instanceof WebView){
				target = (WebView)vv.getChildAt(j);
			}
		}
			
		if(null == target){
			return;
		}
	
		String res = new String();
		String title = new String();	
		String link = new String();
		String description = new String();
		String copyright = new String();
		String pubDate = new String();
		String items = new String();
		String bgColor = new String();
		String titleColor = new String();
		String labelColor = new String();
		
		ColorStruct cs;	

		cs = this.colorStructs.get(this.selectedRSS);
		titleColor = this.getCSSRGBAFromHex(cs.titleColor);
		bgColor = this.getCSSRGBAFromHex(cs.bgColor);
		labelColor = this.getCSSRGBAFromHex(cs.titleColor);
	
		for(int i = 0; i < channel.getLength(); i++){	
		
			Node node0 = channel.item(i);	
			NodeList nodeList0 = node0.getChildNodes();
			
			for(int ii = 0; ii < nodeList0.getLength(); ii++){
	
				if(ii >= 20){ continue; }

				Node node1 = nodeList0.item(ii);
				Node tmp = node1.getChildNodes().item(0);
				
				if(tmp == null){
					continue;
				}

				if(node1.getNodeName().equals("title")){
					title = tmp.getNodeValue().trim();
				}

				if(node1.getNodeName().equals("link")){
					link = tmp.getNodeValue().trim();
				}
					
				if(node1.getNodeName().equals("description")){
					description = tmp.getNodeValue().trim();
				}
					
				if(node1.getNodeName().equals("copyright")){
					copyright = tmp.getNodeValue().trim();
				}
					
				if(node1.getNodeName().equals("pubDate")){
					pubDate = tmp.getNodeValue().trim();
				}

				NodeList nodeList1 = node1.getChildNodes();
				
				String fTitle = null;
				String fDesc = null;
				String fPubDate = null;
				String fLink = null;
				String fAuthor = null;
				
				for(int iii = 0; iii < nodeList1.getLength(); iii++){
					
					Node node2 = nodeList1.item(iii);
					tmp = node2.getChildNodes().item(0);
			
					if(node2.getNodeName().equals("title")){
						fTitle = tmp.getNodeValue().trim();
						Log.d(ViewBuilder.class.getName(), fTitle);
					}
	
					if(node2.getNodeName().equals("link")){
						fLink = tmp.getNodeValue().trim();
						Log.d(ViewBuilder.class.getName(), tmp.getNodeValue().trim());
					}
				
					if(node2.getNodeName().equals("author")){
						fAuthor = tmp.getNodeValue().trim();
						Log.d(ViewBuilder.class.getName(), tmp.getNodeValue().trim());
					}

					if(node2.getNodeName().equals("pubDate")){
						fPubDate = tmp.getNodeValue().trim();
						Log.d(ViewBuilder.class.getName(), tmp.getNodeValue().trim());
					}
				}	
					
				if(null != fTitle){
					items += "<br/>";
					items += "<a href='"+fLink+"' style='text-decoration: none; color:"+titleColor+" font-size: 16px;'>";
					items += fTitle;
					items += "</a>";
					items += "<br/>";
				}
			}	
		}
	
		res += "<style type='text/css'>";	
		res += "#rounded{ padding: 4px; padding-top: 8px; padding-bottom: 8px; background: "+bgColor+"; -webkit-border-radius: 4px; -moz-border-radius: 4px; border-radius: 4px; }";
		res += "td{ vertical-align: top; text-align: left; align: left; }";
		res += "</style>";
		res += "<div id='rounded'>";
		res += "<span style='font-size: 12px; color: "+titleColor+";'>"+title+"</span>";
		res += "<br/>";
		res += "<span style='font-size: 12px; color: "+titleColor+";'>"+link+"</span>";
		res += "<br/>";
		res += "<span style='font-size: 12px; color: "+titleColor+";'>"+items+"</span>";
		res += "</div>";
		
		target.loadDataWithBaseURL(null, res, "text/html", "utf-8", null);
	}

	/* ****
	   ''''
		Re - Build of RWDateTimeCell 
			after iTunes Result
	 */	
	protected void renderiTunesResult(JSONArray ary)
	{
		String artist = "";
		String song = "";
		String collection = "";
		String artwork = "";		
	
		// Parses first entry (which probably fits most...	
		// for(int i = 0; i < ary.length(); i++){
		for(int i = 0; i < 1; i++){
			String res;
			try{
				// -- 		
				artist = ary.getJSONObject(i).getString("artistName");
				collection = ary.getJSONObject(i).getString("collectionName");
				artwork = ary.getJSONObject(i).getString("artworkUrl100");
				song = ary.getJSONObject(i).getString("trackName");
				// -- 		
				Log.d(AppwerkService.class.getName(), "renderiTunesResult(): " +artist);
				Log.d(AppwerkService.class.getName(), "renderiTunesResult(): " +collection);
				Log.d(AppwerkService.class.getName(), "renderiTunesResult(): " +artwork);
				Log.d(AppwerkService.class.getName(), "renderiTunesResult(): " +song);
				// -- 		
			}
			catch(JSONException e){
				e.printStackTrace();
			}
		}

		this.songTitle = collection;
		
		ColorStruct cs = this.colorStructs.get(this.selectedStream);
		
		String titleColor = this.getCSSRGBAFromHex(cs.titleColor);
		String bgColor = this.getCSSRGBAFromHex(cs.bgColor);
		String labelColor = this.getCSSRGBAFromHex(cs.titleColor);

		String tmp = "";
		tmp += "<style type='text/css'>";	
		tmp += "#rounded{ padding: 4px; padding-top: 8px; padding-bottom: 8px; background: "+bgColor+"; -webkit-border-radius: 4px; -moz-border-radius: 4px; border-radius: 4px; }";
		tmp += "td{ vertical-align: top; text-align: left; align: left; }";
		tmp += "</style>";
		tmp += "<div id='rounded'>";
		
		tmp += "<table>";
		
		tmp += "<tr>";
		tmp += "<td><img src='"+artwork+"'/></td>";
		tmp += "<td style='width:100%'><span style='font-size:16px; color:"+titleColor+";'>"+artist+"</span></td>";
		tmp += "</tr>";
		
		tmp += "<tr>";
		tmp += "<td colspan='2'>";
		tmp += "<span style='font-size:16px; color:"+titleColor+";'>"+collection+"</span><br><span style='font-size:16px; color:"+titleColor+";'>"+song+"</span></td>";
		tmp += "</tr>";
		
		tmp += "</table>";

		/*
		tmp += "<hr>";
		tmp += "<span style='font-size:16px; color:"+titleColor+";'>Bewerte diesen Beitrag</span>";
		tmp += "</div>";
		*/

		DynView vv = this.RWDetailCells.get(this.selectedStream);

		for(int i = 0; i < vv.getChildCount(); i++){
			View currentChild = vv.getChildAt(i);
			if(currentChild instanceof WebView){
				WebView webView = (WebView)vv.getChildAt(i);
				webView.loadDataWithBaseURL(null, tmp, "text/html", "utf-8", null);
			}
		}
	}

	protected void buildFromDefaultJSON()
	{
		Log.d(ViewBuilder.class.getName(), "buildFromDefaultJSON()");	
	
		this.formats = this.getDefaultJSON();
		this.buildAppView(this.formats);
	}
	
	protected ArrayList<String> titles;
	protected ArrayList<String> streamURLs;
	
	protected ArrayList<DynView> RWDateTimeCells;
	protected ArrayList<DynView> RWRSSCells;
	protected ArrayList<DynView> RWSocialCells;
	protected ArrayList<DynView> RWWebLinkCells;
	protected ArrayList<DynView> RWDetailCells;
	protected ArrayList<DynView> RWNowPlayingRatingCells;
	
	protected JSONObject configJSON;
	
	protected ArrayList<DynView> DynViews = new ArrayList<DynView>();
	
	protected void buildAppView(JSONObject jsonObj)
	{
		Log.d(ViewBuilder.class.getName(), "buildAppView():" +jsonObj);

		this.configJSON = jsonObj;
	
		// fix disss
		int size = 12;
		
		this.titles = new ArrayList<String>();
		this.streamURLs = new ArrayList<String>();
		
		this.RWDateTimeCells = new ArrayList<DynView>(size);
		this.RWRSSCells = new ArrayList<DynView>(size);
		this.RWWebLinkCells = new ArrayList<DynView>(size);
		this.RWDetailCells = new ArrayList<DynView>(size);
		this.RWNowPlayingRatingCells = new ArrayList<DynView>(size);
		this.RWSocialCells = new ArrayList<DynView>(size);

		for(int i = 0; i < size; i++){
			this.RWDateTimeCells.add(new DynView(this.context));
			this.RWRSSCells.add(new DynView(this.context));
			this.RWWebLinkCells.add(new DynView(this.context));
			this.RWDetailCells.add(new DynView(this.context));
			this.RWNowPlayingRatingCells.add(new DynView(this.context));
		}

		// Catches Formats
		JSONArray radios = new JSONArray();	
		try{
			radios = jsonObj.getJSONArray("formats");
		}
		catch(Exception e){
			e.printStackTrace();
		}
	
		if(null == radios){
			return;
		}	
	
		DynView dynView;
		DynView t;
	
		// Sets up Page Adapter	
		String title = new String();	
		for(int i = 0; i < radios.length(); i++){
			try{
				title = (String)radios.getJSONObject(i).getString("title");
			}
			catch(JSONException e){
				e.printStackTrace();
			}
			this.titles.add(title);
		}
		
		this.pageAdapter = new MPageAdapter(this.titles, this.fragmentManager);
		this.pager.setAdapter(this.pageAdapter);
	
		// Loops through "Screens"
		// Fills Page Adapter
		for(int k = 0; k < radios.length(); k++){
				
			this.setBackgroundColors(k, jsonObj);
			dynView = new DynView(this.context);
			
			JSONObject tmp = new JSONObject();
			JSONArray ftr = new JSONArray();	
			try{
				tmp = (JSONObject)radios.getJSONObject(k);
				ftr = (JSONArray)tmp.getJSONArray("table-cells");
			}
			catch(JSONException e){
				e.printStackTrace();
			}

			// Fetches Icon, BGImage, Stream....
			title = this.titles.get(k); 
			String stream = new String();
			String icon = new String();
			String bgImage = new String();
			try{
				stream = tmp.getString("stream"); 
				bgImage = tmp.getString("bgImage"); 
				icon = tmp.getString("icon"); 
			}
			catch(JSONException e){
				e.printStackTrace();
			}
			
			// Copies 
			this.streamURLs.add(stream);

			Log.d(ViewBuilder.class.getName(), "stream:" +stream);
			Log.d(ViewBuilder.class.getName(), "bgImage:" +bgImage);
			Log.d(ViewBuilder.class.getName(), "icon:" +icon);
			Log.d(ViewBuilder.class.getName(), "title:" +title);

			// Fetches Clazzes
			String clazz = new String();
			// Loops through Features
			for(int m = 0; m < ftr.length(); m++){
				
				try{	
					tmp = (JSONObject)ftr.getJSONObject(m);
				}
				catch(JSONException e){
					e.printStackTrace();
				}
			
				clazz = "";
				try{	
					clazz = tmp.getString("class");
				}
				catch(JSONException e){
					e.printStackTrace();
				}
				
				if(clazz.equals("RWDateTimeCell")){
					t = this.buildRWDateTimeCell(tmp, k);
					dynView.addView(t);
					this.RWDateTimeCells.set(k, t);
				}
		
				if(clazz.equals("RWNowPlayingCell")){
					t = this.buildRWDetailCell(tmp, k);
					dynView.addView(t);
					this.RWDetailCells.set(k, t);
				}
	
				if(clazz.equals("RWNowPlayingRatingCell")){
					t = this.buildRWNowPlayingRatingCell(tmp, k);
					dynView.addView(t);
					this.RWNowPlayingRatingCells.set(k, t);
				}	
					
				if(clazz.equals("RWWebLinkCell")){
					t = this.buildRWWebLinkCell(tmp, k);
					dynView.addView(t);
					this.RWWebLinkCells.set(k, t);
				}
						
				if(clazz.equals("RWRSSCell")){
					t = this.buildRWRSSCell(tmp, k);
					dynView.addView(t);
					this.RWRSSCells.set(k, t);
				}

				if(clazz.equals("RWSocialCell")){
					t = this.buildRWSocialCell(tmp, k);
					dynView.addView(t);
					this.RWSocialCells.add(t);
				}

			}	
			/*
			catch(JSONException e){
				e.printStackTrace();
			}
			*/
	
			// Additional Items	
			
			t = this.buildLikeView(jsonObj, k);	
			dynView.addView(t);
				
			t = this.buildVoiceRecView(jsonObj, k);	
			dynView.addView(t);
				
			t = this.buildSnapshotView(jsonObj, k);	
			dynView.addView(t);

			t = this.buildSitmarkView(jsonObj, k);
			dynView.addView(t);

			t = this.buildCommentView(jsonObj, k);
			dynView.addView(t);

			t = this.buildAdView(jsonObj, k);
			dynView.addView(t);

			this.DynViews.add(k, dynView);
				
			this.pageAdapter.addComp(title, dynView);
		}

		this.setBackgroundImage();
	}

	class ColorStruct 
	{
		protected String bgColor;
		protected String frameColor;
		protected String separatorColor;
		protected String titleColor;
		protected String labelColor;

		public ColorStruct()
		{
		}
		
		public ColorStruct(String bgColor, String frameColor, String separatorColor, String titleColor, String labelColor)
		{
			this.bgColor = bgColor;
			this.frameColor = frameColor;
			this.separatorColor = separatorColor;
			this.titleColor = titleColor;
			this.labelColor = labelColor;
		}
	}	
	
	protected ArrayList<ColorStruct> colorStructs = new ArrayList<ColorStruct>();
	protected void setBackgroundColors(int page, JSONObject jsonObj)
	{
		String title = new String();

		JSONArray channels = new JSONArray();
		JSONObject channel = new JSONObject();
		JSONObject tcd = new JSONObject();	

		ColorStruct cs = new ColorStruct();
		try{
			channels = jsonObj.getJSONArray("formats");
			channel = channels.getJSONObject(page);
			tcd = channel.getJSONObject("table-cell-display");
			
			cs.bgColor = tcd.getString("bgcolor");
			cs.frameColor = tcd.getString("frameColor");
			cs.separatorColor = tcd.getString("separatorColor");
			cs.titleColor = tcd.getString("titleColor");
			cs.labelColor = tcd.getString("labelColor");
		}
		catch(Exception e){
			e.printStackTrace();
		}

		Log.d(ViewBuilder.class.getName(), "setBackgroundColors(): " +cs.bgColor);
		Log.d(ViewBuilder.class.getName(), "setBackgroundColors(): " +cs.frameColor);
		Log.d(ViewBuilder.class.getName(), "setBackgroundColors(): " +cs.separatorColor);
		Log.d(ViewBuilder.class.getName(), "setBackgroundColors(): " +cs.titleColor);
		Log.d(ViewBuilder.class.getName(), "setBackgroundColors(): " +cs.labelColor);

		// Copy
		this.colorStructs.add(page, cs);	
	}

	// bollo style RGBA Value...
	protected String getCSSRGBAFromHex(String hex)
	{
		Log.d(ViewBuilder.class.getName(), "getCSSRGBAFromHex():" +hex);
		
		String res = "";

		try{
			String r = hex.substring(0, 2);
			String g = hex.substring(2, 4);
			String b = hex.substring(4, 6);
			String a = hex.substring(6, 8);
	
			int R = Integer.valueOf(r, 16);	
			int G = Integer.valueOf(g, 16);	
			int B = Integer.valueOf(b, 16);	
		
			int tmp = Integer.valueOf(a, 16);
		
			float A = (float)tmp /255;
	
			res = "rgba(" +R +", " +G +", " +B +", " +A +");";
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return res;
	}

	protected void setBackgroundImage()
	{
		String url = new String("");

		JSONObject jsonObj = this.formats;

		JSONArray formats = new JSONArray();
		JSONObject channel = new JSONObject();

		try{
			formats = jsonObj.getJSONArray("formats");
			channel = formats.getJSONObject(this.selectedPage);
			url = channel.getString("bgimage");
		}
		catch(Exception e){
			e.printStackTrace();
		}

		Drawable l = this.loadAsset(url);
		this.activity.findViewById(R.id.main).setBackgroundDrawable(l);
	}
	
	protected Bitmap cropBitmap(Bitmap bitmap, int orientation)
	{
		if(null == bitmap){
			Bitmap res = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
			return res;
		}
		
		WindowManager wm = (WindowManager)this.context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		
		double dw = display.getWidth();
		double dh = display.getHeight();
		
		double bw = bitmap.getWidth();
		double bh = bitmap.getHeight();

		double rr = dw /bw;
		
		double rw = bw *rr;
		double rh = bh *rr;	

		Bitmap copy = Bitmap.createScaledBitmap(bitmap, (int)rw, (int) rh, false);
		Bitmap res = Bitmap.createBitmap(copy, 0, 0, (int)dw, (int)dh);

		if(orientation == Configuration.ORIENTATION_LANDSCAPE){
			rr = dh /bw;
			rw = bw *rr;
			rh = bh *rr;	
			copy = Bitmap.createScaledBitmap(bitmap, (int)rw, (int) rh, false);
			res = Bitmap.createBitmap(copy, 0, 0, (int)dh, (int)dw);
		}
		
		return res;
	}

	protected BitmapDrawable backgroundDrawableP;
	protected BitmapDrawable backgroundDrawableL;
	protected Drawable loadAsset(String iUrl)
	{
		this.backgroundDrawableP = new BitmapDrawable();
		this.backgroundDrawableL = new BitmapDrawable();

		try{	
			HttpURLConnection connection = (HttpURLConnection)new URL(iUrl).openConnection();
			connection.connect();
			
			InputStream input = connection.getInputStream();
			
			Bitmap bp;
			bp = BitmapFactory.decodeStream(input);
	
			Bitmap rp;
			rp = this.cropBitmap(bp, Configuration.ORIENTATION_PORTRAIT);
			this.backgroundDrawableP.setGravity(Gravity.TOP|Gravity.LEFT);
			this.backgroundDrawableP = this.mBlur(rp);
			
			rp = this.cropBitmap(bp, Configuration.ORIENTATION_LANDSCAPE);
			this.backgroundDrawableL.setGravity(Gravity.TOP|Gravity.LEFT);
			this.backgroundDrawableL = this.mBlur(rp);
		}
		catch(IOException e){
			e.printStackTrace();
		}

		BitmapDrawable res = this.backgroundDrawableL;
		if(this.activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
			res = backgroundDrawableP;
		}

		return res;
	}
	
	protected BitmapDrawable mBlur(Bitmap bitmap)
	{
		if(null == bitmap){
			return new BitmapDrawable(Bitmap.createBitmap(1, 1, Config.ARGB_8888));
		}
		
		BitmapDrawable bd = new BitmapDrawable(this.blur.transform(bitmap, 12));
		
		if(null == bd){
			bd = new BitmapDrawable(bitmap);
		}
		return bd;
	}

	public void orientationChanged(int orientation)
	{
		if(orientation == Configuration.ORIENTATION_PORTRAIT){
			this.activity.findViewById(R.id.main).setBackgroundDrawable(this.backgroundDrawableP);
		}
		
		if(orientation == Configuration.ORIENTATION_LANDSCAPE){
			this.activity.findViewById(R.id.main).setBackgroundDrawable(this.backgroundDrawableL);
		}
	} 

	protected DynView buildRWSocialCell(JSONObject jsonObj, int page)
	{
		Log.d(ViewBuilder.class.getName(), "buildRWSocialCell():" +jsonObj);

		String icon = "";
		String title = "";
		String label = "";
		String url = "";

		try{
			// ....
			icon = jsonObj.getString("adminIcon");
			label = jsonObj.getString("label");
			title = jsonObj.getString("title");
			url = jsonObj.getString("url");
			// ....
			Log.d(ViewBuilder.class.getName(), "buildRWSocialCell(): icon: " +icon);
			Log.d(ViewBuilder.class.getName(), "buildRWSocialCell(): label:" +label);
			Log.d(ViewBuilder.class.getName(), "buildRWSocialCell(): title: " +title);
			Log.d(ViewBuilder.class.getName(), "buildRWSocialCell(): url: " +url);
		}
		catch(JSONException e){
			e.printStackTrace();
		}

		RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.FILL_PARENT, 
			RelativeLayout.LayoutParams.FILL_PARENT 
		);
		p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);	

		ColorStruct cs = this.colorStructs.get(page);
		String bgColor = this.getCSSRGBAFromHex(cs.bgColor);		
		String titleColor = this.getCSSRGBAFromHex(cs.titleColor);
		String labelColor = this.getCSSRGBAFromHex(cs.titleColor);
	
		DynView v = new DynView(this.context);
		
		WebView wv = new WebView(this.context);	
		wv.setId(this.generateViewId());
		wv.setScrollContainer(true);
		wv.setBackgroundColor(0x00000000);
		
		this.linkRWSocialLinkCell(wv.getId(), url);	

		String tmp = "";
		tmp += "<style type='text/css'>";	
		tmp += "#rounded{ padding: 4px; padding-top: 8px; padding-bottom: 8px; background: "+bgColor+"; -webkit-border-radius: 4px; -moz-border-radius: 4px; border-radius: 4px; }";
		tmp += "</style>";

		tmp += "<div id='rounded'>";	
		tmp += "<table>";
		tmp += "<tr>";
		tmp += "<td style='width: 100%'>";
		tmp += "<img style='max-height: 82px;' src='"+icon+"'/>";
		tmp += "<br/>";
		tmp += "<span style='font-size:16px; color:"+titleColor+";'>"+label+"</span>";
		tmp += "</td>";
		tmp += "<td style='font-size:16px; color:"+titleColor+"'>";
		tmp += ">";
		tmp += "</td>";
		tmp += "</tr>";
		tmp += "</table>";	
	
		tmp += "</div>";

		wv.setOnTouchListener(new View.OnTouchListener(){
			public boolean onTouch(View view, MotionEvent me){
				switch(me.getAction()){
					case MotionEvent.ACTION_DOWN:
						rollover(view);
						break;
					case MotionEvent.ACTION_UP:
						rollout(view);
						loadSocialLink(view.getId());;
						break;
				}
				return false;
			}
		});	

		wv.loadDataWithBaseURL(null, tmp, "text/html", "utf-8", null);	
		wv.setLayoutParams(p);
		
		v.addView(wv);
	
		return v;
	}
	
	protected DynView buildRWNowPlayingRatingCell(JSONObject jsonObj, int page)
	{
		DynView v = new DynView(this.context);
		
		RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.FILL_PARENT, 
			RelativeLayout.LayoutParams.FILL_PARENT 
		);
		p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);	

		ColorStruct cs = this.colorStructs.get(page);
		String bgColor = this.getCSSRGBAFromHex(cs.bgColor);		
		String titleColor = this.getCSSRGBAFromHex(cs.titleColor);
		String labelColor = this.getCSSRGBAFromHex(cs.titleColor);
	
		WebView wv = new WebView(this.context);	
		wv.setId(this.generateViewId());
		wv.setScrollContainer(true);
		wv.setBackgroundColor(0x00000000);

		String tmp = "";
		tmp += "<style type='text/css'>";	
		tmp += "#rounded{ padding: 4px; padding-top: 8px; padding-bottom: 8px; background: "+bgColor+"; -webkit-border-radius: 4px; -moz-border-radius: 4px; border-radius: 4px; }";
		tmp += "</style>";
		tmp += "<div id='rounded'>";	
		tmp += "<span style='font-size:16px; color:"+titleColor+";'>Bewerte diesen Beitrag</span>";
		tmp += "</div>";

		wv.loadDataWithBaseURL(null, tmp, "text/html", "utf-8", null);	
		wv.setLayoutParams(p);
		
		v.addView(wv);
	
		return v;
	}
	
	protected DynView buildLikeView(JSONObject jsonObj, int page)
	{
		DynView v = new DynView(this.context);

		RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.FILL_PARENT, 
			RelativeLayout.LayoutParams.FILL_PARENT 
		);
		p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);	

		ColorStruct cs = this.colorStructs.get(page);
		String bgColor = this.getCSSRGBAFromHex(cs.bgColor);		
		String titleColor = this.getCSSRGBAFromHex(cs.titleColor);
		String labelColor = this.getCSSRGBAFromHex(cs.titleColor);
	
		String tmp = "";
		tmp += "<style type='text/css'>";	
		tmp += "#rounded{ padding: 4px; padding-top: 8px; padding-bottom: 8px; background: "+bgColor+"; -webkit-border-radius: 4px; -moz-border-radius: 4px; border-radius: 4px; }";
		tmp += "</style>";
		tmp += "<div id='rounded'>";	
		tmp += "<span style='font-size:16px; color:"+titleColor+";'>Vote</span>";
		tmp += "</div>";

		WebView wv = new WebView(this.context);	
		wv.setId(this.generateViewId());
		wv.setScrollContainer(true);
		wv.setBackgroundColor(0x00000000);

		wv.setOnTouchListener(new View.OnTouchListener(){
			public boolean onTouch(View view, MotionEvent me){
				switch(me.getAction()){
				
				case MotionEvent.ACTION_DOWN:
					rollover(view);
					break;
				
				case MotionEvent.ACTION_UP:
					rollout(view);
					openVoteDialog();
					break;
				}
				
				return false;
			}
		});	

		wv.loadDataWithBaseURL(null, tmp, "text/html", "utf-8", null);	
		wv.setLayoutParams(p);
		
		v.addView(wv);
	
		return v;
	}

	protected void openVoteDialog()
	{
		AlertDialog.Builder ad = new AlertDialog.Builder(this.activity);	
		
		ad.setTitle("Vote Dialog");
		ad.setMessage("Vote");
	
		this.commentInput = new EditText(this.activity);
		
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.MATCH_PARENT
		);
		
		this.commentInput.setLayoutParams(lp);
		
		ad.setView(this.commentInput);
		ad.setPositiveButton("I like this song",
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which){
					Toast.makeText(context, "Vote sent", Toast.LENGTH_SHORT).show();
					sendLikeReq();
				}	
			}
		);
		
		ad.setNegativeButton("Cancel",
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which){
					dialog.cancel();
				}
			}
		);
		
		ad.show();
	}

	protected void loadSocialLink(int id)
	{
		String url = this.context.getString(R.string.social_url);
		String rid = this.context.getString(R.string.radio_config_rid);
		url += "?rid=" +rid;
		Log.d(ViewBuilder.class.getName(), "loadSocialLink():" +url);
		
		if(null != url){if(!url.equals("")){
			this.loadWebLink(url);
		}}
	}
	
	protected void loadWebLink(int id)
	{
		String url = this.links.get(new Integer(id));
		if(null != url){ 
			if(!url.equals("")){
				this.loadWebLink(url);
			}
		}
	}

	protected Map<Integer, String> links = new HashMap<Integer, String>();	
	protected void linkRWWebLinkCell(int id, String url)
	{
		this.links.put(new Integer(id), url);
	}

	protected Map<Integer, String> socialLinks = new HashMap<Integer, String>();	
	protected void linkRWSocialLinkCell(int id, String url)
	{
		this.socialLinks.put(new Integer(id), url);
	}

	protected DynView buildRWWebLinkCell(JSONObject feat, int page)
	{
		Log.d(ViewBuilder.class.getName(), "buildRWWebLinkCell():" +feat);
			
		String icon = "";
		String url = "";
		String title = "";
		String label = "";

		try{
			icon = feat.getString("icon");
			label = feat.getString("label");
			url = feat.getString("url");
			title = feat.getString("title");
		
			Log.d(ViewBuilder.class.getName(), "buildRWWebLinkCell(): icon: " +icon);
			Log.d(ViewBuilder.class.getName(), "buildRWWebLinkCell(): label:" +label);
			Log.d(ViewBuilder.class.getName(), "buildRWWebLinkCell(): title: " +title);
			Log.d(ViewBuilder.class.getName(), "buildRWWebLinkCell(): url: " +url);
		}
		catch(JSONException e){
			e.printStackTrace();
		}

		WebView wv = new WebView(this.context);	
		wv.setId(this.generateViewId());
		
		this.linkRWWebLinkCell(wv.getId(), url);	
		
		wv.setScrollContainer(true);
		wv.setBackgroundColor(0x00000000);

		wv.setOnTouchListener(new View.OnTouchListener(){
		
			public boolean onTouch(View view, MotionEvent me){
		
				switch(me.getAction()){
				
					case MotionEvent.ACTION_DOWN:
						rollover(view);
						break;
				
					case MotionEvent.ACTION_UP:
						rollout(view);
						loadWebLink(view.getId());
						break;
				}
				
				return false;
			}
		});	

		RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.FILL_PARENT, 
			RelativeLayout.LayoutParams.FILL_PARENT 
		);
		p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);	

		ColorStruct cs = this.colorStructs.get(page);
		String bgColor = this.getCSSRGBAFromHex(cs.bgColor);		
		String titleColor = this.getCSSRGBAFromHex(cs.titleColor);
		String labelColor = this.getCSSRGBAFromHex(cs.titleColor);
	
		String tmp = "";
		tmp += "<style type='text/css'>";	
		tmp += "#rounded{ padding: 4px; padding-top: 8px; padding-bottom: 8px; background: "+bgColor+"; -webkit-border-radius: 4px; -moz-border-radius: 4px; border-radius: 4px; }";
		tmp += "</style>";
		
		tmp += "<div id='rounded'>";	
		tmp += "<table>";
		tmp += "<tr>";
		tmp += "<td style='width: 100%'>";
		tmp += "<img style='max-height: 82px;' src='"+icon+"'/>";
		tmp += "<br/>";
		tmp += "<span style='font-size:16px; color:"+titleColor+";'>"+title+"</span>";
		tmp += "</td>";
		tmp += "<td style='font-size:16px; color:"+titleColor+"'>";
		tmp += ">";
		tmp += "</td>";
		tmp += "</tr>";
		tmp += "</table>";	
	
		tmp += "</div>";

		wv.loadDataWithBaseURL(null, tmp, "text/html", "utf-8", null);	
		wv.setLayoutParams(p);
		
		DynView v = new DynView(this.context);
		v.addView(wv);

		return v;
	}

	protected String rssFeedUrl;	
	protected DynView buildRWRSSCell(JSONObject feat, int page)
	{
		Log.d(ViewBuilder.class.getName(), "buildRWRSSCell():" +feat);

		String title = "";
		String url = "";
		String label = "";
		String icon = "";

		try{
			label = feat.getString("label");
			url = feat.getString("url");
			icon = feat.getString("icon");
			title = feat.getString("title");
			Log.d(ViewBuilder.class.getName(), "buildRWRSSCell(): label:" +label);
			Log.d(ViewBuilder.class.getName(), "buildRWRSSCell(): url: " +url);
			Log.d(ViewBuilder.class.getName(), "buildRWRSSCell(): icon: " +icon);
			Log.d(ViewBuilder.class.getName(), "buildRWRSSCell(): title: " +title);
		}
		catch(JSONException e){
			e.printStackTrace();
		}
	
		url = "" +url;	
		this.rssFeedURLs.add(url);
		
		label = TextUtils.htmlEncode(label);
		url = TextUtils.htmlEncode(url);
		icon = TextUtils.htmlEncode(icon);
		title = TextUtils.htmlEncode(title);

		WebView wv = new WebView(this.context);	
		wv.setId(this.generateViewId());
		
		this.linkRWWebLinkCell(wv.getId(), url);	
		
		wv.setScrollContainer(true);
		wv.setBackgroundColor(0x00000000);

		wv.setOnTouchListener(new View.OnTouchListener(){
			public boolean onTouch(View view, MotionEvent me){
				switch(me.getAction()){
					case MotionEvent.ACTION_DOWN:
						rollover(view);
						break;
					case MotionEvent.ACTION_UP:
						rollout(view);
						loadRSSFeed();
						break;
				}
				return false;
			}
		});	

		RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.FILL_PARENT, 
			RelativeLayout.LayoutParams.FILL_PARENT 
		);
		p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);	

		ColorStruct cs = this.colorStructs.get(page);
		
		String titleColor = this.getCSSRGBAFromHex(cs.titleColor);
		String bgColor = this.getCSSRGBAFromHex(cs.bgColor);		
		String labelColor = this.getCSSRGBAFromHex(cs.titleColor);
	
		String tmp = "";
		tmp += "<style type='text/css'>";	
		tmp += "#rounded{ padding: 4px; padding-top: 8px; padding-bottom: 8px; background: "+bgColor+"; -webkit-border-radius: 4px; -moz-border-radius: 4px; border-radius: 4px; }";
		tmp += "</style>";
		tmp += "<div id='rounded'>";	
		tmp += "<span style='font-size:16px; color:"+titleColor+";'>"+title+"</span>";
		tmp += "<br/>";
		tmp += "<img style='max-height: 82px;' src='"+icon+"'/>";
		tmp += "</div>";

		wv.loadDataWithBaseURL(null, tmp, "text/html", "utf-8", null);	
		wv.setLayoutParams(p);
		
		DynView v = new DynView(this.context);
		v.addView(wv);

		WebView rv = new WebView(this.context);
		tmp = "";
		tmp += "<style type='text/css'>";
		tmp += "#rounded{ padding: 4px; padding-top: 8px; padding-bottom: 8px; background: "+bgColor+"; -webkit-border-radius: 4px; -moz-border-radius: 4px; border-radius: 4px; }";
		tmp += "</style>";
		tmp += "<div id='rounded'></div>";
		
		rv.setScrollContainer(true);
		rv.setBackgroundColor(0x00000000);
		rv.loadDataWithBaseURL(null, tmp, "text/html", "utf-8", null);
		
		v.addView(rv);

		return v;
	}

	/* ***
	 '''''
		Initial build of the RWDateTimeCell
	 */
	protected ArrayList<ImageButton> RWDateTimeCellArtistImages = new ArrayList<ImageButton>();
	protected ArrayList<TextView> RWDateTimeCellArtistNames = new ArrayList<TextView>();
	protected ArrayList<TextView> RWDateTimeCellArtistCollections = new ArrayList<TextView>();

	protected DynView buildRWDetailCell(JSONObject feat, int page)
	{
		DynView v = new DynView(this.context);
	
		ImageButton b;
		TextView t1;
		TextView t2;

		String icon = "";
		String artist = "";
		String collection = "";

		try{
			icon = feat.getString("icon");
			artist = feat.getString("artist");
			collection = feat.getString("collection");
		}
		catch(JSONException e){
			e.printStackTrace();
		}

		artist = TextUtils.htmlEncode(artist);
		collection = TextUtils.htmlEncode(collection);

		WebView wv = new WebView(this.context);	
		
		wv.setId(this.generateViewId());
		wv.setScrollContainer(true);
		wv.setBackgroundColor(0x00000000);

		ColorStruct cs = this.colorStructs.get(page);
		
		String bgColor = this.getCSSRGBAFromHex(cs.bgColor);		
		String titleColor = this.getCSSRGBAFromHex(cs.titleColor);
		String labelColor = this.getCSSRGBAFromHex(cs.titleColor);
		
		String tmp = "";
		tmp += "<style type='text/css'>";	
		tmp += "#rounded{ padding: 4px; padding-top: 8px; padding-bottom: 8px; background: "+bgColor+"; -webkit-border-radius: 4px; -moz-border-radius: 4px; border-radius: 4px; }";
		tmp += "</style>";
		tmp += "<div id='rounded'>";	
		tmp += "<img width='82px' src=''/>";
		tmp += "<br/>";
		tmp += "<span style='font-size:16px; color:"+titleColor+";'>"+artist+"</span>";
		tmp += "<br/>";
		tmp += "<span style='font-size:16px; color:"+titleColor+";'>"+collection+"</span>";
		tmp += "</div>";

		wv.loadDataWithBaseURL(null, tmp, "text/html", "utf-8", null);	
		
		v.addView(wv);

		return v;
	}	

	protected DynView buildAdView(JSONObject feat, int page)
	{
		Log.d(ViewBuilder.class.getName(), "buildAdView()");
		
		DynView v = new DynView(this.context);
		WebView wv = new WebView(this.context);	
		
		v.addView(wv);
		
		return v;
	}
	
	protected DynView buildRWDateTimeCell(JSONObject feat, int page)
	{
		Log.d(ViewBuilder.class.getName(), "buildRWDateTimeCell()");

		JSONObject jsonObj = this.formats;

		JSONArray formats = new JSONArray();
		JSONObject channel = new JSONObject();

		String icon = new String();
		String title = new String();
		String desc = new String();
		try{
			formats = jsonObj.getJSONArray("formats");
			channel = formats.getJSONObject(page);
			icon = channel.getString("icon");
			title = channel.getString("title");
			desc = channel.getString("subtitle");
		}
		catch(Exception e){
			e.printStackTrace();
		}

		DynView v = new DynView(this.context);
		WebView wv = new WebView(this.context);	
		
		wv.setOnTouchListener(new View.OnTouchListener(){
			public boolean onTouch(View view, MotionEvent me){
				
				switch(me.getAction()){
				
				case MotionEvent.ACTION_DOWN:
					rollover(view);
					break;
				
				case MotionEvent.ACTION_UP:
					rollout(view);
					startStream();
					break;
				}
				
				return false;
			}
		});	
		
		wv.setId(this.generateViewId());
		
		wv.setScrollContainer(true);
		wv.setBackgroundColor(0x00000000);

		RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.FILL_PARENT, 
			RelativeLayout.LayoutParams.FILL_PARENT 
		);
		p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);	

		SimpleDateFormat df = new SimpleDateFormat("EEEE, MM.dd.yyyy", Locale.GERMAN);	
		String dff = df.format(new Date());
		
		SimpleDateFormat mf = new SimpleDateFormat("hh:mm:ss", Locale.GERMAN);	
		String mff = mf.format(new Date());

		ColorStruct cs = this.colorStructs.get(page);
		
		String bgColor = this.getCSSRGBAFromHex(cs.bgColor);		
		String titleColor = this.getCSSRGBAFromHex(cs.titleColor);		
		String labelColor = this.getCSSRGBAFromHex(cs.titleColor);		
		
		String tmp = "";
		tmp += "<style type='text/css'>";	
		tmp += "#rounded{ min-height: 128px; padding: 4px; padding-top: 8px; padding-bottom: 8px; background: "+bgColor+"; -webkit-border-radius: 4px; -moz-border-radius: 4px; border-radius: 4px; }";
		tmp += "</style>";
		tmp += "<div id='rounded'>";	
		tmp += "<table width='100%'>";
		tmp += "<tr>";
		tmp += "<td>";
		tmp += "<img style='max-height: 82px;' src='"+icon+"'/>";
		tmp += "</td>";
		tmp += "<td align='right' valign='top'>";
		tmp += "<span style='font-size:16px; color:"+titleColor+"'>"+desc+"</span>";
		tmp += "<br>";
		tmp += "<span style='font-size:16px; color:"+titleColor+"'>"+dff+"</span>";
		tmp += "<br>";
		tmp += "<span style='font-size:32px; color:"+titleColor+"';>"+mff+"</span>";
		tmp += "</tr>";
		tmp += "</table>";
		tmp += "</div>";

		wv.loadDataWithBaseURL(null, tmp, "text/html", "utf-8", null);	
		wv.setLayoutParams(p);
		
		v.addView(wv);

		return v;
	}	
	
	protected TextView setupTextView()
	{
		TextView t = new TextView(this.context);
		t.setTextSize(24);
		t.setTextColor(0xffffffff);
		return t;	
	}

	protected ImageButton setupImageButton()
	{
		ImageButton b = new ImageButton(this.context);	
		b.getBackground().setAlpha(98);
		b.setPadding(0, 20, 200, 20);
		return b;
	}

	protected Button setupButton()
	{
		Button b = new Button(this.context);
		b.setTextColor(0xffffffff);
		b.setTextSize(36);
		b.getBackground().setAlpha(98);
		b.setGravity(Gravity.LEFT);
		return b;	
	}

	protected void loadWebLink(String loc)
	{
		Log.d(ViewBuilder.class.getName(), "loadWebLink():" +loc);
		this.broadcast("loadWebLinkReq", loc);
	}

	protected DynView buildVoiceRecView(JSONObject jsonObj, int page)
	{
		Log.d(ViewBuilder.class.getName(), "buildVoiceRecView:");
		
		DynView v = new DynView(this.context);
		WebView wv = new WebView(this.context);

		wv.setOnTouchListener(new View.OnTouchListener(){
			public boolean onTouch(View view, MotionEvent me){
				
				switch(me.getAction()){
				
					case MotionEvent.ACTION_DOWN:
						rollover(view);
						break;
				
					case MotionEvent.ACTION_UP:
						rollout(view);
						toggleMic();
						break;
				
				}
				return false;
			}
		});	

		wv.setId(this.generateViewId());
		
		wv.setScrollContainer(true);
		wv.setBackgroundColor(0x00000000);
		
		RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.FILL_PARENT, 
			RelativeLayout.LayoutParams.FILL_PARENT 
		);
		p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);	

		ColorStruct cs = this.colorStructs.get(page);
		String bgColor = this.getCSSRGBAFromHex(cs.bgColor);
		String titleColor = this.getCSSRGBAFromHex(cs.titleColor);
		String labelColor = this.getCSSRGBAFromHex(cs.titleColor);

		String tmp = "";
		tmp += "<style type='text/css'>";	
		tmp += "#rounded{ padding: 4px; padding-top: 8px; padding-bottom: 8px; background: "+bgColor+"; -webkit-border-radius: 4px; -moz-border-radius: 4px; border-radius: 4px; }";
		tmp += "</style>";
		tmp += "<div id='rounded'>";	
		tmp += "<span style='font-size:16px; color:"+titleColor+";'>AudioRec</span>";
		tmp += "</div>";

		wv.loadDataWithBaseURL(null, tmp, "text/html", "utf-8", null);	
		wv.setLayoutParams(p);
		
		v.addView(wv);

		// -------------- ------------- --------------
		/*	
		wv = new WebView(this.context);

		wv.setOnTouchListener(new View.OnTouchListener(){
			
			public boolean onTouch(View view, MotionEvent me){
				AlphaAnimation disco;

				switch(me.getAction()){
				
				case MotionEvent.ACTION_DOWN:
					disco = new AlphaAnimation(0.7f, 0.7f);
					disco.setDuration(0);
					disco.setFillAfter(true);
					view.startAnimation(disco);
					break;
				
				case MotionEvent.ACTION_UP:
					disco = new AlphaAnimation(1.0f, 1.0f);
					disco.setDuration(0);
					disco.setFillAfter(true);
					view.startAnimation(disco);
					sendRec();
					break;
				
				}
				return false;
			}
		});	

		wv.setId(this.generateViewId());
		
		wv.setScrollContainer(true);
		wv.setBackgroundColor(0x00000000);
		
		tmp = "";
		tmp += "<style type='text/css'>";	
		tmp += "#rounded{ padding: 4px; padding-top: 8px; padding-bottom: 8px; background: "+bgColor+"; -webkit-border-radius: 4px; -moz-border-radius: 4px; border-radius: 4px; }";
		tmp += "</style>";
		tmp += "<div id='rounded'>";	
		tmp += "<span style='font-size:16px; color:"+titleColor+";'>Send Record</span>";
		tmp += "</div>";

		wv.loadDataWithBaseURL(null, tmp, "text/html", "utf-8", null);	
		wv.setLayoutParams(p);
		
		v.addView(wv);
		*/

		return v;
	}

	protected DynView buildSnapshotView(JSONObject jsonObj, int page)
	{
		Log.d(ViewBuilder.class.getName(), "buildSnapshotView:");
		
		DynView v = new DynView(this.context);
		WebView wv = new WebView(this.context);

		wv.setOnTouchListener(new View.OnTouchListener(){
			public boolean onTouch(View view, MotionEvent me){
				
				switch(me.getAction()){
				
					case MotionEvent.ACTION_DOWN:
						rollover(view);
						break;
				
					case MotionEvent.ACTION_UP:
						rollout(view);
						takeSnapshot();
					break;
				
				}
				return false;
			}
		});	

		wv.setId(this.generateViewId());
		
		wv.setScrollContainer(true);
		wv.setBackgroundColor(0x00000000);
		
		RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.FILL_PARENT, 
			RelativeLayout.LayoutParams.FILL_PARENT 
		);
		p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);	

		ColorStruct cs = this.colorStructs.get(page);
		String bgColor = this.getCSSRGBAFromHex(cs.bgColor);
		String titleColor = this.getCSSRGBAFromHex(cs.titleColor);
		String labelColor = this.getCSSRGBAFromHex(cs.titleColor);

		String tmp = "";
		tmp += "<style type='text/css'>";	
		tmp += "#rounded{ padding: 4px; padding-top: 8px; padding-bottom: 8px; background: "+bgColor+"; -webkit-border-radius: 4px; -moz-border-radius: 4px; border-radius: 4px; }";
		tmp += "</style>";
		tmp += "<div id='rounded'>";	
		tmp += "<span style='font-size:16px; color:"+titleColor+";'>Snapshot</span>";
		tmp += "</div>";

		wv.loadDataWithBaseURL(null, tmp, "text/html", "utf-8", null);	
		wv.setLayoutParams(p);
		
		v.addView(wv);

		return v;
	}

	protected EditText commentTextView;
	protected DynView buildCommentView(JSONObject jsonObj, int page)
	{
		Log.d(ViewBuilder.class.getName(), "buildCommentView:");
		
		DynView v = new DynView(this.context);

		ColorStruct cs = this.colorStructs.get(page);
		String titleColor = this.getCSSRGBAFromHex(cs.titleColor);
		String bgColor = this.getCSSRGBAFromHex(cs.bgColor);
		String labelColor = this.getCSSRGBAFromHex(cs.titleColor);

		WebView tv = new WebView(this.context);
		tv.setId(this.generateViewId());
		tv.setId(this.generateViewId());
		tv.setScrollContainer(true);
		tv.setBackgroundColor(0x00000000);
		
		RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.FILL_PARENT, 
			RelativeLayout.LayoutParams.FILL_PARENT 
		);
		p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);	

		/*
		String tmp = "";
		tmp += "<style type='text/css'>";	
		tmp += "#rounded{ padding: 4px; padding-top: 8px; padding-bottom: 8px; background:"+bgColor+"; -webkit-border-radius: 4px; -moz-border-radius: 4px; border-radius: 4px; }";
		tmp += "</style>";
		tmp += "<div id='rounded'>";	
		tmp += "<script type='text/javascript' langauge='javascript'>function commitChanges(){window.INTERFACE.processContent(document.getElementsByTagName('input')[0].innerText);}</script>";
		tmp += "<input type='text' onchange='javascript:commitChangesi();'></input>";
		tmp += "</div>";

		tv.loadDataWithBaseURL(null, tmp, "text/html", "utf-8", null);	
		tv.setLayoutParams(p);
	
		tv.getSettings().setJavaScriptEnabled(true);
		tv.addJavascriptInterface(new MJavascriptInterface(v), "INYOURFACE");	
		v.addView(tv);
		*/

		/*
		this.commentTextView = new EditText(this.context);
		this.commentTextView.setText("Kommentari");
		v.addView(this.commentTextView);
		*/

		// ...............
		WebView wv = new WebView(this.context);
		wv.setOnTouchListener(new View.OnTouchListener(){
			
			public boolean onTouch(View view, MotionEvent me){
			
				switch(me.getAction()){
				
				case MotionEvent.ACTION_DOWN:
					rollover(view);
					break;
				
				case MotionEvent.ACTION_UP:
					rollout(view);
					openCommentDialog();
					break;
				
				}

				return false;
			}
		});	

		wv.setId(this.generateViewId());
		wv.setScrollContainer(true);
		wv.setBackgroundColor(0x00000000);

		String tmp = "";
		tmp += "<style type='text/css'>";	
		tmp += "#rounded{ padding: 4px; padding-top: 8px; padding-bottom: 8px; background:"+bgColor+"; -webkit-border-radius: 4px; -moz-border-radius: 4px; border-radius: 4px; }";
		tmp += "</style>";
		tmp += "<div id='rounded'>";	
		tmp += "<span style='font-size:16px; color:"+titleColor+";'>Comment</span>";
		tmp += "</div>";

		wv.loadDataWithBaseURL(null, tmp, "text/html", "utf-8", null);	
		wv.setLayoutParams(p);
		
		v.addView(wv);

		return v;
	}

	protected EditText commentInput; 
	protected void openCommentDialog()
	{
		AlertDialog.Builder ad = new AlertDialog.Builder(this.activity);	
		
		ad.setTitle("Comment Dialog");
		ad.setMessage("Comment");
	
		this.commentInput = new EditText(this.activity);
		
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.MATCH_PARENT
		);
		
		this.commentInput.setLayoutParams(lp);
		
		ad.setView(this.commentInput);
		ad.setPositiveButton("Send Comment",
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which){
					Toast.makeText(context, "Comment sent", Toast.LENGTH_SHORT).show();
					sendComment();
					// sendTrack("comment", "target", "desc");
				}	
			}
		);
		
		ad.setNegativeButton("Cancel",
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which){
					dialog.cancel();
					// sendTrack("comment-canceled", "target", "desc");
				}
			}
		);
		
		ad.show();
	}

	protected void sendTrack(String event, String target, String desc)
	{
		String track = "1|" +event +"|" +target +"|" +desc;
		this.broadcast("sendTrack", track);
	}
	
	protected void sendComment()
	{
		String m = this.commentInput.getText().toString();
		
		Log.d(ViewBuilder.class.getName(), "sendComment(): " +m);
		
		this.broadcast("sendComment", m);
	}
	
	protected DynView buildSitmarkView(JSONObject jsonObj, int page)
	{
		Log.d(ViewBuilder.class.getName(), "buildSitmarkView(): ");
		
		DynView v = new DynView(this.context);
		WebView wv = new WebView(this.context);

		wv.setOnTouchListener(new View.OnTouchListener(){
			public boolean onTouch(View view, MotionEvent me){
				
				switch(me.getAction()){
				
				case MotionEvent.ACTION_DOWN:
					rollover(view);
					break;
				
				case MotionEvent.ACTION_UP:
					rollout(view);
					openSitmarkDialog();
					break;
				
				}
				return false;
			}
		});	

		wv.setId(this.generateViewId());
		
		wv.setScrollContainer(true);
		wv.setBackgroundColor(0x00000000);
		
		RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.FILL_PARENT, 
			RelativeLayout.LayoutParams.FILL_PARENT 
		);
		p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);	

		ColorStruct cs = this.colorStructs.get(page);
		String bgColor = this.getCSSRGBAFromHex(cs.bgColor);
		String titleColor = this.getCSSRGBAFromHex(cs.titleColor);
		String labelColor = this.getCSSRGBAFromHex(cs.titleColor);

		String tmp = "";
		tmp += "<style type='text/css'>";	
		tmp += "#rounded{ padding: 4px; padding-top: 8px; padding-bottom: 8px; background:"+bgColor+"; -webkit-border-radius: 4px; -moz-border-radius: 4px; border-radius: 4px; }";
		tmp += "</style>";
		tmp += "<div id='rounded'>";	
		tmp += "<span style='font-size:16px; color:"+titleColor+";'>Sitmark</span>";
		tmp += "</div>";

		wv.loadDataWithBaseURL(null, tmp, "text/html", "utf-8", null);	
		wv.setLayoutParams(p);
		
		v.addView(wv);

		return v;
	}	
	
	protected String act;
	protected String arg;
	protected void broadcast(String action, String argument)
	{
		Log.d(ViewBuilder.class.getName(), "" +action +":" +argument);
		
		this.act = action;
		this.arg = argument;
		
		Thread t = new Thread()
		{
			@Override public void run()
			{
				Intent i = new Intent();
				i.setAction(ViewBuilder.ACTION_REQUESTED);
				i.putExtra("act", act);
				i.putExtra("arg", arg);
				context.sendBroadcast(i);
			}
		};
		
		t.start();
	}

	// Outlets of the view
	protected int selectedStream = 0;
	protected void startStream()
	{
		String url = this.streamURLs.get(this.selectedPage);
		
		if(null == url){
			return;
		}

		this.startStream(url);
	}
	
	protected void startStream(String url)
	{
		this.selectedStream = this.selectedPage;	
		this.setBackgroundImage();
		this.broadcast("startStream", url);
		// this.broadcast("startAACPlayer", url);
	}

	protected void stopStream()
	{
		this.broadcast("stopStream", "");
		// this.broadcast("stopAACPlayer", "");
	}
	
	protected void readMetadata()
	{
		this.metaOut.setText(R.string.reading_meta);
		this.broadcast("readMetadata", "");
	}

	protected void openSitmarkDialog()
	{
		AlertDialog.Builder ad = new AlertDialog.Builder(this.activity);	
		ad.setTitle("SITMark2");
		ad.setMessage("Detect SITMark Messages");
		ad.setPositiveButton("Start Detect",
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which){
					Toast.makeText(context, "Started", Toast.LENGTH_SHORT).show();
					toggleSitmark();
				}	
			}
		);
		ad.setNegativeButton("Cancel",
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which){
					dialog.cancel();
				}
			}
		);
		ad.show();
	}	
	
	protected void toggleSitmark()
	{
		this.sitmarkStarted = this.sitmarkStarted ? false : true;
		
		if(this.sitmarkStarted){
			this.broadcast("startSitmark", "");
		}
		else{
			this.broadcast("stopSitmark", "");
		}
	}

	protected void toggleMic()
	{
		// fixdiss	
		if(this.micStarted){
		
			this.broadcast("stopMic", "");	
			return;
		}

		AlertDialog.Builder ad = new AlertDialog.Builder(this.activity);	
		ad.setTitle("Audio Record Comment");
		ad.setMessage("Record your Audio Comment and send it to the RadioStation right away!");
		/*
		ad.setPositiveButton("Start REC",
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which){
					Toast.makeText(context, "REC started", Toast.LENGTH_SHORT).show();
					toggleMicButLikeForRealNow();
				}	
			}
		);
		*/
		ad.setNegativeButton("Close",
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which){
					dialog.cancel();
				}
			}
		);
	
		Button b;	
		DynView dw = new DynView(this.context);
		b = new Button(this.context);
		b.setText("Start REC");
		b.setOnClickListener(new OnClickListener(){
			@Override public void onClick(View v){
				Toast.makeText(context, "REC started", Toast.LENGTH_SHORT).show();
				toggleMicButLikeForRealNow();
			}
		});
		dw.addView(b);
		b = new Button(this.context);
		b.setText("Stop REC");	
		b.setOnClickListener(new OnClickListener(){
			@Override public void onClick(View v){
				Toast.makeText(context, "REC stopped", Toast.LENGTH_SHORT).show();
				toggleMicButLikeForRealNow();
			}
		});
		dw.addView(b);
		b = new Button(this.context);
		b.setText("Send REC");	
		b.setOnClickListener(new OnClickListener(){
			@Override public void onClick(View v){
				Toast.makeText(context, "REC sent", Toast.LENGTH_SHORT).show();
				sendRec();
			}
		});
		dw.addView(b);
		// dw.addView(new CloseButton(this.context, ad));
		ad.setView(dw);
		ad.show();
	
	}

	protected void stopMic()
	{
		Log.d(ViewBuilder.class.getName(), "stopMic():" +this.micStarted);
		
		// todo: send rec somewhere else
		this.sendRec();
		
		this.micStarted = false;
	}

	protected void toggleMicButLikeForRealNow()
	{
		this.micStarted = this.micStarted ? false : true;
		
		if(this.micStarted){
			
			this.broadcast("startMic", "");
		}
		else{
			
			this.broadcast("stopMic", "");
		}
	}	

	protected void sendRec()
	{
		Toast.makeText(context, "REC sent", Toast.LENGTH_SHORT).show();
		this.broadcast("sendRec", MicService.RAW_REC_NAME);
	}

	protected void readPrefs()
	{
		this.broadcast("readPreferences", "");
	}

	protected void sendLikeReq()
	{
		Log.d(ViewBuilder.class.getName(), "sendLikeReq(): selectedChannelTitle: " +this.selectedChannelTitle);
		Log.d(ViewBuilder.class.getName(), "sendLikeReq(): songTitle: " +this.songTitle);

		String arg = "";
		
		arg += this.selectedChannelTitle;
		arg += ";";
		arg += this.songTitle;
		
		this.broadcast("sendLikeReq", arg);
	}

	protected void takeSnapshot()
	{
		this.broadcast("takeSnapshot", "");
	}

	protected void sendSnapshot()
	{
		this.broadcast("sendSnapshot", "");
	}

	protected boolean aacstate = false;
	protected void toggleAACPlayer(String url)
	{
		if(null == url){
			url = "http://85.214.212.84:8000/radio.0.mp3";
		}
		if(url.equals("")){
			url = "http://85.214.212.84:8000/radio.0.mp3";
		}
		
		this.aacstate = this.aacstate == false ? true : false;
		
		if(this.aacstate){
			this.broadcast("startAACPlayer", url);
		}
		else{
			this.broadcast("stopAACPlayer", "");
		}
	}

	// inlets 
	protected void showLikes(String res)
	{
		if(null == this.likeOut){
			
			return;
		}
		
		this.likeOut.setText(res);
	}
	
	protected void showPrefs(String res)
	{
		if(null == this.prefsOut){
			
			return;
		}
		
		this.prefsOut.setText(res);
	}
	

	protected void showDetectResult(String res)
	{
		if(null == this.detectOut){
			
			return;
		}
		
		this.detectOut.setText(res);

		Log.d(ViewBuilder.class.getName(), "showDetectResult(): " +res);
	}

	protected void showMetaResult(String res)
	{
		if(null == this.metaOut){
			return;
		}
		
		this.songTitle = res;
		this.metaOut.setText(res);
	}

	protected JSONObject getDefaultJSON()
	{
		/*
		String json = "{'formats':[{'title':'Default Title 1','subtitle':'Default Subtitle 1','stream':'Default Stream 1','icon':'Default Icon 1','bgimage':'Default bgimage 1','bgimageBlur':'Default bgimagebBlur 1','table-cell-display':{'bgcolor':'#ffffff','frameColor':'#ffffff','separatorColor':'#ffffff','titleColor':'#ffffff','labelColor':'#ffffff'},'table-cells':[{'title':'Default Feature','icon':'http://radioscreen.org//assets//social-network-icons//facebook_128.png','class':'DefaultClass','description':'Description of the Default Feature','created':'1986','additional':'Default Feature is no Feature'}]},{'title':'Default Title 2','subtitle':'Default Subtitle 2','stream':'Default Stream 2','icon':'Default Icon 2','bgimage':'Default bgimage 2','bgimageBlur':'Default bgimagebBlur 2','table-cell-display':{'bgcolor':'#ffffff','frameColor':'#ffffff','separatorColor':'#ffffff','titleColor':'#ffffff','labelColor':'#ffffff'},'table-cells':[{'title':'Default Feature','icon':'http://radioscreen.org//assets//social-network-icons//facebook_128.png','class':'DefaultClass','description':'Description of the Default Feature','created':'1986','additional':'Default Feature is no Feature'}]}]}";
		*/

		String json = "{'formats': [{'title': 'baden.fm','subtitle': 'der beste Musikmix aus 4 Jahrzehnten ','stream': 'http://stream.baden.fm:8006/badenfm','icon': 'http://appwerk.com/RadioWerk/badenFm/logo.jpg','bgimage': 'http://appwerk.com/RadioWerk/badenFm/bg.jpg','bgimageBlur': {'radius': '11','saturationDeltaFactor': '1.0','tintColor': '33333370'},'table-cell-display': {'bgcolor': '4b4c4d89','frameColor': 'fafbff00','separatorColor': 'ff000099','titleColor': 'dfe4f0d3','labelColor': 'dfe4f0ce'},'table-cells': [{'label': 'Social Stream','adminIcon': 'http://radioscreen.org/assets/social-network-icons/delicious_48.png','class': 'RWSocialCell','appearance': 'single','title': 'Social','stationid': 'baden.fm'},{'label': 'Feedback','adminIcon': 'http://radioscreen.org/assets/social-network-icons/delicious_48.png','class': 'RWFeedbackCell','appearance': 'single','title': 'Sprich mit uns!!'},{'label': 'Titelinfo','class': 'RWNowPlayingCell','display': 'first'}]}],'stationId': 'baden.fm'}";

		JSONObject res = new JSONObject();

		try{
			res = new JSONObject(json);
		}
		catch(JSONException e){
			e.printStackTrace();
		}
		
		return res;
	}

	protected int gid = 100000;
	protected int generateViewId()
	{
		
		this.gid++;
		
		int res = this.gid;
		return res;
	}
}

class StreamButton extends Button
{
	protected String streamURL;
	protected String title;
	protected ViewBuilder cb;	

	public StreamButton(ViewBuilder viewBuilder, String t, String url){
		
		super(viewBuilder.context);
		
		this.cb = viewBuilder;
		this.title = t;
		this.streamURL = url;
	
		this.setText(this.title);	
		this.setOnClickListener(new OnClickListener(){
			@Override public void onClick(View v){
				// cb.startStream(streamURL);
				// cb.toggleAACPlayer(streamURL);
			}
		});
		
		this.getBackground().setAlpha(98);
		this.setTextColor(0xffffffff);
		this.setTextSize(36);
		this.setGravity(Gravity.LEFT);
	}
}

class WebLinkButton extends ImageButton
{
	protected ViewBuilder cb;
	protected String loc;
	protected String title;
	protected String icon;

	public WebLinkButton(ViewBuilder icb, String ititle, String iloc, String iicon)
	{
		super(icb.context);

		this.cb = icb;
		this.title = ititle;
		this.icon = iicon;		
		this.loc = iloc;		

		this.setOnClickListener(new OnClickListener(){
			
			@Override public void onClick(View w){
			
				cb.loadWebLink(loc);
			}
		});
		
		try{
			this.setImageBitmap(BitmapFactory.decodeStream((InputStream)new URL(this.icon).getContent()));
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		this.getBackground().setAlpha(98);
		this.setPadding(0, 20, 200, 20);
	}
}

/*
class LoadRSSFeedButton extends ImageButton
{
	protected ViewBuilder cb;
	
	protected String rssFeedLoc;
	protected String title;
	protected String icon;

	public LoadRSSFeedButton(ViewBuilder viewBuilder, String title, String url, String icon)
	{
		super(viewBuilder.context);
		
		this.cb = viewBuilder;
		this.title = title;
		this.rssFeedLoc = url;
		this.icon = icon;
		
		this.setOnClickListener(new OnClickListener(){
			@Override public void onClick(View w){
				cb.loadRSSFeed(rssFeedLoc);
			}
		});
		
		try{
			this.setImageBitmap(BitmapFactory.decodeStream((InputStream)new URL(this.icon).getContent()));
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		this.getBackground().setAlpha(98);
		this.setPadding(0, 20, 200, 20);
	}
}
*/

class MPageAdapter extends FragmentPagerAdapter
{
	
	protected Map<String, MFragment> fragments;
	protected Map<String, DynView> comps;
	protected ArrayList<String> keys;
	
	public MPageAdapter(ArrayList<String> titles, FragmentManager fm)
	{
		super(fm);
	
		this.comps = new HashMap<String, DynView>();	
		this.fragments = new HashMap<String, MFragment>();
		
		this.keys = new ArrayList<String>();

		for(String s : titles){
			
			this.keys.add(s);
		}
	}

	public DynView getComp(String key)
	{
		// Returns "dynamic" (built from JSON)
		// view at 
		// onCreateView of a given fragment
		
		DynView res = null; 	
		res = this.comps.get(key);
		
		return res;
	}
		
	public void addComp(String key, DynView comp)
	{
	
		// Stores components for the "getComp" call
		// at onCreatView() of a given fragment
		this.comps.put(key, comp);

		// Initial add of components 
		MFragment target = (MFragment)this.fragments.get(key);
		if(null != target){
			target.addComp(key, comp);
		}
	}

	@Override public Fragment getItem(int pos)
	{
		String key = this.keys.get(pos);
		MFragment res = this.fragments.get(key);
		
		if(null == res){
			this.fragments.put(key, res = new MFragment(key, this));
		}
	
		return res;
        }

	@Override public int getCount()
	{
		return this.keys.size();
	}
}

class MFragment extends Fragment
{
	protected MPageAdapter adapter;
	protected ScrollView scrollView;
	protected LinearLayout l;
	protected MFragment f;
	protected Bundle b;
	protected View view;
	protected String key;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// Log.d(MFragment.class.getName(), "onCreateView(): " +this.key);
		
		if(null == this.view){
			this.view = (View)inflater.inflate(R.layout.comp, container, false);
			this.scrollView = (ScrollView)this.view.findViewById(R.id.scroll);
		
			// --> 
			MarginLayoutParams p = (MarginLayoutParams)this.scrollView.getLayoutParams();
			p.leftMargin = -8; 
			p.rightMargin = -8; 
			p.topMargin = 0; 
			p.bottomMargin = 0;
			this.scrollView.setLayoutParams(p);
			// -->
			/*
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)this.scrollView.getLayoutParams();
			lp.setMargins(100, 100, 100, 200);
			this.scrollView.setLayoutParams(lp);	
			*/
			// -->

			this.scrollView.getViewTreeObserver().addOnScrollChangedListener(new OnScrollChangedListener(){
				@Override public void onScrollChanged(){
				}
			});
		}
		else{
			// ((ViewGroup)this.view.getParent()).removeView(this.view);
			((ViewGroup)this.view.getParent()).removeAllViews();
		}
	
		this.addComp(this.key, (DynView)this.adapter.getComp(this.key));
		return this.view;
	}

	public void addComp(String key, DynView comp)
	{
		
		if(null == comp){
			return;
		}	

		if(null == this.scrollView){	
			return;
		}
			
		this.scrollView.removeAllViews();
		this.scrollView.addView(comp);
	}

	public MFragment(String key, MPageAdapter adapter)
	{
		// Log.d(MFragment.class.getName(), "MFragment(): " +key);
		this.key = key;
		this.adapter = adapter;
		
		this.b = new Bundle();

		String title = key;
		//...

		this.b.putString("title", title);

		this.setArguments(b);
	}
}

class MJavascriptInterface
{
	protected DynView dv;
	public MJavascriptInterface(DynView dv)
	{
		this.dv = dv;
	}
	
	@SuppressWarnings("unused")
	public void processContent(String face)
	{
		Log.d(ViewBuilder.class.getName(), "processContent():" +face);
	}
}

/*
class CloseButton extends Button
{
	protected DynView view;
	public CloseButton(Context c, DynView v)
	{
		super(c);
		this.view = v;
		this.setText("Close");	
		this.setOnClickListener(new OnClickListener(){
			@Override public void onClick(View v){
				view.dismiss();
			}
		});
	}
}
*/
