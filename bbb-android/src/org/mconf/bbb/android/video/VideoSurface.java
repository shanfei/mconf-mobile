package org.mconf.bbb.android.video;

import org.mconf.bbb.BigBlueButtonClient;
import org.mconf.bbb.android.BigBlueButton;
import org.mconf.bbb.android.Client;
import org.mconf.bbb.video.IVideoListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;

public class VideoSurface extends GLSurfaceView {
	
	private class VideoHandler extends IVideoListener {

		public VideoHandler(int userId, BigBlueButtonClient context) {
			super(userId, context);
		}

		@Override
		public void onVideo(byte[] data) {
			enqueueFrame(data,data.length);
		}
		
	}
	
	
	private static final Logger log = LoggerFactory.getLogger(VideoSurface.class);
	private VideoRenderer mRenderer;		
	private VideoHandler videoHandler;
	private int userId;
	private boolean inDialog;
	private boolean showing = false;
	private static final float defaultAspectRatio = 4 / (float) 3;
	
	public VideoSurface(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void start(int userId, boolean inDialog) {
		this.userId = userId;
		this.inDialog = inDialog;
		
		LayoutParams layoutParams = getLayoutParams();
		DisplayMetrics metrics = getDisplayMetrics(getContext());
		log.debug("Maximum display resolution: {} X {}\n", metrics.widthPixels, metrics.heightPixels);
		int h = 0, w = 0;
		if(inDialog){
			metrics.widthPixels -= 40;
			metrics.heightPixels -= 40;
		}
		float displayAspectRatio = metrics.widthPixels / (float) metrics.heightPixels;
		if (displayAspectRatio < defaultAspectRatio) {
			w = metrics.widthPixels;
			h = (int) (w / defaultAspectRatio);
		} else {
			h = metrics.heightPixels;
			w = (int) (h * defaultAspectRatio);			
		}
		layoutParams.width = w;
		layoutParams.height = h;
		setLayoutParams(layoutParams);		
		 
        initDrawer(metrics.widthPixels, metrics.heightPixels, w, h, 0, 0);

        mRenderer = new VideoRenderer(this);
		setRenderer(mRenderer);

		BigBlueButtonClient bbb = ((BigBlueButton) getContext().getApplicationContext()).getHandler();
		videoHandler = new VideoHandler(userId, bbb);
		videoHandler.start();
		bbb.addVideoListener(videoHandler);
		
		showing = true;
	}
	
	public void stop() {
		BigBlueButtonClient bbb = ((BigBlueButton) getContext().getApplicationContext()).getHandler();
		bbb.removeVideoListener(videoHandler);
		videoHandler.stop();
		
		endDrawer();
		
		showing = false;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		if (showing)
			stop();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if (showing)
			start(userId, inDialog);
	}
	
	static public DisplayMetrics getDisplayMetrics(Context context){
		DisplayMetrics metrics = new DisplayMetrics();
		Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        display.getMetrics(metrics);
        return metrics;
	}
		
	static {
    	System.loadLibrary("avutil");
    	System.loadLibrary("swscale");
        System.loadLibrary("avcodec");
        System.loadLibrary("avformat");
        System.loadLibrary("thread");
    	System.loadLibrary("common");
    	System.loadLibrary("queue");
    	System.loadLibrary("decode");
    	System.loadLibrary("mconfnative");  
        
    	log.debug("Video native libraries loaded");    
    }
	
	private native int initDrawer(int screenW, int screenH, int displayAreaW, int displayAreaH, int displayPositionX, int displayPositionY);
	private native int endDrawer();
    private native int enqueueFrame(byte[] data, int length);	
}
