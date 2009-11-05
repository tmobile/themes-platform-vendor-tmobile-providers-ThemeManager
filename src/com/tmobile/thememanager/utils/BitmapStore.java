package com.tmobile.thememanager.utils;

import com.tmobile.thememanager.ThemeManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implements a file-backed image caching store. Useful for storing and
 * recalling scaled images of various sizes.
 * <p>
 * Locking is performed at the object (and not filesystem) level, however does
 * support per-key locking. That is, if the store is writing to key1, but
 * reading key2, the locking will not interfere.
 */
public class BitmapStore extends AbstractMap<String, Bitmap> implements Map<String, Bitmap> {
    private static final String TAG = "ImageStore";

    private final File mPath;
    private final String mStore;

    /* Each key holds its own re-entrant lock to improve concurrency. */
    private final HashMap<String, ReentrantLock> mKeyLocks = new HashMap<String, ReentrantLock>();

    private static final HashMap<File, BitmapStore> sInstances = new HashMap<File, BitmapStore>();

    /**
     * Access a persistent static instance of the BitmapStore. This can be very
     * helpful to synchronize locking on the same access object across multiple
     * instances of an activity (or other Android component).
     *
     * @return persistent static reference.
     */
    public synchronized static BitmapStore getInstance(File dir, String name) {
        File path = new File(dir, name);
        BitmapStore store = sInstances.get(path);
        if (store == null) {
            store = new BitmapStore(path);
            sInstances.put(path, store);
        }
        return store;
    }

    private BitmapStore(File path) {
        mPath = path;
        mStore = path.getName();
    }

    public BitmapStore(String basePath, String store) {
        this(new File(basePath, store));
    }

    public BitmapStore(File basePath, String store) {
        this(new File(basePath, store));
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

    private synchronized ReentrantLock getKeyLock(String key) {
        ReentrantLock lock = mKeyLocks.get(key);
        if (lock == null) {
            lock = new ReentrantLock();
            mKeyLocks.put(key, lock);
        }
        return lock;
    }

    /**
     * Tests is the specified key is currently locked (either by a put or get call.
     */
    public boolean isKeyLocked(String key) {
        ReentrantLock lock = getKeyLock(key);
        return lock.isHeldByCurrentThread();
    }

    /**
     * Access the bitmap at the specified key. This method acquires a lock on
     * this key. For a safer way to access, test the lock first with
     * {@link #isKeyLocked}.
     *
     * @return the stored bitmap if present; null otherwise.
     */
    @Override
    public Bitmap get(Object key) {
        return get((String)key, null);
    }

    /**
     * @see #get(Object)
     */
    public Bitmap get(String key, int width, int height, boolean filter) {
        return get(key, width, height, filter, null);
    }

    /**
     * @see #get(Object)
     */
    public Bitmap get(String key, BitmapFactory.Options options) {
        return get(key, 0, 0, false, options);
    }

    /**
     * @see #get(Object)
     */
    public Bitmap get(String key, int width, int height, boolean filter,
            BitmapFactory.Options options) {
        ReentrantLock lock = getKeyLock(key);
        lock.lock();
        try {
            return getLocked(key, width, height, filter, options);
        } finally {
            lock.unlock();
        }
    }

    private Bitmap getLocked(String key, int width, int height, boolean filter,
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
        if (value == null) {
            remove(key);
            return;
        }
        ReentrantLock lock = getKeyLock(key);
        lock.lock();
        try {
            putLocked(key, width, height, filter, value, format, quality);
        } finally {
            lock.unlock();
        }
    }

    private void putLocked(String key, int width, int height, boolean filter,
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
        ReentrantLock lock = getKeyLock((String)key);
        lock.lock();
        try {
            getKeyFile((String)key, 0, 0, false).delete();
        } finally {
            lock.unlock();
        }
        return null;
    }

    @Override
    public boolean containsKey(Object key) {
        ReentrantLock lock = getKeyLock((String)key);
        lock.lock();
        try {
            return getKeyFile((String)key, 0, 0, false).exists();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Set<java.util.Map.Entry<String, Bitmap>> entrySet() {
        throw new UnsupportedOperationException("TODO");
    }
}
