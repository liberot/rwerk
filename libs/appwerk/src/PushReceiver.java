package de.appwerk.radioapp.lib;

import android.util.*;
import com.parse.*;
import android.content.*;
import android.os.*;
import java.util.*;
import java.lang.CharSequence;
import org.json.*;
import android.app.*;
import android.support.v4.app.NotificationCompat;
import android.app.AlarmManager;


public class PushReceiver extends ParsePushBroadcastReceiver
{
	static public final String PUSH_RECEIVED = "PushReceiver.PUSH_RECEIVED";
	
	protected String m;
	protected Intent intent;
	protected Context context;
	protected Context c;

	@Override protected void onPushOpen(Context context, Intent intent)
	{
		Log.d(PushReceiver.class.getName(), "onPushOpen()");
	}
	
	@Override protected void onPushReceive(Context context, Intent intent)
	{	
		// ------
		Log.d(PushReceiver.class.getName(), "onPushReceive(): " +context);
		// ------
		this.intent = intent;
		this.context = context;
		this.c = context;
		// ------
		// ------
		JSONObject res = new JSONObject();
		// ------
		Bundle b = intent.getExtras();
		Set<String> keys = b.keySet();
		Iterator<String> i = keys.iterator();
		while(i.hasNext()){
			String key = i.next();
			String value = b.getString(key);
			Log.d(PushReceiver.class.getName(), "key: " +key +" value: " +value);
			if(key.equals("com.parse.Data")){
				try{
					res = new JSONObject(value);
				}
				catch(Exception e){
					e.printStackTrace();
				}
				break;
			}
		}
		// ------
		String msg = "";
		try{
			msg = res.getString("alert");
		}
		catch(JSONException e){
			e.printStackTrace();
		}
		Log.d(PushReceiver.class.getName(), "msg: " +msg);
		
		this.raiseNotification(msg);
		
		// ------	
		this.m = msg;
		this.c = context;
		Thread t = new Thread()
		{
			@Override
			public void run()
			{
				Intent i = new Intent();
				i.setAction(PushReceiver.PUSH_RECEIVED);
				i.putExtra("push", m);
				c.getApplicationContext().sendBroadcast(i);
			}
		};
		// ------	
		t.start();
	}
	
	protected void raiseNotification(String msg)
	{
		// signal
		Notification n = this.getNotification(this.context, this.intent);
		NotificationManager notificationManager = (NotificationManager)this.context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(100, n); 
		
		// temp ui
		Intent intent = new Intent(this.context, NotificationReceiverActivity.class);	
		intent.putExtra("msg", msg);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent pIntent = PendingIntent.getActivity(this.context, 0, intent, 0);
		this.context.startActivity(intent);
	}
}
