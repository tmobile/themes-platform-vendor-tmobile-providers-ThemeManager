package com.tmobile.thememanager.activity;

import com.tmobile.thememanager.R;
import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.activity.Customize.Customizations;
import com.tmobile.thememanager.utils.WallpaperThumbnailCache;
import com.tmobile.thememanager.widget.PreviewContentStub;
import com.tmobile.thememanager.widget.ThumbnailedBitmapView;
import com.tmobile.thememanager.widget.ThemeAdapter.ThemeItem;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

final public class CustomizeColor extends GenericCustomize {
    public static final String EXTRA_PALETTE_NAME = "color_palette_name";
    
    private ThumbnailedBitmapView mWallpaperPreview;
    private PreviewContentStub mPreviewContentStub;

    private static final int REST_COLOR = 0;
    private static final int REST_STROKE_COLOR = 1;
    private static final int PRIMARY_PRESSED_COLOR = 2;
    private static final int PRIMARY_FOCUS_COLOR = 3;
    private static final int SECONDARY_PRESSED_COLOR = 4;
    private static final int SECONDARY_FOCUS_COLOR = 5;
    private static final int DISABLED_COLOR = 6;
    private static final int TERTIARY_COLOR = 7;

    // The color ids must go in the same order as values in R.array.color_pack_names
    private static final int [] colorPaletteIds = {
        R.array.tiki_blossom_colors,
        R.array.sanrobot_colors,
        R.array.planets_colors,
        R.array.cherrybling_colors,
        R.array.sploosh_colors,
        R.array.pluto_colors,
        R.array.grenade_colors,
    };

    private static final int [] colorPaletteThumbnails = {
        R.drawable.tiki,
        R.drawable.sanrobot,
        R.drawable.planets,
        R.drawable.cherry,
        R.drawable.sploosh,
        R.drawable.pluto,
        R.drawable.grenade,
    };

    private static int [][] colorPalettes;

    private static String [] colorPaletteNames;

    public static void initColorPalettes(Context context) {
        if (colorPalettes == null) {
            colorPaletteNames = context.getResources().getStringArray(R.array.color_pack_names);
            colorPalettes = new int [colorPaletteIds.length][];
            for (int i = 0; i < colorPaletteIds.length; i++) {
                initColorPalette(context, i, colorPaletteIds[i]);
            }
        }
    }
    
    private static int getColorPaletteIndexByName(String colorPaletteName) {
        if (colorPaletteNames == null || colorPaletteName == null) {
            return -1;
        }
        for (int paletteIndex = 0; paletteIndex < colorPaletteNames.length; paletteIndex++) {
            if (colorPaletteNames[paletteIndex].equals(colorPaletteName)) {
                return paletteIndex;
            }
        }
        return -1;
    }

    private static int getColorFromPalette(int paletteIndex, int colorIndex) {
        int color = colorPalettes[paletteIndex][colorIndex];
        if ((color & 0xFF000000) == 0xFF000000) {
            color &= 0x00FFFFFF;
        }
        return color;
    }
    
    public static int getColorPaletteThumbnailResourceIdByName(Context context,
            String colorPaletteName) {
        initColorPalettes(context);
        return getColorPaletteThumbnailResourceIdByName(colorPaletteName);
    }

    public static int getColorPaletteThumbnailResourceIdByName(String colorPaletteName) {
        int index = getColorPaletteIndexByName(colorPaletteName);
        if (index < 0) {
            return -1;
        }
        return colorPaletteThumbnails[index];
    }
    
    public static void generateStyle(Context context, Writer out, String colorPaletteName) 
            throws IOException {
        initColorPalettes(context);
        generateStyle(out, colorPaletteName);
    }

