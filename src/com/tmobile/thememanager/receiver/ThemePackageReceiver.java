package com.tmobile.thememanager.receiver;

import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.provider.PackageResources;
import com.tmobile.thememanager.provider.PackageResourcesProvider;
import com.tmobile.thememanager.provider.Themes;
import com.tmobile.thememanager.utils.ThemeBitmapStore;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.SoundsInfo;
import android.content.pm.ThemeInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.CustomTheme;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class ThemePackageReceiver extends BroadcastReceiver {
    private final static int TIMEOUT = 5000; // timeout in milliseconds
    private final static ExecutorService workingThread = Executors.newSingleThreadExecutor();
    private final static Object lock = new Object();

    private static String removedPackageName = null;
    private static String removedThemeId = null;
    private static boolean recreateCustomTheme = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String pkg = intent.getData().getSchemeSpecificPart();

        try {
            if (ThemeManager.DEBUG) {
                Log.d(ThemeManager.TAG, "Handling intent=" + intent);
            }

            boolean isReplacing = intent.getExtras().getBoolean(Intent.EXTRA_REPLACING);

            if (isReplacing) {
                if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {
                    boolean updateConfiguration = deleteThemeResources(context, pkg, true);
                    addThemeResources(context, pkg);
                    PackageResourcesProvider.getResourcesForTheme(context, pkg);
                    updateConfig(context, pkg, updateConfiguration);
                }
            } else if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {
                boolean updateConfiguration = false;
                synchronized(lock) {
                    if (ThemeManager.DEBUG && removedPackageName != null && removedPackageName.equals(pkg)) {
                        updateConfiguration = true;
                        removedPackageName = null;
                    }
                }
                addThemeResources(context, pkg);
                updateConfig(context, pkg, updateConfiguration);
            } else if (action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
                if (deleteThemeResources(context, pkg, false) && ThemeManager.DEBUG) {
                    removedPackageName = pkg;
                    workingThread.execute(new Runnable() {
                        public void run() {
                            try {
                                Thread.sleep(TIMEOUT);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            synchronized(lock) {
                                removedPackageName = null;
                            }
                        }
                    });
                }
            }
        } catch (NameNotFoundException e) {
            if (ThemeManager.DEBUG) {
                Log.e(ThemeManager.TAG, "Unable to process intent=" + intent, e);
            }
        }
    }

    private boolean deleteThemeResources(Context context, String packageName, boolean deferConfigurationUpdate) {
        Themes.deleteThemesByPackage(context, packageName);

        PackageResources.deleteRingtones(context, packageName);
        PackageResources.deleteImages(context, packageName);

        ThemeBitmapStore.deletePackage(context, packageName);

        boolean updateConfiguration = false;

        /* Check if the current theme is defined in the package being removed. */
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        Configuration config = am.getConfiguration();
        if (config.customTheme != null) {
            String currentPkg = config.customTheme.getThemePackageName();
            updateConfiguration = (currentPkg != null && currentPkg.equals(packageName));
            if (updateConfiguration && !deferConfigurationUpdate) {
                removedThemeId = config.customTheme.getThemeId();
                CustomTheme defaultTheme = CustomTheme.getDefault();
                Themes.markAppliedTheme(context, defaultTheme.getThemePackageName(),
                        defaultTheme.getThemeId());
                config.customTheme = defaultTheme;
                am.updateConfiguration(config);
            }
        }
        PackageResourcesProvider.deleteResourcesForTheme(packageName);
        return updateConfiguration;
    }

    private void addThemeResources(Context context, String packageName) throws NameNotFoundException {
        PackageInfo pi = context.getPackageManager().getPackageInfo(packageName, 0);
        if (pi != null && pi.soundInfos != null) {
            for (SoundsInfo si: pi.soundInfos) {
                if (si.ringtoneFileName != null) {
                    PackageResources.insertRingtone(context, pi, si);
                }
                if (si.notificationRingtoneFileName != null) {
                    PackageResources.insertNotificationRingtone(context, pi, si);
                }
            }
        }
        if (pi != null && pi.themeInfos != null) {
            for (ThemeInfo ti: pi.themeInfos) {
                if (removedThemeId != null && ti.themeId.equals(removedThemeId)) {
                    recreateCustomTheme = true;
                }
                if (ti.ringtoneFileName != null) {
                    PackageResources.insertRingtone(context, pi, ti);
                }
                if (ti.notificationRingtoneFileName != null) {
                    PackageResources.insertNotificationRingtone(context, pi, ti);
                }
                if (ti.wallpaperImageName != null) {
                    PackageResources.insertImage(context, pi, ti, PackageResources.ImageColumns.IMAGE_TYPE_WALLPAPER);
                }
                if (ti.favesImageName != null) {
                    PackageResources.insertImage(context, pi, ti, PackageResources.ImageColumns.IMAGE_TYPE_FAVE);
                }
                if (ti.favesAppImageName != null) {
                    PackageResources.insertImage(context, pi, ti, PackageResources.ImageColumns.IMAGE_TYPE_APP_FAVE);
                }
                if (ti.thumbnail != null) {
                    PackageResources.insertImage(context, pi, ti, PackageResources.ImageColumns.IMAGE_TYPE_THUMBNAIL);
                }
                if (ti.preview != null) {
                    PackageResources.insertImage(context, pi, ti, PackageResources.ImageColumns.IMAGE_TYPE_PREVIEW);
                }
                Themes.insertTheme(context, pi, ti, true);
            }
        }
    }

    private void updateConfig(Context context, String pkg, boolean updateConfiguration) {
        if (updateConfiguration && recreateCustomTheme) {
            ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
            Configuration config = am.getConfiguration();
            config.customTheme = new CustomTheme(removedThemeId, pkg);
            Themes.markAppliedTheme(context, pkg, removedThemeId);
            am.updateConfiguration(config);
        }
        removedThemeId = null;
        recreateCustomTheme = false;
    }
}
