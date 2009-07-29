package com.tmobile.thememanager.utils;

import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.provider.PackageResources;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class WallpaperThumbnailCache extends ThemeBitmapStore {
    /* Either of the below can be provided. */
    private String mWallpaperPath;
    private Uri mWallpaperUri;

    public WallpaperThumbnailCache(Context context, String themePackage, Uri wallpaperUri) {
        super(context, themePackage);
        mWallpaperUri = wallpaperUri;
    }

    public WallpaperThumbnailCache(Context context, String themePackage,
            String wallpaperPath) {
        super(context, themePackage);
        mWallpaperPath = wallpaperPath;
    }

    /*
     * Weird hack to allow the wallpaper to be fetched from our content provider
     * rather than from disk, while still allowing multiple scaled versions
     * to exist in this cache store.
     */
    public Bitmap get(String key, BitmapFactory.Options options) {
        if (key != null) {
            return super.get(key, options);
        }
        
        Uri imageUri;
        if (mWallpaperUri != null) {
            imageUri = mWallpaperUri;
        } else if (mWallpaperPath != null) {
            imageUri = PackageResources.getImageUri(mContext, mPackage, mWallpaperPath);
            if (imageUri == null) {
                Log.e(ThemeManager.TAG, "Unable to find wallpaper for package " +
                        mPackage + " (path=" + mWallpaperPath + ")");
                return null;
            }
        } else {
            throw new IllegalArgumentException("Wallpaper URI or path must be non-null.");
        }
        
        InputStream in = null;
        try {
            in = mContext.getContentResolver().openInputStream(imageUri);
            return BitmapFactory.decodeStream(in, null, options);
        } catch (FileNotFoundException e) {
            return null;
        } finally {
            if (in != null) {
                IOUtilities.close(in);
            }
        }
    }
}
