/**
 *
 */
package com.tmobile.thememanager.provider;

import android.content.Context;
import android.content.res.CustomTheme;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.provider.Themes.ThemeColumns;

/**
 * There is plenty of leaky abstraction present in this class as it was ported
 * over from an original design no longer relevant.  Specifically, the interface
 * wraps a cursor but must be positioned prior to usage if more than one row
 * exists.
 */
public class ThemeItem {
    public static final int TYPE_CURSOR = 0;

    public int type;

    private Cursor mCursor;
    private int mColumnThemeId;
    private int mColumnThemePackage;
    private int mColumnName;
    private int mColumnStyleName;
    private int mColumnAuthor;
    private int mColumnIsDRM;
    private int mColumnWallpaperName;
    private int mColumnWallpaperUri;
    private int mColumnRingtoneName;
    private int mColumnRingtoneUri;
    private int mColumnNotifRingtoneName;
    private int mColumnNotifRingtoneUri;
    private int mColumnThumbnailUri;
    private int mColumnIsSystem;

    public static ThemeItem getInstance(Context context, Uri uri) {
        Cursor c = context.getContentResolver().query(uri, null, null, null, null);
        if (c != null && c.moveToFirst() == true) {
            return new ThemeItem(c);
        }
        return null;
    }

    public ThemeItem(Cursor c) {
        this.type = TYPE_CURSOR;
        mCursor = c;
        mColumnThemeId = c.getColumnIndex(ThemeColumns.THEME_ID);
        mColumnThemePackage = c.getColumnIndex(ThemeColumns.THEME_PACKAGE);
        mColumnName = c.getColumnIndex(ThemeColumns.NAME);
        mColumnStyleName = c.getColumnIndex(ThemeColumns.STYLE_NAME);
        mColumnAuthor = c.getColumnIndex(ThemeColumns.AUTHOR);
        mColumnIsDRM = c.getColumnIndex(ThemeColumns.IS_DRM);
        mColumnWallpaperName = c.getColumnIndex(ThemeColumns.WALLPAPER_NAME);
        mColumnWallpaperUri = c.getColumnIndex(ThemeColumns.WALLPAPER_URI);
        mColumnRingtoneName = c.getColumnIndex(ThemeColumns.RINGTONE_NAME);
        mColumnRingtoneUri = c.getColumnIndex(ThemeColumns.RINGTONE_URI);
        mColumnNotifRingtoneName = c.getColumnIndex(ThemeColumns.NOTIFICATION_RINGTONE_NAME);
        mColumnNotifRingtoneUri = c.getColumnIndex(ThemeColumns.NOTIFICATION_RINGTONE_URI);
        mColumnThumbnailUri = c.getColumnIndex(ThemeColumns.THUMBNAIL_URI);
        mColumnIsSystem = c.getColumnIndex(ThemeColumns.IS_SYSTEM);
    }

    public void close() {
        switch (type) {
            case TYPE_CURSOR:
                mCursor.close();
                break;
        }
    }

    public void setPosition(int position) {
        switch (type) {
            case TYPE_CURSOR:
                mCursor.moveToPosition(position);
                break;
        }
    }

    public Uri getUri(Context context) {
        return Themes.getThemeUri(context, getPackageName(), getThemeId());
    }

    private Uri parseUriNullSafe(String uriString) {
        return (uriString != null ? Uri.parse(uriString) : null);
    }

    public String getName() {
        switch (type) {
            case TYPE_CURSOR:
                return mCursor.getString(mColumnName);
        }
        Log.e(ThemeManager.TAG, "Unknown type " + type);
        return null;
    }

    /**
     * Access the name to be displayed for the theme when packages sans
     * wallpaper and ringtone. For different parts of the UI.
     */
    public String getStyleName() {
        switch (type) {
            case TYPE_CURSOR:
                return mCursor.getString(mColumnStyleName);
        }
        Log.e(ThemeManager.TAG, "Unknown type " + type);
        return null;
    }

    public String getAuthor() {
        switch (type) {
            case TYPE_CURSOR:
                return mCursor.getString(mColumnAuthor);
        }
        Log.e(ThemeManager.TAG, "Unknown type " + type);
        return null;
    }

    public boolean isDRMProtected() {
        switch (type) {
            case TYPE_CURSOR:
                return mCursor.getInt(mColumnIsDRM) != 0;
        }
        Log.e(ThemeManager.TAG, "Unknown type " + type);
        return true;
    }

