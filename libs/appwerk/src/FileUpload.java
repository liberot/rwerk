package de.appwerk.radioapp.lib;

import java.io.File;
import java.io.FileInputStream;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.HttpEntity;
import java.net.*;
import java.io.*;
import android.content.Context;
import android.util.Log;
import java.lang.Thread;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/* ****
  '''''
	http://stunningco.de/2010/04/25/uploading-files-to-http-server-using-post-android-sdk/

 */
public class FileUpload implements Runnable
{
	static public final String STATUS = "de.appwerk.radioapp.lib.FileUpload.STATUS";

	protected Context context;
	protected HttpURLConnection connection;
	protected DataOutputStream outputStream;
	protected DataInputStream inputStream;
	protected final String eol = "\r\n";
	protected final String twoHyphens = "--";
	protected final String boundary =  "*****";
	protected int bytesRead, bytesAvailable, bufferSize;
	protected byte[] buffer;
	protected final int maxBufferSize = 1 *1024 *1024;
	protected FileInputStream fileInputStream;
	protected URL url;
	protected String loc;
	protected String filename;
	protected Thread t;
	protected int responseCode;
	protected String responseMessage;
	protected String reply;

	public FileUpload(Context context, String loc, String filename)
	{
		this.context = context;
		this.loc = loc;
		this.filename = filename;
		this.t = new Thread(this);
		this.t.start();
	}

	public void run()
	{
		try
		{
			this.fileInputStream = this.context.openFileInput(this.filename);
			
			this.url = new URL(this.loc);
			
			this.connection = (HttpURLConnection)url.openConnection();
			this.connection.setDoInput(true);
			this.connection.setDoOutput(true);
			this.connection.setUseCaches(false);
			this.connection.setRequestMethod("POST");
			this.connection.setRequestProperty("Connection", "Keep-Alive");
			this.connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);
			
			this.outputStream = new DataOutputStream(this.connection.getOutputStream());
			this.outputStream.writeBytes(this.twoHyphens +this.boundary +this.eol);
			this.outputStream.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" +this.filename +"\"" + this.eol);
			this.outputStream.writeBytes(this.eol);

			this.bytesAvailable = this.fileInputStream.available();
			this.bufferSize = Math.min(this.bytesAvailable, this.maxBufferSize);
			this.buffer = new byte[this.bufferSize];

			this.bytesRead = this.fileInputStream.read(this.buffer, 0, this.bufferSize);

			while(this.bytesRead > 0) {

				this.outputStream.write(this.buffer, 0, this.bufferSize);
				this.bytesAvailable = this.fileInputStream.available();
				this.bufferSize = Math.min(this.bytesAvailable, this.maxBufferSize);
				this.bytesRead = this.fileInputStream.read(this.buffer, 0, this.bufferSize);
			}

			this.outputStream.writeBytes(this.eol);
			this.outputStream.writeBytes(this.twoHyphens +this.boundary +this.twoHyphens +this.eol);
			
			this.responseCode = this.connection.getResponseCode();
			this.responseMessage = this.connection.getResponseMessage();

			StringBuffer sb = new StringBuffer();
			InputStream in = new BufferedInputStream(this.connection.getInputStream());	
			try{
				int chr;
				while(-1 != (chr = in.read())){
					sb.append((char) chr);
				}
				this.reply = sb.toString();
			}
			finally{
				
				in.close();
			}
		
			Log.d(FileUpload.class.getName(), this.reply);

			this.broadcast(this.reply);

			this.fileInputStream.close();
			this.outputStream.flush();
			this.outputStream.close();
		}
		catch (Exception e){
			
			e.printStackTrace();
		}
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
				i.setAction(FileUpload.STATUS);
				i.putExtra("res", bm);
				context.sendBroadcast(i);
			}
		};
		
		t.start();
	}
}
