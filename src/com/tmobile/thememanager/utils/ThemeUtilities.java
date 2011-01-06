/*
 * Copyright (C) 2010, T-Mobile USA, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tmobile.thememanager.utils;

import com.tmobile.thememanager.Constants;
import com.tmobile.themes.ThemeManager;
import com.tmobile.themes.provider.ThemeItem;
import com.tmobile.themes.provider.Themes;
import com.tmobile.themes.provider.Themes.ThemeColumns;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ThemeInfo;
import android.content.res.Configuration;
import android.content.res.CustomTheme;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

public class ThemeUtilities {

    /**
     * Applies just the configuration portion of the theme. No wallpapers or
     * ringtones are set.
     */
    public static void applyStyle(Context context, ThemeItem theme) {
        applyStyleInternal(context, theme);

        /* Broadcast appearance/style change. */
        context.sendBroadcast(new Intent(ThemeManager.ACTION_THEME_CHANGED)
                .setDataAndType(theme.getUri(context), ThemeColumns.STYLE_CONTENT_ITEM_TYPE));
    }

    private static void applyStyleInternal(Context context, ThemeItem theme) {
        // New theme is applied, hence reset the count to 0.
        Intent intent = new Intent(Intent.ACTION_APP_LAUNCH_FAILURE_RESET,
                Uri.fromParts("package", "com.tmobile.thememanager.activity", null));
        context.sendBroadcast(intent);

        Themes.markAppliedTheme(context, theme.getPackageName(), theme.getThemeId());

        /* Trigger a configuration change so that all apps will update their UI.  This will also
         * persist the theme for us across reboots. */
        updateConfiguration(context, theme);
    }

    /**
     * Applies a full theme.  This is a superset of applyStyle.
     */
    public static void applyTheme(Context context, ThemeItem theme) {
        applyTheme(context, theme, new Intent().setType(ThemeColumns.CONTENT_ITEM_TYPE));
    }

    /**
     * Extended API to {@link #applyTheme(Context,ThemeItem)} which allows the
     * caller to override certain components of a theme with user-supplied
     * values.
     */
    public static void applyTheme(Context context, ThemeItem theme, Intent request) {
        String themeType = request.getType();
        boolean extendedThemeChange =
            request.getBooleanExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, false);
        boolean dontSetLockWallpaper =
            request.getBooleanExtra(ThemeManager.EXTRA_DONT_SET_LOCK_WALLPAPER, false);

        Uri wallpaperUri = null;
        Uri lockWallpaperUri = null;
        ComponentName liveWallPaperComponent = null;
        Uri ringtoneUri = null;
        Uri notificationRingtoneUri = null;

        /*
         * Extended API is used by profile switch to supply theme "overrides".
         */
        if (extendedThemeChange) {
            wallpaperUri = (Uri)request.getParcelableExtra(ThemeManager.EXTRA_WALLPAPER_URI);
            lockWallpaperUri = (Uri)request.getParcelableExtra(ThemeManager.EXTRA_LOCK_WALLPAPER_URI);
            ringtoneUri = (Uri)request.getParcelableExtra(ThemeManager.EXTRA_RINGTONE_URI);
            notificationRingtoneUri = (Uri)request.getParcelableExtra(ThemeManager.EXTRA_NOTIFICATION_RINGTONE_URI);
            liveWallPaperComponent = (ComponentName)request.getParcelableExtra(ThemeManager.EXTRA_LIVE_WALLPAPER_COMPONENT);
        }

        if (Constants.DEBUG) {
            Log.i(Constants.TAG, "applyTheme: theme=" + theme.getUri(context) +
                    ", wallpaperUri=" + wallpaperUri +
                    ", lockWallpaperUri=" + lockWallpaperUri +
                    ", liveWallPaperComponent=" + liveWallPaperComponent +
                    ", ringtoneUri=" + ringtoneUri +
                    ", notificationRingtoneUri=" + notificationRingtoneUri);
        }

        if (liveWallPaperComponent != null) {
            WallpaperUtilities.setLiveWallpaper(context, liveWallPaperComponent);
        } else {
            if (wallpaperUri == null) {
                wallpaperUri = theme.getWallpaperUri(context);
            }
            if (wallpaperUri != null) {
                WallpaperUtilities.setWallpaper(context, wallpaperUri);
            }
        }
        if (!dontSetLockWallpaper) {
            if (lockWallpaperUri == null) {
                lockWallpaperUri = theme.getLockWallpaperUri(context);
            }
            if (lockWallpaperUri != null) {
                if (WallpaperUtilities.supportsLockWallpaper(context)) {
                    WallpaperUtilities.setLockWallpaper(context, lockWallpaperUri);
                }
            }
        }

        if (ringtoneUri == null) {
            ringtoneUri = theme.getRingtoneUri(context);
        }
        if (ringtoneUri != null) {
            /* Check for special silent uri */
            RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE,
                    ThemeManager.SILENT_RINGTONE_URI.equals(ringtoneUri) ? null : ringtoneUri);
        }

        if (notificationRingtoneUri == null) {
            notificationRingtoneUri = theme.getNotificationRingtoneUri(context);
        }
        if (notificationRingtoneUri != null) {
            /* Check for special silent uri */
            RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION,
                    ThemeManager.SILENT_RINGTONE_URI.equals(notificationRingtoneUri) ? null :
                            notificationRingtoneUri);
        }

        applyStyleInternal(context, theme);

        /* Broadcast theme change. */
        context.sendBroadcast(new Intent(ThemeManager.ACTION_THEME_CHANGED)
                .setDataAndType(theme.getUri(context), themeType));
    }

    public static void updateConfiguration(Context context, CustomTheme theme) {
        updateConfiguration(context, theme.getThemePackageName(), theme.getThemeId());
    }

    public static void updateConfiguration(Context context, ThemeItem theme) {
        updateConfiguration(context, theme.getPackageName(), theme.getThemeId());
    }

    public static void updateConfiguration(Context context, PackageInfo pi, ThemeInfo ti) {
        updateConfiguration(context, pi.packageName, ti.themeId);
    }

    private static void updateConfiguration(Context context, String packageName, String themeId) {
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        Configuration currentConfig = am.getConfiguration();

        currentConfig.customTheme = new CustomTheme(themeId, packageName);
        am.updateConfiguration(currentConfig);
    }

    public static CustomTheme getAppliedTheme(Context context) {
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        Configuration config = am.getConfiguration();
        return (config.customTheme != null ? config.customTheme : CustomTheme.getBootTheme());
    }

    public static int compareTheme(ThemeItem item, PackageInfo pi, ThemeInfo ti) {
        int cmp = item.getPackageName().compareTo(pi.packageName);
        if (cmp != 0) {
            return cmp;
        }
        return item.getThemeId().compareTo(ti.themeId);
    }

    public static boolean themeEquals(PackageInfo pi, ThemeInfo ti, CustomTheme current) {
        if (!pi.packageName.equals(current.getThemePackageName())) {
            return false;
        }
        if (!ti.themeId.equals(current.getThemeId())) {
            return false;
        }
        return true;
    }
}
