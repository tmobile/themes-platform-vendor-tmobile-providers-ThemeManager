package com.tmobile.thememanager.provider;

import android.net.Uri;
import android.text.TextUtils;

public class PackageResources {
    public static final String AUTHORITY = "com.tmobile.thememanager.packageresources";

    public static final Uri CONTENT_URI =
        Uri.parse("content://" + AUTHORITY);

    private PackageResources() {}

    private static void checkPackage(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            throw new IllegalArgumentException("packageName must not be null or empty.");
        }
    }

    /* package */ static Uri makeResourceIdUri(String packageName, long resId) {
        checkPackage(packageName);
        return CONTENT_URI.buildUpon()
            .appendPath(packageName)
            .appendEncodedPath("res")
            .appendPath(String.valueOf(resId))
            .build();
    }

    /* package */ static Uri makeResourceEntryUri(String packageName, String defType, String name) {
        checkPackage(packageName);
        return CONTENT_URI.buildUpon()
            .appendPath(packageName)
            .appendEncodedPath("res")
            .appendPath(defType)
            .appendPath(name)
            .build();
    }

    /* package */ static Uri makeAssetPathUri(String packageName, String assetPath) {
        checkPackage(packageName);
        return CONTENT_URI.buildUpon()
            .appendPath(packageName)
            .appendEncodedPath("assets")
            .appendPath(assetPath)
            .build();
    }
}
