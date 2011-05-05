package org.example.presents;

import android.app.Activity;
import android.media.MediaPlayer;
import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.FloatMath;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import java.lang.Math;
import java.net.ConnectException;


public class Presents extends Activity
	implements OnTouchListener, AccelerometerListener {
   private static final String TAG = "Presents";
   // These matrices will be used to move and zoom image
   Matrix matrix = new Matrix();
   Matrix savedMatrix = new Matrix();
   static float xLoc = (float) 0.0;
   static float yLoc = (float) 0.0;
   static float newXLoc;
   static float newYLoc;
   static float mx,my,mz;
   static final float xMin = (float)0.0;
   static final float xMax = (float)300.0;
   static final float yMin = (float)0.0;
   static final float yMax = (float)500.0;
   static float tX = (float)0.0;
   static float tY = (float)0.0;
   static int lastx = 1;
   static int lasty = 1;
   static int lastSquare = 1;
   private MediaPlayer affirm;
   private MediaPlayer deny;
   private static Context CONTEXT;
   private static PopupWindow lpu = null;
   private static OSCConnection osccon = null;
   LinearLayout layout;

   private static TextView lpView; 
   // We can be in one of these 3 states
   static final int NONE = 0;
   static final int DRAG = 1;
   static final int ZOOM = 2;
   int mode = NONE;
   
   class DismissPopup implements Runnable {
	   private static final int POPUP_DISMISS_DELAY = 100;   
	   public void run() {
	   if (lpu != null) { 
		   try { Thread.sleep(POPUP_DISMISS_DELAY);} catch (InterruptedException e) {}
		   lpu.dismiss(); 
		   }
	   }
	   }
 //how much time your popup window should appear

   private DismissPopup mDismissPopup = new DismissPopup();
   
   // Remember some things for zooming
   PointF start = new PointF();
   PointF mid = new PointF();
   float oldDist = 1f;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      // start tracing to "/sdcard/tuch.trace"
//      android.os.Debug.startMethodTracing("tuch");
      Context mc = getApplicationContext();
      affirm = MediaPlayer.create(mc,R.raw.gotit);
      deny = MediaPlayer.create(mc,R.raw.nope);  		 

      setContentView(R.layout.main);
      CONTEXT = this;
      ImageView view = (ImageView) findViewById(R.id.imageView);
      view.setOnTouchListener(this);
      matrix.postTranslate(-xLoc,-yLoc);
      view.setImageMatrix(matrix);
      lpView = new TextView(CONTEXT);
      lpView.setTextAppearance(CONTEXT, R.style.MyDefaultTextAppearance);
      osccon = new OSCConnection();
      try {
		osccon.connect("10.0.0.109:7000");
	  } catch (ConnectException e) {	e.printStackTrace();	}
   }
   protected void onResume() {
	   super.onResume();
	   if (AccelerometerManager.isSupported()){
		   AccelerometerManager.startListening(this);
	   }
   }
   protected void onDestroy() {
	   super.onDestroy();
	   if (AccelerometerManager.isListening()){
		   AccelerometerManager.stopListening();
	   }
   }
   public static Context getContext() {
	   return CONTEXT;
   }

   public boolean onTouch(View v, MotionEvent event) {
	  
      ImageView view = (ImageView) v;
      CharSequence pos; 

      layout = new LinearLayout(this);
      // Handle touch events here...
      switch (event.getAction() & 0xFF) {
      case MotionEvent.ACTION_DOWN:
         savedMatrix.set(matrix);
         start.set(event.getX(), event.getY());
         Log.d(TAG, "mode=DRAG");
         mode = DRAG;
         lastx = (int)event.getX();
         lasty = (int)event.getY();
         lastSquare = touch2ss((int)event.getX(), (int)event.getY());
         pos = new String("("+lastx+","+lasty+")");
         osccon.send("/activity", lastSquare, 3);
         lpView.setText(pos);
         lpu = new PopupWindow(lpView,80,20);
         lpu.showAtLocation(layout,Gravity.CENTER,-30,0);  
         lpu.update(50,50,200,80);
         
         break;
      case MotionEvent.ACTION_UP:
         mode = NONE;
         Log.d(TAG, "mode=NONE");
         xLoc -= tX;
         yLoc -= tY;
         if (Math.abs(start.x-event.getX())<10 && Math.abs(start.y-event.getY())<10)
         {
        	 //onScan(v);
         }
         osccon.send("/activity", lastSquare, 0);
         mDismissPopup.run();
         break;
      case MotionEvent.ACTION_MOVE:
         if (mode == DRAG) {
            // ...
            matrix.set(savedMatrix);
            tX = event.getX()-start.x;
            tY = event.getY()-start.y;
            newXLoc = xLoc - tX;
            newYLoc = yLoc - tY;
            if (newXLoc<xMin || newXLoc>xMax) {tX = (float)0.0;}
            if (newYLoc<yMin || newYLoc>yMax) {tY = (float)0.0;}
            matrix.postTranslate(tX,tY);
         }
         else if (mode == ZOOM) {
            float newDist = spacing(event);
            Log.d(TAG, "newDist=" + newDist);
            if (newDist > 10f) {
               matrix.set(savedMatrix);
               float scale = newDist / oldDist;
               matrix.postScale(scale, scale, mid.x, mid.y);
            }
         }
         break;
      }

      view.setImageMatrix(matrix);
      return true; // indicate event was handled
   }


   /** Determine the space between the first two fingers */
   private float spacing(MotionEvent event) {
      float x = event.getX();
      float y = event.getY();
      return FloatMath.sqrt(x * x + y * y);
   }
   
   private int touch2ss(int x, int y)  {
	   int horiz = (x+50)/100;
	   int vert  = (y+75)/150;
	   return (1 + horiz) + 3*vert;
   	}
   
   public void onScan(View v) {
	        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
	        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
	        startActivityForResult(intent, 0);
	    }

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
	    if (requestCode == 0) {
	        if (resultCode == RESULT_OK) {
	            String contents = intent.getStringExtra("SCAN_RESULT");
	        //    String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
	           if (contents.length() > 30)
	           {
	        	   if (affirm != null) deny.start();
	           }
	           else {
	        	   if (deny != null) affirm.start();
	           }
	            
	            // Handle successful scan
	        } else if (resultCode == RESULT_CANCELED) {
	            // Handle cancel
	        }
	    }
	}
	
	public void onAccelerationChanged(float x, float y, float z) {
		if (Math.abs(x-mx) > 4.5 || Math.abs(y-my) > 4.5 || Math.abs(z-mz) > 4.5)
		{
			affirm.start();
		}
		mx =x; my = y; mz =z;	
	}

	public void onShake(float force) {
		 if (force > 0.5) affirm.start();
	}

}
