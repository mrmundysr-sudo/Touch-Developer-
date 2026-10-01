package com.touchdeveloper.app.safety;

import android.app.AlertDialog;
import android.content.Context;

import java.util.List;

/**
 * Confirmation dialogs required by the safety rules.
 *
 * Every destructive or public action goes through here so the user always sees
 * what will change before it happens.
 */
public final class Confirmations {

    public interface Callback {
        void onConfirmed();
    }

    private Confirmations() {
    }

    /** Standard single confirmation for a destructive action. */
    public static void confirmDestructive(Context context, String title, String message,
                                          Callback onConfirmed) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Confirm", (d, w) -> onConfirmed.onConfirmed())
                .show();
    }

    /**
     * Two-step confirmation for the most severe action, deleting a repository.
     * The user must confirm twice before the action is allowed to proceed.
     */
    public static void confirmRepositoryDeletion(Context context, String repoFullName,
                                                 Callback onConfirmed) {
        confirmDestructive(context, "Delete repository?",
                "First confirmation.\n\nThis will delete " + repoFullName
                        + " and cannot be undone.\n\nProtected artifacts (latest APK, handoff ZIP, "
                        + "handoff instructions) are not deleted automatically.",
                () -> confirmDestructive(context, "Delete repository - final confirmation",
                        "Second confirmation.\n\nType nothing is required, but this is the last step. "
                                + "Delete " + repoFullName + " now?",
                        onConfirmed));
    }

    /** Confirmation before pushing changes publicly to GitHub. */
    public static void confirmPublicPush(Context context, String repoFullName, String branch,
                                         List<String> changedPaths, Callback onConfirmed) {
        StringBuilder sb = new StringBuilder();
        sb.append("About to push to ").append(repoFullName).append(" on branch ").append(branch)
                .append(".\n\nThis makes the changes publicly visible on GitHub.\n\n");
        if (changedPaths == null || changedPaths.isEmpty()) {
            sb.append("No staged changes are recorded.");
        } else {
            sb.append("Files that will change:\n");
            for (String path : changedPaths) {
                sb.append("  - ").append(path).append('\n');
            }
        }
        confirmDestructive(context, "Confirm public push", sb.toString(), onConfirmed);
    }

    /** Shows exactly what will change before a commit, then asks for confirmation. */
    public static void confirmCommit(Context context, String branch, List<String> changeLines,
                                     Callback onConfirmed) {
        StringBuilder sb = new StringBuilder();
        sb.append("Branch: ").append(branch).append("\n\nChanges to be committed:\n");
        if (changeLines == null || changeLines.isEmpty()) {
            sb.append("  (nothing staged)");
        } else {
            for (String line : changeLines) {
                sb.append("  - ").append(line).append('\n');
            }
        }
        confirmDestructive(context, "Review changes before commit", sb.toString(), onConfirmed);
    }

    public static void info(Context context, String title, String message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (d, w) -> d.dismiss())
                .show();
    }
}
