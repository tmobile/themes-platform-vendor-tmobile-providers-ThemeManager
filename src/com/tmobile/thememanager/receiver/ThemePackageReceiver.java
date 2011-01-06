/*
 * Copyright (C) 2010, T-Mobile USA, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tmobile.thememanager.receiver;

import com.tmobile.thememanager.Constants;
import com.tmobile.thememanager.utils.ThemeUtilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.CustomTheme;
import android.util.Log;

public class ThemePackageReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String pkg = intent.getData().getSchemeSpecificPart();

        try {
            boolean isReplacing = intent.getExtras().getBoolean(Intent.EXTRA_REPLACING);

            if (isReplacing) {
                if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {
                    PackageInfo pi = context.getPackageManager().getPackageInfo(pkg, 0);
                    if (isThemeFromPackageApplied(context, pkg) &&
                            pi.themeInfos != null && pi.themeInfos.length > 0) {
                        ThemeUtilities.updateConfiguration(context, pi, pi.themeInfos[0]);
                    }
                }
            } else if (action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
                if (isThemeFromPackageApplied(context, pkg)) {
                    /* Switch us back to teh system default. */
                    CustomTheme defaultTheme = CustomTheme.getSystemTheme();
                    if (pkg.equals(defaultTheme.getThemePackageName())) {
                        Log.e(Constants.TAG, "Removed the system default theme?  This should not happen.");
                    } else {
                        ThemeUtilities.updateConfiguration(context, defaultTheme);
                    }
                }
            }
        } catch (NameNotFoundException e) {
            if (Constants.DEBUG) {
                Log.e(Constants.TAG, "Unable to process intent=" + intent, e);
            }
        }
    }

    private static boolean isThemeFromPackageApplied(Context context, String packageName) {
        CustomTheme theme = ThemeUtilities.getAppliedTheme(context);
        return packageName.equals(theme.getThemePackageName());
    }
}
