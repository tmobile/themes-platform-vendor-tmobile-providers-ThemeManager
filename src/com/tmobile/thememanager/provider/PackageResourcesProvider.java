package com.tmobile.thememanager.provider;

import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.provider.PackageResources.ImageColumns;
import com.tmobile.thememanager.provider.PackageResources.RingtoneColumns;

import android.app.ActivityManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.IExtendedContentProvider;
import android.content.UriMatcher;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.provider.DrmStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Proxy uri-based access to assets from theme packages. DRM security is upheld
 * at this layer.
 */
public class PackageResourcesProvider extends ContentProvider
            implements IExtendedContentProvider {
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    private static final int TYPE_RINGTONE = 0;
    private static final int TYPE_RINGTONES = 1;

    private static final int TYPE_IMAGE = 2;
    private static final int TYPE_IMAGES = 3;

    private SQLiteOpenHelper mOpenHelper;

    /* Cache AssetManager objects to speed up ringtone manipulation. */
    private static final Map<String, Resources> mLookUpTable =
        new HashMap<String, Resources>();

    private static class OpenDatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "package_idmap.db";
        private static final int DATABASE_VERSION = 7;

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
            db.execSQL("CREATE TABLE ringtone_map (" +
                    PackageResources.RingtoneColumns._ID + " INTEGER PRIMARY KEY, " +
                    PackageResources.RingtoneColumns.TITLE + " TEXT NOT NULL, " +
                    PackageResources.RingtoneColumns.IS_DRM + " INTEGER, " +
                    PackageResources.RingtoneColumns.PACKAGE + " TEXT NOT NULL, " +
                    PackageResources.RingtoneColumns.THEME_NAME + " TEXT NOT NULL, " +
                    PackageResources.RingtoneColumns.PACKAGE_FILE + " TEXT NOT NULL, " +
                    PackageResources.RingtoneColumns.ASSET_PATH + " TEXT NOT NULL, " +
                    PackageResources.RingtoneColumns.LOCKED_ZIPFILE_PATH + " TEXT, " +
                    PackageResources.RingtoneColumns.IS_RINGTONE + " INTEGER, " +
                    PackageResources.RingtoneColumns.IS_ALARM + " INTEGER, " +
                    PackageResources.RingtoneColumns.IS_NOTIFICATION + " INTEGER" + ")");
            db.execSQL("CREATE INDEX ringtone_map_package ON ringtone_map (package)");
            db.execSQL("CREATE UNIQUE INDEX ringtone_map_path ON ringtone_map (package, asset_path)");

            db.execSQL("CREATE TABLE image_map (" +
                    PackageResources.ImageColumns._ID + " INTEGER PRIMARY KEY, " +
                    PackageResources.ImageColumns.IS_DRM + " INTEGER, " +
                    PackageResources.ImageColumns.PACKAGE + " TEXT NOT NULL, " +
                    PackageResources.RingtoneColumns.THEME_NAME + " TEXT NOT NULL, " +
                    PackageResources.ImageColumns.PACKAGE_FILE + " TEXT NOT NULL, " +
                    PackageResources.ImageColumns.ASSET_PATH + " TEXT NOT NULL, " +
                    PackageResources.ImageColumns.LOCKED_ZIPFILE_PATH + " TEXT, " +
                    PackageResources.ImageColumns.IMAGE_TYPE + " INTEGER" + ")");
            db.execSQL("CREATE INDEX image_map_package ON image_map (package)");
            db.execSQL("CREATE UNIQUE INDEX image_map_path ON image_map (package, asset_path)");
        }

        private void dropTables(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS ringtone_map");
            db.execSQL("DROP TABLE IF EXISTS image_map");
        }
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new OpenDatabaseHelper(getContext());
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
        Resources r = mLookUpTable.get(packageName);
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
            String packageName, String packageFileName, String packageLockedZipFile) {
        Resources r = mLookUpTable.get(packageName);
        if (r != null) {
            return r;
        }

        return createResourcesForTheme(context, packageName,
                packageFileName, packageLockedZipFile);
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

        mLookUpTable.put(packageName, r);
        return r;
    }

    public static final synchronized void deleteResourcesForTheme(String packageName) {
        mLookUpTable.remove(packageName);
    }
    
    private String appendSelection(String selection, String extra) {
        if (TextUtils.isEmpty(extra)) {
            throw new IllegalArgumentException("extra must be non-null and non-empty");
        }
        if (TextUtils.isEmpty(selection)) {
            return extra;
        }
        return "(" + selection + ") AND " + extra;
    }
    
    private String[] appendSelectionArgs(String[] selectionArgs, String... extra) {
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

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        int type = URI_MATCHER.match(uri);
        switch (type) {
            /*
             * XXX: Enforce that projection contains only "safe" columns (_ID,
             * TITLE, and the URI string literal)?
             */
            case TYPE_RINGTONES:
            case TYPE_RINGTONE:
                if (type == TYPE_RINGTONE) {
                    selection = appendSelection(selection, "_id=?");
                    selectionArgs = appendSelectionArgs(selectionArgs, uri.getLastPathSegment());
                }
                return db.query("ringtone_map", projection, selection, selectionArgs, null, null,
                        sortOrder);
            case TYPE_IMAGES:
                return db.query("image_map", projection, selection, selectionArgs, null, null,
                        sortOrder);
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
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
        if (ThemeManager.DEBUG) {
            Log.d(ThemeManager.TAG, "openFile called in place of openAssetFile!");
        }
        AssetFileDescriptor afd = openAssetFile(uri, mode);
        if (afd == null) {
            return null;
        }
        return afd.getParcelFileDescriptor();
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = null;
        switch (URI_MATCHER.match(uri)) {
            case TYPE_RINGTONE:
                c = db.query("ringtone_map", new String[] {
                        RingtoneColumns.PACKAGE,
                        RingtoneColumns.PACKAGE_FILE,
                        RingtoneColumns.ASSET_PATH,
                        RingtoneColumns.IS_DRM,
                        RingtoneColumns.LOCKED_ZIPFILE_PATH
                }, "_id=?", new String[] {
                    uri.getLastPathSegment()
                }, null, null, null);
                if (c == null) {
                    return null;
                }

                try {
                    if (c.moveToFirst() == false) {
                        return null;
                    }
                    String packageName = c.getString(c.getColumnIndex(RingtoneColumns.PACKAGE));
                    String assetPath = c.getString(c.getColumnIndex(RingtoneColumns.ASSET_PATH));
                    int isDrm = c.getInt(c.getColumnIndex(RingtoneColumns.IS_DRM));
                    String lockedZipFilePath = c.getString(c.getColumnIndex(RingtoneColumns.LOCKED_ZIPFILE_PATH));
                    String packageFile = c.getString(c.getColumnIndex(RingtoneColumns.PACKAGE_FILE));

                    if (isDrm != 0) {
                        /*
                         * XXX: WE MUST NOT REQUEST DRM ACCESS PERMISSION OR
                         * THIS WILL ALWAYS SUCCEED!
                         */
                        DrmStore.enforceAccessDrmPermission(getContext());
                    }

                    Resources r = getResourcesForTheme(getContext(), packageName, packageFile,
                            lockedZipFilePath);
                    return r.getAssets().openFd(assetPath);
                } catch (Exception e) {
                    throw new FileNotFoundException("Could not open URI " + uri + ": " + e);
                } finally {
                    c.close();
                }
            case TYPE_IMAGE:
                try {
                    PairResult pair = getPairResultForImage(uri);
                    if (pair != null) {
                        return pair.am.openFd(pair.assetPath);
                    }
                } catch (Exception e) {
                    Log.e(ThemeManager.TAG, "Failed in openAssetFile", e);
                }
                return null;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }
    
    @Override
    public String getType(Uri uri) {
        return null;
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
            case TYPE_RINGTONES:
                _id = db.insert("ringtone_map", RingtoneColumns._ID, values);
                if (_id >= 0) {
                    newUri = ContentUris.withAppendedId(RingtoneColumns.CONTENT_URI, _id);
                }
                break;
            case TYPE_IMAGES:
                _id = db.insert("image_map", ImageColumns._ID, values);
                if (_id >= 0) {
                    newUri = ContentUris.withAppendedId(ImageColumns.CONTENT_URI, _id);
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
        return 0;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (Binder.getCallingPid() != android.os.Process.myPid()) {
            throw new SecurityException("Cannot delete from this provider");
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = 0;

        switch (URI_MATCHER.match(uri)) {
            case TYPE_RINGTONES:
                count = db.delete("ringtone_map", selection, selectionArgs);
                break;
            case TYPE_IMAGES:
                count = db.delete("image_map", selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        return count;
    }

    public InputStream openInputStream(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case TYPE_IMAGE:
                try {
                    PairResult pair = getPairResultForImage(uri);
                    if (pair != null) {
                        return pair.am.open(pair.assetPath);
                    }
                } catch (Exception e) {
                    Log.e(ThemeManager.TAG, "Failed in openInputStream", e);
                }
        }
        return null;
    }

    private static class PairResult {
        AssetManager am;
        String assetPath;

        public PairResult(AssetManager _am, String _assetPath) {
            am = _am;
            assetPath = _assetPath;
        }
    }

    private PairResult getPairResultForImage(Uri uri) throws Exception {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = db.query("image_map", new String[] {
                            ImageColumns.PACKAGE,
                            ImageColumns.PACKAGE_FILE,
                            ImageColumns.ASSET_PATH,
                            ImageColumns.IS_DRM,
                            ImageColumns.LOCKED_ZIPFILE_PATH
            }, "_id=?", new String[] {
                            uri.getLastPathSegment()
            }, null, null, null);
        if (c == null) {
            return null;
        }

        try {
            if (c.moveToFirst() == false) {
                return null;
            }
            String packageName = c.getString(c.getColumnIndex(ImageColumns.PACKAGE));
            String assetPath = c.getString(c.getColumnIndex(ImageColumns.ASSET_PATH));
            int isDrm = c.getInt(c.getColumnIndex(ImageColumns.IS_DRM));
            String lockedZipFilePath = c.getString(c.getColumnIndex(ImageColumns.LOCKED_ZIPFILE_PATH));
            String packageFile = c.getString(c.getColumnIndex(ImageColumns.PACKAGE_FILE));

            int pid = Binder.getCallingPid();
            boolean calledFromSelf = (pid == Process.myPid());
            // If this function is invoked from the ThemeManager process,
            // skip the DRM-permission check, since we intentionally
            // do NOT grant ThemeManager DRM-permissions!
            if (isDrm != 0 && !calledFromSelf) {
                /*
                 * XXX: WE MUST NOT REQUEST DRM ACCESS PERMISSION OR
                 * THIS WILL ALWAYS SUCCEED!
                 */
                DrmStore.enforceAccessDrmPermission(getContext());
            }

            Resources r = getResourcesForTheme(getContext(), packageName, packageFile,
                    lockedZipFilePath);
            return new PairResult(r.getAssets(), assetPath);
        } finally {
            c.close();
        }
    }

    static {
        /* See PackageResources#makeRingtoneUri. */
        URI_MATCHER.addURI(PackageResources.AUTHORITY, "ringtones", TYPE_RINGTONES);
        URI_MATCHER.addURI(PackageResources.AUTHORITY, "ringtone/#", TYPE_RINGTONE);

        URI_MATCHER.addURI(PackageResources.AUTHORITY, "images", TYPE_IMAGES);
        URI_MATCHER.addURI(PackageResources.AUTHORITY, "image/#", TYPE_IMAGE);
    }
}
