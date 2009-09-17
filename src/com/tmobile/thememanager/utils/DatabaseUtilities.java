package com.tmobile.thememanager.utils;

import android.net.Uri;
import android.text.TextUtils;

import java.util.List;

public class DatabaseUtilities {
    public static String[] getLastTwoPathSegments(Uri uri) {
        List<String> segments = uri.getPathSegments();
        int n = segments.size();
        if (n < 2) {
            throw new IllegalArgumentException(uri.toString() + ": " +
                    "Uri must have at least 2 path segments.");
        }
        return new String[] { segments.get(n - 2), segments.get(n - 1) };
    }

    public static String appendSelection(String selection, String extra) {
        if (TextUtils.isEmpty(extra)) {
            throw new IllegalArgumentException("extra must be non-null and non-empty");
        }
        if (TextUtils.isEmpty(selection)) {
            return extra;
        }
        return "(" + selection + ") AND " + extra;
    }
    
    public static String[] appendSelectionArgs(String[] selectionArgs, String... extra) {
        if (extra == null || extra.length == 0) {
            throw new IllegalArgumentException("extra must be non-null and non-empty");
        }
        
        if (selectionArgs == null || selectionArgs.length == 0) {
            return extra;
        }
        
        String[] newArgs = new String[selectionArgs.length + extra.length];
        System.arraycopy(selectionArgs, 0, newArgs, 0, selectionArgs.length);
        System.arraycopy(extra, 0, newArgs, selectionArgs.length, extra.length);
        
        return newArgs;
    }
}
