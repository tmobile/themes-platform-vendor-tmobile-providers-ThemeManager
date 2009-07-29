package com.tmobile.thememanager.widget;

import com.tmobile.thememanager.R;
import com.tmobile.thememanager.utils.BitmapStore;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Manages each item except ringtone and notifications on personalize main screen
 */
public class PersonalizePreference extends Preference {
    private LayoutInflater mInflater;
    private Drawable mImage;
    private BitmapStore mStore;
    private String mBaseKey;
    private String mThumbnailKey;

    public PersonalizePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public PersonalizePreference(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public PersonalizePreference(Context context) {
        super(context, null);
    }
    
    protected LayoutInflater getInflater() {
        if (mInflater == null) {
            mInflater = LayoutInflater.from(getContext());
        }
        return mInflater;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        if (mStore != null) {
            ThumbnailedBitmapView cacheView = 
                (ThumbnailedBitmapView)view.findViewById(R.id.thumb);
            cacheView.setImageStore(mStore, mBaseKey, mThumbnailKey);
        } else {
            PersonalizePreferenceData.bindView(view, mImage);
        }
    }
    
    /* Also used by PersonalizeRingtonePreference. */
    static View createViewCommon(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.personalize_row, parent, false);
        
        /* Recycling is not helpful for small thumbnail type images. */
        ((ThumbnailedBitmapView)view.findViewById(R.id.thumb)).setRecycleOnChange(false);
        
        return view;
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        return createViewCommon(getInflater(), parent);
    }

    /**
     * Sets the image which is displayed at leftmost position in the widget
     * @param image BitmapDrawable image
     */
    public void setImage(Drawable image) {
        mStore = null;
        if (mImage != image) {
            mImage = image;
            notifyChanged();
        }
    }
    
    public void setImage(int id) {
        setImage(getContext().getResources().getDrawable(id));
    }

    public void setImageStore(BitmapStore store, String baseKey, String thumbnailKey) {
        mImage = null;
        mStore = store;
        mBaseKey = baseKey;
        mThumbnailKey = thumbnailKey;
        notifyChanged();
    }
}
