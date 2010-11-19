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

package com.tmobile.thememanager.provider;

import com.tmobile.themes.provider.ThemeItem;
import com.tmobile.themes.provider.Themes;

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
