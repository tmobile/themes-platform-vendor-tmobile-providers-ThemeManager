/*
 * Copyright (C) 2010, T-Mobile USA, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tmobile.thememanager.utils;

import android.app.IWallpaperManager;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class WallpaperUtilities {

    private static final String TAG = "WallpaperUtilities";

    /**
     * Lock held while the temporary lock wallpaper is being written to disk.
     * This is used to prevent a possible race condition if multiple events
     * occur in quick succession to apply a specific theme with a lock paper.
     * <p>
     * TODO: This should be a file lock to avoid breaking down when carried over
     * multiple processes.
     */
    private static Object sLockWallpaperLock = new Object();

    private static boolean sLockscreenTested = false;
    private static Method sLockscreenManager_setStream;

    private WallpaperUtilities() {}

    public static void setWallpaper(Context context, Uri uri) {
        InputStream in = null;
        try {
           in = context.getContentResolver().openInputStream(uri);
           WallpaperManager.getInstance(context).setStream(in);
        } catch (Exception e) {
            Log.w(TAG, "Could not set wallpaper", e);
        } finally {
            if (in != null) {
                IOUtilities.close(in);
            }
        }
    }

    /**
     * Tests if the HTC lockscreen facility is on the current platform. If true,
     * it is safe to call {@link #setLockWallpaper}.
     */
    public static synchronized boolean supportsLockWallpaper(Context context) {
        if (!sLockscreenTested) {
            try {
                Class<?> lockscreenManagerClass = Class.forName("com.htc.lockscreen.LockscreenManager");
                sLockscreenManager_setStream = lockscreenManagerClass.getMethod("setStream", new Class[] { InputStream.class, Context.class });
            } catch (Exception e) {
            }
            sLockscreenTested = true;
        }
        return sLockscreenManager_setStream != null;
    }

    /**
     * Sets an HTC lockscreen wallpaper. It is necessary that you only invoke this method
     * if {@link #supportsLockWallpaper} yields true.
     */
    public static void setLockWallpaper(Context context, Uri uri) {
        Method lockscreenManager_setStream;
        synchronized (WallpaperUtilities.class) {
            if (!sLockscreenTested) {
                throw new IllegalStateException("Invoke supportsLockWallpaper first!");
            }
            lockscreenManager_setStream = sLockscreenManager_setStream;
        }
        synchronized (sLockWallpaperLock) {
            InputStream in = null;
            try {
                in = context.getContentResolver().openInputStream(uri);
                lockscreenManager_setStream.invoke(null, in, context);
            } catch (Exception e) {
                if ((e instanceof InvocationTargetException) || (e instanceof IOException)) {
                    Log.w(TAG, "Unable to set lock screen wallpaper (uri=" + uri + "): " + e);
                } else {
                    /* Explode on unexpected reflection errors... */
                    throw new RuntimeException(e);
                }
            } finally {
                if (in != null) {
                    IOUtilities.close(in);
                }
            }
        }
    }

    public static void setLiveWallpaper(Context context, ComponentName component) {
        try {
            getWallpaperService(context).setWallpaperComponent(component);
        } catch (RemoteException e) {
            Log.w(TAG, "Failure setting wallpaper", e);
        }
    }

    private static IWallpaperManager getWallpaperService(Context context) {
        return WallpaperManager.getInstance(context).getIWallpaperManager();
    }
}
