package com.touchdeveloper.app.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Small formatting helpers shared by screens and services. */
public final class Formats {

    private static final SimpleDateFormat STAMP = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);

    private Formats() {
    }

    public static String timestamp() {
        return STAMP.format(new Date());
    }

    public static String bytes(long size) {
        if (size < 1024) {
            return size + " B";
        }
        if (size < 1024 * 1024) {
            return (size / 1024) + " KB";
        }
        return String.format(Locale.US, "%.1f MB", size / (1024.0 * 1024.0));
    }

    public static String sanitizeFileName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "project";
        }
        return name.trim().replaceAll("[^A-Za-z0-9._-]", "-");
    }
}
