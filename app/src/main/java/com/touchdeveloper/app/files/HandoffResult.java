package com.touchdeveloper.app.files;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Result of creating a handoff ZIP, including per-entry checksums. */
public class HandoffResult {

    private final File file;
    private final List<String> contents = new ArrayList<>();
    private final List<String> checksums = new ArrayList<>();
    private final List<String> missing = new ArrayList<>();

    public HandoffResult(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public List<String> getContents() {
        return contents;
    }

    public List<String> getChecksums() {
        return checksums;
    }

    public List<String> getMissing() {
        return missing;
    }

    public String manifestText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Handoff package: ").append(file.getName()).append('\n');
        sb.append("Path: ").append(file.getAbsolutePath()).append('\n');
        sb.append("Size: ").append(file.length()).append(" bytes\n\n");
        sb.append("Included entries:\n");
        for (String c : contents) {
            sb.append("  - ").append(c).append('\n');
        }
        if (!missing.isEmpty()) {
            sb.append("\nNot included (unavailable):\n");
            for (String m : missing) {
                sb.append("  - ").append(m).append('\n');
            }
        }
        sb.append("\nSHA-256 of package: ").append(checksums.isEmpty() ? "unavailable" : checksums.get(0)).append('\n');
        return sb.toString();
    }
}