    public static void generateStyle(Writer out, String colorPaletteName) throws IOException {
        int paletteIndex = getColorPaletteIndexByName(colorPaletteName);
        if (paletteIndex < 0) {
            throw new RuntimeException("Failed to find " + colorPaletteName);
        }

        out.write(String.format("    <item name=\"android:colorRest\">#%06x</item>\n",
                getColorFromPalette(paletteIndex, REST_COLOR)));
        out.write(String.format("    <item name=\"android:colorRestStroke\">#%06x</item>\n",
                getColorFromPalette(paletteIndex, REST_STROKE_COLOR)));
        out.write(String.format("    <item name=\"android:colorPrimaryPressed\">#%06x</item>\n",
                getColorFromPalette(paletteIndex, PRIMARY_PRESSED_COLOR)));
        out.write(String.format("    <item name=\"android:colorPrimaryFocus\">#%06x</item>\n",
                getColorFromPalette(paletteIndex, PRIMARY_FOCUS_COLOR)));
        out.write(String.format("    <item name=\"android:colorSecondaryPressed\">#%06x</item>\n",
                getColorFromPalette(paletteIndex, SECONDARY_PRESSED_COLOR)));
        out.write(String.format("    <item name=\"android:colorSecondaryFocus\">#%06x</item>\n",
                getColorFromPalette(paletteIndex, SECONDARY_FOCUS_COLOR)));
        out.write(String.format("    <item name=\"android:colorDisabled\">#%06x</item>\n",
                getColorFromPalette(paletteIndex, DISABLED_COLOR)));
        out.write(String.format("    <item name=\"android:colorTertiary\">#%06x</item>\n",
                getColorFromPalette(paletteIndex, TERTIARY_COLOR)));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        setDirty(true);

        super.onItemClick(parent, v, position, id);

        /* Inflate the preview widget and attach; this is deferred until the next layout pass. */
        mPreviewContentStub.postInflate();
    }

    protected BaseAdapter initCustomAdapter() {
        mWallpaperPreview = (ThumbnailedBitmapView)findViewById(R.id.preview_wallpaper);
        
        mPreviewContentStub = (PreviewContentStub)findViewById(R.id.preview_content_stub);
        mPreviewContentStub.setOnInflateListener(mInflateListener);

        selectedPos = getColorPaletteIndexByName(getCustomizations().colorPaletteName);
        if (colorPalettes == null) {
            initColorPalettes(this);
        }
        return new ColorAdapter();
    }
    
    public static void show(Activity context, Customizations customizations,
            ThemeItem baseTheme, int requestCode) {
        context.startActivityForResult(getShowIntent(context, customizations, baseTheme)
                .setClass(context, CustomizeColor.class), requestCode);
    }
    
    @Override
    protected void setResult(boolean ok) {
        Intent data = new Intent();
        if (ok) {
            CustomizableItem current = (CustomizableItem)mAdapter.getItem(selectedPos);
            data.putExtra(EXTRA_PALETTE_NAME, current.name);
        }
        setResult(ok ? RESULT_OK : RESULT_CANCELED, data);
    }

    @Override
    protected void persistChanges() {
    }

    @Override
    protected int getViewLayout() {
        return R.layout.customizecolor;
    }

    private static void initColorPalette(Context context, int index, int colorPackId) {
        String [] colorValues = context.getResources().getStringArray(colorPackId);
        colorPalettes[index] = new int[colorValues.length];
        for (int i = 0; i < colorValues.length; i++) {
            int colorValue = Integer.decode(colorValues[i]);
            if ((colorValue & 0xFF000000) == 0) {
                colorValue |= 0xFF000000;
            }
            colorPalettes[index][i] = colorValue;
        }
    }

    private final PreviewContentStub.OnInflateListener mInflateListener = new PreviewContentStub.OnInflateListener() {
        public void onPostInflate(PreviewContentStub stub) {
            WallpaperThumbnailCache cache = 
                new WallpaperThumbnailCache(CustomizeColor.this, getBaseTheme().getPackageName(),
                    getCustomizations().wallpaperUri);
            
            mWallpaperPreview.setImageStore(cache, null, 
                    getCustomizations().wallpaperName + "-medium_cropped");

            ThemeChooser.revertTemporaryTheming();
        }

        public void onPreInflate(PreviewContentStub stub) {
            ThemeChooser.applyTemporaryTheming(CustomizeColor.this, getBaseTheme());
            
            if (selectedPos != -1) {
                applyColorPack((CustomizableItem)mAdapter.getItem(selectedPos));
            }
        }
    };

