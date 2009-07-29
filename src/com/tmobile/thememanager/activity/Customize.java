package com.tmobile.thememanager.activity;

import com.tmobile.thememanager.R;
import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.utils.BitmapStore;
import com.tmobile.thememanager.utils.FileUtilities;
import com.tmobile.thememanager.utils.IOUtilities;
import com.tmobile.thememanager.utils.WallpaperThumbnailCache;
import com.tmobile.thememanager.widget.PersonalizePreference;
import com.tmobile.thememanager.widget.ThemeAdapter.ThemeItem;
import com.tmobile.widget.HeaderTwinButton;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Customize extends Activity {
    private static final int CUSTOMIZE_WALLPAPER_MSG = 1;
    private static final int CUSTOMIZE_WALLPAPER_EFFECTS_MSG = 2;
    private static final int CUSTOMIZE_SOUND_RINGTONE_MSG = 3;
    private static final int CUSTOMIZE_SOUND_NOTIFICATION_MSG = 4;
    private static final int CUSTOMIZE_SOUNDPACK_MSG = 5;
    private static final int CUSTOMIZE_COLOR_MSG = 6;
    private static final int CUSTOMIZE_FONT_MSG = 7;
    private static final int CUSTOMIZE_MOTION_MSG = 8;

    private static final int SOUND_TYPE_PICKER_DIALOG = 1;
    private static final int SAVE_DIALOG_MESSAGE = 2;

    private static final int RINGTONE_TYPE = 0;
    private static final int NOTIFICATION_RINGTONE_TYPE = 1;
    private static final int SOUNDPACK_TYPE = 2;
    
    public static final String EXTRA_CUSTOMIZATIONS = "customizations";
    
    private HeaderTwinButton mHeaderButtons;
    private CustomizeAdapter mAdapter;

    /* Reference to the original, uncustomized theme to which this new theme
     * will be based. */
    ThemeItem mBaseTheme;
    
    /* Holds the values customized on top of the base theme. */
    Customizations mCustomizations;
    
    WallpaperPreference mWallpaperPref;
    WallpaperEffectsPreference mWallpaperEffectsPref;
    SoundPreference mSoundPref;
    ColorPreference mColorPref;
    FontPreference mFontPref;
    MotionPreference mMotionPref;
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        mBaseTheme = getIntent().getExtras().getParcelable(ThemeManager.EXTRA_THEME_ITEM);

        setContentView(R.layout.customize);
        
        mCustomizations = new Customizations();
        
        mHeaderButtons = (HeaderTwinButton)findViewById(R.id.customize_header);
        mHeaderButtons.getButton().setOnClickListener(mSaveClicked);
        mHeaderButtons.getButton2().setOnClickListener(mCancelClicked);
        
        ListView list = (ListView)findViewById(android.R.id.list);
        mAdapter = new CustomizeAdapter(this);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(mItemClick);

        setEnabledSaveCancelButtons(false);
    }
    
    private static Intent getShowIntent(Context context, ThemeItem theme) {
        Intent i = new Intent(context, Customize.class);
        i.putExtra(ThemeManager.EXTRA_THEME_ITEM, theme);
        return i;
    }
    
    public static void show(Activity context, ThemeItem theme, int requestCode) {
        context.startActivityForResult(getShowIntent(context, theme), requestCode);
    }
    
    public static void show(Context context, ThemeItem theme) {
        context.startActivity(getShowIntent(context, theme));
    }

    private void setEnabledSaveCancelButtons(boolean enabled) {
        mHeaderButtons.getButton().setEnabled(enabled);
        mHeaderButtons.getButton2().setEnabled(enabled);
    }

    private final OnClickListener mSaveClicked = new OnClickListener() {
        public void onClick(View v) {            
            /* Pass back to the ThemeChooser to actually make the diff theme. */
            Intent data = new Intent();
            data.putExtra(ThemeManager.EXTRA_THEME_ITEM, mBaseTheme);
            data.putExtra(Customize.EXTRA_CUSTOMIZATIONS, mCustomizations);
            setResult(RESULT_OK, data);
            finish();
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mHeaderButtons.getButton().isEnabled()) {
            showDialog(SAVE_DIALOG_MESSAGE);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private final OnClickListener mCancelClicked = new OnClickListener() {
        public void onClick(View v) {
            setResult(RESULT_CANCELED);
            finish();
        }
    };
    
    private final OnItemClickListener mItemClick = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Preference pref = (Preference)parent.getItemAtPosition(position);
            
            /* performClick() is protected? */
            pref.getOnPreferenceClickListener().onPreferenceClick(pref);
        }
    };
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case SOUND_TYPE_PICKER_DIALOG:
                return new AlertDialog.Builder(this)
                    .setItems(R.array.sound_types, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == RINGTONE_TYPE || which == NOTIFICATION_RINGTONE_TYPE) {
                                int type = (which == RINGTONE_TYPE) ?
                                        RingtoneManager.TYPE_RINGTONE :
                                        RingtoneManager.TYPE_NOTIFICATION;
                                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                                
                                Uri existingUri = mSoundPref.getValueForType(which);
                                if (existingUri != null) {
                                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, 
                                            existingUri);
                                }
                                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_INCLUDE_DRM, false);
                                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
                                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, type);
                                startActivityForResult(intent, (which == RINGTONE_TYPE) ?
                                        CUSTOMIZE_SOUND_RINGTONE_MSG :
                                        CUSTOMIZE_SOUND_NOTIFICATION_MSG);
                            } else {
                                CustomizeSound.show(Customize.this, mCustomizations.merge(Customize.this, mBaseTheme),
                                        mBaseTheme, CUSTOMIZE_SOUNDPACK_MSG);
                            }
                        }
                    })
                    .create();

            case SAVE_DIALOG_MESSAGE:
                return new AlertDialog.Builder(this)
