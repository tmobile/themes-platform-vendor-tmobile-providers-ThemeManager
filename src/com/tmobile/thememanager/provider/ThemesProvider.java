package com.tmobile.thememanager.provider;

import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.provider.Themes.ThemeColumns;
import com.tmobile.thememanager.utils.DatabaseUtilities;
import com.tmobile.thememanager.utils.FileUtilities;
import com.tmobile.thememanager.utils.ThemeUtilities;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriMatcher;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ThemeInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.CustomTheme;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Binder;
import android.os.Process;
import android.util.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Provider
 */
public class ThemesProvider extends ContentProvider {
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    private static final String TABLE_NAME = "themeitem_map";

    private static final int TYPE_THEMES = 0;
    private static final int TYPE_THEME = 1;
    private static final int TYPE_THEME_SYSTEM = 2;

    private SQLiteOpenHelper mOpenHelper;

    private static class OpenDatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "theme_item.db";
        private static final int DATABASE_VERSION = 8;

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
                    ThemeColumns._ID + " INTEGER PRIMARY KEY, " +
                    ThemeColumns.THEME_PACKAGE + " TEXT NOT NULL, " +
                    ThemeColumns.THEME_ID + " TEXT, " +
                    ThemeColumns.IS_APPLIED + " INTEGER DEFAULT 0, " +
                    ThemeColumns.AUTHOR + " TEXT NOT NULL, " +
                    ThemeColumns.IS_DRM + " INTEGER DEFAULT 0, " +
                    ThemeColumns.IS_SYSTEM + " INTEGER DEFAULT 0, " +
                    ThemeColumns.NAME + " TEXT NOT NULL, " +
                    ThemeColumns.STYLE_NAME + " TEXT NOT NULL, " +
                    ThemeColumns.WALLPAPER_NAME + " TEXT, " +
                    ThemeColumns.WALLPAPER_URI + " TEXT, " +
                    ThemeColumns.LOCK_WALLPAPER_NAME + " TEXT, " +
                    ThemeColumns.LOCK_WALLPAPER_URI + " TEXT, " +
                    ThemeColumns.RINGTONE_NAME + " TEXT, " +
                    ThemeColumns.RINGTONE_URI + " TEXT, " +
                    ThemeColumns.NOTIFICATION_RINGTONE_NAME + " TEXT, " +
                    ThemeColumns.NOTIFICATION_RINGTONE_URI + " TEXT, " +
                    ThemeColumns.THUMBNAIL_URI + " TEXT, " +
                    ThemeColumns.PREVIEW_URI + " TEXT" +
                    ")");
            db.execSQL("CREATE INDEX themeitem_map_package ON themeitem_map (theme_package)");
            db.execSQL("CREATE UNIQUE INDEX themeitem_map_key ON themeitem_map (theme_package, theme_id)");

            db.execSQL("INSERT INTO themeitem_map (" +
                    ThemeColumns.THEME_PACKAGE + ", " +
                    ThemeColumns.THEME_ID + ", " +
                    ThemeColumns.AUTHOR + ", " +
                    ThemeColumns.IS_SYSTEM + ", " +
                    ThemeColumns.NAME + ", " +
                    ThemeColumns.STYLE_NAME +
                    ") VALUES (?, ?, ?, ?, ?, ?)",
                    new Object[] { "", "", "Google", 1, "Default", "Default" } );
        }

        private void dropTables(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS themeitem_map");
        }
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new OpenDatabaseHelper(getContext());

        /*
         * Detect theme package changes while the provider (that is, our
         * process) is alive. This is the more likely catch for theme package
         * changes as the user is somehow interacting with the theme manager
         * application when these events occur.
         */
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addCategory(Intent.CATEGORY_THEME_PACKAGE_INSTALLED_STATE_CHANGE);
        filter.addDataScheme("package");
        getContext().registerReceiver(mThemePackageReceiver, filter);

        /**
         * Start a background task to make sure the database is in sync with the
         * package manager. This will also detect inserted or deleted themes
         * which didn't come through our application, and that occurred while
         * this provider was not alive.
         * <p>
         * This is not the common case for users, but it is possible and must be
         * supported. Development invokes this feature often when restarting the
         * emulator with changes to theme packages, or by using "adb sync".
         */
        new VerifyInstalledThemesThread().start();

        return true;
    }

    private class VerifyInstalledThemesThread extends Thread {
        private final SQLiteDatabase mDb;

        public VerifyInstalledThemesThread() {
            mDb = mOpenHelper.getWritableDatabase();
        }

        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            long start;

            if (ThemeManager.DEBUG) {
                start = System.currentTimeMillis();
            }

            SQLiteDatabase db = mDb;
            db.beginTransaction();
            try {
                verifyPackages();
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();

                if (ThemeManager.DEBUG) {
                    Log.i(ThemeManager.TAG, "VerifyInstalledThemesThread took " +
                            (System.currentTimeMillis() - start) + " ms.");
                }
            }
        }

        private void verifyPackages() {
            /* List all currently installed theme packages. */
            List<PackageInfo> themePackages = getContext().getPackageManager()
                    .getInstalledThemePackages();

            CustomTheme appliedTheme = ThemeUtilities.getAppliedTheme(getContext());

            /*
             * Get a sorted cursor of all currently known themes. We'll walk
             * this cursor along with the package managers sorted output to
             * determine changes. This cursor intentionally excludes the
             * "special" case system default theme (which has THEME_PACKAGE set
             * to NULL).
             */
            Cursor current = mDb.query(TABLE_NAME,
                    null, "LENGTH(" + ThemeColumns.THEME_PACKAGE + ") > 0", null, null, null,
                    ThemeColumns.THEME_PACKAGE + ", " + ThemeColumns.THEME_ID);
            ThemeItem currentItem = ThemeItem.getInstance(current);

            Collections.sort(themePackages, new Comparator<PackageInfo>() {
                public int compare(PackageInfo a, PackageInfo b) {
                    return a.packageName.compareTo(b.packageName);
                }
            });

            boolean notifyChanges = false;

            try {
                for (PackageInfo pi: themePackages) {
                    if (pi.themeInfos == null) {
                        continue;
                    }

                    /*
                     * Deal with potential package change, moving `current'
                     * along to efficiently detect differences. This method
                     * handles insert, delete, and modify returning with
                     * `current' positioned ahead of the theme matching the last
                     * of `pi's ThemeInfo objects (or passed the last
                     * entry if the cursor is exhausted).
                     */
                    boolean invalidated = detectPackageChange(getContext(), mDb, pi, current,
                            currentItem, appliedTheme);
                    if (invalidated) {
                        notifyChanges = true;
                    }

                    mDb.yieldIfContendedSafely();
                }

                /*
                 * Delete any items left-over that were not found in
                 * `themePackages'.
                 */
                while (current.moveToNext()) {
                    deleteTheme(mDb, currentItem);
                    notifyChanges = true;
                }
            } finally {
                if (currentItem != null) {
                    currentItem.close();
                }
                if (notifyChanges) {
                    notifyChanges();
                }
            }
        }
    }

    private static boolean detectPackageChange(Context context, SQLiteDatabase db,
            PackageInfo pi, Cursor current, ThemeItem currentItem, CustomTheme appliedTheme) {
        boolean notifyChanges = false;

        if (pi.themeInfos == null)
            return false;

        Arrays.sort(pi.themeInfos, new Comparator<ThemeInfo>() {
            public int compare(ThemeInfo a, ThemeInfo b) {
                return a.themeId.compareTo(b.themeId);
            }
        });

        for (ThemeInfo ti: pi.themeInfos) {
            String currPackageName = null;
            String currThemeId = null;

            /*
             * The local cursor is sorted to require only 1 iteration
             * through to detect inserts, updates, and deletes.
             */
            while (!current.isAfterLast()) {
                String packageName = currentItem.getPackageName();
                String themeId = currentItem.getThemeId();

                /* currentItem less than, equal to, or greater than pi/ti? */
                int cmp = ThemeUtilities.compareTheme(currentItem, pi, ti);

                if (cmp < 0) {
                    /*
                     * This theme isn't in the package list, delete and
                     * go to the next; rinse, lather, repeat.
                     */
                    deleteTheme(db, currentItem);
                    notifyChanges = true;
                    current.moveToNext();
                    continue;
                }

                /*
                 * Either we need to verify this entry in the database or we
                 * need to insert a new one. Either way, the current cursor
                 * is correctly positioned so we should break out of this
                 * loop to do the real work.
                 */
                if (cmp == 0) {
                    currPackageName = packageName;
                    currThemeId = themeId;
                } else /* if (cmp > 0) */ {
                    currPackageName = null;
                    currThemeId = null;
                }

                /* Handle either an insert or verify/update. */
                break;
            }

            boolean isCurrentTheme = ThemeUtilities.themeEquals(pi, ti, appliedTheme);

            if (currPackageName != null && currThemeId != null) {
                boolean invalidated = verifyOrUpdateTheme(context, db, pi, ti, currentItem,
                        isCurrentTheme);
                if (invalidated) {
                    notifyChanges = true;
                }
                current.moveToNext();
            } else {
                insertTheme(context, db, pi, ti, isCurrentTheme);
                notifyChanges = true;
            }
        }

        return notifyChanges;
    }

    private static void populateContentValues(Context context, ContentValues outValues,
            PackageInfo pi, ThemeInfo ti, boolean isCurrentTheme) {
        outValues.put(ThemeColumns.IS_APPLIED, isCurrentTheme ? 1 : 0);
        outValues.put(ThemeColumns.THEME_ID, ti.themeId);
        outValues.put(ThemeColumns.THEME_PACKAGE, pi.packageName);
        outValues.put(ThemeColumns.NAME, ti.name);
        outValues.put(ThemeColumns.STYLE_NAME,
                ti.themeStyleName != null ? ti.themeStyleName : ti.name);
        outValues.put(ThemeColumns.AUTHOR, ti.author);
        outValues.put(ThemeColumns.IS_DRM, ti.isDrmProtected ? 1 : 0);
        outValues.put(ThemeColumns.IS_SYSTEM,
                ((pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) ? 1 : 0);
        if (ti.wallpaperImageName != null) {
            outValues.put(ThemeColumns.WALLPAPER_NAME,
                    FileUtilities.basename(ti.wallpaperImageName));
            outValues.put(ThemeColumns.WALLPAPER_URI,
                    PackageResources.makeAssetPathUri(pi.packageName, ti.wallpaperImageName)
                        .toString());
        }
        if (ti.ringtoneFileName != null) {
            outValues.put(ThemeColumns.RINGTONE_NAME, ti.ringtoneName);
            outValues.put(ThemeColumns.RINGTONE_URI,
                    PackageResources.makeAssetPathUri(pi.packageName, ti.ringtoneFileName)
                        .toString());
        }
        if (ti.notificationRingtoneFileName != null) {
            outValues.put(ThemeColumns.NOTIFICATION_RINGTONE_NAME, ti.notificationRingtoneName);
            outValues.put(ThemeColumns.NOTIFICATION_RINGTONE_URI,
                    PackageResources.makeAssetPathUri(pi.packageName,
                            ti.notificationRingtoneFileName).toString());
        }
        if (ti.thumbnail != null) {
            outValues.put(ThemeColumns.THUMBNAIL_URI,
                    PackageResources.makeAssetPathUri(pi.packageName, ti.thumbnail)
                        .toString());
        }
        if (ti.preview != null) {
            outValues.put(ThemeColumns.PREVIEW_URI,
                    PackageResources.makeAssetPathUri(pi.packageName, ti.preview)
                        .toString());
        }

        /* Try to find theme attributes by convention, like HTC lock screen wallpaper. */
        Resources themeRes = PackageResourcesProvider.getResourcesForTheme(context, pi);

        int lockWallpaperResId =
            themeRes.getIdentifier("com_htc_launcher_lockscreen_wallpaper", "drawable",
                    pi.packageName);
        if (lockWallpaperResId != 0) {
            int nameId = themeRes.getIdentifier("com_htc_launcher_lockscreen_wallpaper_name",
                    "string", pi.packageName);
            if (nameId != 0) {
                outValues.put(ThemeColumns.LOCK_WALLPAPER_NAME,
                        themeRes.getString(nameId));
            }
            outValues.put(ThemeColumns.LOCK_WALLPAPER_URI,
                    PackageResources.makeResourceIdUri(pi.packageName, lockWallpaperResId)
                        .toString());
        }
    }

    private static void deleteTheme(SQLiteDatabase db, ThemeItem item) {
        if (ThemeManager.DEBUG) {
            Log.i(ThemeManager.TAG, "ThemesProvider out of sync: removing " +
                    item.getPackageName() + "/" + item.getThemeId());
        }
        db.delete(TABLE_NAME,
                ThemeColumns._ID + " = " + item.getId(), null);
    }

    private static void insertTheme(Context context, SQLiteDatabase db,
            PackageInfo pi, ThemeInfo ti, boolean isCurrentTheme) {
        if (ThemeManager.DEBUG) {
            Log.i(ThemeManager.TAG, "ThemesProvider out of sync: inserting " +
                    pi.packageName + "/" + ti.themeId);
        }

        ContentValues values = new ContentValues();
        populateContentValues(context, values, pi, ti, isCurrentTheme);
        db.insert(TABLE_NAME, ThemeColumns._ID, values);
    }

    private static boolean verifyOrUpdateTheme(Context context, SQLiteDatabase db,
            PackageInfo pi, ThemeInfo ti, ThemeItem existing, boolean isCurrentTheme) {
        boolean invalidated = false;

        /*
         * Pretend we would insert this record fresh, then compare the
         * resulting ContentValues with the actual database row. If any
         * differences are found, adjust them with an update query.
         */
        ContentValues values = new ContentValues();
        populateContentValues(context, values, pi, ti, isCurrentTheme);

        invalidated = !equalContentValuesAndCursor(values, existing.getCursor());

        if (invalidated) {
            if (ThemeManager.DEBUG) {
                Log.i(ThemeManager.TAG, "ThemesProvider out of sync: updating " +
                        existing.getPackageName() + "/" + existing.getThemeId());
            }

            db.update(TABLE_NAME, values,
                    ThemeColumns._ID + " = " + existing.getId(), null);
        }

        return invalidated;
    }

    private static boolean equalContentValuesAndCursor(ContentValues values, Cursor cursor) {
        int n = cursor.getColumnCount();
        while (n-- > 0) {
            String columnName = cursor.getColumnName(n);
            if (columnName.equals("_id")) {
                continue;
            }
            if (cursor.isNull(n)) {
                if (values.getAsString(columnName) != null) {
                    return false;
                }
            } else if (!cursor.getString(n).equals(values.getAsString(columnName))) {
                return false;
            }
        }
        return true;
    }

    private final BroadcastReceiver mThemePackageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            new Thread() {
                public void run() {
                    SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                    db.beginTransaction();
                    try {
                        handlePackageEvent(context, db, intent);
                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                    }
                }
            }.start();
        }

        private void handlePackageEvent(Context context, SQLiteDatabase db, Intent intent) {
            String action = intent.getAction();
            String pkg = intent.getData().getSchemeSpecificPart();

            boolean isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);

            try {
                if (isReplacing) {
                    if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {
                        if (ThemeManager.DEBUG) {
                            Log.i(ThemeManager.TAG, "Handling replaced theme package: " + pkg);
                        }
                        PackageInfo pi = context.getPackageManager().getPackageInfo(pkg, 0);
                        Cursor cursor = db.query(TABLE_NAME, null,
                                ThemeColumns.THEME_PACKAGE + " = ?",
                                new String[] { pkg }, null, null, ThemeColumns.THEME_ID);
                        ThemeItem dao = ThemeItem.getInstance(cursor);
                        boolean invalidated =
                            detectPackageChange(context, db, pi, cursor, dao,
                                    ThemeUtilities.getAppliedTheme(context));
                        if (invalidated) {
                            notifyChanges();
                        }
                    }
                } else if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {
                    if (ThemeManager.DEBUG) {
                        Log.i(ThemeManager.TAG, "Handling new theme package: " + pkg);
                    }
                    PackageInfo pi = context.getPackageManager().getPackageInfo(pkg, 0);
                    if (pi != null && pi.themeInfos != null) {
                        for (ThemeInfo ti: pi.themeInfos) {
                            insertTheme(context, db, pi, ti, false);
                        }
                    }
                    notifyChanges();
                } else if (action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
                    if (ThemeManager.DEBUG) {
                        Log.i(ThemeManager.TAG, "Handling removed theme package: " + pkg);
                    }
                    db.delete(TABLE_NAME, ThemeColumns.THEME_PACKAGE + " = ?",
                            new String[] { pkg });
                    notifyChanges();
                }
            } catch (NameNotFoundException e) {
                if (ThemeManager.DEBUG) {
                    Log.d(ThemeManager.TAG, "Unexpected package manager inconsistency detected", e);
                }
            }
        }
    };

    private Cursor queryThemes(int type, Uri uri, SQLiteDatabase db, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        if (type == TYPE_THEMES) {
            if (sortOrder == null) {
                sortOrder = ThemeColumns.NAME;
            }
        } else if (type == TYPE_THEME) {
            List<String> segments = uri.getPathSegments();
            int n = segments.size();
            if (n == 3) {
                String packageName = segments.get(1);
                String themeId = segments.get(2);
                selection = DatabaseUtilities.appendSelection(selection,
                        ThemeColumns.THEME_PACKAGE + "=? AND " +
                        ThemeColumns.THEME_ID + "=?");
                selectionArgs = DatabaseUtilities.appendSelectionArgs(selectionArgs,
                        packageName, themeId);
            } else {
                throw new IllegalArgumentException("Can't parse URI: " + uri);
            }
        } else if (type == TYPE_THEME_SYSTEM) {
            selection = DatabaseUtilities.appendSelection(selection,
                    ThemeColumns.THEME_PACKAGE + " = ? AND " +
                    ThemeColumns.THEME_ID + " = ?");
            selectionArgs = DatabaseUtilities.appendSelectionArgs(selectionArgs,
                    "", "");
        }
        return db.query(TABLE_NAME, projection, selection, selectionArgs, null, null,
                sortOrder);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        int type = URI_MATCHER.match(uri);
        Cursor c;
        switch (type) {
            case TYPE_THEMES:
            case TYPE_THEME:
            case TYPE_THEME_SYSTEM:
                c = queryThemes(type, uri, db, projection, selection, selectionArgs, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return c;
    }

    @Override
    public String getType(Uri uri) {
        int type = URI_MATCHER.match(uri);
        switch (type) {
            case TYPE_THEMES:
                return ThemeColumns.CONTENT_TYPE;
            case TYPE_THEME:
            case TYPE_THEME_SYSTEM:
                return ThemeColumns.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    private void checkForRequiredArguments(ContentValues values) {
        if (!values.containsKey(ThemeColumns.THEME_PACKAGE)) {
            throw new IllegalArgumentException("Required argument missing: " +
                    ThemeColumns.THEME_PACKAGE);
        }
        if (!values.containsKey(ThemeColumns.THEME_ID)) {
            throw new IllegalArgumentException("Required argument missing: " +
                    ThemeColumns.THEME_ID);
        }

    }

    private void notifyChanges() {
        getContext().getContentResolver()
            .notifyChange(Themes.ThemeColumns.CONTENT_PLURAL_URI, null);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (Binder.getCallingPid() != android.os.Process.myPid()) {
            throw new SecurityException("Cannot insert into this provider");
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Uri newUri = null;
        long _id;

        switch (URI_MATCHER.match(uri)) {
            case TYPE_THEMES:
                checkForRequiredArguments(values);
                String packageName = values.getAsString(ThemeColumns.THEME_PACKAGE);
                String themeId = values.getAsString(ThemeColumns.THEME_ID);
                _id = db.insert(TABLE_NAME, ThemeColumns._ID, values);
                if (_id >= 0) {
                    newUri = ThemeColumns.CONTENT_URI.buildUpon()
                        .appendPath(packageName)
                        .appendPath(themeId).build();
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (newUri != null) {
            notifyChanges();
        }

        return newUri;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (Binder.getCallingPid() != android.os.Process.myPid()) {
            throw new SecurityException("Cannot update this provider");
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = 0;

        int type = URI_MATCHER.match(uri);
        switch (type) {
            case TYPE_THEMES:
            case TYPE_THEME:
                if (type == TYPE_THEME) {
                    List<String> segments = uri.getPathSegments();
                    int n = segments.size();
                    if (n == 3) {
                        String packageName = segments.get(1);
                        String themeId = segments.get(2);
                        selection = DatabaseUtilities.appendSelection(selection,
                                ThemeColumns.THEME_PACKAGE + "=? AND " +
                                ThemeColumns.THEME_ID + "=?");
                        selectionArgs = DatabaseUtilities.appendSelectionArgs(selectionArgs,
                                packageName, themeId);
                    } else {
                        throw new IllegalArgumentException("Can't parse URI: " + uri);
                    }
                }
                count = db.update(TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (count > 0) {
            notifyChanges();
        }

        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (Binder.getCallingPid() != android.os.Process.myPid()) {
            throw new SecurityException("Cannot delete from this provider");
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = 0;

        switch (URI_MATCHER.match(uri)) {
            case TYPE_THEMES:
                count = db.delete(TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (count > 0) {
            notifyChanges();
        }

        return count;
    }

    static {
        URI_MATCHER.addURI(Themes.AUTHORITY, "themes", TYPE_THEMES);
        URI_MATCHER.addURI(Themes.AUTHORITY, "theme/system", TYPE_THEME_SYSTEM);
        URI_MATCHER.addURI(Themes.AUTHORITY, "theme/*/*", TYPE_THEME);
    }
}
