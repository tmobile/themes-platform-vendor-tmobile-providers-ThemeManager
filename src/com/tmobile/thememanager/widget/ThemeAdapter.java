package com.tmobile.thememanager.widget;

import android.content.Context;
import android.content.ContentValues;
import android.content.pm.ApplicationInfo;
import android.content.pm.BaseThemeInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ThemeInfo;
import android.content.pm.PackageManager;
import android.content.res.CustomTheme;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.text.TextUtils;

import java.util.*;

import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.provider.PackageResources;
import com.tmobile.thememanager.utils.IOUtilities;

/**
 * Re-usable adapter which fills itself with all currently installed visual
 * themes. Includes a convenient inner-class which can represent all types of
 * visual themes with helpful accessors.
 * 
 * @todo Possibly refactor into something even more general?
 */
public abstract class ThemeAdapter extends BaseAdapter {
    final ArrayList<ThemeItem> mThemes = new ArrayList<ThemeItem>();
    private final Context mContext;
    private final LayoutInflater mInflater;

    private static final String DB_SELECTION = OpenDatabaseHelper.THEME_PACKAGE + " = ? AND " + OpenDatabaseHelper.THEME_ID + " = ?";

    public static SQLiteOpenHelper createThemeItemDbHelper(Context context) {
        return new OpenDatabaseHelper(context);
    }

    public static void insertIntoThemeItemDb(String packageName, String themeId, boolean overwrite) {
        SQLiteOpenHelper helper = ThemeManager.getThemesDatabaseHelper();
        Cursor c = helper.getReadableDatabase().query(
                OpenDatabaseHelper.TABLE_NAME,
                new String[] { OpenDatabaseHelper._ID },
                DB_SELECTION,
                new String[] { packageName, themeId },
                null, null, null);
        int count = 0;
        if (c != null) {
            count = c.getCount();
            c.close();
        }
        ContentValues values = new ContentValues();
        values.put(OpenDatabaseHelper.THEME_PACKAGE, packageName);
        values.put(OpenDatabaseHelper.THEME_ID, themeId);
        if (count == 0) {
            helper.getWritableDatabase().insert(OpenDatabaseHelper.TABLE_NAME, null, values);
        } else if (overwrite && c.getCount() == 1) {
            helper.getWritableDatabase().update(
                    OpenDatabaseHelper.TABLE_NAME, values, DB_SELECTION,
                    new String[] { packageName, themeId });
        }
    }

    public static boolean themeItemExistsInDb(String packageName, String themeId) {
        SQLiteOpenHelper helper = ThemeManager.getThemesDatabaseHelper();
        Cursor c = helper.getReadableDatabase().query(
                OpenDatabaseHelper.TABLE_NAME,
                new String[] { OpenDatabaseHelper._ID },
                DB_SELECTION,
                new String[] { packageName, themeId },
                null, null, null);
        int count = 0;
        if (c != null) {
            count = c.getCount();
            c.close();
        }
        return count == 1;
    }

    public static void deleteFromThemeItemDb(String packageName, String themeId) {
        SQLiteOpenHelper helper = ThemeManager.getThemesDatabaseHelper();
            helper.getWritableDatabase().delete(
                OpenDatabaseHelper.TABLE_NAME, DB_SELECTION,
                new String[] { packageName, themeId });
    }

    public static void deleteFromThemeItemDb(String packageName) {
        SQLiteOpenHelper helper = ThemeManager.getThemesDatabaseHelper();
            helper.getWritableDatabase().delete(
                OpenDatabaseHelper.TABLE_NAME, OpenDatabaseHelper.THEME_PACKAGE + " = ?",
                new String[] { packageName });
    }

