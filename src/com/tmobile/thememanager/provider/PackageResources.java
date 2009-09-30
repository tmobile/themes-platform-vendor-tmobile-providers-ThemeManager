package com.tmobile.thememanager.provider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.ThemeInfo;
import android.content.pm.BaseThemeInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.tmobile.thememanager.ThemeManager;

public class PackageResources {
    public static final String AUTHORITY = "com.tmobile.thememanager.packageresources";

    public static final Uri CONTENT_URI =
        Uri.parse("content://" + AUTHORITY);

    private PackageResources() {}

    /* package */ static Uri makeUri(Uri baseUri, String packageName, long id) {
        if (TextUtils.isEmpty(packageName)) {
            throw new IllegalArgumentException("packageName must not be null or empty.");
        }
        return baseUri.buildUpon()
            .appendPath(packageName)
            .appendPath(String.valueOf(id))
            .build();
    }

    /****************************************************/
    /* Ringtones section                                */
    /****************************************************/

    /**
     * @param context Context - specifies the context
     * @param packageName String - the name of the package containing resource
     * @param assetPath String - the resource's asset path
     * @return uri
     **/
    public static Uri getRingtoneUri(Context context, String packageName, String assetPath) {
        Cursor c = context.getContentResolver().query(RingtoneColumns.CONTENT_PLURAL_URI,
                new String[] { RingtoneColumns._ID },
                RingtoneColumns.PACKAGE + " = ? AND " + RingtoneColumns.ASSET_PATH + " = ?",
                new String[] { packageName, assetPath }, null);
        if (c == null) {
            return null;
        }

        try {
            if (c.moveToFirst() == false) {
                return null;
            }
            
            long _id = c.getLong(0);
            return makeUri(RingtoneColumns.CONTENT_URI, packageName, _id);
        } finally {
            c.close();
        }
    }

    public static void insertRingtone(Context context, PackageInfo pi, BaseThemeInfo ti) {
        ContentValues values = new ContentValues();
        values.put(RingtoneColumns.TITLE, ti.ringtoneName);
        values.put(RingtoneColumns.ASSET_PATH, ti.ringtoneFileName);
        values.put(RingtoneColumns.PACKAGE, pi.packageName);
        values.put(RingtoneColumns.THEME_NAME, ti.name);
        values.put(RingtoneColumns.IS_DRM, ti.ringtoneFileName.contains("locked/"));
        values.put(RingtoneColumns.IS_NOTIFICATION, 0);
        values.put(RingtoneColumns.IS_RINGTONE, 1);
        values.put(RingtoneColumns.IS_ALARM, 0);
        values.put(RingtoneColumns.LOCKED_ZIPFILE_PATH, pi.getLockedZipFilePath());
        values.put(RingtoneColumns.PACKAGE_FILE, pi.applicationInfo.sourceDir);
        context.getContentResolver().insert(RingtoneColumns.CONTENT_PLURAL_URI, values);
    }

    public static void insertNotificationRingtone(Context context, PackageInfo pi, BaseThemeInfo ti) {
        ContentValues values = new ContentValues();
        values.put(RingtoneColumns.TITLE, ti.notificationRingtoneName);
        values.put(RingtoneColumns.ASSET_PATH, ti.notificationRingtoneFileName);
        values.put(RingtoneColumns.PACKAGE, pi.packageName);
        values.put(RingtoneColumns.THEME_NAME, ti.name);
        values.put(RingtoneColumns.IS_DRM, ti.notificationRingtoneFileName.contains("locked/"));
        values.put(RingtoneColumns.IS_NOTIFICATION, 1);
        values.put(RingtoneColumns.IS_RINGTONE, 0);
        values.put(RingtoneColumns.IS_ALARM, 0);
        values.put(RingtoneColumns.LOCKED_ZIPFILE_PATH, pi.getLockedZipFilePath());
        values.put(RingtoneColumns.PACKAGE_FILE, pi.applicationInfo.sourceDir);
        context.getContentResolver().insert(RingtoneColumns.CONTENT_PLURAL_URI, values);
    }

    public static void deleteRingtones(Context context, String pkg) {
        /* XXX: Fix-up the current default ringtone? */
        context.getContentResolver().delete(RingtoneColumns.CONTENT_PLURAL_URI,
                RingtoneColumns.PACKAGE + " = ?", new String[] { pkg });
    }

    public interface RingtoneColumns {
        /** Base uri to which _ID is appended to construct a reference to a specific ringtone. */
        public static final Uri CONTENT_URI =
            Uri.parse("content://" + AUTHORITY + "/ringtone");

        public static final Uri CONTENT_PLURAL_URI =
            Uri.parse("content://" + AUTHORITY + "/ringtones");

        /** Id which is used to construct the ringtone uri. */
        public static final String _ID = MediaStore.Audio.Media._ID;

        /** Display-friendly ringtone name. */
        public static final String TITLE = MediaStore.Audio.Media.TITLE;

        /** Boolean indicating if this ringtone is DRM protected.  If true, you must possess
         * {@link android.provider.DrmStore.ACCESS_DRM_PERMISSION} to access the resource. */
        public static final String IS_DRM = "is_drm";

