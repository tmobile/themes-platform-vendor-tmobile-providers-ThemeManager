package com.tmobile.thememanager.widget;

import android.app.Activity;
import android.content.res.CustomTheme;
import android.view.LayoutInflater;
import android.widget.CursorAdapter;
import android.database.Cursor;

import com.tmobile.thememanager.provider.ThemeItem;
import com.tmobile.thememanager.provider.Themes;
import com.tmobile.thememanager.provider.Themes.ThemeColumns;

/**
 * Re-usable adapter which fills itself with all currently installed visual
 * themes. Includes a convenient inner-class which can represent all types of
 * visual themes with helpful accessors.
 */
public abstract class ThemeAdapter extends CursorAdapter {
    /*
     * This array holds cached ThemeItem objects to preserve the original
     * ThemeAdapter API, prior to using CursorAdapter. When the underlying
     * cursor changes, we recreate the array at the correct size which could be
     * a source of performance problems under certain conditions.
     */
    private ThemeItem[] mThemes;

    private final LayoutInflater mInflater;

    public ThemeAdapter(Activity context) {
        super(context, loadThemes(context), true);
        mInflater = LayoutInflater.from(context);
        allocInternal();
    }
    
    protected LayoutInflater getInflater() {
        return mInflater;
    }

    private static Cursor loadThemes(Activity context) {
        return context.managedQuery(ThemeColumns.CONTENT_PLURAL_URI,
                null, null, ThemeColumns.NAME);
    }

    private void allocInternal() {
        Cursor c = getCursor();
        if (c != null) {
            mThemes = new ThemeItem[getCursor().getCount()];
        } else {
            mThemes = null;
        }
    }

    @Override
    public void notifyDataSetChanged() {
        allocInternal();
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        mThemes = null;
        super.notifyDataSetInvalidated();
    }

    public int findItem(CustomTheme theme) {
        if (theme == null) return -1;
        int n = getCount();
        while (n-- > 0) {
            ThemeItem item = getTheme(n);
            if (item.equals(theme) == true) {
                return n;
            }
        }
        return -1;
    }

    public ThemeItem getTheme(int position) {
        if (position >= 0 && getCount() >= 0) {
            if (mThemes[position] == null) {
                mThemes[position] = new ThemeItem((Cursor)getItem(position));
            }
            return mThemes[position];
        }
        return null;
    }

    public int deleteThemeItem(int pos) {
        if (pos != -1) {
            ThemeItem item = getTheme(pos);
            Themes.deleteTheme(mContext, item.getPackageName(), item.getThemeId());
            Cursor c = Themes.listThemesByPackage(mContext, item.getPackageName());
            if (c != null) {
                int count;
                try {
                    count = c.getCount();
                } finally {
                    c.close();
                }
                if (count == 0) {
                    // un-install theme package
                    mContext.getPackageManager().deletePackage(item.getPackageName(), null, 0);
                }
            }

            mThemes[pos] = null;
            notifyDataSetChanged();
        }

        return findItem(CustomTheme.getDefault());
    }
}
