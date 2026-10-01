package com.touchdeveloper.app.ui.screens;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.touchdeveloper.app.MainActivity;
import com.touchdeveloper.app.files.HandoffRequest;
import com.touchdeveloper.app.files.HandoffResult;
import com.touchdeveloper.app.model.BuildRecord;
import com.touchdeveloper.app.model.Repo;
import com.touchdeveloper.app.safety.Confirmations;
import com.touchdeveloper.app.ui.Screen;
import com.touchdeveloper.app.ui.Ui;
import com.touchdeveloper.app.util.Formats;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Handoff Package Screen: builds a complete handoff ZIP containing the source
 * project, APK, README, AI instructions, source code, assets, build log, error
 * log, test results, version and commit information, and a SHA-256 checksum.
 *
 * Sections with no available data are listed as unavailable rather than invented.
 */
public class HandoffPackageScreen extends Screen {

    private TextView manifestView;

    public HandoffPackageScreen(MainActivity main) {
        super(main);
    }

    @Override
    public String title() {
        return "Handoff Package";
    }

    @Override
    public View create() {
        LinearLayout column = main.column();
        Repo repo = main.selectedRepo();

        column.addView(Ui.body(main, "Project: " + (repo == null ? "(none selected)"
                : repo.getFullName())));
        column.addView(Ui.body(main, "Package contents: source project, APK, README, AI instructions, "
                + "source code, assets, build log, error log, test results, version info, commit info, "
                + "SHA-256 checksum when available."));

        Button create = Ui.button(main, "Create handoff ZIP");
        create.setOnClickListener(v -> createHandoff());
        column.addView(create);

        Button show = Ui.button(main, "Show last handoff manifest");
        show.setOnClickListener(v -> showManifest());
        column.addView(show);

        column.addView(Ui.sectionLabel(main, "Manifest"));
        manifestView = Ui.mono(main, main.lastHandoff() == null
                ? "No handoff ZIP has been created in this session."
                : main.lastHandoff().manifestText());
        column.addView(manifestView);
        return column;
    }

    private void createHandoff() {
        Repo repo = main.selectedRepo();

        String projectName = repo == null ? "project" : repo.getName();
        String repoFullName = repo == null ? "(none)" : repo.getFullName();
        String branch = repo == null ? "(none)" : repo.getCurrentBranch();

        HandoffRequest request = new HandoffRequest()
                .setProjectName(projectName)
                .setReadme(buildReadme(projectName, repoFullName, branch))
                .setAiInstructions(main.pendingRequest().getAiInstructions())
                .setSourceCode(main.pendingRequest().getSourceCode())
                .setVersionInfo(versionInfo(projectName))
                .setCommitInfo(commitInfo(repo))
                .setTestResults(testResults())
                .setBuildLog(main.lastBuildLog());

        if (main.buildRecord() != null && main.buildRecord().hasErrors()) {
            request.setErrorLog(main.buildRecord().getErrorOutput());
        } else {
            request.setErrorLog("No errors were recorded for the latest build.");
        }

        List<File> assets = new ArrayList<>(main.pendingRequest().getAssets());
        for (File asset : assets) {
            request.getAssets().add(asset);
        }

        // Source roots: locally staged and uploaded files. A full GitHub tree
        // archive is not implemented in version 1 and is reported as unavailable.
        File uploads = main.services().fileTransfer().uploadsDir();
        if (uploads.exists()) {
            request.getSourceRoots().add(uploads);
        }

        BuildRecord record = main.buildRecord();
        if (record != null && record.getApkPath() != null) {
            File apk = new File(record.getApkPath());
            if (apk.exists()) {
                request.setApkFile(apk);
            }
        }

        List<String> unavailable = new ArrayList<>();
        if (request.getSourceRoots().isEmpty()) {
            unavailable.add("source tree (no locally staged files)");
        }
        if (request.getApkFile() == null) {
            unavailable.add("APK (no successful build artifact)");
        }
        if (request.getAiInstructions().trim().isEmpty()) {
            unavailable.add("AI instructions (empty)");
        }
        if (request.getSourceCode().trim().isEmpty()) {
            unavailable.add("pasted source code (empty)");
        }

        try {
            HandoffResult result = main.services().zip()
                    .createHandoffZip(request, main.services().fileTransfer().exportsDir());
            main.setLastHandoff(result);
            log("handoff", "Created " + result.getFile().getName() + " with "
                    + result.getContents().size() + " entries", false);
            String message = result.manifestText();
            if (!unavailable.isEmpty()) {
                message += "\nNote: some sections were not available and are listed above as not included.";
            }
            Confirmations.info(main, "Handoff ZIP created", message);
            main.replace(new HandoffPackageScreen(main));
        } catch (Exception e) {
            log("handoff", "Handoff ZIP failed: " + e.getMessage(), false);
            Confirmations.info(main, "Handoff ZIP failed",
                    "The handoff ZIP could not be created: " + e.getMessage());
        }
    }

