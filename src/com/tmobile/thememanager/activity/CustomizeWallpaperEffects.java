package com.tmobile.thememanager.activity;

import com.tmobile.thememanager.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class CustomizeWallpaperEffects extends GenericCustomize  {
    
    private static final int EFFECT_NOREQUEST = -1;
    private static final int EFFECT_NONE      =  0;
    private static final int EFFECT_BW        =  1;
    private static final int EFFECT_SEPIA     =  2;
    private static final int EFFECT_DUOTONE   =  3;
    private static final int EFFECT_DESATURATE=  4;
    private static final int EFFECT_RECOLOR   =  5;

    private static final int[] mEffects = {
            R.string.wpeffect_none,
            R.string.wpeffect_bw,
            R.string.wpeffect_sepia,
            R.string.wpeffect_duotone,
            R.string.wpeffect_desaturate,
//            R.string.wpeffect_recol,
        };
    
    private int mFormerEffect; // The effect that was in-effect before coming into the Effect Chooser.
    private AtomicInteger mDesiredEffect = new AtomicInteger(EFFECT_NOREQUEST);
    private volatile int mMostRecentlyComputedEffect = EFFECT_NOREQUEST; // Only used by ComputeEffectThread
    private Lock mThreadLock = new ReentrantLock();

    private Context mContext = null;

    private LayoutInflater mInflater;

    private String chosen_wallpaper;
    
    private ImageView mWpPreviewImgView = null;
    
    private volatile Bitmap mWallpaperBitmap = null;
    
    private volatile BitmapDrawable mOriginalWallpaper;
    
    // pixelcache is used by blackAndWhiteHelper() in ComputeEffectsThread.
    // It has to be declared here, otherwise it doesn't survive spinning a
    // new thread each time a long-running operation is performed.
    IntBuffer pixbuf = null;
    int[] pixels = null;
    private int[] pixelcache = null;
    private static final int CACHETYPE_NODATA = 0;
    private static final int CACHETYPE_BW     = 1;
    private static final int CACHETYPE_COLOR  = 2;
    private int pixelcacheContent = CACHETYPE_NODATA;

    // Need handler for callbacks to the UI thread
    private final Handler mHandler = new Handler();

    // Create runnable for posting
    final Runnable mUpdateWpPreview = new Runnable() {
        public void run() {
            int effect = mMostRecentlyComputedEffect; // The effect just set by the effectThread
            if(mWallpaperBitmap != null)
            {
                mWpPreviewImgView.setImageBitmap(mWallpaperBitmap);
            }
            else if (mOriginalWallpaper != null)
            {
                mWpPreviewImgView.setImageDrawable(mOriginalWallpaper);
            }
            if (effect != mFormerEffect) // compare to effect when we entered activity
            {
                setDirty(true);
            }
            else
            {
                setDirty(false);
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        // super.onCreate() calls initCustomAdaptor()
        super.onCreate(savedInstanceState);
        mDesiredEffect.set(EFFECT_NONE);
        
        // Get name of any previously applied effect
        mFormerEffect = EFFECT_NONE;
        String formerEffectName = bundle.getString(getResources().getString(R.string.KEY_WallPaperEffect));
        if (formerEffectName != null)
        {
            if(formerEffectName.compareTo(getResources().getString(mEffects[EFFECT_NONE])) == 0)
            {
                mFormerEffect = EFFECT_NONE;
            }
            else if (formerEffectName.compareTo(getResources().getString(mEffects[EFFECT_BW])) == 0)
            {
                mFormerEffect = EFFECT_BW;
            }
            else if (formerEffectName.compareTo(getResources().getString(mEffects[EFFECT_SEPIA])) == 0)
            {
                mFormerEffect = EFFECT_SEPIA;
            }
            else if (formerEffectName.compareTo(getResources().getString(mEffects[EFFECT_DUOTONE])) == 0)
            {
                mFormerEffect = EFFECT_DUOTONE;
            }
            else if (formerEffectName.compareTo(getResources().getString(mEffects[EFFECT_DESATURATE])) == 0)
            {
                mFormerEffect = EFFECT_DESATURATE;
            }
            else if (formerEffectName.compareTo(getResources().getString(mEffects[EFFECT_RECOLOR])) == 0)
            {
                mFormerEffect = EFFECT_RECOLOR;
            }
        }
        if (mFormerEffect > EFFECT_NONE)
        {
            startEffectComputationThread(mFormerEffect);
        }
        mInflater = LayoutInflater.from(this);
    }
    
    @Override
    protected int getViewLayout() {
        return R.layout.customize_wallpaper_effects;
    }
    
    protected void startEffectComputationThread(int effect)
    {
        mDesiredEffect.compareAndSet(EFFECT_NONE, effect);
//      boolean threadAvailable = mDesiredEffect.compareAndSet(EFFECT_NONE, effect);
//        if (threadAvailable == true)
//        {
            Thread effectThread = new ComputeEffectThread();

            effectThread.start();
//        }
        
    }
    
    // called during super.onCreate()
    protected BaseAdapter initCustomAdapter() {
        mContext = this;

        chosen_wallpaper = bundle.getString("wallpaperImage");
        loadOriginalWallpaper();

        mWpPreviewImgView = (ImageView) findViewById(R.id.wpeffect_preview);
        if(mWallpaperBitmap != null)
        {
            mWpPreviewImgView.setImageBitmap(mWallpaperBitmap);
        }
        else if (mOriginalWallpaper != null)
        {
            mWpPreviewImgView.setImageDrawable(mOriginalWallpaper);
        }
        return new WPEffectsAdapter();
    }
    
    private void loadOriginalWallpaper()
    {
        // Find the image to which to apply the effect...
        // First check if the user has already used the Customize Wallpaper activity to change the wallpaper
        if (chosen_wallpaper != null)
        {
            // If they did choose a new wallpaper, then load the chosen image
            try {
                int targetHeight = 240;
                int targetWidth = 320;
                
                // Convert the URI String back to a real URI
                int ndx = chosen_wallpaper.indexOf("://");
                int ndx2 = 0;
                String Scheme = null;
                String Authority = null;
                if (ndx > 0) {
                    Scheme = chosen_wallpaper.substring(0, ndx);
                    ndx2 = chosen_wallpaper.substring(ndx+3).indexOf("/");
                    if (ndx2 > 0) {
                        Authority = chosen_wallpaper.substring(ndx+3,ndx+ndx2+3);
                    }
                }
                Uri wp_uri = new Uri.Builder().scheme(Scheme).authority(Authority).path(chosen_wallpaper.substring(ndx+ndx2+4)).build();
                
                // Open the URI as a stream and load to a Bitmap.
                InputStream is;
                is = mContext.getContentResolver().openInputStream(wp_uri);
                BitmapFactory.Options myOpts = new BitmapFactory.Options();
                myOpts.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(is, null, myOpts);
                computeScaleFactor(myOpts, targetHeight, targetWidth);
                myOpts.inJustDecodeBounds = false;
                mWallpaperBitmap = BitmapFactory.decodeStream(is, null, myOpts);
                is.close();
                
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else
        {
            // If not, use the currently-applied wallpaper for this theme
            mOriginalWallpaper = ((BitmapDrawable)mContext.getWallpaper());
            if (mOriginalWallpaper != null)
            {
                mWallpaperBitmap = mOriginalWallpaper.getBitmap().copy(Bitmap.Config.ARGB_8888, true);
            }
        }

    }

    protected void persistChanges() {
        // Store into bundle
        bundle.putString(getResources().getString(R.string.KEY_WallPaperEffect), getResources().getString(mEffects[mDesiredEffect.get()]));
        // Set the wallpaper
        try {
            mContext.setWallpaper(mWallpaperBitmap);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        pixbuf = null;
        pixels = null;
        pixelcache = null;
    }

    private class WPEffectsAdapter extends BaseAdapter {
        

        public int getCount() {
            return CustomizeWallpaperEffects.mEffects.length;
        }

        public Object getItem(int position) {
            return mEffects[position];
        }

        public long getItemId(int position) {
            // If setting mDesiredEffect succeeds, it means its value
            // has been read and reset since the last time it was set.
            // Therefore the effectThread can be started with this effect.
            //
            // If setting mDesiredEffect fails, it means its value was
            // set and the thread started, but the thread hasn't run yet
            // and picked up the previous value.  In this case, just
            // ignore the new value... like a switch debounce.
            startEffectComputationThread(position);
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.personalize_row,
                        parent, false);
            }
            textView = (TextView)convertView.findViewById(android.R.id.title);

            textView.setText(mEffects[position]);

            return convertView;
        }
        
    } // class WPEffectsAdapter
    
    public static void computeScaleFactor(BitmapFactory.Options factoryOptions, int targetHeight,
            int targetWidth) {
        int h = factoryOptions.outHeight;
        int w = factoryOptions.outWidth;

        int hScale = Math.round(((float)h) / ((float)targetHeight));
        int wScale = Math.round(((float)w) / ((float)targetWidth));

        int scaleBoth = Math.max(hScale, wScale);

        if (scaleBoth < 1)
            scaleBoth = 1;

        factoryOptions.inSampleSize = scaleBoth;
    }

    public class ComputeEffectThread extends Thread {
        
        private int current_effect = EFFECT_NOREQUEST;
        private int width  = 0;
        private int height = 0;
        private int length = 0;
        private final int opaq = (0xFF<<24);
        
        @Override
        public void run() {
            
            Bitmap wallpaper = mWallpaperBitmap;
            
            // Can proceed only if no other thread has mThreadLock, otherwise die
            // This is because sometimes I've seen the thread started multiple times!
            if (mThreadLock.tryLock())
            {
                try {
                    // only read mDesiredEffect once, and reset it atomically
                    current_effect = mDesiredEffect.getAndSet(EFFECT_NONE);
                    
                    if (current_effect != mMostRecentlyComputedEffect)
                    {
                        // Compute and apply current effect
                        if (wallpaper != null)
                        {
                            width  = wallpaper.getWidth();
                            height = wallpaper.getHeight();
                            length = width*height;

                            switch (current_effect) {
                                case EFFECT_NONE:
                                    reloadOriginalWallpaper();
                                    ComputeEffectNone();
                                    break;
                                case EFFECT_BW:
                                    reloadWallpaperIfNecessary();
                                    ComputeEffectBlackAndWhite();
                                    break;
                                case EFFECT_SEPIA:
                                    reloadWallpaperIfNecessary();
                                    ComputeEffectSepia();
                                    break;
                                case EFFECT_DUOTONE:
                                    reloadWallpaperIfNecessary();
                                    ComputeEffectDuotone();
                                    break;
                                case EFFECT_DESATURATE:
                                    reloadOriginalWallpaper();
                                    ComputeEffectDesaturate();
                                    break;
                                case EFFECT_RECOLOR:
                                    reloadOriginalWallpaper();
                                    ComputeEffectRecolor();
                                    break;
                            }
                            
                            // Store the resulting bitmap
                            
                            
                            
                            // Finally, set previous to current
                            mMostRecentlyComputedEffect = current_effect;
                        }
                    } // end if (current_effect != mMostRecentlyComputedEffect)
                    mHandler.post(mUpdateWpPreview);
                }  // end try
                finally
                {
                    mThreadLock.unlock();
                }
            } // end if tryLock()
        }

        private void reloadWallpaperIfNecessary()
        {
            if (pixelcacheContent == CACHETYPE_BW)
            {
                // Don't bother reloading if the pixelcache
                // already contains a B&W version of the image
                return;
            }
            else
            {
                reloadOriginalWallpaper();
            }
        }

        private void reloadOriginalWallpaper()
        {
            loadOriginalWallpaper();
            if ((pixbuf == null) || (pixbuf.array().length < length))
            {
                pixbuf = IntBuffer.allocate(length);
            }
            else
            {
                pixbuf.rewind();
            }
            mWallpaperBitmap.copyPixelsToBuffer(pixbuf);
            pixels = pixbuf.array();
        }

        private void ComputeEffectNone()
        {
        }
        
        private void ComputeEffectBlackAndWhite()
        {
            int theAvg;
            int opaq = (0xFF<<24);

            blackAndWhiteHelper(pixels);

            for (int x = 0; x < length; x++) {
                theAvg = pixelcache[x];
                pixels[x] = opaq | (theAvg<<16) | (theAvg<<8) | theAvg;
            }

            mWallpaperBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        }
        
        // blackAndWhiteHelper() takes the input image and converts it
        // to Black & White.  However, since many of the effects rely on
        // this as a starting point, it caches the result.  Pass in true
        // for the value force if you want to be sure that the values
        // are regenerated.
        // The output is generated in the array pixelcache[].  
        void blackAndWhiteHelper(int[] pixels)
        {
            if (pixelcacheContent != CACHETYPE_BW)
            {
                if ((pixelcache == null) || (pixelcache.length != length))
                {
                    // allocate an array of the correct length
                    pixelcache = new int[length];
                }
                if (pixelcache != null)
                {
                    int oneColor;
    
                    // This algorithm computes the average of the r,g,b channels
                    // and sticks it back into the pixel array
                    int r;
                    int g;
                    int b;
                    final int sBy3 = (1<<16)/3 + 1; // 2-to-the-Sixteen dividedBy3
                    for (int x = 0; x < length; x++) {
                        oneColor = pixels[x];
                        r = (oneColor&0x00FF0000)>>16;
                        g = (oneColor&0x0000FF00)>>8;
                        b = (oneColor&0x000000FF);
                        pixelcache[x] = ((r+g+b) * sBy3) >> 16;
                    }
                    pixelcacheContent = CACHETYPE_BW;
                }
            }
        }
        
        private void ComputeEffectSepia()
        {
            int lum;
            int targR = 75;
            int targG = 46;
            int targB = 15;
            int factR = (256 - targR);
            int factG = (256 - targG);
            int factB = (256 - targB);
            int red;
            int grn;
            int blu;

            // convert image to B&W first
            blackAndWhiteHelper(pixels);
            
            for (int x = 0; x < length; x++)
            {
                lum = pixelcache[x];
                red = ((lum * factR)>>8) + targR;
                grn = ((lum * factG)>>8) + targG;
                blu = ((lum * factB)>>8) + targB;

                pixels[x] = opaq | (red<<16) | (grn<<8) | blu;
            }
            mWallpaperBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        }
        
        private void ComputeEffectDuotone()
        {
            int theAvg;
            // Use a blue color (27.5, 27.5, 110) up to avg = 55. Ratio is (0.5,0.5,2)
            int targRG = 27;
            int targB = 110;
            int factRG = 65536 * (255 - targRG) / 200;
            int factB  = 65536 * (255 - targB)  / 200;
            int r;
            int g;
            int b;
            
            // convert image to B&W first
            blackAndWhiteHelper(pixels);
            
            // Interesting blue-green effect
            for (int x = 0; x < length; x++) {
                theAvg = pixelcache[x];
                if (theAvg <= 55)
                {
                    // Below 55, get the blue color simply
                    r = theAvg >> 1;
                    g = r + (r>>1);
                    if (g>255) g=255;
                    b = theAvg * 2;
                }
                else
                {
                    r = (((theAvg-55) * factRG)>>16);
                    g = r + (r>>1) + targRG;
                    if (g>255) g = 255;
                    r = r + targRG;
                    b = (((theAvg-55) *  factB)>>16) + targB;
                }
                pixels[x] = opaq | (r<<16) | (g<<8) | b;
            }
            mWallpaperBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        }
        
        private void ComputeEffectDesaturate()
        {
            int oneColor;
            int r;
            int g;
            int b;
            int difrg;
            int difrb;
            int difgb;
            
            // desatshift=0, no effect, original color
            // desatshift=1, some effect, paler
            // desatshift=2, lots of effect, very pale
            // desatshift=3, practically black&white
            final int desatshift = 2;
            
            // Move the 2 smaller channels closer to the max channel
            // This alg has the same effect as dialing down the 
            // saturation setting in HSV in Photoshop or GIMP
            for (int x = 0; x < length; x++)
            {
                oneColor = pixels[x];
                r = (oneColor&0x00FF0000)>>16;
                g = (oneColor&0x0000FF00)>>8;
                b = (oneColor&0x000000FF);
                
                difrg = r-g;
                difrb = r-b;
                difgb = g-b;
                
                if (difrg>0)
                {
                    // r > g
                    if (difrb>0)
                    {
                        // r > g && r > b; r is max
                        g = r - (difrg>>desatshift);
                        b = r - (difrb>>desatshift);
                    }
                    else
                    {
                        // b > r > g; b is max
                        r = b + (difrb>>desatshift);
                        g = b + (difgb>>desatshift);
                    }
                }
                else
                {
                    // g > r
                    if (difgb>0)
                    {
                        // g > r && g > b; g is max
                        r = g + (difrg>>desatshift);
                        b = g - (difgb>>desatshift);
                    }
                    else
                    {
                        // b > g > r; b is max
                        r = b + (difrb>>desatshift);
                        g = b + (difgb>>desatshift);
                    }
                }
                pixels[x] = opaq | (r<<16) | (g<<8) | b;
            }

            pixbuf.rewind();
            mWallpaperBitmap.copyPixelsFromBuffer(pixbuf);
        }
        
        private void ComputeEffectRecolor()
        {
            
        }
        
    } // class ComputeEffectThread

} // class CustomizeWallpaperEffects
