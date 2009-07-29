package com.tmobile.thememanager.utils;

import android.content.Context;
import android.content.res.Configuration;

public class ResourceUtilities {
    public static String getOrientationString(Context context) {
        Configuration config = context.getResources().getConfiguration();
        switch (config.orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                return "land";
            case Configuration.ORIENTATION_PORTRAIT:
                return "port";
            case Configuration.ORIENTATION_SQUARE:
                return "square";
            default:
                return String.valueOf(config.orientation);
        }
    }
}