    private void showManifest() {
        if (main.lastHandoff() == null) {
            Confirmations.info(main, "No handoff yet",
                    "Create a handoff ZIP first. Nothing has been generated in this session.");
            return;
        }
        Confirmations.info(main, "Handoff manifest", main.lastHandoff().manifestText());
    }

    private String buildReadme(String projectName, String repoFullName, String branch) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(projectName).append(" - Touch Developer handoff\n\n");
        sb.append("Generated: ").append(Formats.timestamp()).append("\n");
        sb.append("Repository: ").append(repoFullName).append("\n");
        sb.append("Branch: ").append(branch).append("\n\n");
        sb.append("## What this package contains\n");
        sb.append("- Full source folders that were staged locally\n");
        sb.append("- The debug APK when a build produced one\n");
        sb.append("- AI instructions and pasted source code\n");
        sb.append("- Build log, error log, and test results\n");
        sb.append("- Version and commit information\n");
        sb.append("- SHA-256 checksum of the package\n\n");
        sb.append("## How to build\n");
        sb.append("Open the project in Android Studio, or run ./gradlew assembleDebug.\n");
        sb.append("Required: Java 17 and Android SDK platform 34 or newer.\n\n");
        sb.append("## Safety\n");
        sb.append("No credentials are included in this package. Credentials live only in encrypted "
                + "Android storage.\n");
        return sb.toString();
    }

    private String versionInfo(String projectName) {
        return "App package: com.touchdeveloper.app\n"
                + "App version: 1.0 (versionCode 1)\n"
                + "Handoff format: version 1\n"
                + "Project: " + projectName + "\n"
                + "Generated: " + Formats.timestamp() + "\n";
    }

    private String commitInfo(Repo repo) {
        if (repo == null) {
            return "No repository selected, so no commit information is available.";
        }
        return "Repository: " + repo.getFullName() + "\n"
                + "Branch: " + repo.getCurrentBranch() + "\n"
                + "Latest commit: " + repo.shortCommit() + "\n"
                + "Commit message: " + (repo.getLatestCommitMessage() == null ? "(unknown)"
                : repo.getLatestCommitMessage()) + "\n"
                + "Build status: " + repo.getBuildStatus().display() + "\n";
    }

    private String testResults() {
        BuildRecord record = main.buildRecord();
        if (record == null) {
            return "No build was run in this session, so no test results are available.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Build id: ").append(record.getId()).append('\n');
        sb.append("Build status: ").append(record.getStatus().display()).append('\n');
        sb.append("Start: ").append(record.startText()).append('\n');
        sb.append("End: ").append(record.endText()).append('\n');
        sb.append(record.isDemo()
                ? "Note: this build was a simulated demo build, so these results are not device test results.\n"
                : "Owner device testing is still required before release.\n");
        return sb.toString();
    }

    private void log(String category, String message, boolean confirmed) {
        main.services().activityLog().record(category, message, confirmed);
    }
}
