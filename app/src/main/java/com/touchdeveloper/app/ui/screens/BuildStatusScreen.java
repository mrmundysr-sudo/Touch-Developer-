package com.touchdeveloper.app.ui.screens;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.touchdeveloper.app.MainActivity;
import com.touchdeveloper.app.model.BuildRecord;
import com.touchdeveloper.app.safety.Confirmations;
import com.touchdeveloper.app.ui.Screen;
import com.touchdeveloper.app.ui.TaskRunner;
import com.touchdeveloper.app.ui.Ui;

import java.io.File;

/**
 * Build Status Screen: project, status, live progress, start and completion times,
 * Gradle output, error output, and the copy/send/rebuild/download actions.
 *
 * Status text always reflects the recorded build result. A build is only shown as
 * successful when the build service actually confirmed success.
 */
public class BuildStatusScreen extends Screen {

    private TextView progressView;
    private TextView gradleView;

    public BuildStatusScreen(MainActivity main) {
        super(main);
    }

    @Override
    public String title() {
        return "Build Status";
    }

    @Override
    public View create() {
        LinearLayout column = main.column();
        BuildRecord record = main.buildRecord();

        if (record == null) {
            column.addView(Ui.body(main, "No build has been started in this session."));
            column.addView(Ui.body(main, "Open AI Build Prompt to start one."));
            Button prompt = Ui.button(main, "Open AI Build Prompt");
            prompt.setOnClickListener(v -> main.show(new AiBuildPromptScreen(main)));
            column.addView(prompt);
            return column;
        }

        column.addView(Ui.sectionLabel(main, "Project"));
        column.addView(Ui.body(main, record.getProjectName() + "  (" + record.getRepoFullName()
                + " / " + record.getBranch() + ")"));

        column.addView(Ui.sectionLabel(main, "Build status"));
        column.addView(Ui.status(main, record.getStatus()));
        if (record.isDemo()) {
            column.addView(Ui.body(main, "[DEMO] This build was simulated. No real build ran, and no real "
                    + "APK exists."));
        }

        column.addView(Ui.sectionLabel(main, "Timing"));
        column.addView(Ui.body(main, "Start time: " + record.startText()));
        column.addView(Ui.body(main, "Completion time: " + record.endText()));

        column.addView(Ui.sectionLabel(main, "Live progress"));
        progressView = Ui.mono(main, record.progressText().isEmpty()
                ? "No progress messages yet." : record.progressText());
        column.addView(progressView);

        column.addView(Ui.sectionLabel(main, "Gradle output"));
        gradleView = Ui.mono(main, record.getGradleOutput().isEmpty()
                ? "No Gradle output yet." : record.getGradleOutput());
        column.addView(gradleView);

        column.addView(Ui.sectionLabel(main, "Error output"));
        column.addView(Ui.mono(main, record.hasErrors() ? record.getErrorOutput()
                : "No errors recorded."));

        column.addView(Ui.sectionLabel(main, "Actions"));

        addButton(column, "Refresh status", v -> main.replace(new BuildStatusScreen(main)));

        addButton(column, "Copy Errors", v -> {
            String errors = record.hasErrors() ? record.getErrorOutput()
                    : "No errors were recorded for this build.";
            copyToClipboard(errors);
            log("build", "Copied build errors to clipboard");
            main.toast("Build errors copied.");
        });

        addButton(column, "Send Errors to OpenHands", v -> {
            if (!main.services().openHands().isConfigured()) {
                Confirmations.info(main, "OpenHands not configured",
                        "[DEMO] No OpenHands endpoint is configured, so the errors were not sent. "
                                + "Errors copied to the clipboard instead.");
                copyToClipboard(record.getErrorOutput());
                log("build", "Send errors to OpenHands unavailable (not configured)");
                return;
            }
            com.touchdeveloper.app.build.BuildRequest request = main.pendingRequest();
            request.setAiInstructions("Fix these Gradle errors:\n" + record.getErrorOutput());
            TaskRunner.run(main, "OpenHands", "Sending errors\u2026",
                    () -> main.services().openHands().sendInstructions(request),
                    result -> {
                        log("build", "Send errors to OpenHands (" + result.display() + ")");
                        Confirmations.info(main, result.ok ? "Errors sent" : "Errors not sent",
                                result.display());
                    });
        });

        addButton(column, "Rebuild", v -> {
            log("build", "Rebuild requested");
            main.show(new AiBuildPromptScreen(main));
        });

        addButton(column, "Download APK", v -> {
            File apk = record.getApkPath() == null ? null : new File(record.getApkPath());
            if (apk == null || !apk.exists()) {
                Confirmations.info(main, "No APK available",
                        "No APK file exists for this build. "
                                + (record.isDemo() ? "Demo builds only create a labelled placeholder."
                                : "Real APK download is not configured in version 1."));
                return;
            }
            if (record.isDemo()) {
                Confirmations.info(main, "Demo placeholder",
                        "[DEMO] This file is a placeholder, not a real APK:\n" + apk.getAbsolutePath());
                return;
            }
            Confirmations.info(main, "APK available", apk.getAbsolutePath());
        });

        addButton(column, "Download Source ZIP", v -> {
            File zip = record.getSourceZipPath() == null ? null : new File(record.getSourceZipPath());
            if (zip == null || !zip.exists()) {
                Confirmations.info(main, "No source ZIP yet",
                        "Create a source ZIP from Repository actions -> Create source ZIP.");
                return;
            }
            Confirmations.info(main, "Source ZIP", zip.getAbsolutePath());
        });

        addButton(column, "Download Complete Handoff", v -> main.show(new HandoffPackageScreen(main)));

        addButton(column, "View activity history", v -> main.show(new SettingsScreen(main)));
        return column;
    }

    private void addButton(LinearLayout column, String text, View.OnClickListener listener) {
        Button button = Ui.button(main, text);
        button.setOnClickListener(listener);
        column.addView(button);
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) main.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("TouchDeveloper build errors", text));
        }
    }

    private void log(String category, String message) {
        main.services().activityLog().record(category, message, false);
    }
}
