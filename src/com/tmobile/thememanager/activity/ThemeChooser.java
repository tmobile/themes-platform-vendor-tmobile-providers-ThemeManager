package com.tmobile.thememanager.activity;

import com.tmobile.thememanager.R;
import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.activity.Customize.Customizations;
import com.tmobile.thememanager.delta_themes.DeltaThemeGenerator;
import com.tmobile.thememanager.delta_themes.DeltaThemeInfo;
import com.tmobile.thememanager.delta_themes.DeltaThemesStore;
import com.tmobile.thememanager.provider.PackageResources;
import com.tmobile.thememanager.provider.PackageResourcesProvider;
import com.tmobile.thememanager.utils.BitmapStore;
import com.tmobile.thememanager.utils.IOUtilities;
import com.tmobile.thememanager.utils.ResourceUtilities;
import com.tmobile.thememanager.utils.ThemeBitmapStore;
import com.tmobile.thememanager.utils.WallpaperThumbnailCache;
import com.tmobile.thememanager.widget.CheckOverlay;
import com.tmobile.thememanager.widget.PreviewContentStub;
import com.tmobile.thememanager.widget.ThemeAdapter;
import com.tmobile.thememanager.widget.ThumbnailedBitmapView;
import com.tmobile.thememanager.widget.PreviewContentStub.OnInflateListener;
import com.tmobile.thememanager.widget.ThemeAdapter.ThemeItem;
import com.tmobile.widget.Filmstrip;
import com.tmobile.widget.HeaderButton;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ThemeInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.CustomTheme;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

import java.io.InputStream;

public class ThemeChooser extends Activity {

    public static final int THEME_CUSTOMIZE_MSG = 1;

    private static final int REVERT_DIALOG = 1;
    private static final int DELETE_DIALOG = 2;
    private static final int APPLY_DEFAULT_THEME_DIALOG = 3;

    private HeaderButton mApplyButton;
    private BroadcastReceiver mInstallReceiver;

    private Filmstrip mFilmstrip;
    private ThumbnailAdapter mAdapter;

    private Bitmap mPreviewCacheBitmap;
    private ImageView mPreviewCache;
    private PreviewContentStub mPreviewContentStub;
    
    ActivityManager mAM;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // This is defensive measure to guarantee that Personalization Manager
        // can recover after a malicious theme was applied.
        try {
            setContentView(R.layout.theme_chooser);
        } catch (InflateException e) {
            Log.e(ThemeManager.TAG, "InflateException thrown in onCreate\nSwitch to the default theme.", e);
            showDialog(APPLY_DEFAULT_THEME_DIALOG);
            return;
        }

        mPreviewContentStub = (PreviewContentStub)findViewById(R.id.preview_content_stub);
        mPreviewContentStub.setOnInflateListener(mInflateListener);
        
        mFilmstrip = (Filmstrip)findViewById(R.id.filmstrip);
        mPreviewCache = (ImageView)findViewById(R.id.preview_image);
        mApplyButton = (HeaderButton)findViewById(R.id.preview_header);
        mApplyButton.getButton().setOnClickListener(mClickListener);

        /* Even though we have a global receiver, we also need a receiver to be registered
         * so long as the themes UI is present (or could be present soon). */
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addCategory(Intent.CATEGORY_THEME_PACKAGE_INSTALLED_STATE_CHANGE);
        filter.addDataScheme("package");
        if (mInstallReceiver == null) {
            mInstallReceiver = new InstallReceiver();
        }
        registerReceiver(mInstallReceiver, filter);

        mAM = (ActivityManager)getSystemService(ACTIVITY_SERVICE);

        mAdapter = new ThumbnailAdapter(this);
        mFilmstrip.setAdapter(mAdapter);
        mFilmstrip.setOnItemSelectedListener(mThemeSelected);
        mFilmstrip.setCallbackDuringFling(false);

        selectAppliedTheme(true);
        
