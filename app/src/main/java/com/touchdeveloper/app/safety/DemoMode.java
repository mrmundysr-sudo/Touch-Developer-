package com.touchdeveloper.app.safety;

/**
 * The single place where demo mode is labelled, so no screen can accidentally
 * present mock data as a confirmed real result.
 */
public final class DemoMode {

    public static final String TAG = "[DEMO]";

    private DemoMode() {
    }

    /** Prefix that must be used on every message derived from mock data. */
    public static String label(String message) {
        return TAG + " " + message;
    }

    /** Banner text shown whenever an integration is running without credentials. */
    public static String banner(String integration) {
        return TAG + " " + integration + " is not configured. Showing clearly labelled demo data only. "
                + "No real request was sent.";
    }
}
