package com.tmobile.thememanager.receiver;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ThemeInfo;
import android.content.pm.SoundsInfo;
import android.net.Uri;

import com.tmobile.thememanager.provider.PackageResources;
import com.tmobile.thememanager.provider.Themes;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        List<PackageInfo> themePackages = context.getPackageManager().getInstalledThemePackages();

        for (PackageInfo pi: themePackages) {
            if (pi.soundInfos != null) {
                for (SoundsInfo si : pi.soundInfos) {
                    if (si.ringtoneFileName != null) {
                        Uri ringtoneUri =
                            PackageResources.getRingtoneUri(context, pi.packageName, si.ringtoneFileName);

                        if (ringtoneUri == null) {
                            PackageResources.insertRingtone(context, pi, si);
                        }
                    }
                    if (si.notificationRingtoneFileName != null) {
                        Uri ringtoneUri =
                            PackageResources.getRingtoneUri(context, pi.packageName, si.notificationRingtoneFileName);

                        if (ringtoneUri == null) {
                            PackageResources.insertNotificationRingtone(context, pi, si);
                        }
                    }
                }
            }
            if (pi.themeInfos == null) continue;
            for (ThemeInfo ti: pi.themeInfos) {
                if (ti.ringtoneFileName != null) {
                    Uri ringtoneUri =
                        PackageResources.getRingtoneUri(context, pi.packageName, ti.ringtoneFileName);

                    if (ringtoneUri == null) {
                        PackageResources.insertRingtone(context, pi, ti);
                    }
                }
                if (ti.notificationRingtoneFileName != null) {
                    Uri ringtoneUri =
                        PackageResources.getRingtoneUri(context, pi.packageName, ti.notificationRingtoneFileName);

                    if (ringtoneUri == null) {
                        PackageResources.insertNotificationRingtone(context, pi, ti);
                    }
                }
                if (ti.wallpaperImageName != null) {
                    Uri imageUri =
                        PackageResources.getImageUri(context, pi.packageName, ti.wallpaperImageName);

                    if (imageUri == null) {
                        PackageResources.insertImage(context, pi, ti, PackageResources.ImageColumns.IMAGE_TYPE_WALLPAPER);
                    }
                }
                if (ti.favesImageName != null) {
                    Uri imageUri =
                        PackageResources.getImageUri(context, pi.packageName, ti.favesImageName);

                    if (imageUri == null) {
                        PackageResources.insertImage(context, pi, ti, PackageResources.ImageColumns.IMAGE_TYPE_FAVE);
                    }
                }
                if (ti.favesAppImageName != null) {
                    Uri imageUri =
                        PackageResources.getImageUri(context, pi.packageName, ti.favesAppImageName);

                    if (imageUri == null) {
                        PackageResources.insertImage(context, pi, ti, PackageResources.ImageColumns.IMAGE_TYPE_APP_FAVE);
                    }
                }
                if (ti.thumbnail != null) {
                    Uri thumbnailUri =
                        PackageResources.getImageUri(context, pi.packageName, ti.thumbnail);

                    if (thumbnailUri == null) {
                        PackageResources.insertImage(context, pi, ti, PackageResources.ImageColumns.IMAGE_TYPE_THUMBNAIL);
                    }
                }
                Themes.insertTheme(context, pi, ti, false);
            }
        }
    }

}