        registerForContextMenu(mPreviewContentStub);
    }

    @Override
    public void onDestroy() {
        if (mInstallReceiver != null) {
            unregisterReceiver(mInstallReceiver);
            mInstallReceiver = null;
        }
        if (mPreviewContentStub != null) {
            unregisterForContextMenu(mPreviewContentStub);
        }
        recyclePreviewCacheBitmap();
        super.onDestroy();
    }
    
    private void recyclePreviewCacheBitmap() {
        if (mPreviewCacheBitmap != null) {
            mPreviewCacheBitmap.recycle();
        }
        mPreviewCacheBitmap = null;
    }
    
    private ThemeItem getSelectedThemeItem() {
        return (ThemeItem)mFilmstrip.getSelectedItem();
    }

/** A fix for THEMES-104: disable Customize flow
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the currently selected menu XML resource.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.theme, menu);

        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem revertItem = menu.findItem(R.id.revert);
        MenuItem deleteItem = menu.findItem(R.id.delete);
        
        // Only customized themes can be reverted to original.
        if (revertItem != null) {
            ThemeItem selected = getSelectedThemeItem();
            if (selected == null || selected.type != ThemeItem.TYPE_DIFF) {
                revertItem.setVisible(false);
            } else {
                revertItem.setVisible(true);
            }
        }
        
        // Only third-party themes can be deleted.
        if (deleteItem != null) {
            ThemeItem selected = getSelectedThemeItem();
            if (selected == null || selected.type != ThemeItem.TYPE_PURCHASE) {
                deleteItem.setVisible(false);
            } else {
                deleteItem.setVisible(true);
            }
        }
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.customize:
                Customize.show(this, getSelectedThemeItem(), THEME_CUSTOMIZE_MSG);
                return true;

            case R.id.revert:
                showDialog(REVERT_DIALOG);
                return true;

            case R.id.delete:
                showDialog(DELETE_DIALOG);
                return true;

            // Generic catch all for all the other menu resources
            default:
                Log.e(ThemeManager.TAG, "Unknown action");
                break;
        }

        return false;
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        onCreateOptionsMenu(menu);        
        onPrepareOptionsMenu(menu);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case REVERT_DIALOG:
                return new AlertDialog.Builder(ThemeChooser.this)
//                  .setIcon(R.drawable.revert_dialog_icon) // TODO: Need the dialog icon
                    .setTitle(R.string.revert_dialog_title)
                    .setMessage(R.string.revert_dialog_message)
                    .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ThemeItem theme = getSelectedThemeItem();
                            int selectedPos = mFilmstrip.getSelectedItemPosition();
                            if (theme.hasParentTheme()) {
                                theme = mAdapter.revertToOriginal(selectedPos, theme);
                                if (theme != null) {
                                    previewTheme(selectedPos);
                                    if (selectedPos == mAdapter.getAppliedPosition()) {
                                        applyTheme();
                                    }
                                } else {
                                    Log.e(ThemeManager.TAG, "Failed to find diff theme");
                                }
                            } else {
                                Log.e(ThemeManager.TAG, "Not a diff theme");
                            }
                        }
                    })
                    .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    })
                    .create();

            case DELETE_DIALOG:
                return new AlertDialog.Builder(ThemeChooser.this)
//                  .setIcon(R.drawable.delete_dialog_icon) // TODO: Need the dialog icon
                    .setTitle(R.string.delete_dialog_title)
                    .setMessage(R.string.delete_dialog_message)
                    .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ThemeItem selected = getSelectedThemeItem();
                            if (!selected.hasParentTheme()) {
                                int oldSelection = mFilmstrip.getSelectedItemPosition();
                                int newSelection = mAdapter.deleteThemeItem(oldSelection);
                                mAdapter.setAppliedPosition(newSelection);
                                updateConfiguration(mAdapter.getTheme(newSelection));
                                finish();
                            } else {
                                Log.e(ThemeManager.TAG, "Must be not a diff theme");
                            }
                        }
                    })
                    .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    })
                    .create();

            case APPLY_DEFAULT_THEME_DIALOG:
                return new AlertDialog.Builder(ThemeChooser.this)
//                  .setIcon(R.drawable.delete_dialog_icon) // TODO: Need the dialog icon
                    .setTitle(R.string.applydefaulttheme_dialog_title)
                    .setMessage(R.string.applydefaulttheme_dialog_message)
                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mAdapter = new ThumbnailAdapter(ThemeChooser.this);
                            int defaultThemeIndex = mAdapter.findItem(CustomTheme.getDefault());
                            mAM = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
                            applyTheme(mAdapter.getTheme(defaultThemeIndex));
                            finish();
                        }
                    })
                    .create();
        }
        return null;
    }
*/

    private class ThumbnailAdapter extends ThemeAdapter {
        private int mAppliedPos = -1;
        private final SparseArray<BitmapStore> mBitmapStores =
            new SparseArray<BitmapStore>();

        public ThumbnailAdapter(Context context) {
            super(context);
        }

        public void setAppliedItem(ThemeItem item) {
            for (int pos = 0; pos < getCount(); pos++) {
                ThemeItem it = getTheme(pos);
                if (item == it) {
                    setAppliedPosition(pos);
                }
            }
        }

        public int getAppliedPosition() {
            return mAppliedPos;
        }

        public void setAppliedPosition(int position) {
            if (mAppliedPos != position) {
                mAppliedPos = position;
                notifyDataSetChanged();
            }
        }
        
        public BitmapStore getBitmapStore(int position) {
            BitmapStore store = mBitmapStores.get(position);
            if (store == null) {
                ThemeItem theme = getTheme(position);
                store = new ThemeBitmapStore(ThemeChooser.this, theme.getPackageName());
                mBitmapStores.put(position, store);
            }
            return store;
        }
        
        public View getView(int position, View convertView, ViewGroup parent) {
            CheckOverlay emblem;
            ThumbnailedBitmapView image;

            if (convertView == null) {
                emblem = (CheckOverlay)getInflater().inflate(R.layout.theme_thumbnail,
                        parent, false);
                ((ThumbnailedBitmapView)emblem.getWrappedView()).setRecycleOnChange(false);
            } else {
                emblem = (CheckOverlay)convertView;
            }

            image = (ThumbnailedBitmapView)emblem.getWrappedView();
            ThemeItem item = getTheme(position);

            /* If the diff theme provides a wallpaper, we need to show it here.
             * Otherwise, fall back to the parent theme's thumbnail. */
            if (item.type == ThemeItem.TYPE_DIFF &&
                    item.delta.getWallpaperUri() != null) {
                WallpaperThumbnailCache cache =
                    new WallpaperThumbnailCache(ThemeChooser.this, item.getPackageName(),
                            item.getWallpaperUri(ThemeChooser.this));
                image.setImageStore(cache, null, item.getWallpaperIdentifier() + "-small");
            } else {
                ThemeItem thumbnailTheme;
                if (item.type == ThemeItem.TYPE_DIFF) {
                    thumbnailTheme = item.getParentTheme();
                } else {
                    thumbnailTheme = item;
                }

                try {
                    Resources r = PackageResourcesProvider.getResourcesForTheme(ThemeChooser.this,
                            thumbnailTheme.getPackageName());

                    Drawable d = r.getDrawable(thumbnailTheme.info.thumbnail);
                    d.setDither(true);
                    image.setImageDrawable(d);
                } catch (NameNotFoundException e) {
                    Log.e(ThemeManager.TAG, "Unable to retrieve theme thumbnail for theme: " + 
                            thumbnailTheme);
                }
            }
            
            emblem.setChecked(mAppliedPos == position);

            return emblem;
        }
    }

    private void selectAppliedTheme(boolean setSelection) {
        Configuration config = mAM.getConfiguration();
        int position = mAdapter.findItem(config.customTheme);
        if (position >= 0) {
            if (setSelection == true) {
                mFilmstrip.setSelection(position);
            }
            mAdapter.setAppliedPosition(position);
        }
    }

    private OnClickListener mClickListener = new OnClickListener() {
        public void onClick(View v) {
            Log.d(ThemeManager.TAG, "--------apply theme clicked-----------");
            setResult(RESULT_OK);
            finish();
            applyTheme();
        }
    };

    private final OnItemSelectedListener mThemeSelected = new OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent, View view,
                int position, long id) {
            previewTheme(position);
        }

        public void onNothingSelected(AdapterView<?> parent) {
            Log.d(ThemeManager.TAG, "No theme selected?  When does this happen?");
        }
    };
    
    private String getPreviewKey(ThemeItem theme) {
        /* TODO: Should be changed to use unique theme name. */
        return theme.getResourceId() + "-preview-" + 
            ResourceUtilities.getOrientationString(this);
    }
    
    private boolean tryPreviewFromCache(int position) {
        BitmapStore cache = mAdapter.getBitmapStore(position);
        if (cache != null) {
            String baseKey = getPreviewKey(mAdapter.getTheme(position));
            Bitmap preview = cache.get(baseKey);
            if (preview != null) {
                recyclePreviewCacheBitmap();
                mPreviewCache.setImageBitmap(preview);
                mPreviewCacheBitmap = preview;
                return true;
            }
        }
        return false;
    }
    
    private void storePreviewInCache(int position) {
        BitmapStore cache = mAdapter.getBitmapStore(position);
        if (cache != null) {
            String key = getPreviewKey(mAdapter.getTheme(position));
            mPreviewContentStub.buildDrawingCache();
            Bitmap bmp = mPreviewContentStub.getDrawingCache();
            if (bmp != null) {
                cache.put(key, bmp);
                mPreviewContentStub.destroyDrawingCache();
            } else {
                if (ThemeManager.DEBUG) {
                    Log.d(ThemeManager.TAG, "Unable to access preview cache area bitmap.");
                }
            }
        }
    }

    private void previewTheme(int position) {
        /* Previewing themes is very heavy, so we store the result each time in
         * a bitmap store and attempt to recall it here. */
        boolean loadedFromCache = tryPreviewFromCache(position);
        
        if (loadedFromCache == false) {
            mPreviewCache.setImageDrawable(null);
            
            /* Inflate the preview widget and attach; this is deferred until the
             * next layout pass. */
            mPreviewContentStub.postInflate();
        } else {
            /* We know it is not being displayed so we might as well tidy up the
             * memory it might be using. */
            mPreviewContentStub.removeAllViews();
        }
    }

    private static ThemeInfo getParentThemeInfo(Context context, String packageName, String parentThemeId) {
        if (packageName != null && packageName.length() > 0) {
            try {
                PackageInfo parentPI = context.getPackageManager().getPackageInfo(packageName, 0);
                ThemeInfo [] infos = parentPI.themeInfos;
                if (infos != null && infos.length > 0) {
                    for (ThemeInfo info : infos) {
                        if (info.themeId.equals(parentThemeId)) {
                            return info;
                        }
                    }
                }
                throw new PackageManager.NameNotFoundException("Theme <" + parentThemeId + "> not found");
            } catch (Exception e) {
                Log.e(ThemeManager.TAG, "parent package is not found", e);
            }
        }
        return null;
    }

    /* package */ static Uri getParentThemeWallpaperUri(Context context, String packageName, String parentThemeId) {
        ThemeInfo info = getParentThemeInfo(context, packageName, parentThemeId);
        if (info != null && info.wallpaperImageName != null) {
            return PackageResources.getImageUri(
                context,
                packageName,
                info.wallpaperImageName);
        }
        return null;
    }

    private final OnInflateListener mInflateListener = new OnInflateListener() {
        public void onPostInflate(PreviewContentStub stub) {
            int selectedPos = mFilmstrip.getSelectedItemPosition();
            ThemeItem selected = mAdapter.getTheme(selectedPos);
            
            ImageView wallpaper = (ImageView)stub.findViewById(R.id.wallpaper);
            wallpaper.setImageURI(selected.getWallpaperUri(ThemeChooser.this));
            
            TextView title = (TextView)findViewById(R.id.theme_title);
            TextView author = (TextView)findViewById(R.id.theme_author);
            
            title.setText(selected.getName());
            author.setText(getString(R.string.author) + ": " + selected.getAuthor());

            revertTemporaryTheming();
            
            /* Schedule a preview cache attempt to happen only after the widget
             * is properly measured. */
            mHandler.sendTryPreviewSave(selectedPos);
        }

        public void onPreInflate(PreviewContentStub stub) {
            applyTemporaryTheming();
        }
    };
    
    public static void show(Context context) {
        Intent i = new Intent(context, ThemeChooser.class);
        context.startActivity(i);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case THEME_CUSTOMIZE_MSG:
                if (resultCode == RESULT_OK) {
                    Bundle extras = data.getExtras();
                    ThemeItem baseTheme = extras.getParcelable(ThemeManager.EXTRA_THEME_ITEM);
                    Customizations changes = extras.getParcelable(Customize.EXTRA_CUSTOMIZATIONS);
                    
                    DeltaThemeInfo delta = DeltaThemeGenerator.createDeltaThemeInfo(this,
                            baseTheme, changes);
                    
                    DeltaThemesStore.getDeltaThemesStore().persist(delta);
                    
                    int position = mAdapter.putDeltaTheme(delta);
                    
                    /* Make sure we delete the preview cache to both save
                     * storage space and to prevent uniqueness issues if a diff
                     * theme is modified a second time (the unique theme id
                     * would remain the same, and thus the cache must be
                     * invalidated). */
                    BitmapStore store = mAdapter.getBitmapStore(position);
                    if (store != null) {
                        store.clear();
                    }
                    
                    if (position == mAdapter.getAppliedPosition()) {
                        applyTheme();
                    } else {
                        previewTheme(position);
                    }
                }
                break;
        }
    }

    private void applyTemporaryTheming() {
        applyTemporaryTheming(this, getSelectedThemeItem());
    }

    /* package */ static void applyTemporaryTheming(Activity context, ThemeItem theme) {
        String pkg = theme.getPackageName();
        int resId = theme.getResourceId();

        if (pkg == null && resId == -1) {
            CustomTheme defaultTheme = CustomTheme.getDefault();
            resId = CustomTheme.getStyleId(context, defaultTheme.getThemePackageName(), defaultTheme.getThemeId());
        }

        if (pkg != null) {
            String path = null;
            int parentThemeId = -1;
            if (theme.hasParentTheme()) {
                path = theme.delta.getResourceBundlePath();
                parentThemeId = theme.getParentTheme().getResourceId();
            }
            context.useThemedResources(pkg, path, parentThemeId);
        }

        context.setTheme(resId);
    }

    /* package */ static void revertTemporaryTheming() {
        /* Nothing to do, we only affected this activity. */
    }

    private void applyTheme() {
        // New theme is applied, hence reset the count to 0.
        Intent intent = new Intent(Intent.ACTION_APP_LAUNCH_FAILURE_RESET,
                Uri.fromParts("package", "com.tmobile.thememanager.activity", null));
        this.getApplicationContext().sendBroadcast(intent);

        ThemeItem theme = getSelectedThemeItem();
        applyTheme(theme);
    }

    private void applyTheme(ThemeItem theme) {
        Uri wallpaperUri = theme.getWallpaperUri(this);
        if (wallpaperUri != null) {
            setWallpaper(wallpaperUri);
        }
        
        Uri ringtoneUri = theme.getRingtoneUri(this);
        if (ringtoneUri != null) {
            setDefaultRingtone(ringtoneUri);
        }
        
        Uri notificationRingtoneUri = theme.getNotificationRingtoneUri(this);
        if (notificationRingtoneUri != null) {
            setDefaultNotificationRingtone(notificationRingtoneUri);
        }

        /* Trigger a configuration change so that all apps will update their UI.  This will also
         * persist the theme for us across reboots. */
        updateConfiguration(theme);
    }

    private void updateConfiguration(ThemeItem theme) {
        Configuration currentConfig = mAM.getConfiguration();

//        if (TextUtils.isEmpty(theme.getThemeId())) {
//            /* Reset to system default. */
//            currentConfig.customTheme = CustomTheme.getDefault();
//        } else {
            /* Set the runtime user-provided theme. */
            boolean hasParent = theme.hasParentTheme();
            currentConfig.customTheme = new CustomTheme(
                    hasParent? theme.delta.getParentThemeId() : theme.getThemeId(),
                    theme.getPackageName(),
                    hasParent);
            if (theme.hasParentTheme()) {
                currentConfig.customTheme.setForceUpdate(true);
                String resourceBundlePath = theme.delta.getResourceBundlePath();
                if (resourceBundlePath != null) {
                    currentConfig.customTheme.setThemeResourcePath(resourceBundlePath);
                }
            }
//        }

        mAM.updateConfiguration(currentConfig);

        if (hasParent) {
            currentConfig.customTheme.setForceUpdate(false);
        }
    }

    private void setWallpaper(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            try {
                setWallpaper(in);
            } finally {
                IOUtilities.close(in);
            }
        } catch (Exception e) {
            Log.e(ThemeManager.TAG, "Could not set wallpaper", e);
        }
    }

    private void setDefaultRingtone(Uri ringtoneUri) {
        if (ThemeManager.DEBUG) {
            Log.i(ThemeManager.TAG, "ringtoneUri=" + ringtoneUri);
        }

        if (ringtoneUri != null) {
            RingtoneManager.setActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE,
                    ringtoneUri);
        }
    }

    private void setDefaultNotificationRingtone(Uri ringtoneUri) {
        if (ThemeManager.DEBUG) {
            Log.i(ThemeManager.TAG, "ringtoneUri=" + ringtoneUri);
        }

        if (ringtoneUri != null) {
            RingtoneManager.setActualDefaultRingtoneUri(this, RingtoneManager.TYPE_NOTIFICATION,
                    ringtoneUri);
        }
    }

    private class InstallReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String pkg = intent.getData().getSchemeSpecificPart();

            if (action.equals(Intent.ACTION_PACKAGE_REMOVED) == true) {
                mAdapter.removeThemesByPackage(pkg);
                selectAppliedTheme(false);
                DeltaThemesStore.getDeltaThemesStore().deleteThemesForPackage(pkg);
            } else if (action.equals(Intent.ACTION_PACKAGE_ADDED) == true) {
                try {
                    mAdapter.addThemesFromPackage(getPackageManager().getPackageInfo(pkg, 0));
                } catch (NameNotFoundException e) {
                    Log.e(ThemeManager.TAG, "Failed to get package info for recently added theme: " + pkg, e);
                }
            }
        }
    }
    
    private final CacheStorageHandler mHandler = new CacheStorageHandler();
    private class CacheStorageHandler extends Handler {
        private static final int MSG_TRY_PREVIEW_SAVE = 1;
        
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_TRY_PREVIEW_SAVE:
                    if (mPreviewContentStub.getWidth() > 0 && 
                            mPreviewContentStub.getHeight() > 0) {
                        if (msg.arg2 > 0 && ThemeManager.DEBUG) {
                            Log.d(ThemeManager.TAG, "Took " + msg.arg2 + " retries to save preview cache.");
                        }
                        if (msg.arg1 != mFilmstrip.getSelectedItemPosition()) {
                            if (ThemeManager.DEBUG) {
                                Log.d(ThemeManager.TAG, "Race condition: couldn't save preview bitmap before selection changed!");
                            }
                        } else {
                            storePreviewInCache(msg.arg1);
                        }
                    } else {
                        msg.arg2++;
                        sendMessageDelayed(msg, 100);
                    }
                    break;
                    
                default:
                    super.handleMessage(msg);
            }
        }
        
        public void sendTryPreviewSave(int adapterPosition) {
            removeMessages(MSG_TRY_PREVIEW_SAVE);
            sendMessageDelayed(obtainMessage(MSG_TRY_PREVIEW_SAVE, adapterPosition, 0), 200);
        }
    }
}
