package com.tmobile.thememanager;

import android.app.Application;
import android.content.pm.BaseThemeInfo;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.*;

import com.tmobile.thememanager.delta_themes.DeltaThemesStore;
import com.tmobile.thememanager.widget.ThemeAdapter;

public class ThemeManager extends Application {
    public static final String TAG = "ThemeManager";

    public static boolean DEBUG = true;
    
    /**
     * Commonly passed between activities.
     * 
     * @see com.tmobile.thememanager.widget.ThemeAdapter.ThemeItem
     */
    public static final String EXTRA_THEME_ITEM = "theme_item";

    private static final Map<String, BaseThemeInfo> _installedThemePackages = new HashMap<String, BaseThemeInfo>();

    private static SQLiteOpenHelper _themesDatabaseHelper = null;

    @Override
    public void onCreate() {
        super.onCreate();
        _installedThemePackages.clear();
        DeltaThemesStore.createDeltaThemesStore(this);
        _themesDatabaseHelper = ThemeAdapter.createThemeItemDbHelper(this);

    }

    public static synchronized BaseThemeInfo getInstalledThemePackage(String packageName) {
        return _installedThemePackages.get(packageName);
    }

    public static synchronized void addThemePackage(String packageName, BaseThemeInfo themePackage) {
        _installedThemePackages.put(packageName, themePackage);
    }

    // delete ALL values, which key starts with packageName/
    public static synchronized void deleteThemePackage(String packageName) {
        List<String> elementsToRemove = new ArrayList<String>();
        for (String key : _installedThemePackages.keySet()) {
            if (key.startsWith(packageName + '/')) {
                elementsToRemove.add(key);
            }
        }
        if (elementsToRemove.size() > 0) {
            for (String name : elementsToRemove) {
                _installedThemePackages.remove(name);
            }
        }
    }

    public static synchronized List<String> getSoundPackages() {
        List<String> sounds = new ArrayList<String>();
        for (String key : _installedThemePackages.keySet()) {
            BaseThemeInfo value = _installedThemePackages.get(key);
            if (value.type.equals(BaseThemeInfo.InfoObjectType.TYPE_SOUNDPACK)) {
                sounds.add(key);
            }
        }
        return sounds;
    }

    public static SQLiteOpenHelper getThemesDatabaseHelper() {
        return _themesDatabaseHelper;
    }

}
