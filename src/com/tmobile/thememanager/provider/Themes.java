package com.tmobile.thememanager.provider;

import android.content.Context;
import android.net.Uri;

public class Themes {
    public static final String AUTHORITY = "com.tmobile.thememanager.themes";

    public static final Uri CONTENT_URI =
        Uri.parse("content://" + AUTHORITY);

    private Themes() {}
    
    public static Uri getThemeUri(Context context, String packageName, String themeId) {
        return ThemeColumns.CONTENT_URI.buildUpon()
            .appendPath(packageName)
            .appendPath(themeId).build();
    }
    
    public interface ThemeColumns {
        public static final Uri CONTENT_URI =
            Uri.parse("content://" + AUTHORITY + "/theme");

        public static final Uri CONTENT_PLURAL_URI =
            Uri.parse("content://" + AUTHORITY + "/themes");

        public static final String _ID = "_id";
        public static final String THEME_ID = "theme_id";
        public static final String THEME_PACKAGE = "theme_package";
    }
}
