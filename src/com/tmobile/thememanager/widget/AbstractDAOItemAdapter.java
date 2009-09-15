package com.tmobile.thememanager.widget;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.widget.CursorAdapter;
import android.database.Cursor;

import com.tmobile.thememanager.provider.AbstractDAOItem;

/**
 * Re-usable adapter which fills itself with all currently installed visual
 * themes. Includes a convenient inner-class which can represent all types of
 * visual themes with helpful accessors.
 */
public abstract class AbstractDAOItemAdapter<T extends AbstractDAOItem> extends CursorAdapter {
    protected T mDAOItem;
    private final LayoutInflater mInflater;

    public AbstractDAOItemAdapter(Activity context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
        mInflater = LayoutInflater.from(context);
        allocInternal(c);
    }

    protected LayoutInflater getInflater() {
        return mInflater;
    }

    protected abstract void onAllocInternal(Cursor c);
    
    private void allocInternal(Cursor c) {
        if (c != null && c.getCount() > 0) {
            onAllocInternal(c); 
        } else {
            mDAOItem = null;
        }
    }

    @Override
    public void notifyDataSetChanged() {
        allocInternal(getCursor());
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        mDAOItem = null;
        super.notifyDataSetInvalidated();
    }

    /**
     * Get the currently applied item backed by this DAO item.
     * 
     * @return The currently applied item or null if none exist.
     */
    protected abstract T getCurrentlyAppliedItem(Context context);

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
            T current = getCurrentlyAppliedItem(context);
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
            T item = getDAOItem(n);
            if (uri.equals(item.getUri(context)) == true) {
                return n;
            }
        }
        return -1;
    }

    public T getDAOItem(int position) {
        if (position >= 0 && getCount() >= 0) {
            mDAOItem.setPosition(position);
            return mDAOItem;
        }
        return null;
    }
}
