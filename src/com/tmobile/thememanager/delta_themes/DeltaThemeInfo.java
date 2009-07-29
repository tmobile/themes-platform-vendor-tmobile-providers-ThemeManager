package com.tmobile.thememanager.delta_themes;

import android.net.Uri;
import android.os.Parcelable;
import android.os.Parcel;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;

import com.tmobile.thememanager.utils.SerializationUtilities;

/*
 * This class supports both serialization/deserialization (to be able to persist the object into a file)
 * and writing into a parcel/reading from a parcel (to pass across processes/activities).
 */
public final class DeltaThemeInfo implements Parcelable {

    private static final String TAG = "_DTI_";

    private String mPackageName;

    private String mThemeName;

    private String mThemeId;

    private int mStyleId = -1;

    private String mParentThemeId;

    private Uri mWallpaperUri;

    private Uri mRingtoneUri;

    private Uri mNotificationRingtoneUri;

    private String mResourceBundle;

    private String mColorPaletteName;

    private String mAuthor;

    private boolean mIsDrmProtected;

    private String mWallpaperName;

    private String mRingtoneName;

    private String mNotificationRingtoneName;

    private String mSoundPackName;


    public DeltaThemeInfo(String packageName, String themeId) {
        mPackageName = packageName;
        mThemeId = themeId;
    }

    public String getName() {
        return mThemeName;
    }

    public void setName(String name) {
        mThemeName = name;
    }

    public Uri getWallpaperUri() {
        return mWallpaperUri;
    }

    public void setWallpaperUri(Uri uri) {
        // TODO: copy resource specified by uri into file, put new file uri into mWallpaperUri
        mWallpaperUri = uri;
    }

    public Uri getRingtoneUri() {
        return mRingtoneUri;
    }

    public void setRingtoneUri(Uri uri) {
        // TODO: copy resource specified by uri into file, put new file uri into mRingtoneUri
        mRingtoneUri = uri;
    }

    public Uri getNotificationRingtoneUri() {
        return mNotificationRingtoneUri;
    }

    public void setNotificationRingtoneUri(Uri uri) {
        // TODO: copy resource specified by uri into file, put new file uri into mNotificationRingtoneUri
        mNotificationRingtoneUri = uri;
    }

    public String getResourceBundlePath() {
        return mResourceBundle;
    }

    public void setResourceBundlePath(String path) {
        mResourceBundle = path;
    }

    public String getColorPaletteName() {
        return mColorPaletteName;
    }

