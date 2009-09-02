package com.tmobile.thememanager.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.RingtonePreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Manages ringtone and notifications on personalize main screen
 */
public class PersonalizeRingtonePreference extends RingtonePreference {
    private LayoutInflater mInflater;
    Drawable mImage = null;
    
    public PersonalizeRingtonePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public PersonalizeRingtonePreference(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }
    
    public PersonalizeRingtonePreference(Context context) {
        super(context, null);
    }
    
    protected LayoutInflater getInflater() {
        if (mInflater == null) {
            mInflater = LayoutInflater.from(getContext());
        }
        return mInflater;
    }
    
    @Override
    protected void onPrepareRingtonePickerIntent(Intent ringtonePickerIntent) {
        super.onPrepareRingtonePickerIntent(ringtonePickerIntent);
        
        /*
         * Since this preference is for choosing the default ringtone, it
         * doesn't make sense to show a 'Default' item.
         */
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
        
        /*
         * Similarly, 'Silent' shouldn't be shown here. 
         */
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
    }

    @Override
    protected void onSaveRingtone(Uri ringtoneUri) {
        RingtoneManager.setActualDefaultRingtoneUri(getContext(), getRingtoneType(), ringtoneUri);
    }

    @Override
    protected Uri onRestoreRingtone() {
        return RingtoneManager.getActualDefaultRingtoneUri(getContext(), getRingtoneType());
    }
    
    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        PersonalizePreferenceData.bindView(view, mImage);
    }
    
    @Override
    protected View onCreateView(ViewGroup parent) {
        return PersonalizePreference.createViewCommon(getInflater(), parent);
    }
    
    /**
     * Sets the image which is displayed at leftmost position in the widget
     * @param image BitmapDrawable image
     */
    public void setImage(Drawable image) {
        mImage = image;
        notifyChanged();
    }
    
    public void setImage(int id) {
        setImage(getContext().getResources().getDrawable(id));
    }
}
