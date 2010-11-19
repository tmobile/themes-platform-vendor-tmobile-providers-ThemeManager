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

package com.tmobile.thememanager.provider;

import com.tmobile.thememanager.Constants;

import android.app.ActivityManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DrmStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Proxy uri-based access to assets from theme packages. DRM security is upheld
 * at this layer which is why we can't just use the built-in android.resource://
 * uri scheme.
 */
public class PackageResourcesProvider extends ContentProvider {
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    private static final int TYPE_RESOURCE_ID = 0;
    private static final int TYPE_RESOURCE_ENTRY = 1;
    private static final int TYPE_ASSET_PATH = 2;

    /* Cache AssetManager objects to speed up ringtone manipulation. */
    private static final Map<String, Resources> mResourcesTable =
        new HashMap<String, Resources>();

    @Override
    public boolean onCreate() {
        return true;
    }

    private static AssetManager createAssetManager(String packageFileName,
            String packageLockedZipFile) {
        AssetManager assets = new AssetManager();
        assets.addAssetPath(packageFileName);
        if (packageLockedZipFile != null) {
            assets.addAssetPath(packageLockedZipFile);
        }
        return assets;
    }

    public static final synchronized Resources getResourcesForTheme(Context context,
            String packageName) throws NameNotFoundException {
        Resources r = mResourcesTable.get(packageName);
        if (r != null) {
            return r;
        }

        PackageInfo pi = context.getPackageManager().getPackageInfo(packageName, 0);
        if (pi == null || pi.applicationInfo == null) {
            return null;
        }

        return createResourcesForTheme(context, packageName,
                pi.applicationInfo.publicSourceDir, pi.getLockedZipFilePath());
    }

    public static final synchronized Resources getResourcesForTheme(Context context,
            PackageInfo pi) {
        Resources r = mResourcesTable.get(pi.packageName);
        if (r != null) {
            return r;
        }
        return createResourcesForTheme(context, pi.packageName,
                pi.applicationInfo.publicSourceDir, pi.getLockedZipFilePath());
    }

    private static final synchronized Resources createResourcesForTheme(Context context,
            String packageName, String packageFileName, String packageLockedZipFile) {
        AssetManager assets = createAssetManager(packageFileName, packageLockedZipFile);

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);

        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        Configuration config = am.getConfiguration();
        Resources r = new Resources(assets, metrics, config);

        mResourcesTable.put(packageName, r);
        return r;
    }

    private static final synchronized void deleteResourcesForTheme(String packageName) {
        mResourcesTable.remove(packageName);
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        /*
         * When the C++ side (usually through MediaPlayerService) tries to open
         * the file, it uses ActivityManagerService#openContentUri which differs
         * in implementation from ContentResolver#openFileDescriptor(Uri, ...).
         * Specifically, openFile is called directly instead of openAssetFile,
         * with no logic in place to wrap the AssetFileDescriptor returned.
         */
        if (Constants.DEBUG) {
            Log.d(Constants.TAG, "openFile called in place of openAssetFile!");
        }
        AssetFileDescriptor afd = openAssetFile(uri, mode);
        if (afd == null) {
            return null;
        }
        return afd.getParcelFileDescriptor();
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        int type = URI_MATCHER.match(uri);

        List<String> segments = uri.getPathSegments();
        if (segments.size() < 3) {
            throw new IllegalArgumentException("Can't handle URI: " + uri);
        }

        String packageName = segments.get(0);
        Resources packageRes = null;
        try {
            packageRes = getResourcesForTheme(getContext(), packageName);
        } catch (NameNotFoundException e) {
            throw new FileNotFoundException(e.toString());
        }
        if (packageRes == null) {
            throw new FileNotFoundException("Unable to access package: " + packageName);
        }

        switch (type) {
            case TYPE_RESOURCE_ID:
            case TYPE_RESOURCE_ENTRY:
                int resId;
                if (type == TYPE_RESOURCE_ID) {
                    resId = (int)ContentUris.parseId(uri);
                } else {
                    resId = packageRes.getIdentifier(segments.get(3), segments.get(2),
                            packageName);
                }
                if (resId == 0) {
                    throw new IllegalArgumentException("No resource found for URI: " + uri);
                }
                return packageRes.openRawResourceFd(resId);

            case TYPE_ASSET_PATH:
                String assetPath = uri.getLastPathSegment();
                if (assetPath.contains("/locked/")) {
                    /* Make sure the caller has DRM access permission.  This should basically only be the media service process.  This call technically checks whether our own process holds this permission as well so it's extremely important the ThemeManager never requests this in the manifest. */
                    DrmStore.enforceAccessDrmPermission(getContext());
                }
                try {
                    return packageRes.getAssets().openFd(assetPath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    static {
        /* See PackageResources#makeRingtoneUri. */
        URI_MATCHER.addURI(PackageResources.AUTHORITY, "*/res/#", TYPE_RESOURCE_ID);
        URI_MATCHER.addURI(PackageResources.AUTHORITY, "*/res/*/*", TYPE_RESOURCE_ENTRY);
        URI_MATCHER.addURI(PackageResources.AUTHORITY, "*/assets/*", TYPE_ASSET_PATH);
    }
}
