package com.touchdeveloper.app.safety;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * One recorded user or system action.
 */
public class ActivityEntry {

    private static final SimpleDateFormat TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    private final long timestamp;
    private final String category;
    private final String message;
    private final boolean confirmedByUser;

    public ActivityEntry(String category, String message, boolean confirmedByUser) {
        this.timestamp = System.currentTimeMillis();
        this.category = category;
        this.message = message;
        this.confirmedByUser = confirmedByUser;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getCategory() {
        return category;
    }

    public String getMessage() {
        return message;
    }

    public boolean isConfirmedByUser() {
        return confirmedByUser;
    }

    public String display() {
        return TIME.format(new Date(timestamp)) + "  [" + category + "]  " + message
                + (confirmedByUser ? " (confirmed)" : "");
    }
}
