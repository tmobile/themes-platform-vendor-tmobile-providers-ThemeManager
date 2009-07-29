package com.tmobile.thememanager.receiver;

import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.activity.Personalize;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class WallpaperChanged extends BroadcastReceiver {
    private static Personalize mActivity;
    
    public static void setActivity(Personalize activity) {
        mActivity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ThemeManager.DEBUG) {
            Log.i(ThemeManager.TAG, "wallpaper changed, activity=" + mActivity);
        }
        if (mActivity != null) {
            Drawable wallpaper = context.getApplicationContext().getWallpaper();
            if (wallpaper != null && wallpaper instanceof BitmapDrawable) {
                mActivity.updateWallpaper(((BitmapDrawable)wallpaper).getBitmap());
            }
        } else {
            Personalize.deleteWallpaperCache(context);
        }
    }
}
