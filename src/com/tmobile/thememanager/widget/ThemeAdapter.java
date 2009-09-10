package com.tmobile.thememanager.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.CustomTheme;
import android.net.Uri;
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
    private ThemeItem mThemeDAO;
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
            mThemeDAO = new ThemeItem(c);
        } else {
            mThemeDAO = null;
        }
    }

    @Override
    public void notifyDataSetChanged() {
        allocInternal();
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        mThemeDAO = null;
        super.notifyDataSetInvalidated();
    }

    /**
     * Utility function to work out which theme item should be shown as checked.
     * 
     * <p>This method is implemented with way too much allocation.</p>
     * 
     * @param existingUri Requested existing URI if provided via Intent extras.
     */
    public int findExistingOrCurrentItem(Context context, Uri existingUri) {
        if (existingUri != null) {
            return findItem(context, existingUri);
        } else {
            ThemeItem current = ThemeItem.getInstance(Themes.getAppliedTheme(context));
            if (current != null) {
                try {
                    return findItem(context, current.getUri(context));
                } finally {
                    current.close();
                }
            } else {
                return -1;
            }
        }
    }

    public int findItem(Context context, Uri uri) {
        if (uri == null) return -1;
        int n = getCount();
        while (n-- > 0) {
            ThemeItem item = getTheme(n);
            if (uri.equals(item.getUri(context)) == true) {
                return n;
            }
        }
        return -1;
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
            mThemeDAO.setPosition(position);
            return mThemeDAO;
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

            notifyDataSetChanged();
        }

        return findItem(CustomTheme.getDefault());
    }
}
