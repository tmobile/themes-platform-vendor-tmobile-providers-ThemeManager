package com.tmobile.thememanager.delta_themes;

import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.activity.CustomizeColor;
import com.tmobile.thememanager.activity.Customize.Customizations;
import com.tmobile.thememanager.utils.FileUtilities;
import com.tmobile.thememanager.widget.ThemeAdapter.ThemeItem;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.*;
import android.os.FileUtils;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class DeltaThemeGenerator {
    private static final String MANIFEST =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n\n" +
        "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
        "    package=\"%s\"\n" + // placeholder for package name
        "    android:hasCode=\"false\"\n" +
        "    android:versionCode=\"1\"\n" +
        "    android:versionName=\"1.0.0\">\n" +
        "</manifest>";

    public static DeltaThemeInfo createDeltaThemeInfo(Context context, ThemeItem baseTheme,
            Customizations changes) {
        DeltaThemeInfo delta = baseTheme.delta;
        if (delta == null) {
            // We customize "regular" theme
            String parentPackage = baseTheme.getPackageName();
            String parentThemeId = baseTheme.getThemeId();
            delta = new DeltaThemeInfo(parentPackage, parentThemeId);
            delta.setAuthor(baseTheme.getAuthor());
            delta.setIsDrmProtected(baseTheme.isDRMProtected());
            delta.setName(baseTheme.getName());
            delta.setParentThemeId(parentThemeId);
        }
        DeltaThemesStore.getDeltaThemesStore().addDeltaTheme(delta);

        File dir = DeltaThemesStore.getDeltaThemesStore().getDeltaThemeFolder(delta);

        if (changes.wallpaperUri != null) {
            delta.setWallpaperUri(changes.wallpaperUri);
            delta.setWallpaperName(changes.wallpaperName);
        }
        if (changes.ringtoneUri != null) {
            delta.setRingtoneUri(changes.ringtoneUri);
            delta.setRingtoneName(changes.ringtoneName);
        }
        if (changes.notificationRingtoneUri != null) {
            delta.setNotificationRingtoneUri(changes.notificationRingtoneUri);
            delta.setNotificationRingtoneName(changes.notificationRingtoneName);
        }
        if (changes.soundPackName != null) {
            delta.setSoundPackName(changes.soundPackName);
        }
        if (changes.colorPaletteName != null) {
            delta.setColorPaletteName(changes.colorPaletteName);
            try {
                generateStyle(context, delta, dir, changes.colorPaletteName);
            } catch (Exception e) {
                Log.d(ThemeManager.TAG, "Failed to create style: " + dir.getPath(), e);
            } finally {
                try {
                    FileUtilities.deleteDirectory(new File(dir, "res"));
                } catch (IOException e){
                    Log.e(ThemeManager.TAG, "Failed to delete folder: " + dir.getPath() + "/res", e);
                }
            }
        }

        return delta;
    }

    private static void generateStyle(Context context, DeltaThemeInfo delta, File dir, 
            String colorPaletteName)
            throws IOException, PackageManager.NameNotFoundException, ClassNotFoundException {
        // Create themes.xml
        String styleName = CustomTheme.getDeltaThemeStyleName(delta.getParentThemeId());
        String packageName = CustomTheme.getDeltaThemePackageName(styleName);

        FileUtilities.forceMkdir(dir);

        // forceMkdir sets directory permissions to 0750.
        // Also this process owner is theme_manager, which is the memeber
        // of the theme_manager group ONLY.
        // As a result, other processes won't be able
        // to access directories/files created by this process.
        //
        // To address the issue described above, change the directories
        // permissions to allow other processes to read resource bundle
        FileUtils.setPermissions(dir.getPath(), 0755, -1, -1);
        FileUtils.setPermissions(dir.getParent(), 0755, -1, -1);
        FileUtils.setPermissions(dir.getParentFile().getParent(), 0755, -1, -1);

        File manifest = new File(dir, "AndroidManifest.xml");
        Writer out = new BufferedWriter(new FileWriter(manifest));
        out.write(String.format(MANIFEST, packageName));
        out.close();

        File resDir = new File(dir, "res/values");
        FileUtilities.forceMkdir(resDir);
        File stylesFile = new File(resDir, "themes.xml");
        out = new BufferedWriter(new FileWriter(stylesFile));
        out.write("<resources>\n");
        out.write(String.format("  <style name=\"%s\">\n", styleName));
        CustomizeColor.generateStyle(context, out, colorPaletteName);
        out.write("  </style>\n");
        out.write("</resources>\n");
        out.close();

        // Build resource bundle using aapt tool
        String [] args =
            new String [] {
                "aapt",
                "p",
                "-M",
                dir.getPath() +"/AndroidManifest.xml",
                "-I",
                "/system/framework/framework-res.apk",
                "-S",
                dir.getPath() + "/res",
                "-x",
                "3",
                "-F",
                dir.getPath() + "/resources.res",
                "-f",
        };
        /* Functionality removed during rebase project onto android-1.5r2. */
        int result = 1; // Aapt.invokeAapt(args);
        if (result != 0) {
            throw new RuntimeException("Creation of resource bundle failed!");
        }

        // Get new style id
        delta.setResourceBundlePath(dir.getPath() + "/resources.res");

        // Make sure resource bundle can be only read by other processes
        FileUtils.setPermissions(delta.getResourceBundlePath(), 0644, -1, -1);

        int id = CustomTheme.getDeltaThemeStyleId(context, styleName, packageName, delta.getResourceBundlePath());
        delta.setStyleId(id);
        delta.setThemeId(styleName);
    }
}