//                  .setIcon(R.drawable.save_dialog_icon) // TODO: Need the dialog icon
                    .setTitle(R.string.save_dialog_title)
                    .setMessage(R.string.save_dialog_message)
                    .setPositiveButton(R.string.btn_save, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mSaveClicked.onClick(null);
                        }
                    })
                    .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mCancelClicked.onClick(null);
                        }
                    })
                    .create();

            default:
                return super.onCreateDialog(id);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CUSTOMIZE_WALLPAPER_MSG:
                if (resultCode == RESULT_OK) {
                    Bundle extras = data.getExtras();
                    Uri uri = extras.getParcelable(Intent.EXTRA_WALLPAPER_IMAGE);
                    String name = extras.getString(Intent.EXTRA_WALLPAPER_IMAGE_NAME);
                    
                    mCustomizations.setWallpaper(uri, name);                    
                    mWallpaperPref.refreshFromCustomizations();
                }
                break;
                
            case CUSTOMIZE_SOUND_RINGTONE_MSG:
            case CUSTOMIZE_SOUND_NOTIFICATION_MSG:
                if (resultCode == RESULT_OK) {
                    Bundle extras = data.getExtras();
                    Uri uri = extras.getParcelable(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    String name = extras.getString(RingtoneManager.EXTRA_RINGTONE_TITLE);
                    
                    if (requestCode == CUSTOMIZE_SOUND_RINGTONE_MSG) {
                        mCustomizations.setRingtone(uri, name);
                    } else {
                        mCustomizations.setNotificationRingtone(uri, name);
                    }
                    mSoundPref.refreshFromCustomizations();
                }
                break;
                
            case CUSTOMIZE_SOUNDPACK_MSG:
                if (resultCode == RESULT_OK) {
                    Bundle extras = data.getExtras();
                    String name = extras.getString(CustomizeSound.EXTRA_PACK_NAME);
                    
                    mCustomizations.setSoundPack(name);
                    mSoundPref.refreshFromCustomizations();
                }
                break;
                
            case CUSTOMIZE_COLOR_MSG:
                if (resultCode == RESULT_OK) {
                    Bundle extras = data.getExtras();
                    String name = extras.getString(CustomizeColor.EXTRA_PALETTE_NAME);

                    mCustomizations.setColorPalette(name);
                    mColorPref.refreshFromCustomizations();
                }
                break;
        }
    }

    private class CustomizeAdapter extends BaseAdapter {
        private final List<Preference> mPreferences = new ArrayList<Preference>();
        
        public CustomizeAdapter(Context context) {
            mWallpaperPref = new WallpaperPreference(context, this);
            mPreferences.add(mWallpaperPref);
            
            mWallpaperEffectsPref = new WallpaperEffectsPreference(context, this);
            mPreferences.add(mWallpaperEffectsPref);
            
            mSoundPref = new SoundPreference(context, this);
            mPreferences.add(mSoundPref);
            
            mColorPref = new ColorPreference(context, this);
            mPreferences.add(mColorPref);
            
            mFontPref = new FontPreference(context, this);
            mPreferences.add(mFontPref);
            
            mMotionPref = new MotionPreference(context, this);
            mPreferences.add(mMotionPref);
        }

        public int getCount() {
            return mPreferences.size();
        }

        public Object getItem(int position) {
            return mPreferences.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            Preference pref = mPreferences.get(position);
            return pref.getView(convertView, parent);
        }        
    }
    
    private abstract class Customizable extends PersonalizePreference
            implements OnPreferenceClickListener {
        private CustomizeAdapter mAdapter;
        
        public Customizable(Context context, CustomizeAdapter adapter) {
            super(context);
            this.mAdapter = adapter;
            setOnPreferenceClickListener(this);
        }
        
        @Override
        protected void notifyChanged() {
            super.notifyChanged();
            this.mAdapter.notifyDataSetChanged();
            setEnabledSaveCancelButtons(true);
        }

        /**
         * Refresh from the Customizations object.
         */
        public abstract void refreshFromCustomizations();
        
        /** @deprecated */
        public void unimplemented() {
            Toast.makeText(getContext(), "TODO", Toast.LENGTH_SHORT).show();
        }
    }

    private class WallpaperPreference extends Customizable {
        public WallpaperPreference(Context context, CustomizeAdapter adapter) {
            super(context, adapter);
            
            setTitle(R.string.title_wallpaper);
                        
            if (mCustomizations.wallpaperUri == null) {
                setSummary(FileUtilities.basename(mBaseTheme.getWallpaperIdentifier()));
                BitmapStore store = new WallpaperThumbnailCache(context, mBaseTheme.getPackageName(),
                        mBaseTheme.getWallpaperUri(context));
                setImageStore(store, null, mBaseTheme.getWallpaperIdentifier() + "-small");
            } else {
                refreshFromCustomizations();
            }
        }

        public boolean onPreferenceClick(Preference preference) {
            Intent pickWallpaper = new Intent(Intent.ACTION_PICK_WALLPAPER);
            startActivityForResult(Intent.createChooser(pickWallpaper,
                    getString(R.string.pick_wallpaper)), CUSTOMIZE_WALLPAPER_MSG);
            return true;
        }
        
        private Bitmap getThumbnail(Uri uri) throws IOException {
            InputStream in = null;
            try {
                in = getContext().getContentResolver().openInputStream(uri);
                Bitmap thumb = BitmapFactory.decodeStream(in);
                if (thumb == null) {
                    return null;
                }
                int dimension = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50,
                        getContext().getResources().getDisplayMetrics());
                return ThemeWallpaperChooser.createBitmapThumbnail(thumb,
                        dimension, dimension);
            } finally {
                if (in != null) {
                    IOUtilities.close(in);
                }
            }
        }
        
        public void refreshFromCustomizations() {
            setSummary(mCustomizations.wallpaperName);

            Bitmap thumb = null;
            Uri uri = mCustomizations.wallpaperUri;
            try {
                thumb = getThumbnail(uri);
                if (thumb != null) {
                    setImage(new BitmapDrawable(thumb));
                }
            } catch (IOException e) {
                if (ThemeManager.DEBUG) {
                    Log.e(ThemeManager.TAG, "Unable to read wallpaper URI " + uri);
                }
            }
        }
    }
    
    private class WallpaperEffectsPreference extends Customizable {
        public WallpaperEffectsPreference(Context context, CustomizeAdapter adapter) {
            super(context, adapter);
            
            setTitle(R.string.title_wallpaper_effects);
            
            setSummary(R.string.summary_no_customization);
        }

        @Override
        public void refreshFromCustomizations() {
            throw new RuntimeException("TODO");
        }

        public boolean onPreferenceClick(Preference preference) {
            unimplemented();
            return false;
        }
    }
    
    private class SoundPreference extends Customizable {
        public SoundPreference(Context context, CustomizeAdapter adapter) {
            super(context, adapter);
            
            setTitle(R.string.title_sound);
            
            setSummary(R.string.summary_default_customization);
        }

        @Override
        public void refreshFromCustomizations() {
            /* Nothing to show; ambiguous UI. */
            notifyChanged();
        }
        
        public Uri getValueForType(int type) {
            switch (type) {
                case RINGTONE_TYPE:
                    if (mCustomizations.ringtoneUri != null) {
                        return mCustomizations.ringtoneUri;
                    } else {
                        return mBaseTheme.getRingtoneUri(getContext());
                    }
                    
                case NOTIFICATION_RINGTONE_TYPE:
                    if (mCustomizations.notificationRingtoneUri != null) {
                        return mCustomizations.notificationRingtoneUri;
                    } else {
                        return mBaseTheme.getNotificationRingtoneUri(getContext());
                    }
                    
                default:
                    throw new IllegalArgumentException("Unknown type " + type);
            }
        }

        public boolean onPreferenceClick(Preference preference) {
            showDialog(SOUND_TYPE_PICKER_DIALOG);
            return true;
        }
    }
    
    private class ColorPreference extends Customizable {
        public ColorPreference(Context context, CustomizeAdapter adapter) {
            super(context, adapter);
            
            setTitle(R.string.title_color);
            
            if (mCustomizations.colorPaletteName == null) {
                setSummary(R.string.summary_default_customization);
                setImageFromPaletteName(mBaseTheme.getColorPaletteName());
            } else {
                refreshFromCustomizations();
            }
        }
        
        private void setImageFromPaletteName(String name) {
            int resId = CustomizeColor.getColorPaletteThumbnailResourceIdByName(getContext(), name);
            if (resId > 0) {
                setImage(resId);
            }
        }
        
        @Override
        public void refreshFromCustomizations() {
            if (mBaseTheme.getName().equals(mCustomizations.colorPaletteName) == false) {
                setSummary(mCustomizations.colorPaletteName);
            } else {
                setSummary(R.string.summary_default_customization);
            }
            setImageFromPaletteName(mCustomizations.colorPaletteName);
        }

        public boolean onPreferenceClick(Preference preference) {
            CustomizeColor.show(Customize.this, mCustomizations.merge(getContext(), mBaseTheme),
                    mBaseTheme, CUSTOMIZE_COLOR_MSG);
            return true;
        }
    }
    
    private class FontPreference extends Customizable {
        public FontPreference(Context context, CustomizeAdapter adapter) {
            super(context, adapter);
            
            setTitle(R.string.title_font);
            
            setSummary(R.string.summary_default_customization);
        }

        @Override
        public void refreshFromCustomizations() {
            throw new RuntimeException("TODO");
        }

        public boolean onPreferenceClick(Preference preference) {
            unimplemented();
            return false;
        }
    }
    
    private class MotionPreference extends Customizable {
        public MotionPreference(Context context, CustomizeAdapter adapter) {
            super(context, adapter);
            
            setTitle(R.string.title_motion);
            
            setSummary(R.string.summary_default_customization);
        }

        @Override
        public void refreshFromCustomizations() {
            throw new RuntimeException("TODO");
        }

        public boolean onPreferenceClick(Preference preference) {
            unimplemented();
            return false;
        }
    }

    /**
     * Holder class which represents the changes atop the base theme. This class
     * can serialized in two ways: as a Parcelable and into SharedPreferences.
     * The former is to pass to subactivities, the latter is to store
     * persistently to disk so that the next time the user returns we have
     * preserved their uncommitted customizations.
     */
    public static class Customizations implements Parcelable {
        public Uri wallpaperUri;
        public String wallpaperName;
        
        public Uri ringtoneUri;
        public String ringtoneName;
        
        public Uri notificationRingtoneUri;
        public String notificationRingtoneName;
        
        public String soundPackName;
        
        public String colorPaletteName;
        
        private Customizations() {}
        
        private Customizations(Context context, Customizations orig, ThemeItem baseTheme) {
            if (orig.wallpaperUri != null) {
                wallpaperUri = orig.wallpaperUri;
                wallpaperName = orig.wallpaperName;
            } else {
                wallpaperUri = baseTheme.getWallpaperUri(context);
                wallpaperName = FileUtilities.basename(baseTheme.getWallpaperIdentifier());
            }
            
            if (orig.ringtoneUri != null) {
                ringtoneUri = orig.ringtoneUri;
                ringtoneName = orig.ringtoneName;
            } else {
                ringtoneUri = baseTheme.getRingtoneUri(context);
                ringtoneName = baseTheme.getRingtoneName();
            }
            
            if (orig.notificationRingtoneUri != null) {
                notificationRingtoneUri = orig.notificationRingtoneUri;
                notificationRingtoneName = orig.notificationRingtoneName;
            } else {
                notificationRingtoneUri = baseTheme.getNotificationRingtoneUri(context);
                notificationRingtoneName = baseTheme.getNotificationRingtoneName();
            }
            
            if (orig.soundPackName != null) {
                soundPackName = orig.soundPackName;
            } else {
                /* TODO: Get this from the base theme!? */
                soundPackName = null;
            }
            
            if (orig.colorPaletteName != null) {
                colorPaletteName = orig.colorPaletteName;
            } else {
                colorPaletteName = baseTheme.getColorPaletteName();
            }
        }
        
        public void writeToParcel(Parcel dest, int flags) {
            IOUtilities.writeParcelableToParcel(dest, wallpaperUri, 0);
            dest.writeString(wallpaperName);
            IOUtilities.writeParcelableToParcel(dest, ringtoneUri, 0);
            dest.writeString(ringtoneName);
            IOUtilities.writeParcelableToParcel(dest, notificationRingtoneUri, 0);
            dest.writeString(notificationRingtoneName);
            dest.writeString(soundPackName);
            dest.writeString(colorPaletteName);
        }

        public static final Parcelable.Creator<Customizations> CREATOR =
            new Parcelable.Creator<Customizations>() {
            public Customizations createFromParcel(Parcel source) {
                Customizations instance = new Customizations();
                instance.wallpaperUri = IOUtilities.readParcelableFromParcel(source,
                        Uri.CREATOR);
                instance.wallpaperName = source.readString();
                instance.ringtoneUri = IOUtilities.readParcelableFromParcel(source,
                        Uri.CREATOR);
                instance.ringtoneName = source.readString();
                instance.notificationRingtoneUri = IOUtilities.readParcelableFromParcel(source,
                        Uri.CREATOR);
                instance.notificationRingtoneName = source.readString();
                instance.soundPackName = source.readString();
                instance.colorPaletteName = source.readString();
                return instance;
            }

            public Customizations[] newArray(int size) {
                return new Customizations[size];
            }
        };
        
        public int describeContents() {
            return 0;
        }
        
        public void setWallpaper(Uri uri, String name) {
            this.wallpaperUri = uri;
            this.wallpaperName = name;
        }
        
        public void setRingtone(Uri uri, String name) {
            this.ringtoneUri = uri;
            this.ringtoneName = name;
        }
        
        public void setNotificationRingtone(Uri uri, String name) {
            this.notificationRingtoneUri = uri;
            this.notificationRingtoneName = name;
        }
        
        public void setSoundPack(String name) {
            this.soundPackName = name;
        }
        
        public void setColorPalette(String name) {
            this.colorPaletteName = name;
        }

        /**
         * @return 
         *   True if this object contains any differences from the base
         *   theme; false otherwise.
         */
        public boolean hasChanges() {
            if (wallpaperUri != null) {
                return true;
            }
            if (ringtoneUri != null) {
                return true;
            }
            if (notificationRingtoneUri != null) {
                return true;
            }
            if (soundPackName != null) {
                return true;
            }
            if (colorPaletteName != null) {
                return true;
            }
            return false;
        }

        /**
         * Utility method which merges in ThemeItem values and returns a new
         * Customization object. This is used to pass a single Customizations
         * object to subactivities which contains base theme data to fill in
         * uncustomized default members.
         */
        public Customizations merge(Context context, ThemeItem theme) {
            return new Customizations(context, this, theme);
        }
    }
}
