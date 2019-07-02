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
import android.widget.TextView;

public class NotificationReceiverActivity extends Activity
{
	@Override protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.res);
		Bundle extras = this.getIntent().getExtras();
		TextView f = (TextView)this.findViewById(R.id.notif_out);
		String msg = extras.getString("msg");
		f.setText(msg); 
	}
}
