package com.tmobile.thememanager.utils;

import java.io.Closeable;
import java.io.IOException;

public class IOUtilities {
    public static void close(Closeable stream) {
        try {
            stream.close();
        } catch (IOException e) {}
    }
}
