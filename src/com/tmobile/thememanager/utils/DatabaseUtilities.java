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
