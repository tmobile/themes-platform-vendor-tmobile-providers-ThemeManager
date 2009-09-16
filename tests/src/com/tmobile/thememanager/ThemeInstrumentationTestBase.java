
package com.tmobile.thememanager;

import com.tmobile.thememanager.utils.FileUtilities;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.IOException;

public abstract class ThemeInstrumentationTestBase<T extends Activity> extends
        ActivityInstrumentationTestCase2<T> {

    private static final String TAG = "ThemeInstrumentationTestBase";
    
    protected static final String PAK_GRENADE = "com.tmobile.pluto.theme.Grenade";
    protected static final String PAK_TIKIBLOSSOM = "com.tmobile.pluto.theme.tikiblossom";
    protected static final String PAK_SANROBOT = "com.tmobile.pluto.theme.sanrobot";
    protected static final String PAK_PLANETS = "com.tmobile.pluto.theme.planets";
    protected static final String PAK_CHERRYBLING = "com.tmobile.pluto.theme.cherrybling";
    protected static final String PAK_SPLOOSH = "com.tmobile.pluto.theme.sploosh";
    protected static final String PAK_PLUTO = "com.tmobile.pluto.theme";
    protected static final String PAK_FIFTYCENT = "com.tmobile.pluto.theme.fiftycent";
    protected static final String PAK_EXAMPLE = "com.tmobile.example.themes";
    
    protected T mActivity;
    protected Instrumentation mInst;
    protected String mCacheDir;
    protected long mStartTime;
    protected long mEndTime;
    protected String PAK_NAMES[];

    public ThemeInstrumentationTestBase(String pkg, Class activityClass) {
        super(pkg, activityClass);
    }
    
    protected void deleteCache(String packageName) {
        if (mCacheDir == null) {
            return;
        }
        try {
            File file = new File(mCacheDir + File.separator + packageName);
            if (!file.exists()) {
                return;
            }
            FileUtilities.cleanDirectory(file);
            Log.v(TAG, "deleteCache: " + file.getAbsolutePath());
        } catch (IOException e) {
        }
    }

    protected boolean cacheExists(String packageName) {
        if (mCacheDir == null) {
            return false;
        }
        File file = new File(mCacheDir + File.separator + packageName);
        if (file.exists() && file.list() != null) {
            if (file.list().length > 0) {
                Log.v(TAG, "cacheExists: " + file.getAbsolutePath());
                return true;
            }
        }
        return false;
    }

    protected void press(int keycode) {
        mInst.sendKeyDownUpSync(keycode);
    }

    protected boolean tap(int id) {
        View view = mActivity.findViewById(id);
        if (view != null) {
            TouchUtils.clickView(this, view);
            return true;
        }
        return false;
    }

    protected void restartActivity() {
        if (mInst == null || mActivity == null) {
            return;
        }
        Log.v(TAG, "onPause");
        mInst.callActivityOnPause(mActivity);
        mInst.waitForIdleSync();
        Log.v(TAG, "onStop");
        mInst.callActivityOnStop(mActivity);
        mInst.waitForIdleSync();
//        Log.v(TAG, "onRestart");
//        mInst.callActivityOnRestart(mActivity);
//        mInst.waitForIdleSync();
//        Log.v(TAG, "onStart");
//        mInst.callActivityOnStart(mActivity);
//        mInst.waitForIdleSync();
//        Log.v(TAG, "onResume");
//        mInst.callActivityOnResume(mActivity);
//        mInst.waitForIdleSync();
        
        Intent intent = mActivity.getIntent();
        mInst.startActivitySync(intent);
//        assert(mActivity.hasWindowFocus());
    }

    protected void clearThumbnailCache(int themePos) {
        if (PAK_NAMES == null) {
            return;
        }
        if (cacheExists(PAK_NAMES[themePos])) {
            deleteCache(PAK_NAMES[themePos]);
        }
    }

    protected void clearAllThumbnailCaches() {
        if (PAK_NAMES == null) {
            return;
        }
        for (int i = 0; i < PAK_NAMES.length; i++) {
            if (cacheExists(PAK_NAMES[i])) {
                deleteCache(PAK_NAMES[i]);
            }
        }
    }

}
