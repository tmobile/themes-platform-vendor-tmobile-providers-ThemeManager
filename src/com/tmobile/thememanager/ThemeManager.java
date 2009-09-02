package com.tmobile.thememanager;

public interface ThemeManager {
    public static final String TAG = "ThemeManager";

    public static boolean DEBUG = true;
    
    /**
     * Commonly passed between activities.
     * 
     * @see com.tmobile.thememanager.widget.ThemeAdapter.ThemeItem
     */
    public static final String EXTRA_THEME_ITEM = "theme_item";
}
