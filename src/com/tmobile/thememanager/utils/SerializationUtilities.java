package com.tmobile.thememanager.utils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public final class SerializationUtilities {

    private static final int TAG_INT = 0;
    private static final int TAG_STRING = 1;

    private static final String UTF8_ENCODING = "UTF-8";


    // Write methods

    private static void writeByte(byte value, OutputStream os) throws IOException {
        os.write(value);
    }

    public static void writeInt(int value, OutputStream os) throws IOException {
        writeByte((byte)TAG_INT, os);
        byte [] array = new byte[Integer.SIZE / 8];
        for (int i = 0, j = array.length - 1; i < array.length; i++, j--) {
            int byteValue = (value & 0xFF);
            array[j] = (byte)byteValue;
            value = Integer.rotateRight(value, 8);
        }
        os.write(array);
    }

    public static void writeString(String s, OutputStream os) throws IOException {
        writeByte((byte)TAG_STRING, os);
        if (s == null || s.length() == 0) {
            writeInt(0, os);
            return;
        }
        byte [] array = s.getBytes(UTF8_ENCODING);
        writeInt(array.length, os);
        os.write(array);
    }


    // Read methods

    private static int readByte(InputStream is) throws IOException {
        return is.read();
    }

    public static int readInt(InputStream is) throws IOException {
        int tag = readByte(is);
        if (tag != TAG_INT) {
            throw new IOException("TAG_INT is expected!");
        }
        byte [] array = new byte[Integer.SIZE / 8];
        int bytesRead = is.read(array);
        if (bytesRead != array.length) {
            throw new IOException("Insufficient data!");
        }

        int value = 0;
        for (int i = 0; i < bytesRead; i++) {
            value = Integer.rotateLeft(value, 8) | array[i];
        }
        return value;
    }

    public static String readString(InputStream is) throws IOException {
        int tag = readByte(is);
        if (tag != TAG_STRING) {
            throw new IOException("TAG_STRING is expected!");
        }
        int len = readInt(is);
        if (len == 0) {
            return null;
        }
        byte [] array = new byte[len];
        if (len != is.read(array)) {
            throw new IOException("Insufficient data!");
        }
        return new String(array, 0, len, UTF8_ENCODING);
    }

}
