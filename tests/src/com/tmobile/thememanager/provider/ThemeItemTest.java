package com.tmobile.thememanager.provider;

import android.test.AndroidTestCase;

public class ThemeItemTest extends AndroidTestCase {
    public void testListThemes() {
        ThemeItem items = ThemeItem.getInstance(Themes.listThemes(getContext()));
        assertTrue("No themes found or failure querying provider", items != null);
    }
    
    public void testGetAppliedTheme() {
        ThemeItem item = ThemeItem.getInstance(Themes.getAppliedTheme(getContext()));
        assertTrue("Cannot identify currently applied theme", item != null);
        assertTrue("More than one theme is marked as applied", item.getCount() == 1);
    }
}
