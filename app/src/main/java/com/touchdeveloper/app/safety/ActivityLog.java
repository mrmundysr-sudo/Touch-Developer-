package com.touchdeveloper.app.safety;

import android.content.Context;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory activity history that also appends each entry to a private log file.
 *
 * Credential-shaped content is never passed to this service; callers log
 * descriptions of actions, not secret values.
 */
public class ActivityLog implements ActivityLogService {

    private static final int MAX_ENTRIES = 500;

    private final List<ActivityEntry> entries = new ArrayList<>();
    private final File logFile;

    public ActivityLog(Context context) {
        File dir = context.getApplicationContext().getFilesDir();
        this.logFile = new File(dir, "activity-history.log");
        record("system", "Activity log started", false);
    }

    @Override
    public synchronized void record(String category, String message, boolean confirmedByUser) {
        ActivityEntry entry = new ActivityEntry(category, message, confirmedByUser);
        entries.add(entry);
        if (entries.size() > MAX_ENTRIES) {
            entries.remove(0);
        }
        try {
            Files.write(logFile.toPath(),
                    (entry.display() + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // History remains available in memory even if the file write fails.
        }
    }

    @Override
    public synchronized List<ActivityEntry> entries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    @Override
    public synchronized String asText() {
        StringBuilder sb = new StringBuilder();
        for (ActivityEntry entry : entries) {
            sb.append(entry.display()).append('\n');
        }
        return sb.toString();
    }
}
