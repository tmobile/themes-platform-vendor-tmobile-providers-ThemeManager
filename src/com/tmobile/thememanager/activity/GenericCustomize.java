package com.tmobile.thememanager.activity;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.BaseThemeInfo;
import android.os.Bundle;
import android.view.Window;
import android.view.View;
import android.view.KeyEvent;
import android.widget.ListView;
import android.widget.BaseAdapter;
import android.widget.AdapterView;
import com.tmobile.thememanager.R;
import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.activity.Customize.Customizations;
import com.tmobile.thememanager.delta_themes.DeltaThemeInfo;
import com.tmobile.thememanager.widget.ThemeAdapter.ThemeItem;
import com.tmobile.widget.HeaderTwinButton;

public abstract class GenericCustomize extends Activity
        implements AdapterView.OnItemClickListener {
    /** @deprecated */
    public static final String CUSTOMIZE_THEME_MAPPING_BUNDLE = "customize_theme_mapping_bundle";
    
    /** @deprecated */
    public static final String PACKAGE_INFO = "package_info";
    
    /** @deprecated */
    public static final String THEME_INFO = "theme_info";
    
    /** @deprecated */
    public static final String DELTA_THEME_INFO = "delta_theme_info";
    
    /** @deprecated */
    protected Bundle bundle;
    
    protected BaseAdapter mAdapter;
    protected ListView list;
    protected int selectedPos = -1;
    
    private ThemeItem mBaseTheme;
    private Customizations mCustomizations;

    // <pi, bi> and delta are mutually exclusive: all 3 of them can't exist at the same time!
    /** @deprecated */
    private PackageInfo pi;
    
    /** @deprecated */
    private BaseThemeInfo bi;
    
    /** @deprecated */
    private DeltaThemeInfo delta;

    private boolean isDirty;

    private static final int SAVE_DIALOG_MESSAGE = 1;

    public void setDirty(boolean dirty) {
        isDirty = dirty;
        HeaderTwinButton twinButton = (HeaderTwinButton)findViewById(R.id.generic_customize_header);
        twinButton.getButton().setEnabled(dirty);
        twinButton.getButton2().setEnabled(dirty);
    }

    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        selectedPos = position;
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int myLayout = 0;
        
        Bundle extras = getIntent().getExtras();
        mBaseTheme = extras.getParcelable(ThemeManager.EXTRA_THEME_ITEM);
        mCustomizations = extras.getParcelable(Customize.EXTRA_CUSTOMIZATIONS);

        bundle = extras.getBundle(CUSTOMIZE_THEME_MAPPING_BUNDLE);
        if (bundle != null) {
            pi = bundle.getParcelable(PACKAGE_INFO);
            bi = bundle.getParcelable(THEME_INFO);
            delta = bundle.getParcelable(DELTA_THEME_INFO);
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        myLayout = getViewLayout();
        setContentView(myLayout);

        mAdapter = initCustomAdapter();
        list = (ListView)findViewById(android.R.id.list);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(this); 

        HeaderTwinButton twinButton = (HeaderTwinButton) findViewById(R.id.generic_customize_header);
        twinButton.getButton().setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                setResult(true);
                persistChanges();
                finish();
            }
        });
        twinButton.getButton2().setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                setResult(false);
                finish();
            }
        });
        twinButton.setHeaderText(getTitle());

        setDirty(false);
    }
    
    protected static Intent getShowIntent(Context context, Customizations customizations,
            ThemeItem baseTheme) {
        Intent i = new Intent();
        i.putExtra(Customize.EXTRA_CUSTOMIZATIONS, customizations);
        i.putExtra(ThemeManager.EXTRA_THEME_ITEM, baseTheme);
        return i;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case SAVE_DIALOG_MESSAGE:
                return new AlertDialog.Builder(GenericCustomize.this)
//                  .setIcon(R.drawable.save_dialog_icon) // TODO: Need the dialog icon
                    .setTitle(R.string.save_dialog_title)
                    .setPositiveButton(R.string.btn_save, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            GenericCustomize.this.setResult(true);
                            persistChanges();
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            GenericCustomize.this.setResult(false);
                            finish();
                        }
                    })
                    .create();
        }
        return null;
    }
    
    protected abstract BaseAdapter initCustomAdapter();
    
    /** @deprecated */
    protected abstract void persistChanges();

    /** 
     * Override this!  The default implementation is deprecated!
     */
    protected void setResult(boolean ok) {
        Intent intent = new Intent();
        intent.putExtra(CUSTOMIZE_THEME_MAPPING_BUNDLE, bundle);
        setResult(ok? RESULT_OK : RESULT_CANCELED, intent);
    }

    /** @deprecated */
    protected int getResourceId() {
        if (delta != null) {
            return delta.getStyleId();
        }
        assert (bi != null);
        return bi.styleResourceId;
    }

    /** @deprecated */
    protected String getThemeId() {
        if (delta != null) {
            return delta.getThemeId();
        }
        assert (bi != null);
        return bi.themeId;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && isDirty) {
            showDialog(SAVE_DIALOG_MESSAGE);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    protected int getViewLayout() {
        return R.layout.generic_customize;
    }

    /** @deprecated */
    protected PackageInfo getPackageInfo() {
        return pi;
    }

    /** @deprecated */
    protected BaseThemeInfo getBaseThemeInfo() {
        return bi;
    }

    /** @deprecated */
    protected DeltaThemeInfo getDeltaThemeInfo() {
        return delta;
    }

    protected ThemeItem getBaseTheme() {
        return mBaseTheme;
    }
    
    protected Customizations getCustomizations() {
        return mCustomizations;
    }
}
