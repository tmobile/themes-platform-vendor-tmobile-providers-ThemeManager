package com.tmobile.thememanager;

import com.tmobile.thememanager.utils.BitmapStore;

import java.io.File;

import junit.framework.TestCase;

public class BitmapStoreTest extends TestCase {
    public void testGetInstance() throws InterruptedException {
        final File file = new File("/sdcard/bitmap-store-test", "test");
        Thread t = new GetInstanceThread(file, "test");
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
        }
    }

    private static class GetInstanceThread extends Thread {
        BitmapStore mStore;
        File mFile;
        String mName;

        public GetInstanceThread(File file, String name) {
            mFile = file;
            mName = name;
            mStore = BitmapStore.getInstance(file, name);
        }

        public void run() {
            BitmapStore store = BitmapStore.getInstance(new File(mFile.getAbsolutePath()), mName);
            assertEquals("getInstance() should return a static reference.", store, mStore);
        }
    }
}
