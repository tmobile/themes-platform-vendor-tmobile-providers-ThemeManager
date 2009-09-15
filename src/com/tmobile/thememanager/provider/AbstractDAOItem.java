package com.tmobile.thememanager.provider;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public abstract class AbstractDAOItem {
    protected Cursor mCursor;

    public AbstractDAOItem(Cursor c) {
        if (c == null || c.getCount() == 0) {
            throw new IllegalArgumentException("Cursor cannot be null or empty");
        }
        mCursor = c;
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public void close() {
        mCursor.close();
    }

    public void setPosition(int position) {
        mCursor.moveToPosition(position);
    }

    public int getPosition() {
        return mCursor.getPosition();
    }

    public int getCount() {
        return mCursor.getCount();
    }

    protected static Uri parseUriNullSafe(String uriString) {
        return (uriString != null ? Uri.parse(uriString) : null);
    }

    public abstract Uri getUri(Context context);

    protected abstract static class Creator<T extends AbstractDAOItem> {
        public T newInstance(Context context, Uri uri) {
            if (uri != null) {
                Cursor c = context.getContentResolver().query(uri, null, null, null, null);
                return newInstance(c);
            }
            return null;
        }

        public T newInstance(Cursor c) {
            if (c != null && c.moveToFirst() == true) {
                return init(c);
            }
            return null;
        }

        public abstract T init(Cursor c);
    }
}
