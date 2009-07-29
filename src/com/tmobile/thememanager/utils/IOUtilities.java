package com.tmobile.thememanager.utils;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Closeable;
import java.io.IOException;

public class IOUtilities {
    public static void close(Closeable stream) {
        try {
            stream.close();
        } catch (IOException e) {}
    }

    /**
     * Efficient alternative to {@link Parcel#writeParcelable} which avoids the
     * reflection overhead.
     */
    public static void writeParcelableToParcel(Parcel dest, Parcelable parcelable, int flags) {
        if (parcelable == null) {
            dest.writeInt(0);
        } else {
            dest.writeInt(1);
            parcelable.writeToParcel(dest, flags);
        }
    }
    
    /**
     * Efficient alternative to {@link Parcel#readParcelable} which avoids the
     * reflection overhead.
     */
    public static <T extends Parcelable> T readParcelableFromParcel(Parcel source,
            Parcelable.Creator<T> creator) {
        int nullFlag = source.readInt();
        if (nullFlag == 0) {
            return null;
        } else {
            return creator.createFromParcel(source);
        }
    }
}
