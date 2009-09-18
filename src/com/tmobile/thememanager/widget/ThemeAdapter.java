package com.tmobile.thememanager.widget;

import com.tmobile.thememanager.provider.ThemeItem;
import com.tmobile.thememanager.provider.Themes;
import com.tmobile.thememanager.provider.Themes.ThemeColumns;

import android.app.Activity;
import android.content.Context;
import android.content.res.CustomTheme;
import android.database.Cursor;
import android.net.Uri;

public abstract class ThemeAdapter extends AbstractDAOItemAdapter<ThemeItem> {
    private int mMarkedPosition;

    public ThemeAdapter(Activity context) {
        super(context, loadThemes(context), true);
    }

    private static Cursor loadThemes(Activity context) {
        return context.managedQuery(ThemeColumns.CONTENT_PLURAL_URI,
                null, null, ThemeColumns.NAME);
    }

    /**
     * Mark the applied theme's position.
     *
     * @param existingUri uri to select, or null to use the currently applied
     *   item.
     *
     * @see {@link #getMarkedPosition}
     */
    public int markCurrentOrExistingTheme(Uri existingUri) {
        int position = findExistingOrCurrentItem(getContext(), existingUri);
        if (mMarkedPosition != position) {
            mMarkedPosition = position;
            notifyDataSetChanged();
        }
        return position;
    }

    /**
     * @return the previously marked position.
     */
    public int getMarkedPosition() {
        return mMarkedPosition;
    }

    @Override
    protected ThemeItem getCurrentlyAppliedItem(Context context) {
        return ThemeItem.getInstance(Themes.getAppliedTheme(context));
    }

    @Override
    protected void onAllocInternal(Cursor c) {
        mDAOItem = new ThemeItem(c);
    }

    public ThemeItem getTheme(int position) {
        return getDAOItem(position);
    }

    public int findItem(CustomTheme theme) {
        if (theme == null) return -1;
        int n = getCount();
        while (n-- > 0) {
            ThemeItem item = getDAOItem(n);
            if (item.equals(theme) == true) {
                return n;
            }
        }
        return -1;
    }

    public int deleteItem(int pos) {
        if (pos != -1) {
            ThemeItem item = getDAOItem(pos);
            Themes.deleteTheme(getContext(), item.getPackageName(), item.getThemeId());
            Cursor c = Themes.listThemesByPackage(getContext(), item.getPackageName());
            if (c != null) {
                int count;
                try {
                    count = c.getCount();
                } finally {
                    c.close();
                }
                if (count == 0) {
                    // un-install theme package
                    getContext().getPackageManager().deletePackage(item.getPackageName(), null, 0);
                }
            }

            notifyDataSetChanged();
        }

        return findItem(CustomTheme.getDefault());
    }
}