    private void applyColorPack(CustomizableItem item) {
        try {
            int primaryColor = colorPalettes[item.index][PRIMARY_FOCUS_COLOR];
            int secondaryColor = colorPalettes[item.index][SECONDARY_FOCUS_COLOR];
            int tertiaryColor = colorPalettes[item.index][TERTIARY_COLOR];
            int primaryPressed = colorPalettes[item.index][PRIMARY_PRESSED_COLOR];
            int secondaryPressed = colorPalettes[item.index][SECONDARY_PRESSED_COLOR];
            int colorDisabled = colorPalettes[item.index][DISABLED_COLOR];
            int colorRest = colorPalettes[item.index][REST_COLOR];
            int colorRsetStroke = colorPalettes[item.index][REST_STROKE_COLOR];

            getTheme().setAttributeValue(android.R.attr.colorPrimaryFocus,  primaryColor);

            getTheme().setAttributeValue(android.R.attr.colorSecondaryFocus,  secondaryColor);

            getTheme().setAttributeValue(android.R.attr.colorTertiary,  tertiaryColor);

            getTheme().setAttributeValue(android.R.attr.colorPrimaryPressed,  primaryPressed);

            getTheme().setAttributeValue(android.R.attr.colorSecondaryPressed,  secondaryPressed);

            getTheme().setAttributeValue(android.R.attr.colorDisabled,  colorDisabled);

            getTheme().setAttributeValue(android.R.attr.colorRest,  colorRest);

            getTheme().setAttributeValue(android.R.attr.colorRestStroke,  colorRsetStroke);
        } catch (Exception e) {
            Log.e(ThemeManager.TAG, "Exception in color palette ", e);
        }
    }

    private class ColorAdapter extends BaseAdapter {
        private List<CustomizableItem> mItems;
        private final LayoutInflater mInflater;

        public ColorAdapter() {
            mInflater = LayoutInflater.from(CustomizeColor.this);
            mItems = new ArrayList<CustomizableItem>();
            for (int i = 0; i < colorPaletteIds.length; i++) {
                CustomizableItem item = new CustomizableItem();
                item.name = colorPaletteNames[i];
                item.index = i;
                item.thumbnail = getResources().getDrawable(colorPaletteThumbnails[i]);
                mItems.add(item);
            }

            /* Inflate the preview widget and attach; this is deferred until the next layout pass. */
            mPreviewContentStub.postInflate();
        }

        public int getCount() {
            return mItems.size();
        }

        public Object getItem(int position) {
            return mItems.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.customizecolor_row,
                        parent, false);
                holder = new ViewHolder(convertView.findViewById(R.id.p_row));
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }
            CheckedTextView view = holder.getCheck();
            view.setText(mItems.get(position).name);
            view.setChecked(position == selectedPos);
            holder.getThumbnail().setImageDrawable(mItems.get(position).thumbnail);
            return convertView;
        }

    }

    private class ViewHolder {
        private View view;
        private CheckedTextView check;
        private ImageView thumbnail;

        public ViewHolder(View view) {
            this.view = view;
        }

        public CheckedTextView getCheck() {
            if (check == null) {
                check = (CheckedTextView)view.findViewById(R.id.checkedTextView);
            }
            return check;
        }

        public ImageView getThumbnail() {
            if (thumbnail == null) {
                thumbnail = (ImageView)view.findViewById(R.id.icon1);
            }
            return thumbnail;
        }
    }

    private class CustomizableItem {
        public String name;
        public Drawable thumbnail;
        public int index;
    }

}