        static final String PACKAGE = "package";
        static final String THEME_NAME = "theme_name";
        static final String ASSET_PATH = "asset_path";
        static final String LOCKED_ZIPFILE_PATH = "locked_zipfile_path";
        static final String IS_RINGTONE = MediaStore.Audio.Media.IS_RINGTONE;
        static final String IS_NOTIFICATION = MediaStore.Audio.Media.IS_NOTIFICATION;
        static final String IS_ALARM = MediaStore.Audio.Media.IS_ALARM;
        static final String PACKAGE_FILE = "package_file";
    }


    /****************************************************/
    /* Images section                                   */
    /****************************************************/

    /**
     * @param context Context - specifies the context
     * @param packageName String - the name of the package containing resource
     * @param assetPath String - the resource's asset path
     * @return uri
     **/
    public static Uri getImageUri(Context context, String packageName, String assetPath) {
        Cursor c = context.getContentResolver().query(ImageColumns.CONTENT_PLURAL_URI,
                new String[] { ImageColumns._ID },
                ImageColumns.PACKAGE + " = ? AND " + ImageColumns.ASSET_PATH + " = ?",
                new String[] { packageName, assetPath }, null);
        if (c == null) {
            return null;
        }

        try {
            if (c.moveToFirst() == false) {
                return null;
            }
            
            long _id = c.getLong(0);
            return makeUri(ImageColumns.CONTENT_URI, packageName, _id);
        } finally {
            c.close();
        }
    }

    public static void insertImage(Context context, PackageInfo pi, ThemeInfo ti, int imageType) {
        ContentValues values = new ContentValues();
        String assetPath = null;

        switch (imageType) {
            case ImageColumns.IMAGE_TYPE_APP_FAVE:
                    assetPath = ti.favesAppImageName;
                    break;

            case ImageColumns.IMAGE_TYPE_FAVE:
                    assetPath = ti.favesImageName;
                    break;

            case ImageColumns.IMAGE_TYPE_WALLPAPER:
                    assetPath = ti.wallpaperImageName;
                    break;

            case ImageColumns.IMAGE_TYPE_THUMBNAIL:
                    assetPath = ti.thumbnail;
                    break;

            case ImageColumns.IMAGE_TYPE_PREVIEW:
                    assetPath = ti.preview;
                    break;

            default:
                    Log.e(ThemeManager.TAG, "Unknown image asset type: " + imageType);
                    assetPath = "UNKNOWN";
                    break;
        }
        if (assetPath == null) {
            Log.e(ThemeManager.TAG, "Unindentified asset path for (type) " + imageType);
            assetPath = "UNKNOWN";
        }
        values.put(ImageColumns.IMAGE_TYPE, imageType);
        values.put(ImageColumns.ASSET_PATH, assetPath);
        values.put(ImageColumns.PACKAGE, pi.packageName);
        values.put(ImageColumns.THEME_NAME, ti.name);
        values.put(ImageColumns.IS_DRM, assetPath.contains("locked/"));
        values.put(ImageColumns.LOCKED_ZIPFILE_PATH, pi.getLockedZipFilePath());
        values.put(ImageColumns.PACKAGE_FILE, pi.applicationInfo.sourceDir);
        context.getContentResolver().insert(ImageColumns.CONTENT_PLURAL_URI, values);
    }

    public static void deleteImages(Context context, String pkg) {
        /* XXX: Fix-up the current default wallpaper? */
        context.getContentResolver().delete(ImageColumns.CONTENT_PLURAL_URI,
            ImageColumns.PACKAGE + " = ?", new String[] { pkg });
    }

    public interface ImageColumns {
        /** Base uri to which _ID is appended to construct a reference to a specific image. */
        public static final Uri CONTENT_URI =
            Uri.parse("content://" + AUTHORITY + "/image");

        public static final Uri CONTENT_PLURAL_URI =
            Uri.parse("content://" + AUTHORITY + "/images");

        /** Id which is used to construct the image uri. */
        public static final String _ID = MediaStore.Images.Media._ID;

        /** Image type.*/
        public static final String IMAGE_TYPE = "image_type";

        /** Boolean indicating if this ringtone is DRM protected.  If true, you must possess
         * {@link android.provider.DrmStore.ACCESS_DRM_PERMISSION} to access the resource. */
        public static final String IS_DRM = "is_drm";

        static final String PACKAGE = "package";
        static final String THEME_NAME = "theme_name";
        static final String ASSET_PATH = "asset_path";
        static final String LOCKED_ZIPFILE_PATH = "locked_zipfile_path";
        static final String PACKAGE_FILE = "package_file";

        /** Recognized image types:
         **/
        public static final int IMAGE_TYPE_WALLPAPER = 0;
        public static final int IMAGE_TYPE_FAVE = 1;
        public static final int IMAGE_TYPE_APP_FAVE = 2;
        public static final int IMAGE_TYPE_THUMBNAIL = 3;
        public static final int IMAGE_TYPE_PREVIEW = 4;
    }
}
