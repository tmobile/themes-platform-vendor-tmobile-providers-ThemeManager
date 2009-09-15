package com.tmobile.thememanager.utils;

import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.provider.ThemeItem;
import com.tmobile.thememanager.provider.Themes;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.CustomTheme;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import java.io.InputStream;

public class ThemeUtilities {
    /**
     * Applies just the configuration portion of the theme. No wallpapers or
     * ringtones are set.
     */
    public static void applyStyle(Context context, ThemeItem theme) {
        // New theme is applied, hence reset the count to 0.
        Intent intent = new Intent(Intent.ACTION_APP_LAUNCH_FAILURE_RESET,
                Uri.fromParts("package", "com.tmobile.thememanager.activity", null));
        context.sendBroadcast(intent);
        
        Themes.markAppliedTheme(context, theme.getPackageName(), theme.getThemeId());

        /* Trigger a configuration change so that all apps will update their UI.  This will also
         * persist the theme for us across reboots. */
        updateConfiguration(context, theme);

        /* Broadcast theme change. */
        context.sendBroadcast(new Intent(ThemeManager.ACTION_THEME_CHANGED,
                theme.getUri(context)));
    }

    /**
     * Applies a full theme.  This is a superset of applyStyle.
     */
    public static void applyTheme(Context context, ThemeItem theme) {
        applyTheme(context, theme, null, null, null);
    }

    /**
     * Extended API to {@link #applyTheme(Context,ThemeItem)} which allows the
     * caller to override certain components of a theme with user-supplied
     * values.
     */
    public static void applyTheme(Context context, ThemeItem theme,
            Uri wallpaperUri, Uri ringtoneUri, Uri notificationRingtoneUri) {
        if (ThemeManager.DEBUG) {
            Log.i(ThemeManager.TAG, "applyTheme: theme=" + theme.getUri(context) +
                    ", wallpaperUri=" + wallpaperUri +
                    ", ringtoneUri=" + ringtoneUri +
                    ", notificationRingtoneUri=" + notificationRingtoneUri);
        }

        if (wallpaperUri == null) {
            wallpaperUri = theme.getWallpaperUri(context);
        }
        if (wallpaperUri != null) {
            setWallpaper(context, wallpaperUri);
        }

        if (ringtoneUri == null) {
            ringtoneUri = theme.getRingtoneUri(context);
        }
        if (ringtoneUri != null) {
            setDefaultRingtone(context, ringtoneUri);
        }

        if (notificationRingtoneUri == null) {
            notificationRingtoneUri = theme.getNotificationRingtoneUri(context);
        }
        if (notificationRingtoneUri != null) {
            setDefaultNotificationRingtone(context, notificationRingtoneUri);
        }

        applyStyle(context, theme);
    }

    private static void updateConfiguration(Context context, ThemeItem theme) {
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        Configuration currentConfig = am.getConfiguration();

        currentConfig.customTheme = new CustomTheme(theme.getThemeId(), theme.getPackageName());
        am.updateConfiguration(currentConfig);
    }

    private static void setWallpaper(Context context, Uri uri) {
        try {
            InputStream in = context.getContentResolver().openInputStream(uri);
            try {
                context.setWallpaper(in);
            } finally {
                IOUtilities.close(in);
            }
        } catch (Exception e) {
            Log.e(ThemeManager.TAG, "Could not set wallpaper", e);
        }
    }

    private static void setDefaultRingtone(Context context, Uri ringtoneUri) {
        if (ThemeManager.DEBUG) {
            Log.i(ThemeManager.TAG, "ringtoneUri=" + ringtoneUri);
        }

        if (ringtoneUri != null) {
            RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE,
                    ringtoneUri);
        }
    }

    private static void setDefaultNotificationRingtone(Context context, Uri ringtoneUri) {
        if (ThemeManager.DEBUG) {
            Log.i(ThemeManager.TAG, "ringtoneUri=" + ringtoneUri);
        }

        if (ringtoneUri != null) {
            RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION,
                    ringtoneUri);
        }
    }
}
