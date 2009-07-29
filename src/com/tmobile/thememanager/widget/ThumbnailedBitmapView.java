package com.tmobile.thememanager.widget;

import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.utils.BitmapStore;
import com.tmobile.thememanager.utils.ResourceUtilities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

public class ThumbnailedBitmapView extends ImageView {
    private BitmapStore mStore;
    private String mBaseKey;
    private String mThumbnailKey;
    
    private String mConfiguration;
    
    private boolean mCachingEnabled = false;
    private boolean mCached = false;
    
    private boolean mRecycle = false;

    /**
     * Special case where mRecycle is true when setImageResource was called, and
     * a follow-up called to setImageBitmap or Drawable was called which might
     * result in the resource drawable being recycled. This would be bad.
     */
    private boolean mLastWasResource = false;
    
    public ThumbnailedBitmapView(Context context) {
        this(context, null);
    }

    public ThumbnailedBitmapView(Context context, AttributeSet set) {
        this(context, set, 0);
    }

    public ThumbnailedBitmapView(Context context, AttributeSet set, int defStyle) {
        super(context, set, defStyle);
        init(context);
    }
    
    private void init(Context context) {
        mConfiguration = ResourceUtilities.getOrientationString(context);
        setRecycleOnChange(true);
    }
    
    public void setCachingEnabled(boolean enabled) {
        mCachingEnabled = enabled;
    }
    
    public boolean isCachingEnabled() {
        return mCachingEnabled;
    }
    
    /**
     * When set to true, any change to the source image will recycle the
     * previously displayed {@link Bitmap}, if applicable. Default is on.
     */
    public void setRecycleOnChange(boolean recycle) {
        mRecycle = recycle;
    }
    
    public boolean willRecycleOnChange() {
        return mRecycle;
    }
    
    private void recycleIfNecessary(Bitmap set) {
        if (mRecycle == true && mLastWasResource == false) {
            Drawable d = getDrawable();
            if (d != null && d instanceof BitmapDrawable) {
                Bitmap bmp = ((BitmapDrawable)d).getBitmap();
                if (bmp.isRecycled() == false && bmp != null && set != bmp) {
                    if (ThemeManager.DEBUG) {
                        Log.w(ThemeManager.TAG, "ThumbnailedBitmapView: Recycling bitmap " + bmp);
                    }
                    bmp.recycle();
                }
            }
        }
    }
    
    @Override
    public void setImageBitmap(Bitmap bm) {
        setCachingEnabled(false);
        recycleIfNecessary(bm);
        super.setImageBitmap(bm);
        mLastWasResource = false;
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        setCachingEnabled(false);
        Bitmap set = null;
        if (drawable != null && drawable instanceof BitmapDrawable) {
            set = ((BitmapDrawable)drawable).getBitmap();
        }
        recycleIfNecessary(set);
        super.setImageDrawable(drawable);
        mLastWasResource = false;
    }

    @Override
    public void setImageResource(int resId) {
        setCachingEnabled(false);
        recycleIfNecessary(null);
        super.setImageResource(resId);
        mLastWasResource = true;
    }

    @Override
    public void setImageURI(Uri uri) {
        setCachingEnabled(false);
        recycleIfNecessary(null);
        super.setImageURI(uri);
        mLastWasResource = false;
    }
    
    public String createKey(String thumbnailKey) {
        return thumbnailKey + '-' + mConfiguration;
    }

    public void setImageStore(BitmapStore store, String baseKey, String thumbnailKey) {
        mStore = store;
        mBaseKey = baseKey;
        mThumbnailKey = createKey(thumbnailKey);
        
        Bitmap thumbnail = mStore.get(mThumbnailKey);
        if (thumbnail != null) {
            setImageBitmap(thumbnail);
            mCached = true;
        } else {
            Bitmap bmp = mStore.get(baseKey);
            if (bmp != null) {
                setImageBitmap(bmp);
            }
            mCached = false;
        }
        
        setCachingEnabled(true);
        mLastWasResource = false;
    }

    public BitmapStore getImageStore() {
        return mStore;
    }
    
    public String getImageStoreBaseKey() {
        return mBaseKey;
    }
    
    public String getImageStoreThumbnailKey() {
        return mThumbnailKey;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable d = getDrawable();
        if (isCachingEnabled() == true && mCached == false && 
                d != null && d instanceof BitmapDrawable) {
            if (mStore == null) {
                throw new IllegalStateException("Call setImageStore prior to setting the image bitmap.");
            }

            Bitmap scaledBitmap = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(),
                    Bitmap.Config.RGB_565);
            Canvas scratch = new Canvas(scaledBitmap);
            super.onDraw(scratch);

            ((BitmapDrawable)d).getBitmap().recycle();
            super.setImageBitmap(scaledBitmap);
            mStore.put(mThumbnailKey, scaledBitmap);
            mCached = true;
        } else {
            super.onDraw(canvas);
        }
    }
}
