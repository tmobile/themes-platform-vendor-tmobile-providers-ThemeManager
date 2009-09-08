package com.tmobile.thememanager.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.CustomTheme;
import android.view.LayoutInflater;
import android.widget.CursorAdapter;
import android.net.Uri;
import android.util.Log;
import android.database.Cursor;

import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.provider.Themes;
import com.tmobile.thememanager.provider.Themes.ThemeColumns;

/**
 * Re-usable adapter which fills itself with all currently installed visual
 * themes. Includes a convenient inner-class which can represent all types of
 * visual themes with helpful accessors.
 */
public abstract class ThemeAdapter extends CursorAdapter {
    /*
     * This array holds cached ThemeItem objects to preserve the original
     * ThemeAdapter API, prior to using CursorAdapter. When the underlying
     * cursor changes, we recreate the array at the correct size which could be
     * a source of performance problems under certain conditions.
     */
    private ThemeItem[] mThemes;

    private final LayoutInflater mInflater;

    public ThemeAdapter(Activity context) {
        super(context, loadThemes(context));
        mInflater = LayoutInflater.from(context);
        allocInternal();
    }
    
    protected LayoutInflater getInflater() {
        return mInflater;
    }

    private static Cursor loadThemes(Activity context) {
        return context.managedQuery(ThemeColumns.CONTENT_PLURAL_URI,
                null, null, ThemeColumns.NAME);
    }

    private void allocInternal() {
        Cursor c = getCursor();
        if (c != null) {
            mThemes = new ThemeItem[getCursor().getCount()];
        } else {
            mThemes = null;
        }
    }

    @Override
    public void notifyDataSetChanged() {
        allocInternal();
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        mThemes = null;
        super.notifyDataSetInvalidated();
    }

    public int findItem(CustomTheme theme) {
        if (theme == null) return -1;
        int n = getCount();
        while (n-- > 0) {
            ThemeItem item = getTheme(n);
            if (item.equals(theme) == true) {
                return n;
            }
        }
        return -1;
    }

    public ThemeItem getTheme(int position) {
        if (position >= 0 && getCount() >= 0) {
            if (mThemes[position] == null) {
                mThemes[position] = new ThemeItem((Cursor)getItem(position));
            }
            return mThemes[position];
        }
        return null;
    }

    public int deleteThemeItem(int pos) {
        if (pos != -1) {
            ThemeItem item = getTheme(pos);
            Themes.deleteTheme(mContext, item.getPackageName(), item.getThemeId());
            Cursor c = Themes.listThemesByPackage(mContext, item.getPackageName());
            if (c != null) {
                int count;
                try {
                    count = c.getCount();
                } finally {
                    c.close();
                }
                if (count == 0) {
                    // un-install theme package
                    mContext.getPackageManager().deletePackage(item.getPackageName(), null, 0);
                }
            }

            mThemes[pos] = null;
            notifyDataSetChanged();
        }

        return findItem(CustomTheme.getDefault());
    }

    /*
     * This class was once used to serve an entirely different purpose than it
     * does now. For this reason, some of the functionality might appear
     * redundant or strangely abstracted.
     */
    public static class ThemeItem {
        public static final int TYPE_CURSOR = 0;

        public int type;

        private Cursor mCursor;
        private int mColumnThemeId;
        private int mColumnThemePackage;
        private int mColumnName;
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

        protected ThemeItem() {
        }
        
        public ThemeItem(Cursor c) {
            this.type = TYPE_CURSOR;
            mCursor = c;
            mColumnThemeId = c.getColumnIndexOrThrow(ThemeColumns.THEME_ID);
            mColumnThemePackage = c.getColumnIndexOrThrow(ThemeColumns.THEME_PACKAGE);
            mColumnName = c.getColumnIndexOrThrow(ThemeColumns.NAME);
            mColumnAuthor = c.getColumnIndexOrThrow(ThemeColumns.AUTHOR);
            mColumnIsDRM = c.getColumnIndexOrThrow(ThemeColumns.IS_DRM);
            mColumnWallpaperName = c.getColumnIndexOrThrow(ThemeColumns.WALLPAPER_NAME);
            mColumnWallpaperUri = c.getColumnIndexOrThrow(ThemeColumns.WALLPAPER_URI);
            mColumnRingtoneName = c.getColumnIndexOrThrow(ThemeColumns.RINGTONE_NAME);
            mColumnRingtoneUri = c.getColumnIndexOrThrow(ThemeColumns.RINGTONE_URI);
            mColumnNotifRingtoneName = c.getColumnIndexOrThrow(ThemeColumns.NOTIFICATION_RINGTONE_NAME);
            mColumnNotifRingtoneUri = c.getColumnIndexOrThrow(ThemeColumns.NOTIFICATION_RINGTONE_URI);
            mColumnThumbnailUri = c.getColumnIndexOrThrow(ThemeColumns.THUMBNAIL_URI);
            mColumnIsSystem = c.getColumnIndexOrThrow(ThemeColumns.IS_SYSTEM);
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
}
