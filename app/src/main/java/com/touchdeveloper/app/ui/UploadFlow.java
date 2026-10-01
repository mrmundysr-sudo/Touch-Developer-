package com.touchdeveloper.app.ui;

import com.touchdeveloper.app.util.Result;

import java.io.File;

/**
 * Pure decision logic for the upload flow, kept free of Android types so it can be
 * tested on the JVM.
 *
 * The UI picks a file, asks for a target path, then imports and stages it. This
 * class owns the parts that must stay correct regardless of the views: the path a
 * picked file suggests, whether a typed path is usable, and how the import/read/stage
 * results are reported to the user.
 */
public final class UploadFlow {

    private UploadFlow() {
    }

    /** What happened to one upload attempt. */
    public static final class Outcome {

        public enum Status {
            /** The file was imported and staged locally. */
            STAGED,
            /** The file was imported but is binary, which version 1 cannot stage. */
            BINARY_UNSUPPORTED,
            /** The upload did not happen. */
            FAILED
        }

        public final Status status;
        public final String message;
        public final String sourceName;
        public final boolean demo;

        Outcome(Status status, String message, String sourceName, boolean demo) {
            this.status = status;
            this.message = message;
            this.sourceName = sourceName;
            this.demo = demo;
        }

        public boolean ok() {
            return status == Status.STAGED;
        }

        /** True when the outcome should be shown as an error rather than a success. */
        public boolean isError() {
            return status != Status.STAGED;
        }
    }

    /**
     * Suggests a repository path from a picked file's display name. A document
     * picker may report a full path, so only the final segment is used.
     */
    public static String suggestedTargetPath(String displayName) {
        String name = displayName == null ? "" : displayName.trim();
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        return name.trim();
    }

    /**
     * Returns a user-facing problem with the target path, or {@code null} when the
     * path is usable. The path must name a file inside the repository.
     */
    public static String validateTargetPath(String target) {
        String path = target == null ? "" : target.trim();
        if (path.isEmpty()) {
            return "Enter a target path first.";
        }
        if (path.startsWith("/")) {
            return "Use a path relative to the repository root (no leading \"/\").";
        }
        if (path.endsWith("/")) {
            return "The target must name a file, not a folder.";
        }
        if (path.equals("..") || path.startsWith("../") || path.contains("/../") || path.endsWith("/..")) {
            return "The target must stay inside the repository (no \"..\").";
        }
        return null;
    }

    /**
     * Classifies the import, read, and stage results into one outcome.
     *
     * A failure at any step stops the chain and is reported as-is; demo staging is
     * kept but labelled, so it is never presented as a real upload.
     */
    public static Outcome from(String target, Result<File> imported, Result<String> read,
                               Result<String> staged) {
        if (imported == null || !imported.ok) {
            return new Outcome(Outcome.Status.FAILED,
                    imported == null ? "No file was selected." : imported.display(),
                    suggestedTargetPath(target), false);
        }
        String source = imported.data == null ? suggestedTargetPath(target) : imported.data.getName();
        if (read == null || !read.ok) {
            String reason = read == null ? "The file could not be read." : read.display();
            return new Outcome(Outcome.Status.BINARY_UNSUPPORTED,
                    reason + " The file was saved locally but is not staged, because binary upload "
                            + "is not implemented in version 1.",
                    source, false);
        }
        if (staged == null || !(staged.ok || staged.demo)) {
            return new Outcome(Outcome.Status.FAILED,
                    staged == null ? "The file could not be staged." : staged.display(), source, false);
        }
        return new Outcome(Outcome.Status.STAGED, staged.display(), source, staged.demo);
    }
}
