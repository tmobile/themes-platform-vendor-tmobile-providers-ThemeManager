package com.tmobile.thememanager.activity;

import com.tmobile.thememanager.R;
import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.provider.PackageResources;
import com.tmobile.thememanager.provider.ThemeItem;
import com.tmobile.thememanager.utils.BitmapStore;
import com.tmobile.thememanager.utils.ResourceUtilities;
import com.tmobile.thememanager.utils.ThemeBitmapStore;
import com.tmobile.thememanager.utils.ThemeUtilities;
import com.tmobile.thememanager.widget.CheckOverlay;
import com.tmobile.thememanager.widget.PreviewContentStub;
import com.tmobile.thememanager.widget.AbstractDAOItemAdapter;
import com.tmobile.thememanager.widget.ThemeAdapter;
import com.tmobile.thememanager.widget.ThumbnailedBitmapView;
import com.tmobile.thememanager.widget.PreviewContentStub.OnInflateListener;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ThemeInfo;
import android.content.res.CustomTheme;
import android.database.Cursor;
import android.graphics.Bitmap;
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
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class ThemeChooser extends Activity {

    private static final int DELETE_DIALOG = 2;
    private static final int APPLY_DEFAULT_THEME_DIALOG = 3;

    private Button mApplyButton;

    private Gallery mFilmstrip;
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
        
        mFilmstrip = (Gallery)findViewById(R.id.filmstrip);
        mPreviewCache = (ImageView)findViewById(R.id.preview_image);
        mApplyButton = (Button)findViewById(R.id.apply_btn);
        mApplyButton.setOnClickListener(mClickListener);

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
        return mAdapter.getTheme(mFilmstrip.getSelectedItemPosition());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.theme, menu);

        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem deleteItem = menu.findItem(R.id.delete);
        
        ThemeItem selected = getSelectedThemeItem();
        if (selected == null || selected.isRemovable() == false) {
            deleteItem.setVisible(false);
            return false;
        } else {
            deleteItem.setVisible(true);
            return true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
            case DELETE_DIALOG:
                int messageId;
                if (mFilmstrip.getSelectedItemPosition() == mAdapter.getAppliedPosition()) {
                    messageId = R.string.delete_dialog_message_if_applied;
                } else {
                    messageId = R.string.delete_dialog_message;
                }
                return new AlertDialog.Builder(ThemeChooser.this)
//                  .setIcon(R.drawable.delete_dialog_icon) // TODO: Need the dialog icon
                    .setTitle(R.string.delete_dialog_title)
                    .setMessage(messageId)
                    .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            deleteTheme();
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
                            ThemeUtilities.applyTheme(ThemeChooser.this,
                                    mAdapter.getTheme(defaultThemeIndex));
                            finish();
                        }
                    })
                    .create();
        }
        return null;
    }

    private class ThumbnailAdapter extends ThemeAdapter {
        private int mAppliedPos = -1;
        private final SparseArray<BitmapStore> mBitmapStores =
            new SparseArray<BitmapStore>();

        public ThumbnailAdapter(Activity context) {
            super(context);
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            selectAppliedTheme(false);
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

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            CheckOverlay emblem = (CheckOverlay)getInflater().inflate(R.layout.theme_thumbnail,
                    parent, false);
            ((ThumbnailedBitmapView)emblem.getWrappedView()).setRecycleOnChange(false);
            return emblem;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            CheckOverlay emblem = (CheckOverlay)view;

            ThumbnailedBitmapView image = (ThumbnailedBitmapView)emblem.getWrappedView();

            int position = cursor.getPosition();
            ThemeItem item = getTheme(position);
            image.setImageURI(item.getThumbnailUri());
            
            emblem.setChecked(mAppliedPos == position);
        }
    }

    private void selectAppliedTheme(boolean setSelection) {
        int existingPos = mAdapter.findExistingOrCurrentItem(this,
                (Uri)getIntent().getParcelableExtra(ThemeManager.EXTRA_THEME_EXISTING_URI));
        if (existingPos >= 0) {
            if (setSelection == true) {
                mFilmstrip.setSelection(existingPos);
            }
            mAdapter.setAppliedPosition(existingPos);
        }
    }

    private final OnClickListener mClickListener = new OnClickListener() {
        public void onClick(View v) {
            Log.d(ThemeManager.TAG, "--------apply theme clicked-----------");
            ThemeItem theme = getSelectedThemeItem();
            setResult(RESULT_OK, new Intent().setData(theme.getUri(ThemeChooser.this)));
            finish();
            ThemeUtilities.applyTheme(ThemeChooser.this, theme);
        }
    };

    private final OnItemSelectedListener mThemeSelected = new OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent, View view,
                int position, long id) {
            previewTheme(position);
        }

        public void onNothingSelected(AdapterView<?> parent) {
        }
    };
    
    private String getPreviewKey(ThemeItem theme) {
        return theme.getThemeId() + "-preview-" +
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

    private void applyTemporaryTheming() {
        applyTemporaryTheming(this, getSelectedThemeItem());
    }

    /* package */ static void applyTemporaryTheming(Activity context, ThemeItem theme) {
        String pkg = theme.getPackageName();
        int resId = theme.getResourceId(context);

        if (pkg == null && resId == -1) {
            CustomTheme defaultTheme = CustomTheme.getDefault();
            resId = CustomTheme.getStyleId(context, defaultTheme.getThemePackageName(), defaultTheme.getThemeId());
        }

        if (pkg != null) {
            context.useThemedResources(pkg);
        }

        context.setTheme(resId);
    }

    /* package */ static void revertTemporaryTheming() {
        /* Nothing to do, we only affected this activity. */
    }

    private void deleteTheme() {
        ThemeItem theme = getSelectedThemeItem();
        deleteTheme(theme);
    }

    private void deleteTheme(ThemeItem theme) {
        int oldSelection = mFilmstrip.getSelectedItemPosition();
        boolean resetToDefault = (oldSelection == mAdapter.getAppliedPosition());

        int defaultPos = mAdapter.deleteItem(oldSelection);

        if (resetToDefault) {
            if (defaultPos == -1) {
                if (mAdapter.getCount() > 0) {
                    defaultPos = 0;
                } else {
                    Log.e(ThemeManager.TAG, "No default theme to restore to!");
                }
            }
            if (defaultPos >= 0) {
                mAdapter.setAppliedPosition(defaultPos);
                mFilmstrip.setSelection(defaultPos);
                ThemeUtilities.applyTheme(this, mAdapter.getTheme(defaultPos));
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
