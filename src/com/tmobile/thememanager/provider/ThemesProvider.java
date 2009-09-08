package com.tmobile.thememanager.provider;

import com.tmobile.thememanager.provider.Themes.ThemeColumns;
import com.tmobile.thememanager.utils.DatabaseUtilities;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Binder;

import java.util.List;

/**
 * Provider 
 */
public class ThemesProvider extends ContentProvider {
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    private static final String TABLE_NAME = "themeitem_map";

    private static final int TYPE_THEMES = 0;
    private static final int TYPE_THEME = 1;
    
    private SQLiteOpenHelper mOpenHelper;

    private static class OpenDatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "theme_item.db";
        private static final int DATABASE_VERSION = 4;

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
                    ThemeColumns.AUTHOR + " TEXT NOT NULL, " +
                    ThemeColumns.IS_DRM + " INTEGER DEFAULT 0, " +
                    ThemeColumns.IS_SYSTEM + " INTEGER DEFAULT 0, " +
                    ThemeColumns.NAME + " TEXT NOT NULL, " +
                    ThemeColumns.STYLE_NAME + " TEXT NOT NULL, " +
                    ThemeColumns.WALLPAPER_NAME + " TEXT, " +
                    ThemeColumns.WALLPAPER_URI + " TEXT, " +
                    ThemeColumns.RINGTONE_NAME + " TEXT, " +
                    ThemeColumns.RINGTONE_URI + " TEXT, " +
                    ThemeColumns.NOTIFICATION_RINGTONE_NAME + " TEXT, " +
                    ThemeColumns.NOTIFICATION_RINGTONE_URI + " TEXT, " +
                    ThemeColumns.THUMBNAIL_URI + " TEXT" +
                    ")");
            db.execSQL("CREATE INDEX themeitem_map_package ON themeitem_map (theme_package)");
            db.execSQL("CREATE UNIQUE INDEX themeitem_map_key ON themeitem_map (theme_package, theme_id)");
        }

        private void dropTables(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS themeitem_map");
        }
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new OpenDatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (sortOrder == null) {
            sortOrder = ThemeColumns.NAME;
        }
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
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
                return db.query(TABLE_NAME, projection, selection, selectionArgs, null, null,
                        sortOrder);
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }    
    
    @Override
    public String getType(Uri uri) {
        return null;
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
            getContext().getContentResolver().notifyChange(newUri, null);
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

        return count;
    }

    static {
        URI_MATCHER.addURI(Themes.AUTHORITY, "themes", TYPE_THEMES);
        URI_MATCHER.addURI(Themes.AUTHORITY, "theme/*/*", TYPE_THEME);
    }
}
