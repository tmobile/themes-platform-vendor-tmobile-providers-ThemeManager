package com.tmobile.thememanager.utils;

import android.content.Context;

import java.io.File;
import java.io.IOException;

public class ThemeBitmapStore extends BitmapStore {
    protected final Context mContext;
    protected final String mPackage;
    
    public ThemeBitmapStore(Context context, String themePackage) {
        super(getBasePath(context), themePackage);
        mContext = context;
        mPackage = themePackage;
    }
    
    public static void deletePackage(Context context, String packageName) {
        try {
            File file = new File(getBasePath(context) + File.separator + packageName);
            FileUtilities.deleteDirectory(file);
        } catch (IOException e) {}
    }

    private static File getBasePath(Context context) {
        return new File(context.getCacheDir() + File.separator + "bitmaps");
    }

    public Context getContext() {
        return mContext;
    }
    
    public String getPackage() {
        return mPackage;
    }
}
