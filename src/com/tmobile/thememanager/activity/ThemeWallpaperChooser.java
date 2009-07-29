package com.tmobile.thememanager.activity;

import com.tmobile.thememanager.R;
import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.provider.PackageResources;
import com.tmobile.thememanager.utils.BitmapStore;
import com.tmobile.thememanager.utils.FileUtilities;
import com.tmobile.thememanager.utils.IOUtilities;
import com.tmobile.thememanager.utils.WallpaperThumbnailCache;
import com.tmobile.thememanager.widget.ThumbnailedBitmapView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ResourceCursorAdapter;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ThemeWallpaperChooser extends Activity implements AdapterView.OnItemSelectedListener,
        OnClickListener {
    private static final String[] QUERY_FIELDS = {
        PackageResources.ImageColumns._ID,
        PackageResources.ImageColumns.PACKAGE, 
        PackageResources.ImageColumns.ASSET_PATH
    };
    
    private Gallery mGallery;
    private ThumbnailedBitmapView mImageView;
    private ThumbnailAdapter mAdapter;

    private boolean mPickAction;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.wallpaper_chooser);

        mPickAction = getIntent().getAction().equals(Intent.ACTION_PICK_WALLPAPER);
        
        Cursor wallpapers = getWallpapers();

        mGallery = (Gallery)findViewById(R.id.gallery);
        mAdapter = new ThumbnailAdapter(this, wallpapers);
        mGallery.setAdapter(mAdapter);
        mGallery.setOnItemSelectedListener(this);
        mGallery.setCallbackDuringFling(false);

        Button b = (Button)findViewById(R.id.set);
        b.setOnClickListener(this);

        mImageView = (ThumbnailedBitmapView)findViewById(R.id.wallpaper);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mImageView.setImageDrawable(null);
        mAdapter.clearCache();
        mAdapter.changeCursor(null);
    }
    
    private Cursor getWallpapers() {
        StringBuilder b = new StringBuilder();
        b.append(PackageResources.ImageColumns.IMAGE_TYPE).append('=')
            .append(PackageResources.ImageColumns.IMAGE_TYPE_WALLPAPER);
        if (mPickAction == true) {
            b.append(" AND ").append(PackageResources.ImageColumns.IS_DRM).append("=0");
        }
        
        Cursor c = getContentResolver().query(PackageResources.ImageColumns.CONTENT_PLURAL_URI,
                QUERY_FIELDS, b.toString(), null, null);
        if (c == null) {
            throw new RuntimeException("Could not retrieve theme wallpapers.");
        }
        
        return c;
    }

    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
        Cursor c = (Cursor)parent.getItemAtPosition(position);
        String assetPath = c.getString(c.getColumnIndexOrThrow(PackageResources.ImageColumns.ASSET_PATH));
        mImageView.setImageStore(mAdapter.getStore(c), null, assetPath + "-medium_cropped");
    }

    public void onNothingSelected(AdapterView<?> parent) {
    }
    
    private void chooseCurrentWallpaper() {
        int position = mGallery.getSelectedItemPosition();
        Cursor c = (Cursor)mAdapter.getItem(position);
        if (mPickAction == false) {
            InputStream in = null;
            try {
                in = mAdapter.getWallpaperStream(c);
                setWallpaper(in);
            } catch (IOException e) {
                Toast.makeText(this, R.string.set_wallpaper_error_toast,
                        Toast.LENGTH_SHORT).show();
                Log.e(ThemeManager.TAG, "Could not set wallpaper", e);
            } finally {
                if (in != null) {
                    IOUtilities.close(in);
                }
            }
            setResult(RESULT_OK);
        } else {
            String pkg = c.getString(c.getColumnIndexOrThrow(PackageResources.ImageColumns.PACKAGE));
            String assetPath = c.getString(c.getColumnIndexOrThrow(PackageResources.ImageColumns.ASSET_PATH));

            Intent result = new Intent();
            result.putExtra(Intent.EXTRA_WALLPAPER_IMAGE,
                    PackageResources.getImageUri(this, pkg, assetPath));
            result.putExtra(Intent.EXTRA_WALLPAPER_IMAGE_NAME,
                    FileUtilities.basename(assetPath));
            // A fix for THEMES-57
//            result.putExtra(Intent.EXTRA_WALLPAPER_THUMBNAIL,
//                    getCurrentThumbnailBitmap());

            setResult(RESULT_OK, result);
        }
        finish();
    }

    public void onClick(View v) {
        chooseCurrentWallpaper();
    }

    private class ThumbnailAdapter extends ResourceCursorAdapter {
        private final SparseArray<WallpaperThumbnailCache> mCaches =
            new SparseArray<WallpaperThumbnailCache>();

        ThumbnailAdapter(Context context, Cursor c) {
            super(context, R.layout.wallpaper_item, c);
        }
        
        public void clearCache() {
            mCaches.clear();
        }
        
        public BitmapStore getStore(Cursor c) {
            int pos = c.getPosition();
            WallpaperThumbnailCache cache = mCaches.get(pos);
            if (cache != null) {
                return cache;
            }

            String pkg = c.getString(c.getColumnIndexOrThrow(PackageResources.ImageColumns.PACKAGE));
            String assetPath = c.getString(c.getColumnIndexOrThrow(PackageResources.ImageColumns.ASSET_PATH));

            cache = new WallpaperThumbnailCache(mContext, pkg, assetPath);
            mCaches.put(pos, cache);

            return cache;
        }
        
        public InputStream getWallpaperStream(Cursor c) throws FileNotFoundException {
            String pkg = c.getString(c.getColumnIndexOrThrow(PackageResources.ImageColumns.PACKAGE));
            String assetPath = c.getString(c.getColumnIndexOrThrow(PackageResources.ImageColumns.ASSET_PATH));
            
            Uri uri = PackageResources.getImageUri(ThemeWallpaperChooser.this, pkg, assetPath);
            return getContentResolver().openInputStream(uri);
        }

        @Override
        public void bindView(View v, Context context, Cursor c) {
            ThumbnailedBitmapView vv = (ThumbnailedBitmapView)v.findViewById(R.id.thumbnail);
            
            String assetPath = c.getString(c.getColumnIndexOrThrow(PackageResources.ImageColumns.ASSET_PATH));
            vv.setImageStore(getStore(c), null, assetPath + "-small");
        }
    }

    public static Bitmap createBitmapThumbnail(Bitmap bitmap, int sIconWidth, int sIconHeight) {
        int width = sIconWidth;
        int height = sIconHeight;

        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();

        Paint sPaint = new Paint();
        Rect sBounds = new Rect();
        Rect sOldBounds = new Rect();
        Canvas sCanvas = new Canvas();
        
        sCanvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG,
                Paint.FILTER_BITMAP_FLAG));
   
        if (width > 0 && height > 0 && (width < bitmapWidth || height < bitmapHeight)) {
            final float ratio = (float) bitmapWidth / bitmapHeight;

            if (bitmapWidth > bitmapHeight) {
                height = (int) (width / ratio);
            } else if (bitmapHeight > bitmapWidth) {
                width = (int) (height * ratio);
            }

            final Bitmap.Config c = (width == sIconWidth && height == sIconHeight) ?
                    bitmap.getConfig() : Bitmap.Config.ARGB_8888;
            final Bitmap thumb = Bitmap.createBitmap(sIconWidth, sIconHeight, c);
            sCanvas.setBitmap(thumb);
            sPaint.setDither(false);
            sPaint.setFilterBitmap(true);
            sBounds.set((sIconWidth - width) / 2, (sIconHeight - height) / 2, width, height);
            sOldBounds.set(0, 0, bitmapWidth, bitmapHeight);
            sCanvas.drawBitmap(bitmap, sOldBounds, sBounds, sPaint);
            return thumb;
        }

        return bitmap;
    }
}
