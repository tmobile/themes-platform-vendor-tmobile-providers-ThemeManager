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
