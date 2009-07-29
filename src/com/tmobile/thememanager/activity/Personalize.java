package com.tmobile.thememanager.activity;

import com.tmobile.thememanager.R;
import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.receiver.WallpaperChanged;
import com.tmobile.thememanager.utils.BitmapStore;
import com.tmobile.thememanager.widget.PersonalizePreference;
import com.tmobile.thememanager.widget.PersonalizeRingtonePreference;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.util.TypedValue;

public class Personalize extends PreferenceActivity {
    /* KEY_WALLPAPER is being dual-utilized, both for the preference key and 
     * for the BitmapStore key for the wallpaper thumb. Sorry for the
     * confusion :) */
    private static final String KEY_WALLPAPER = "wallpaper";
    private static final String KEY_RINGTONE = "ringtone";
    private static final String KEY_NOTIFICATION = "notification";
    private static final String KEY_THEME = "themes";
//    private static final String KEY_SHOP = "shop";
    
    private static BitmapStore sWallpaperStore;
    
    private PersonalizePreference mWallpaperPref;
    private PersonalizeRingtonePreference mRingtonePref;
    private PersonalizeRingtonePreference mNotificationPref;
    private PersonalizePreference mThemesPref;
//    private PersonalizePreference mShopPref;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WallpaperChanged.setActivity(this);
        
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.personalize_pref);
        
        mWallpaperPref = (PersonalizePreference)findPreference(KEY_WALLPAPER);
        mRingtonePref = (PersonalizeRingtonePreference)findPreference(KEY_RINGTONE);
        mNotificationPref = (PersonalizeRingtonePreference)findPreference(KEY_NOTIFICATION);
        mThemesPref = (PersonalizePreference)findPreference(KEY_THEME);
//        mShopPref = (PersonalizePreference)findPreference(KEY_SHOP);
        
        mRingtonePref.setImage(R.drawable.ringtones);
        mNotificationPref.setImage(R.drawable.notifications);
        mThemesPref.setImage(R.drawable.themes);
//        mShopPref.setImage(R.drawable.wallet);
        
        updateWallpaper(null);
    }
    
    @Override
    protected void onDestroy() {
        WallpaperChanged.setActivity(null);
        super.onDestroy();
    }
    
    public void updateWallpaper(Bitmap wallpaperBitmap) {
        BitmapStore store = getStore(this);
        Bitmap thumb = null;
        if (wallpaperBitmap != null || (thumb = store.get(KEY_WALLPAPER)) == null) {
            int dimension = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, 
                    getResources().getDisplayMetrics());
            if (wallpaperBitmap == null) {
                Drawable wallpaper = getWallpaper();
                if (wallpaper != null && wallpaper instanceof BitmapDrawable) {
                    wallpaperBitmap = ((BitmapDrawable)wallpaper).getBitmap();
                }
            }

            if (wallpaperBitmap != null) {
                thumb = ThemeWallpaperChooser.createBitmapThumbnail(wallpaperBitmap,
                        dimension, dimension);
                store.put(KEY_WALLPAPER, thumb);
            }
        } else {
            if (ThemeManager.DEBUG) {
                Log.i(ThemeManager.TAG, "updateWallpaper: Cache HIT!");
            }
        }

        if (thumb != null) {
            mWallpaperPref.setImage(new BitmapDrawable(thumb));
        }
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mWallpaperPref) {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_SET_WALLPAPER),
                    getString(R.string.set_wallpaper)));
            return true;
        } else if (preference == mThemesPref) {
            ThemeChooser.show(Personalize.this);
            return true;
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
    }
    
    private static BitmapStore getStore(Context context) {
        if (sWallpaperStore == null) {
            sWallpaperStore = new BitmapStore(context.getCacheDir(), "personalize");
        }
        return sWallpaperStore;
    }
    
    /* Called by com.tmobile.thememanager.receiver.WallpaperChanged. */
    public static void deleteWallpaperCache(Context context) {
        getStore(context).remove(KEY_WALLPAPER);
    }
}
