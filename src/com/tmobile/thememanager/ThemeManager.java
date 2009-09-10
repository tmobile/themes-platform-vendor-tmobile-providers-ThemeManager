package com.tmobile.thememanager;

import android.content.Intent;

public interface ThemeManager {
    public static final String TAG = "ThemeManager";

    public static boolean DEBUG = true;

    /**
     * Commonly passed between activities.
     *
     * @see com.tmobile.thememanager.provider.ThemeItem
     */
    public static final String EXTRA_THEME_ITEM = "theme_item";

    /**
     * Permission required to send a broadcast to the ThemeManager requesting
     * theme change. This permission is not required to fire a chooser for
     * {@link #ACTION_SET_THEME}, which presents the ThemeManager's normal UI.
     */
    public static final String PERMISSION_CHANGE_THEME = "com.tmobile.permission.CHANGE_THEME";

    /**
     * Broadcast intent to use to change theme without going through the normal
     * ThemeManager UI.  Requires {@link #PERMISSION_SET_THEME}.
     */
    public static final String ACTION_CHANGE_THEME = "com.tmobile.intent.action.CHANGE_THEME";

    /**
     * Broadcast intent fired on theme change.
     */
    public static final String ACTION_THEME_CHANGED = "com.tmobile.intent.action.THEME_CHANGED";

    /**
     * Similar to {@link Intent#ACTION_SET_WALLPAPER}.
     */
    public static final String ACTION_SET_THEME = "com.tmobile.intent.action.SET_THEME";

    /**
     * URI for the item which should be checked in both the theme and style
     * choosers. If null, will use the current global theme.
     */
    public static final String EXTRA_THEME_EXISTING_URI = "com.tmobile.intent.extra.theme.EXISTING_URI";
}