    public ThemeAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        loadThemes();
    }
    
    protected LayoutInflater getInflater() {
        return mInflater;
    }

    protected Context getContext() {
        return mContext;
    }

    public void addThemesFromPackage(PackageInfo pi) {
        addThemesFromPackage(pi, true);
    }

    private void addThemesFromPackage(PackageInfo pi, boolean notify) {
        if (pi == null) {
            return;
        }
        if (pi.themeInfos != null) {
            for (ThemeInfo ti: pi.themeInfos) {
                if (!TextUtils.isEmpty(ti.themeId) && ti.type == BaseThemeInfo.InfoObjectType.TYPE_THEME) {
                    mThemes.add(new ThemeItem(pi, ti));
                }
            }
        }
        if (notify == true) {
            notifyDataSetChanged();
        }
    }

    public void removeThemesByPackage(String pkg) {
        boolean matched = false;

        for (Iterator<ThemeItem> i = mThemes.iterator(); i.hasNext(); ) {
            ThemeItem theme = i.next();
            String packageName = theme.getPackageName();
            if (packageName != null && packageName.equals(pkg) == true) {
                i.remove();
                matched = true;
            }
        }

        if (matched == true) {
            notifyDataSetChanged();
        }
    }

    private void loadThemes() {
        mThemes.clear();

        Map<String, Integer> map = new HashMap<String, Integer>();
        SQLiteDatabase db = ThemeManager.getThemesDatabaseHelper().getReadableDatabase();
        Cursor c = db.query("themeitem_map", new String[] {
                            OpenDatabaseHelper.THEME_PACKAGE,
            }, null, null, null, null, OpenDatabaseHelper._ID);
        if (c != null) {
            try {
                boolean exist = c.moveToFirst();
                String currentPackageName = "";
                while (exist) {
                    String packageName = c.getString(c.getColumnIndex(OpenDatabaseHelper.THEME_PACKAGE));
                    if (!currentPackageName.equals(packageName)) {
                        currentPackageName = packageName;
                        map.put(packageName, mThemes.size());
                        try {
                            PackageInfo pi = getContext().getPackageManager().getPackageInfo(packageName, 0);
                            addThemesFromPackage(pi, false);
                        } catch (PackageManager.NameNotFoundException e) {
                            Log.e(ThemeManager.TAG, "Failed to find package", e);
                        }
                    }
                    exist = c.moveToNext();
                }
            } finally {
                c.close();
            }
        }
        notifyDataSetChanged();
    }

    public int getCount() {
        return mThemes.size();
    }

    public int findItem(CustomTheme theme) {
        if (theme == null) return -1;
        int n = mThemes.size();
        while (n-- > 0) {
            if (mThemes.get(n).equals(theme) == true) {
                return n;
            }
        }
        return -1;
    }

    public int findItem(ThemeItem theme) {
        if (theme == null) return -1;
        int n = mThemes.size();
        while (n-- > 0) {
            if (mThemes.get(n) == theme) {
                return n;
            }
        }
        return -1;
    }

    public ThemeItem getTheme(int position) {
        return (ThemeItem)getItem(position);
    }

    public Object getItem(int position) {
        if (position < 0 || position >= mThemes.size()) {
            return null;
        }
        return mThemes.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public int deleteThemeItem(int pos) {
        if (pos != -1) {
            ThemeItem item = mThemes.get(pos);
            deleteFromThemeItemDb(item.getPackageName(), item.getThemeId());
            SQLiteOpenHelper helper = ThemeManager.getThemesDatabaseHelper();
            Cursor c = helper.getReadableDatabase().query(OpenDatabaseHelper.TABLE_NAME,
                    new String[] { OpenDatabaseHelper._ID },
                    OpenDatabaseHelper.THEME_PACKAGE + " = ?",
                    new String[] { item.getPackageName() }, null, null, null);
            int count = 0;
            if (c != null) {
                count = c.getCount();
                c.close();
            }
            if (count == 0) {
                // un-install theme package
                getContext().getPackageManager().deletePackage(item.getPackageName(), null, 0);
            }

            mThemes.remove(pos);
            notifyDataSetChanged();
        }

        CustomTheme defaultTheme = CustomTheme.getDefault();
        for (int i = 0; i < mThemes.size(); i++) {
            ThemeItem item = mThemes.get(i);
            if (item.getPackageName().equals(defaultTheme.getThemePackageName()) && item.getThemeId().equals(defaultTheme.getThemeId())) {
                return i;
            }
        }
        return 0;
    }

    public static class ThemeItem implements Parcelable {
        public static final int TYPE_FRAMEWORK = 0;
        public static final int TYPE_USER = 1;

        public int type;

        public PackageInfo packageInfo;
        public BaseThemeInfo info;
        private ThemeItem parent;

        String name;
        String themeId;
        int resId;

        protected ThemeItem() {
        }

        public ThemeItem(PackageInfo packageInfo, BaseThemeInfo info) {
            this.type = TYPE_USER;
            this.packageInfo = packageInfo;
            this.info = info;
        }

        public ThemeItem(String name, int resId) {
            this.type = TYPE_FRAMEWORK;
            this.name = name;
            this.resId = resId;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(type);
            dest.writeString(name);
            dest.writeString(themeId);
            dest.writeInt(resId);
            IOUtilities.writeParcelableToParcel(dest, packageInfo, 0);
            IOUtilities.writeParcelableToParcel(dest, info, 0);
            IOUtilities.writeParcelableToParcel(dest, parent, 0);
        }
        
        public static final Parcelable.Creator<ThemeItem> CREATOR =
                new Parcelable.Creator<ThemeItem>() {
            public ThemeItem createFromParcel(Parcel source) {
                ThemeItem item = new ThemeItem();
                
                item.type = source.readInt();
                item.name = source.readString();
                item.themeId = source.readString();
                item.resId = source.readInt();
                item.packageInfo = IOUtilities.readParcelableFromParcel(source,
                        PackageInfo.CREATOR);
                item.info = IOUtilities.readParcelableFromParcel(source,
                        BaseThemeInfo.CREATOR);
                item.parent = IOUtilities.readParcelableFromParcel(source,
                        ThemeItem.CREATOR);
                
                return item;
            }

            public ThemeItem[] newArray(int size) {
                return new ThemeItem[size];
            }
        };
        
        public int describeContents() {
            return 0;
        }

        public String getName() {
            switch (type) {
                case TYPE_FRAMEWORK:
                    return name;

                case TYPE_USER:
                    return info.name;
            }
            Log.e(ThemeManager.TAG, "Unknown type " + type);
            return null;
        }

        public String getAuthor() {
            switch (type) {
                case TYPE_FRAMEWORK:
                    return "T-Mobile";

                case TYPE_USER:
                    return info.author;
            }
            Log.e(ThemeManager.TAG, "Unknown type " + type);
            return null;
        }

        public boolean isDRMProtected() {
            switch (type) {
                case TYPE_FRAMEWORK:
                    return false;

                case TYPE_USER:
                    return info.isDrmProtected;
            }
            Log.e(ThemeManager.TAG, "Unknown type " + type);
            return true;
        }

        public int getResourceId() {
            switch (type) {
                case TYPE_FRAMEWORK:
                    return resId;

                case TYPE_USER:
                    return info.styleResourceId;
            }
            Log.e(ThemeManager.TAG, "Unknown type " + type);
            return -1;
        }

        public String getThemeId() {
            switch (type) {
                case TYPE_FRAMEWORK:
                    return null;

                case TYPE_USER:
                    return info.themeId;
            }
            Log.e(ThemeManager.TAG, "Unknown type " + type);
            return null;
        }

        public String getPackageName() {
            switch (type) {
                case TYPE_FRAMEWORK:
                    return null;

                case TYPE_USER:
                    return packageInfo.packageName;
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
                case TYPE_FRAMEWORK:
                    return null;
                    
                case TYPE_USER:
                    return info.wallpaperImageName;
            }
            Log.e(ThemeManager.TAG, "Unknown type " + type);
            return null;
        }
        
        public Uri getWallpaperUri(Context context) {
            switch (type) {
                case TYPE_FRAMEWORK:
                    return null;
                    
                case TYPE_USER:
                    if (info.wallpaperImageName == null) {
                        return null;
                    } else {
                        return PackageResources.getImageUri(context, getPackageName(),
                                info.wallpaperImageName);
                    }
            }
            Log.e(ThemeManager.TAG, "Unknown type " + type);
            return null;
        }
        
        public Uri getRingtoneUri(Context context) {
            switch (type) {
                case TYPE_FRAMEWORK:
                    return null;
                    
                case TYPE_USER:
                    if (info.ringtoneFileName == null) {
                        return null;
                    } else {
                        return PackageResources.getRingtoneUri(context, getPackageName(),
                                info.ringtoneFileName);
                    }
            }
            Log.e(ThemeManager.TAG, "Unknown type " + type);
            return null;
        }
        
        public String getRingtoneName() {
            switch (type) {
                case TYPE_FRAMEWORK:
                    return null;
                    
                case TYPE_USER:
                    return info.ringtoneName;
            }
            Log.e(ThemeManager.TAG, "Unknown type " + type);
            return null;
        }
        
        public Uri getNotificationRingtoneUri(Context context) {
            switch (type) {
                case TYPE_FRAMEWORK:
                    return null;
                    
                case TYPE_USER:
                    if (info.notificationRingtoneFileName == null) {
                        return null;
                    } else {
                        return PackageResources.getRingtoneUri(context, getPackageName(),
                                info.notificationRingtoneFileName);
                    }
            }
            Log.e(ThemeManager.TAG, "Unknown type " + type);
            return null;
        }
        
        public String getNotificationRingtoneName() {
            switch (type) {
                case TYPE_FRAMEWORK:
                    return null;
                    
                case TYPE_USER:
                    return info.notificationRingtoneName;
            }
            Log.e(ThemeManager.TAG, "Unknown type " + type);
            return null;
        }

        public String getSoundPackName() {
            switch (type) {
                case TYPE_FRAMEWORK:
                    return null;

                case TYPE_USER:
                    return info.soundPackName;
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
                case TYPE_FRAMEWORK:
                    return false;

                case TYPE_USER:
                    return (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0;
            }
            Log.e(ThemeManager.TAG, "Unknown type " + type);
            return false;
        }

        public boolean equals(CustomTheme theme) {
            switch (type) {
                case TYPE_FRAMEWORK:
                    if (theme == null) {
                        return getResourceId() == -1;
                    }
                    if (theme.getThemePackageName() != null) {
                        return false;
                    }
                    /*
                     * TODO: This test is incomplete because the CustomTheme
                     * object is no longer capable of expressing framework
                     * themes.  This is a bug in CustomTheme.
                     */
                    return false;

                case TYPE_USER:
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
            b.append("resId=").append(getResourceId()).append("; ");
            b.append("name=").append(getName()).append("; ");
            b.append("drm=").append(isDRMProtected());
            b.append('}');

            return b.toString();
        }
    }

    private static class OpenDatabaseHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "theme_item.db";
        private static final int DATABASE_VERSION = 1;

        private static final String TABLE_NAME = "themeitem_map";
        private static final String _ID = "_id";
        private static final String THEME_ID = "theme_id";
        private static final String THEME_PACKAGE = "theme_package";

        public OpenDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        /**
         * Creates database the first time we try to open it.
         */
        @Override
        public void onCreate(final SQLiteDatabase db) {
            createTables(db);
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, int oldVer, int newVer) {
            dropTables(db);
            createTables(db);
        }

        private void createTables(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE themeitem_map (" +
                    _ID + " INTEGER PRIMARY KEY, " +
                    THEME_PACKAGE + " TEXT NOT NULL, " +
                    THEME_ID + " TEXT" + ")");
            db.execSQL("CREATE INDEX themeitem_map_package ON themeitem_map (theme_package)");
            db.execSQL("CREATE UNIQUE INDEX themeitem_map_key ON themeitem_map (theme_package, theme_id)");
        }

        private void dropTables(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS themeitem_map");
        }
    }

}
