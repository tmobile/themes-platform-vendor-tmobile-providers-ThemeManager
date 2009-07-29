
package com.tmobile.thememanager;

import com.tmobile.thememanager.R;
import com.tmobile.thememanager.activity.ThemeWallpaperChooser;

import android.os.SystemClock;
import android.view.KeyEvent;
import android.util.Log;


public class ThemeWallpaperChooserTestCase extends
        ThemeInstrumentationTestBase<ThemeWallpaperChooser> {

    private static final String TAG = "WallpaperChooserTests";

    public ThemeWallpaperChooserTestCase() {
        super("com.tmobile.thememanager", ThemeWallpaperChooser.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        PAK_NAMES = new String[]{PAK_GRENADE, PAK_TIKIBLOSSOM, PAK_SANROBOT, 
            PAK_EXAMPLE, PAK_PLANETS, PAK_CHERRYBLING, PAK_SPLOOSH, PAK_PLUTO, PAK_FIFTYCENT};
        
        mCacheDir = "/data/data/com.tmobile.thememanager/cache/thumbnails";
        mInst = getInstrumentation();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testAWallpaperPreview() {
        Log.v(TAG, "Test wallpaper preview without cache is started...");

        // make sure cache is cleared before launching the activity
        clearAllThumbnailCaches();
        mActivity = (ThemeWallpaperChooser)getActivity();

        for (int i = 0; i < PAK_NAMES.length; i++) {
            Log.v(TAG, "Preview wallpaper: " + PAK_NAMES[i]);
            mStartTime = SystemClock.uptimeMillis();
            press(KeyEvent.KEYCODE_DPAD_RIGHT);
            mEndTime = SystemClock.uptimeMillis();
            Log.v(TAG, "Time without cache: " + (mEndTime - mStartTime));
        }
       
        // restart the activity
        Log.v(TAG, "restarting the activity...");
//        restartActivity();

        // make sure cache is there this time
//        for (int i = 0; i < PAK_NAMES.length; i++) {
//            Log.v(TAG, "Preview wallpaper: " + PAK_NAMES[i]);
//            if (cacheExists(PAK_NAMES[i])) {
//                mStartTime = SystemClock.uptimeMillis();
//                press(KeyEvent.KEYCODE_DPAD_RIGHT);
//                mEndTime = SystemClock.uptimeMillis();
//                Log.v(TAG, "Time with cache: " + (mEndTime - mStartTime));
//            }
//        }
    }
 
    public void testBWallpaperApply0() {
        applyWallpaperTest(0);
    }

    public void testBWallpaperApply1() {
        applyWallpaperTest(1);
    }

    public void testBWallpaperApply2() {
        applyWallpaperTest(2);
    }

    public void testBWallpaperApply3() {
        applyWallpaperTest(3);
    }

    public void testBWallpaperApply4() {
        applyWallpaperTest(4);
    }

    public void testBWallpaperApply5() {
        applyWallpaperTest(5);
    }

    public void testBWallpaperApply6() {
        applyWallpaperTest(6);
    }

    public void testBWallpaperApply7() {
        applyWallpaperTest(7);
    }

    public void testBWallpaperApply8() {
        applyWallpaperTest(8);
    }
    
    private void applyWallpaper(int wallpaperPos) {
        Log.v(TAG, "Test wallpaper apply: " + PAK_NAMES[wallpaperPos]);

        for (int i = 0; i < wallpaperPos; i++) {
            press(KeyEvent.KEYCODE_DPAD_RIGHT);
        }
        mStartTime = SystemClock.uptimeMillis();
        tap(R.id.set);
        mEndTime = SystemClock.uptimeMillis();
    }

    private void applyUncachedWallpaper(int wallpaperPos) {
        applyWallpaper(wallpaperPos);
        Log.v(TAG, "Time without cache: " + (mEndTime - mStartTime));
    }

    private void applyCachedWallpaper(int wallpaperPos) {
        if (cacheExists(PAK_NAMES[wallpaperPos])) {
            applyWallpaper(wallpaperPos);
            Log.v(TAG, "Time with cache: " + (mEndTime - mStartTime));
        }
    }
    
    private void applyWallpaperTest(int wallpaperPos) {
        clearThumbnailCache(wallpaperPos);
        mActivity = (ThemeWallpaperChooser)getActivity();
        applyUncachedWallpaper(wallpaperPos);
        
        Log.v(TAG, "restarting the activity...");
//        restartActivity();
        
//        applyCachedWallpaper(wallpaperPos);
    }
}
