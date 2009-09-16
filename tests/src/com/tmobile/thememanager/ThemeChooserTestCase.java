
package com.tmobile.thememanager;

import com.tmobile.thememanager.activity.ThemeChooser;
import com.tmobile.thememanager.R;

import android.os.SystemClock;
import android.view.KeyEvent;
import android.widget.Button;
import android.test.TouchUtils;
import android.util.Log;


public class ThemeChooserTestCase extends ThemeInstrumentationTestBase<ThemeChooser> {

    private static final String TAG = "ThemeChooserTests";

    public ThemeChooserTestCase() {
        super("com.tmobile.thememanager", ThemeChooser.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        PAK_NAMES = new String[] {
                PAK_GRENADE, PAK_TIKIBLOSSOM, PAK_SANROBOT, PAK_PLANETS, PAK_CHERRYBLING,
                PAK_SPLOOSH, PAK_PLUTO, PAK_FIFTYCENT
        };

        mCacheDir = "/data/data/com.tmobile.thememanager/cache/thumbnails";
        mInst = getInstrumentation();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test duration of previewing each theme to be loaded
     */
    public void testThemePreview() {
        Log.v(TAG, "Test theme preview without cache is started...");

        // make sure cache is cleared before launching the activity
        clearAllThumbnailCaches();
        mActivity = (ThemeChooser)getActivity();

        for (int i = 0; i < PAK_NAMES.length; i++) {
            Log.v(TAG, "Preview theme: " + PAK_NAMES[i]);
            mStartTime = SystemClock.uptimeMillis();
            press(KeyEvent.KEYCODE_DPAD_RIGHT);
            mEndTime = SystemClock.uptimeMillis();
            Log.v(TAG, "Time without cache: " + (mEndTime - mStartTime));
        }
       
        // restart the activity
        Log.v(TAG, "restarting the activity...");
        restartActivity();

        // make sure cache is there this time
        for (int i = 0; i < PAK_NAMES.length; i++) {
            Log.v(TAG, "Preview theme: " + PAK_NAMES[i]);
            if (cacheExists(PAK_NAMES[i])) {
                mStartTime = SystemClock.uptimeMillis();
                press(KeyEvent.KEYCODE_DPAD_RIGHT);
                mEndTime = SystemClock.uptimeMillis();
                Log.v(TAG, "Time with cache: " + (mEndTime - mStartTime));
            }
        }
    }
    
    public void testThemeApply0() {
        applyThemeTest(0);
    }
    
    public void testThemeApply1() {
        applyThemeTest(1);
    }

    public void testThemeApply2() {
        applyThemeTest(2);
    }

    public void testThemeApply3() {
        applyThemeTest(3);
    }

    public void testThemeApply4() {
        applyThemeTest(4);
    }

    public void testThemeApply5() {
        applyThemeTest(5);
    }

    public void testThemeApply6() {
        applyThemeTest(6);
    }

    public void testThemeApply7() {
        applyThemeTest(7);
    }

    protected boolean tap(int id) {
        Button view = (Button)mActivity.findViewById(id);
        if (view != null) {
            TouchUtils.clickView(this, view);
            return true;
        }
        return false;
    }
    
    private void applyTheme(int themePos) {
        Log.v(TAG, "Test theme apply: " + PAK_NAMES[themePos]);

        mStartTime = SystemClock.uptimeMillis();
        tap(R.id.apply_btn);
        mEndTime = SystemClock.uptimeMillis();
    }

    private void applyUncachedTheme(int themePos) {
        applyTheme(themePos);
        Log.v(TAG, "Time without cache: " + (mEndTime - mStartTime));
    }

    private void applyCachedTheme(int themePos) {
        if (cacheExists(PAK_NAMES[themePos])) {
            applyTheme(themePos);
            Log.v(TAG, "Time with cache: " + (mEndTime - mStartTime));
        }
    }
    
    private void applyThemeTest(int themePos) {
        clearThumbnailCache(themePos);
        mActivity = (ThemeChooser)getActivity();
        // find the theme to apply
        for (int i = 0; i < themePos; i++) {
            press(KeyEvent.KEYCODE_DPAD_RIGHT);
        }
        applyUncachedTheme(themePos);
        
        Log.v(TAG, "restarting the activity...");
        restartActivity();
        
        applyCachedTheme(themePos);
    }
}
