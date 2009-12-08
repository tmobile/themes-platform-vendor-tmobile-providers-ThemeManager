package com.tmobile.thememanager.receiver;

import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.provider.ThemeItem;
import com.tmobile.thememanager.provider.Themes.ThemeColumns;
import com.tmobile.thememanager.utils.ThemeUtilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

public class ChangeThemeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (ThemeManager.DEBUG) {
            Log.d(ThemeManager.TAG, "ChangeThemeReceiver: intent=" + intent);
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
            Log.e(ThemeManager.TAG, "Could not retrieve theme item for uri=" + intent.getData());
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
                Log.w(ThemeManager.TAG,
                        "Ignoring unknown change theme request (but we aborted it, sorry)...");
            }
        } finally {
            item.close();
        }
    }
}