    /** @deprecated */
    public int getResourceId(Context context) {
        switch (type) {
            case TYPE_CURSOR:
                return CustomTheme.getStyleId(context, getPackageName(),
                        getThemeId());
        }
        Log.e(ThemeManager.TAG, "Unknown type " + type);
        return -1;
    }

    public String getThemeId() {
        switch (type) {
            case TYPE_CURSOR:
                return mCursor.getString(mColumnThemeId);
        }
        Log.e(ThemeManager.TAG, "Unknown type " + type);
        return null;
    }

    public String getPackageName() {
        switch (type) {
            case TYPE_CURSOR:
                return mCursor.getString(mColumnThemePackage);
        }
        Log.e(ThemeManager.TAG, "Unknown type " + type);
        return null;
    }

    /**
     * Requests a unique identifier for a wallpaper. Useful to distinguish
     * different wallpaper items contained in a single theme package. Though
     * the result appears to be a filename, it should never be treated in
     * this way. It is merely useful as a unique key to feed a BitmapStore
     * surrounding this theme package.
     */
    public String getWallpaperIdentifier() {
        switch (type) {
            case TYPE_CURSOR:
                return mCursor.getString(mColumnWallpaperName);
        }
        Log.e(ThemeManager.TAG, "Unknown type " + type);
        return null;
    }

    public Uri getWallpaperUri(Context context) {
        switch (type) {
            case TYPE_CURSOR:
                return parseUriNullSafe(mCursor.getString(mColumnWallpaperUri));
        }
        Log.e(ThemeManager.TAG, "Unknown type " + type);
        return null;
    }

    public Uri getRingtoneUri(Context context) {
        switch (type) {
            case TYPE_CURSOR:
                return parseUriNullSafe(mCursor.getString(mColumnRingtoneUri));
        }
        Log.e(ThemeManager.TAG, "Unknown type " + type);
        return null;
    }

    public String getRingtoneName() {
        switch (type) {
            case TYPE_CURSOR:
                return mCursor.getString(mColumnRingtoneName);
        }
        Log.e(ThemeManager.TAG, "Unknown type " + type);
        return null;
    }

    public Uri getNotificationRingtoneUri(Context context) {
        switch (type) {
            case TYPE_CURSOR:
                return parseUriNullSafe(mCursor.getString(mColumnNotifRingtoneUri));
        }
        Log.e(ThemeManager.TAG, "Unknown type " + type);
        return null;
    }

    public String getNotificationRingtoneName() {
        switch (type) {
            case TYPE_CURSOR:
                return mCursor.getString(mColumnNotifRingtoneName);
        }
        Log.e(ThemeManager.TAG, "Unknown type " + type);
        return null;
    }

    public Uri getThumbnailUri() {
        switch (type) {
            case TYPE_CURSOR:
                return parseUriNullSafe(mCursor.getString(mColumnThumbnailUri));
        }
        Log.e(ThemeManager.TAG, "Unknown type " + type);
        return null;
    }

    /** @deprecated */
    public String getSoundPackName() {
        switch (type) {
            case TYPE_CURSOR:
                return null;
        }
        Log.e(ThemeManager.TAG, "Unknown type " + type);
        return null;
    }

    /**
     * Tests whether the theme item can be uninstalled. This condition
     * is true for all theme APKs not part of the system image.
     *
     * @return Returns true if the theme can be uninstalled.
     */
    public boolean isRemovable() {
        switch (type) {
            case TYPE_CURSOR:
                return mCursor.getInt(mColumnIsSystem) == 0;
        }
        Log.e(ThemeManager.TAG, "Unknown type " + type);
        return false;
    }

    public boolean equals(CustomTheme theme) {
        switch (type) {
            case TYPE_CURSOR:
                if (theme == null) {
                    return false;
                }
                if (getPackageName().equals(theme.getThemePackageName()) == false) {
                    return false;
                }
                return theme.getThemeId().equals(getThemeId());
        }
        Log.e(ThemeManager.TAG, "Unknown type " + type);
        return false;
    }

    public String toString() {
        StringBuilder b = new StringBuilder();

        b.append('{');
        b.append("type=").append(type).append("; ");
        b.append("pkg=").append(getPackageName()).append("; ");
        b.append("themeId=").append(getThemeId()).append("; ");
        b.append("name=").append(getName()).append("; ");
        b.append("drm=").append(isDRMProtected());
        b.append('}');

        return b.toString();
    }
}
