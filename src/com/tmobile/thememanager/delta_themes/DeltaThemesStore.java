package com.tmobile.thememanager.delta_themes;

import android.content.Context;
import android.util.Log;
import android.os.FileUtils;

import java.io.*;
import java.util.*;

import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.utils.FileUtilities;

/*
 * This class provides the implementation of persistence store of delta/diff themes.
 * The store has the following directory structure on disk:
 *                                          store root (under /data/data/com.tmobile.thememanager)
 *                 <package_name_1>            ...          <package_name_K>
 *          <theme_name_1> ... <theme_name_L>  ...  <theme_name_1> ... <theme_name_M>
 *
 * Inside each <theme_name_i> subfolder, the following files are stored:
 *    - index - contains DeltaThemeInfo (persisted) object; compulsory
 *    - wallpaper - specifies the diff theme wallpaper; optional
 *    - rigntone - specifies the diff theme ringtone; optional
 *    - notification - species the diff theme notification ringtone; optional
 *    - res/values/themes.xml - specifies the diff theme styled resources (colors for now); optional
 * Please notice, that if index file exists, at least one of the optional files must also exist.
 *
 * High level description of how diff theming works:
 *
 * 1. If <package_name_i, theme_name_j> does NOT have a diff theme,
 *    subfolder <package_name_i>/<theme_name_j> does not exist (is empty).
 * 2. As soon as a diff theme is created AND saved (via one of customize screens),
 *    the following happens:
 *    - DeltaThemeInfo object is created and persisted into <package_name_i>/<theme_name_j>/index file;
 *    - Resource files will be created in <package_name_i>/<theme_name_j> as appropriate;
 *    - ThemeChooser will substitute the original theme in the caroussel view with diff theme.
 * 3. If either the diff theme is deleted or "Revert to original" command is executed,
 *    the following actions will take place:
 *    - <package_name_i>/<theme_name_j> subfolder is nuked;
 *    - Corresponding DeltaThemeInfo object is removed from look-up table;
 *    - ThemeChooser will put back the original theme in the caroussel view.
 * 4. ApplyTheme PreviewTheme will use DeltaThemeInfo as appropriate.
 * 5. At the app start up, we will scan through all sub-folders in <store_root> to find index files.
 *    Upong finding an index file, it will be parsed to generate DeltaThemeInfo object and the
 *    depersisted object will be placed into a look-up table.
 */
public final class DeltaThemesStore {

    private static final String STORE_ROOT = "delta_themes_store";
    private static final String INDEX_FILE = "index";

    private static DeltaThemesStore _singleton;

    private Context mContext;
    private File mRootFolder;
    private Map<String, DeltaThemeInfo> mDeltaThemes;

    public static DeltaThemesStore getDeltaThemesStore() {
        assert _singleton != null;
        return _singleton;
    }

    public static void createDeltaThemesStore(Context context) {
        assert _singleton == null;
        _singleton = new DeltaThemesStore(context);
    }

    private DeltaThemesStore(Context context) {
        mContext = context;
        mDeltaThemes = new HashMap<String, DeltaThemeInfo>();
        init();
    }

    private void init() {
        mRootFolder = mContext.getDir(STORE_ROOT, Context.MODE_PRIVATE);
        if (mRootFolder != null && mRootFolder.exists() && mRootFolder.isDirectory()) {
            FileUtils.setPermissions(mRootFolder.getPath(), 0755, -1, -1);
            File [] children = mRootFolder.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (child.isDirectory()) {
                        File [] grandChildren = child.listFiles();
                        if (grandChildren != null) {
                            for (File grandChild : grandChildren) {
                                if (grandChild.isDirectory()) {
                                    // check if index file exists
                                    File file = new File(grandChild, INDEX_FILE);
                                    if (file.exists() && file.isFile()) {
                                        try {
                                            InputStream is = new FileInputStream(file);
                                            DeltaThemeInfo delta = DeltaThemeInfo.deserialize(is);
                                            is.close();
                                            addDeltaTheme(delta);
                                        } catch (Exception e) {
                                            Log.e(ThemeManager.TAG, "Failed to deserialize DeltaThemeInfo", e);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void persist(DeltaThemeInfo info) {
        File file = getIndexFile(info);
        try {
            FileUtilities.forceMkdir(file.getParentFile());
            OutputStream os = new FileOutputStream(file);
            info.serialize(os);
            os.close();
        } catch (IOException e) {
            Log.e(ThemeManager.TAG, "Failed to serialize object", e);
        }
    }

    public File getDeltaThemeFolder(DeltaThemeInfo info) {
        StringBuffer sb = new StringBuffer(info.getPackageName());
        sb.append('/');
        sb.append(info.getParentThemeId());
        return new File (mRootFolder, sb.toString());
    }

    public synchronized DeltaThemeInfo getDeltaTheme(String packageName, String parentThemeId) {
        return mDeltaThemes.get(getDeltaThemeKey(packageName, parentThemeId));
    }

    public synchronized DeltaThemeInfo getDeltaTheme(String themeId) {
        for (DeltaThemeInfo delta : mDeltaThemes.values()) {
            if (delta.getThemeId().equals(themeId)) {
                return delta;
            }
        }
        return null;
    }

    public synchronized void addDeltaTheme(DeltaThemeInfo theme) {
        mDeltaThemes.put(getDeltaThemeKey(theme.getPackageName(), theme.getParentThemeId()), theme);
    }

    public synchronized void deleteThemesForPackage(String packageName) {
        for (String key : mDeltaThemes.keySet()) {
            if (key.startsWith(packageName + '/')) {
                mDeltaThemes.remove(key);
            }
        }

        File dir = new File(mRootFolder, packageName);
        try {
            FileUtilities.deleteDirectory(dir);
        } catch (IOException e) {
            Log.e(ThemeManager.TAG, "Failed to remove directory: " + dir.getPath(), e);
        }
    }

    public synchronized void deleteTheme(DeltaThemeInfo theme) {
        mDeltaThemes.remove(getDeltaThemeKey(theme.getPackageName(), theme.getParentThemeId()));

        File dir = getDeltaThemeFolder(theme);
        try {
            FileUtilities.deleteDirectory(dir);
        } catch (IOException e) {
            Log.e(ThemeManager.TAG, "Failed to remove directory: " + dir.getPath(), e);
        }
    }

    public synchronized List<DeltaThemeInfo> getInstalledDeltaThemes() {
        return new ArrayList<DeltaThemeInfo>(mDeltaThemes.values());
    }

    private File getIndexFile(DeltaThemeInfo info) {
        File dir = getDeltaThemeFolder(info);
        return new File(dir, INDEX_FILE);
    }

    private String getDeltaThemeKey(String packageName, String parentThemeId) {
        StringBuilder sb = new StringBuilder(packageName);
        sb.append('/');
        sb.append(parentThemeId);
        return sb.toString();
    }

}
