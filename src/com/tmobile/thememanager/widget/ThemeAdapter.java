package com.tmobile.thememanager.widget;

import com.tmobile.thememanager.provider.ThemeItem;
import com.tmobile.thememanager.provider.Themes;
import com.tmobile.thememanager.provider.Themes.ThemeColumns;

import android.app.Activity;
import android.content.Context;
import android.content.res.CustomTheme;
import android.database.Cursor;

public abstract class ThemeAdapter extends AbstractDAOItemAdapter<ThemeItem> {
    public ThemeAdapter(Activity context) {
        super(context, loadThemes(context), true);
    }

    private static Cursor loadThemes(Activity context) {
        return context.managedQuery(ThemeColumns.CONTENT_PLURAL_URI,
                null, null, ThemeColumns.NAME);
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
