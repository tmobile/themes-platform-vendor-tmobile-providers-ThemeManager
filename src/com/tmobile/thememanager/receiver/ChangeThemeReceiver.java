package com.tmobile.thememanager.receiver;

import com.tmobile.thememanager.Constants;
import com.tmobile.thememanager.utils.ThemeUtilities;
import com.tmobile.themes.ThemeManager;
import com.tmobile.themes.provider.ThemeItem;
import com.tmobile.themes.provider.Themes.ThemeColumns;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ChangeThemeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (Constants.DEBUG) {
            Log.d(Constants.TAG, "ChangeThemeReceiver: intent=" + intent);
        }

        abortBroadcast();

        ReceiverExecutor.execute(getClass().getSimpleName(), new Runnable() {
            public void run() {
                handleChangeTheme(context, intent);
            }
        });
    }

    private void handleChangeTheme(Context context, Intent intent) {
        ThemeItem item = ThemeItem.getInstance(context, intent.getData());
        if (item == null) {
            Log.e(Constants.TAG, "Could not retrieve theme item for uri=" + intent.getData());
            return;
        }
        try {
            if (intent.getBooleanExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, false) ||
                    intent.getType() == null ||
                    ThemeColumns.CONTENT_ITEM_TYPE.equals(intent.getType())) {
                ThemeUtilities.applyTheme(context, item, intent);
            } else if (ThemeColumns.STYLE_CONTENT_ITEM_TYPE.equals(intent.getType())) {
                ThemeUtilities.applyStyle(context, item);
            } else {
                Log.w(Constants.TAG,
                        "Ignoring unknown change theme request (but we aborted it, sorry)...");
            }
        } finally {
            item.close();
        }
    }
}