    public void setColorPaletteName(String name) {
        mColorPaletteName = name;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public String getThemeId() {
        return mThemeId;
    }

    public void setThemeId(String id) {
        mThemeId = id;
    }

    public int getStyleId() {
        return mStyleId;
    }

    public void setStyleId(int id) {
        mStyleId = id;
    }

    public String getParentThemeId() {
        return mParentThemeId;
    }

    public void setParentThemeId(String themeId) {
        mParentThemeId = themeId;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public void setAuthor(String author) {
        mAuthor = author;
    }

    public boolean isDrmProtected() {
        return mIsDrmProtected;
    }

    public void setIsDrmProtected(boolean drmProtected) {
        mIsDrmProtected = drmProtected;
    }

    public String getWallpaperName() {
        return mWallpaperName;
    }

    public void setWallpaperName(String wallpaperName) {
        mWallpaperName = wallpaperName;
    }

    public String getRingtoneName() {
        return mRingtoneName;
    }

    public void setRingtoneName(String ringtoneName) {
        mRingtoneName = ringtoneName;
    }

    public String getNotificationRingtoneName() {
        return mNotificationRingtoneName;
    }

    public void setNotificationRingtoneName(String notificationRingtoneName) {
        mNotificationRingtoneName = notificationRingtoneName;
    }

    public String getSoundPackName() {
        return mSoundPackName;
    }

    public void setSoundPackName(String soundPackName) {
        mSoundPackName = soundPackName;
    }


    /*
     * Serialize this into os.
     *
     * @param os OutputStream where the object should be written.
     */
    public void serialize(OutputStream os) throws IOException {
        SerializationUtilities.writeString(TAG, os);
        SerializationUtilities.writeString(mPackageName, os);
        SerializationUtilities.writeString(mThemeId, os);
        SerializationUtilities.writeInt(mStyleId, os);
        SerializationUtilities.writeString(mThemeName, os);
        SerializationUtilities.writeString(mResourceBundle, os);
        serializeUri(mWallpaperUri, os);
        serializeUri(mRingtoneUri, os);
        serializeUri(mNotificationRingtoneUri, os);
        SerializationUtilities.writeString(mColorPaletteName, os);
        SerializationUtilities.writeString(mParentThemeId, os);
        SerializationUtilities.writeString(mAuthor, os);
        SerializationUtilities.writeInt((mIsDrmProtected)? 1 : 0, os);
        SerializationUtilities.writeString(mWallpaperName, os);
        SerializationUtilities.writeString(mRingtoneName, os);
        SerializationUtilities.writeString(mNotificationRingtoneName, os);
        SerializationUtilities.writeString(mSoundPackName, os);
    }

    /*
     * De-serialize DeltaThemeInfo.
     *
     * @param is InputStream from where the object should be read.
     * @return  DeltaThemeInfo deserialized object
     */
    public static DeltaThemeInfo deserialize(InputStream is) throws IOException {
        String tag = SerializationUtilities.readString(is);
        if (!tag.equals(TAG)) {
            throw new IOException("Incorrect type of object");
        }
        String packageName = SerializationUtilities.readString(is);
        String themeId = SerializationUtilities.readString(is);
        DeltaThemeInfo delta = new DeltaThemeInfo(packageName, themeId);
        delta.setStyleId(SerializationUtilities.readInt(is));
        delta.setName(SerializationUtilities.readString(is));
        delta.setResourceBundlePath(SerializationUtilities.readString(is));
        delta.setWallpaperUri(deserializeUri(is));
        delta.setRingtoneUri(deserializeUri(is));
        delta.setNotificationRingtoneUri(deserializeUri(is));
        delta.setColorPaletteName(SerializationUtilities.readString(is));
        delta.setParentThemeId(SerializationUtilities.readString(is));
        delta.setAuthor(SerializationUtilities.readString(is));
        delta.setIsDrmProtected(SerializationUtilities.readInt(is) != 0);
        delta.setWallpaperName(SerializationUtilities.readString(is));
        delta.setRingtoneName(SerializationUtilities.readString(is));
        delta.setNotificationRingtoneName(SerializationUtilities.readString(is));
        delta.setSoundPackName(SerializationUtilities.readString(is));

        return delta;
    }

    private static void serializeUri(Uri uri, OutputStream os) throws IOException {
        if (uri != null) {
            SerializationUtilities.writeInt(1, os);
            SerializationUtilities.writeString(uri.toString(), os);
        } else {
            SerializationUtilities.writeInt(0, os);
        }
    }

    private static Uri deserializeUri(InputStream is) throws IOException {
        int flag = SerializationUtilities.readInt(is);
        if (flag == 0) {
            return null;
        }
        String s = SerializationUtilities.readString(is);
        return Uri.parse(s);
    }


    /*
     * Describe the kinds of special objects contained in this Parcelable's
     * marshalled representation.
     *
     * @return a bitmask indicating the set of special object types marshalled
     * by the Parcelable.
     *
     * @see android.os.Parcelable#describeContents()
     */
    public int describeContents() {
        return 0;
    }

    /*
     * Flatten this object in to a Parcel.
     *
     * @param dest The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     * May be 0 or {@link #PARCELABLE_WRITE_RETURN_VALUE}.
     *
     * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
     */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mPackageName);
        dest.writeString(mThemeId);
        dest.writeInt(mStyleId);
        dest.writeString(mThemeName);
        dest.writeString(mColorPaletteName);
        dest.writeString(mParentThemeId);
        dest.writeString(mResourceBundle);
        dest.writeString(mAuthor);
        dest.writeInt((mIsDrmProtected)? 1 : 0);
        Uri.writeToParcel(dest, mWallpaperUri);
        Uri.writeToParcel(dest, mRingtoneUri);
        Uri.writeToParcel(dest, mNotificationRingtoneUri);
        dest.writeString(mWallpaperName);
        dest.writeString(mRingtoneName);
        dest.writeString(mNotificationRingtoneName);
        dest.writeString(mSoundPackName);
    }

    public static final Parcelable.Creator<DeltaThemeInfo> CREATOR
            = new Parcelable.Creator<DeltaThemeInfo>() {
        public DeltaThemeInfo createFromParcel(Parcel source) {
            return new DeltaThemeInfo(source);
        }

        public DeltaThemeInfo[] newArray(int size) {
            return new DeltaThemeInfo[size];
        }
    };

    private DeltaThemeInfo(Parcel source) {
        mPackageName = source.readString();
        mThemeId = source.readString();
        mStyleId = source.readInt();
        mThemeName = source.readString();
        mColorPaletteName = source.readString();
        mParentThemeId = source.readString();
        mResourceBundle = source.readString();
        mAuthor = source.readString();
        mIsDrmProtected = source.readInt() != 0;
        mWallpaperUri = Uri.CREATOR.createFromParcel(source);
        mRingtoneUri = Uri.CREATOR.createFromParcel(source);
        mNotificationRingtoneUri = Uri.CREATOR.createFromParcel(source);
        mWallpaperName = source.readString();
        mRingtoneName = source.readString();
        mNotificationRingtoneName = source.readString();
        mSoundPackName = source.readString();
    }

}
