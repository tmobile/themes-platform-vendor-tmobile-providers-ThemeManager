package com.tmobile.thememanager.widget;

import com.tmobile.thememanager.R;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

/**
 * This class maintains the data displayed by personalized preferences. This
 * also contains the common bindView method to map the data to the appropriate
 * view.
 */
public class PersonalizePreferenceData {
    public Drawable mImage;

    public PersonalizePreferenceData(Drawable image) {
        mImage = image;        
    }
    
    /**
     * Common method to map the data elements in the view
     * @param v preference layout view
     */
    public static void bindView(View v, Drawable image){
        ImageView imageView = (ImageView)v.findViewById(R.id.thumb);
        if (imageView != null) {
            imageView.setImageDrawable(image);
        }
    }
    
    public void bindView(View v) {
        bindView(v, mImage);
    }
}
