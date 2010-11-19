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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.text.TextUtils;
import android.view.InflateException;

import java.io.IOException;

/**
 * Holds information about the "default" system theme. This is inserted as a
 * dummy row the first time the themes database is created.
 */
class SystemThemeMeta {
    public String author;
    public String name;
    public String styleName;
    public String wallpaperName;
    public Uri wallpaperUri;
    public Uri previewUri;

    private static final String XML_THEME_TAG = "theme";
    private static final String XML_AUTHOR_TAG = "author";
    private static final String XML_NAME_TAG = "name";
    private static final String XML_STYLE_NAME_TAG = "styleName";
    private static final String XML_WALLPAPER_NAME_TAG = "wallpaperName";
    private static final String XML_WALLPAPER_URI_TAG = "wallpaperUri";
    private static final String XML_PREVIEW_URI_TAG = "previewUri";

    private SystemThemeMeta() {}

    public static SystemThemeMeta inflate(Resources res, int resId) {
        SystemThemeMeta theme = new SystemThemeMeta();
        XmlResourceParser parser = res.getXml(resId);
        try {
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tagName = parser.getName();
                    if (XML_THEME_TAG.equals(tagName)) {
                        if (handleThemeTag(parser, theme)) {
                            return theme;
                        }
                    }
                }
                eventType = parser.next();
            }
            throw new InflateException("Unexpected end of document");
        } catch (XmlPullParserException e) {
            throw new InflateException("Error inflating system theme meta XML", e);
        } catch (IOException e) {
            throw new InflateException("Error inflating system theme meta XML", e);
        } finally {
            parser.close();
        }
    }

    private static boolean handleThemeTag(XmlPullParser parser, SystemThemeMeta theme)
            throws XmlPullParserException, IOException {
        int eventType;
        while ((eventType = parser.getEventType()) != XmlPullParser.END_TAG) {
            if (eventType == XmlPullParser.START_TAG) {
                String tagName = parser.getName();
                if (XML_AUTHOR_TAG.equals(tagName)) {
                    theme.author = parser.nextText();
                    parser.require(XmlPullParser.END_TAG, null, XML_AUTHOR_TAG);
                } else if (XML_NAME_TAG.equals(tagName)) {
                    theme.name = parser.nextText();
                    parser.require(XmlPullParser.END_TAG, null, XML_NAME_TAG);
                } else if (XML_STYLE_NAME_TAG.equals(tagName)) {
                    theme.styleName = parser.nextText();
                    parser.require(XmlPullParser.END_TAG, null, XML_STYLE_NAME_TAG);
                } else if (XML_WALLPAPER_NAME_TAG.equals(tagName)) {
                    theme.wallpaperName = parser.nextText();
                    parser.require(XmlPullParser.END_TAG, null, XML_WALLPAPER_NAME_TAG);
                } else if (XML_WALLPAPER_URI_TAG.equals(tagName)) {
                    theme.wallpaperUri = Uri.parse(parser.nextText());
                    parser.require(XmlPullParser.END_TAG, null, XML_WALLPAPER_URI_TAG);
                } else if (XML_PREVIEW_URI_TAG.equals(tagName)) {
                    theme.previewUri = Uri.parse(parser.nextText());
                    parser.require(XmlPullParser.END_TAG, null, XML_PREVIEW_URI_TAG);
                }
            } else if (eventType == XmlPullParser.END_DOCUMENT) {
                return false;
            }
            parser.next();
        }

        if (TextUtils.isEmpty(theme.name)) {
            throw new InflateException("Missing minimum required tag: <name>");
        }
        if (TextUtils.isEmpty(theme.author)) {
            throw new InflateException("Missing minimum required tag: <author>");
        }

        return true;
    }
}
