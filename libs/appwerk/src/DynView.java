package de.appwerk.radioapp.lib;

import android.view.View;
import android.content.Context;
import android.widget.TextView;
import android.widget.RelativeLayout;
import android.widget.LinearLayout;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;
import android.util.Log;

public class DynView extends LinearLayout 
// public class DynView extends ListView 
{
	public DynView(Context c)
	{
		super(c);
		this.setOrientation(LinearLayout.VERTICAL);
		this.setPadding(0, 0, 0, 0);
		////  
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
		LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		layoutParams.setMargins(0, -3, 0, -20);
		this.setLayoutParams(layoutParams);
	}

	protected Bitmap buff;
	protected int top; 
	protected int left;
	protected int childCenterX;
	protected int childCenterY;
	protected int parentCenterX;
	protected int parentCenterY;
	protected int distanceY;
	protected int absChildCenterX;
	protected int absChildCenterY;

	/*
	@Override protected boolean drawChild(Canvas canvas, View child, long drawingTime)
	{
		Log.d(DynView.class.getName(), "drawChild()");
		
		if(null == this.buff){

			this.buff = this.getChildDrawingCache(child);
		}
			
		if(null == this.buff){

			child.setDrawingCacheEnabled(true);
			child.buildDrawingCache();
			this.buff = child.getDrawingCache();
		}	

		this.top = child.getTop();
		this.left = child.getLeft();
		this.childCenterX = child.getHeight();
		this.childCenterY = child.getWidth();
		this.parentCenterX = this.getWidth() /2;		
		this.parentCenterY = this.getHeight() /2;
		this.absChildCenterY = child.getTop() +this.childCenterY; 
		this.distanceY = this.parentCenterY -this.absChildCenterY;
		
		Log.d(DynView.class.getName(), "drawChild()" +this.buff);
		
		return true;
	}

	protected Bitmap getChildDrawingCache(final View child)
	{
		Bitmap bitmap = child.getDrawingCache();
		if(null == bitmap){

			child.setDrawingCacheEnabled(true);
			child.buildDrawingCache();
			bitmap = child.getDrawingCache();
		}

		return bitmap;
	}
	*/
}
