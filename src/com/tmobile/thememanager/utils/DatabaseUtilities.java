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

import android.database.Cursor;
import android.text.TextUtils;

public class DatabaseUtilities {
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

    /**
     * @deprecated Use {@link #cursorToLong(Cursor, long)}.
     */
    public static long cursorToResult(Cursor cursor, long defaultValue) {
        return cursorToLong(cursor, defaultValue);
    }

    /**
     * Return the first column of the first row of the supplied cursor as a
     * long. Useful for accessing the result of a simple 1x1 query.
     */
    public static long cursorToLong(Cursor cursor, long defaultValue) {
        if (cursor == null) {
            return defaultValue;
        }
        try {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } finally {
            cursor.close();
        }
        return defaultValue;
    }

    /**
     * @see #cursorToLong(Cursor, long)
     */
    public static boolean cursorToBoolean(Cursor cursor, boolean defaultValue) {
        if (cursor == null) {
            return defaultValue;
        }
        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0) != 0;
            }
        } finally {
            cursor.close();
        }
        return defaultValue;
    }
}
