package com.tmobile.thememanager.utils;

import com.tmobile.thememanager.ThemeManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

/**
 * Implements a file-backed image caching store. Useful for storing and
 * recalling scaled images of various sizes.
 */
public class BitmapStore extends AbstractMap<String, Bitmap> implements Map<String, Bitmap> {
    private static final String TAG = "ImageStore";

    private final File mPath;
    private final String mStore;
    
    public BitmapStore(String basePath, String store) {
        this(new File(basePath), store);
    }
    
    public BitmapStore(File basePath, String store) {
        if (TextUtils.isEmpty(store) == true) {
            throw new IllegalArgumentException("store cannot be empty.");
        }

        mPath = new File(basePath + File.separator + store);
        mStore = store;
    }
    
    private File getKeyFile(String key, int width, int height, boolean filter) {
        StringBuilder b = new StringBuilder(key.replaceAll(File.separator, "+"));
        if (width > 0 || height > 0) {
            b.append('-');
            b.append(width).append('x').append(height);
            if (filter == true) {
                b.append("-filter");
            }
        }
        return new File(mPath + File.separator + b);
    }

    /**
     * Deletes the entire backing store. Subsequent accesses to this object are
     * invalid.
     * 
     * @throws IOException 
     */
    public void delete() throws IOException {
        FileUtilities.deleteDirectory(mPath);
    }
    
    @Override
    public void clear() {
        try {
            FileUtilities.cleanDirectory(mPath);
        } catch (IOException e) {
            if (ThemeManager.DEBUG) {
                Log.e(TAG, "Unable to clear image store.", e);
            }
        }
    }

    @Override
    public Bitmap get(Object key) {
        return get((String)key, null);
    }

    public Bitmap get(String key, int width, int height, boolean filter) {
        return get(key, width, height, filter, null);
    }

    public Bitmap get(String key, BitmapFactory.Options options) {
        return get(key, 0, 0, false, options);
    }
    
    public Bitmap get(String key, int width, int height, boolean filter,
            BitmapFactory.Options options) {
        File file = getKeyFile(key, width, height, filter);
        if (file.exists() == true) {
            return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        } else  if (width > 0 || height > 0) {
            Bitmap fullSize = get(key, options);
            if (fullSize != null) {
                Bitmap bmp = Bitmap.createScaledBitmap(fullSize, width, height, filter);
                fullSize.recycle();
                try {
                    put(key, width, height, filter, bmp);
                } catch (IOException e) {
                    if (ThemeManager.DEBUG) {
                        Log.e(TAG, "Unable to store bitmap in " + mStore + " at " + file, e);
                    }
                }
                return bmp;
            }
        }
        return null;
    }

    /**
     * @return null always.
     */
    @Override
    public Bitmap put(String key, Bitmap value) {
        try {
            put(key, 0, 0, false, value);
        } catch (IOException e) {
            if (ThemeManager.DEBUG) {
                Log.e(TAG, "Unable to store bitmap in " + mStore + " at " + key);
            }
        }
        return null;
    }
    
    public void put(String key, int width, int height, boolean filter,
            Bitmap value) throws IOException {
        put(key, width, height, filter, value, Bitmap.CompressFormat.PNG, 100);
    }

    public void put(String key, Bitmap value, Bitmap.CompressFormat format, 
            int quality) throws IOException {
        put(key, 0, 0, false, value, format, quality);
    }

    public void put(String key, int width, int height, boolean filter,
            Bitmap value, Bitmap.CompressFormat format, int quality) throws IOException {
        FileOutputStream out = null;
        try {
            out = FileUtilities.openOutputStream(getKeyFile(key, width, height, filter));
            value.compress(format, quality, out);
        } finally {
            if (out != null)
                IOUtilities.close(out);
        }
    }

    /**
     * @return null always.
     */
    @Override
    public Bitmap remove(Object key) {
        getKeyFile((String)key, 0, 0, false).delete();
        return null;
    }

    @Override
    public boolean containsKey(Object key) {
        return getKeyFile((String)key, 0, 0, false).exists();
    }

    @Override
    public Set<java.util.Map.Entry<String, Bitmap>> entrySet() {
        throw new UnsupportedOperationException("TODO");
    }
}
