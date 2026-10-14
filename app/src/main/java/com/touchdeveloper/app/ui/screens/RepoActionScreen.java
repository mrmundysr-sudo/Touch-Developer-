package com.touchdeveloper.app.ui.screens;

import android.app.AlertDialog;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.touchdeveloper.app.MainActivity;
import com.touchdeveloper.app.files.HandoffRequest;
import com.touchdeveloper.app.files.HandoffResult;
import com.touchdeveloper.app.model.BuildRecord;
import com.touchdeveloper.app.model.CommitChange;
import com.touchdeveloper.app.model.Repo;
import com.touchdeveloper.app.safety.Confirmations;
import com.touchdeveloper.app.ui.Screen;
import com.touchdeveloper.app.ui.TaskRunner;
import com.touchdeveloper.app.ui.Ui;
import com.touchdeveloper.app.ui.UploadFlow;
import com.touchdeveloper.app.util.Result;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository Action Menu: pull, upload, branch, commit, push, source ZIP, build
 * APK, download APK/project, open in OpenHands, and delete repository.
 *
 * Every destructive or public action requires confirmation, and every result is
 * reported exactly as the service returned it.
 */
public class RepoActionScreen extends Screen {

    public RepoActionScreen(MainActivity main) {
        super(main);
    }

    @Override
    public String title() {
        return "Repository actions";
    }

    @Override
    public View create() {
        LinearLayout column = main.column();
        Repo repo = main.selectedRepo();
        if (repo == null) {
            column.addView(Ui.body(main, "No repository is selected."));
            return column;
        }

        column.addView(Ui.body(main, "Target: " + repo.getFullName() + "  (" + repo.getCurrentBranch() + ")"));
        column.addView(Ui.body(main, main.services().gitHub().modeDescription()));

        column.addView(button("Pull latest changes", v -> pullLatest()));
        column.addView(button("Upload files", v -> uploadFiles()));
        column.addView(button("Create branch", v -> createBranch()));
        column.addView(button("Commit changes", v -> commitChanges()));
        column.addView(button("Push to GitHub", v -> pushToGitHub()));
        column.addView(button("Create source ZIP", v -> createSourceZip()));
        column.addView(button("Build APK", v -> main.show(new AiBuildPromptScreen(main))));
        column.addView(button("Download latest APK", v -> downloadLatestApk()));
        column.addView(button("Download complete project", v -> main.show(new HandoffPackageScreen(main))));
        column.addView(button("Open in OpenHands", v -> openInOpenHands()));
        column.addView(button("Delete repository", v -> deleteRepository()));
        return column;
    }

    private Button button(String text, View.OnClickListener listener) {
        Button button = Ui.button(main, text);
        button.setOnClickListener(listener);
        return button;
    }

    private void pullLatest() {
        TaskRunner.run(main, "GitHub", "Pulling latest changes\u2026",
                () -> main.services().gitHub().pullLatest(main.selectedRepo()),
                result -> {
                    log("repo", "Pull latest (" + result.display() + ")", false);
                    main.toast(result.display());
                    if (result.ok) {
                        rebuild();
                    }
                });
    }

    private void uploadFiles() {
        // Open Android's normal system picker immediately. The previous version
        // incorrectly required a repository path before the picker appeared.
        main.pickFile(new MainActivity.PickerCallback() {
            @Override public void onCancelled() {
                main.toast("Upload cancelled. Nothing was changed.");
            }

            @Override public void onPicked(android.net.Uri uri) {
                String selectedName = main.services().fileTransfer().displayName(uri);
                if (selectedName == null || selectedName.trim().isEmpty()) {
                    selectedName = "upload.bin";
                }
                Result<File> imported = main.services().fileTransfer().importUri(uri, selectedName);
                if (!imported.ok) {
                    main.toast(imported.display());
                    return;
                }
                byte[] bytes;
                try {
                    bytes = java.nio.file.Files.readAllBytes(imported.data.toPath());
                } catch (Exception e) {
                    main.toast("Could not read imported file: " + e.getMessage());
                    return;
                }
                if (selectedName.toLowerCase(java.util.Locale.US).endsWith(".zip")) {
                    final String zipName = selectedName;
                    final byte[] zipBytes = bytes;
                    new AlertDialog.Builder(main)
                            .setTitle("Upload ZIP")
                            .setMessage("Upload " + zipName + " unchanged, or extract its files into the selected repository branch? Extracting will stage each file; review before pushing.")
                            .setNegativeButton("Cancel", (d, w) -> {})
                            .setNeutralButton("Upload ZIP as file", (d, w) -> stageUpload(zipName, zipBytes))
                            .setPositiveButton("Extract files", (d, w) -> extractZip(imported.data))
                            .show();
                    return;
                }
                stageUpload(selectedName, bytes);
                return;
            }
        });
    }

    private void stageUpload(String name, byte[] bytes) {
        Result<String> staged = main.services().gitHub()
                .stageFileBytes(main.selectedRepo(), name, bytes);
        log("repo", "Staged " + name + ": " + staged.display(), false);
        Confirmations.info(main, staged.ok ? "Upload staged" : "Upload failed",
                staged.display() + "\nReview changes, then push to publish.");
    }

    private void extractZip(File zipFile) {
        TaskRunner.run(main, "ZIP", "Inspecting archive…", () -> {
            java.util.LinkedHashMap<String, byte[]> files = new java.util.LinkedHashMap<>();
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(zipFile)) {
                java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
                long total = 0;
                while (entries.hasMoreElements()) {
                    java.util.zip.ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory()) continue;
                    String path = entry.getName().replace('\\', '/');
                    if (path.startsWith("/") || path.contains("../") || path.equals("..")
                            || path.contains(":") || path.startsWith("__MACOSX/")
                            || path.endsWith(".DS_Store")) {
                        return Result.<java.util.LinkedHashMap<String, byte[]>>failure("Unsafe ZIP path: " + path);
                    }
                    if (files.size() >= 2000) return Result.failure("ZIP has over 2,000 files.");
                    try (java.io.InputStream in = zip.getInputStream(entry);
                         java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                        byte[] buffer = new byte[8192];
                        int n;
                        while ((n = in.read(buffer)) != -1) {
                            total += n;
                            if (total > 150L * 1024 * 1024)
                                return Result.failure("ZIP expands beyond 150 MiB.");
                            out.write(buffer, 0, n);
                        }
                        files.put(path, out.toByteArray());
                    }
                }
            } catch (Exception e) {
                return Result.failure("Cannot extract ZIP: " + e.getMessage());
            }
            if (files.isEmpty()) return Result.failure("ZIP contains no files.");
            // Strip wrapper directories until the Android project root is reached.
            String prefix = "";
            for (int depth = 0; depth < 6; depth++) {
                boolean hasRoot = files.containsKey(prefix + "settings.gradle")
                        || files.containsKey(prefix + "settings.gradle.kts")
                        || files.containsKey(prefix + "build.gradle");
                if (hasRoot) break;
                String child = null;
                boolean oneFolder = true;
                for (String p : files.keySet()) {
                    if (!p.startsWith(prefix)) { oneFolder = false; break; }
                    String remainder = p.substring(prefix.length());
                    int slash = remainder.indexOf('/');
                    if (slash < 0) { oneFolder = false; break; }
                    String segment = remainder.substring(0, slash);
                    if (child == null) child = segment;
                    else if (!child.equals(segment)) { oneFolder = false; break; }
                }
                if (!oneFolder || child == null) break;
                prefix += child + "/";
            }
            java.util.LinkedHashMap<String, byte[]> normalized = new java.util.LinkedHashMap<>();
            for (java.util.Map.Entry<String, byte[]> e : files.entrySet()) {
                normalized.put(e.getKey().substring(prefix.length()), e.getValue());
            }
            return Result.success("Ready", normalized);
        }, result -> {
            if (!result.ok || result.data == null) {
                Confirmations.info(main, "ZIP extraction failed", result.display());
                return;
            }
            java.util.LinkedHashMap<String, byte[]> files = result.data;
            StringBuilder preview = new StringBuilder();
            preview.append(files.size()).append(" files will be staged on ")
                    .append(main.selectedRepo().getCurrentBranch()).append(".\\n\\n");
            int shown = 0;
            for (String path : files.keySet()) {
                if (shown++ == 18) { preview.append("…and more"); break; }
                preview.append(path).append('\n');
            }
            new AlertDialog.Builder(main)
                    .setTitle("Confirm extracted paths")
                    .setMessage(preview.toString())
                    .setNegativeButton("Cancel", (d, w) -> {})
                    .setPositiveButton("Stage files", (d, w) -> {
                        for (java.util.Map.Entry<String, byte[]> e : files.entrySet()) {
                            main.services().gitHub().stageFileBytes(main.selectedRepo(), e.getKey(), e.getValue());
                        }
                        Confirmations.info(main, "Files staged",
                                files.size() + " files staged. Review changes and push to GitHub.");
                    }).show();
        });
    }

    private void createBranch() {
        EditText input = new EditText(main);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("new-branch-name");
        new AlertDialog.Builder(main)
                .setTitle("Create branch")
                .setView(input)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Review", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        main.toast("Enter a branch name.");
                        return;
                    }
                    Confirmations.confirmDestructive(main, "Create branch",
                            "Create branch " + name + " from " + main.selectedRepo().getCurrentBranch()
                                    + " in " + main.selectedRepo().getFullName() + "?",
                            () -> {
                                TaskRunner.run(main, "GitHub", "Creating branch\u2026",
                                        () -> main.services().gitHub().createBranch(main.selectedRepo(), name),
                                        result -> {
                                            log("repo", "Create branch " + name + " (" + result.display()
                                                    + ")", true);
                                            Confirmations.info(main,
                                                    result.ok ? "Branch created" : "Branch not created",
                                                    result.display());
                                            rebuild();
                                        });
                            });
                })
                .show();
    }

    private void commitChanges() {
        List<CommitChange> changes = main.services().gitHub().previewChanges(main.selectedRepo());
        List<String> lines = new ArrayList<>();
        for (CommitChange change : changes) {
            lines.add(change.display());
        }
        Confirmations.confirmCommit(main, main.selectedRepo().getCurrentBranch(), lines, () -> {
            EditText input = new EditText(main);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setText("Touch Developer changes");
            new AlertDialog.Builder(main)
                    .setTitle("Commit message")
                    .setView(input)
                    .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                    .setPositiveButton("Commit", (d, w) -> {
                        Result<String> result = main.services().gitHub()
                                .commitStaged(main.selectedRepo(), input.getText().toString());
                        log("repo", "Commit staged changes (" + result.display() + ")", true);
                        if (result.ok) {
                            main.selectedRepo().setBuildStatus(
                                    com.touchdeveloper.app.model.StatusLabel.COMMITTED);
                        }
                        Confirmations.info(main, result.ok ? "Committed" : "Commit not recorded",
                                result.display());
                        rebuild();
                    })
                    .show();
        });
    }

    private void pushToGitHub() {
        List<CommitChange> changes = main.services().gitHub().previewChanges(main.selectedRepo());
        List<String> lines = new ArrayList<>();
        for (CommitChange change : changes) {
            lines.add(change.display());
        }
        Confirmations.confirmPublicPush(main, main.selectedRepo().getFullName(),
                main.selectedRepo().getCurrentBranch(), lines, () -> {
                    TaskRunner.run(main, "GitHub", "Pushing to GitHub\u2026",
                            () -> main.services().gitHub()
                                    .pushStaged(main.selectedRepo(), "Touch Developer push"),
                            result -> {
                                log("repo", "Push to GitHub (" + result.display() + ")", true);
                                if (result.ok) {
                                    main.selectedRepo().setBuildStatus(
                                            com.touchdeveloper.app.model.StatusLabel.PUSHED);
                                }
                                Confirmations.info(main, result.ok ? "Push confirmed" : "Push not confirmed",
                                        result.display());
                                rebuild();
                            });
                });
    }

    private void createSourceZip() {
        try {
            List<File> roots = new ArrayList<>();
            File uploads = main.services().fileTransfer().uploadsDir();
            if (uploads.exists()) {
                roots.add(uploads);
            }
            File staged = main.services().fileTransfer().stagedDir();
            if (staged.exists()) {
                roots.add(staged);
            }
            File out = new File(main.services().fileTransfer().exportsDir(),
                    main.selectedRepo().getName() + "-source-" + com.touchdeveloper.app.util.Formats.timestamp()
                            + ".zip");
            if (roots.isEmpty()) {
                main.toast("No local files are staged or uploaded yet, so there is nothing to zip. "
                        + "Upload or stage files first.");
                return;
            }
            List<String> written = main.services().zip()
                    .zipDirectories(main.selectedRepo().getName(), roots, out);
            BuildRecord record = main.buildRecord();
            if (record != null) {
                record.setSourceZipPath(out.getAbsolutePath());
            }
            log("repo", "Created source ZIP " + out.getName() + " with " + written.size() + " entries", false);
            Confirmations.info(main, "Source ZIP created",
                    out.getName() + "\n" + written.size() + " entries\n" + out.getAbsolutePath()
                            + "\n\nThis ZIP contains locally staged/uploaded files. Exporting a complete "
                            + "GitHub tree archive is not implemented in version 1.");
        } catch (Exception e) {
            log("repo", "Source ZIP failed: " + e.getMessage(), false);
            Confirmations.info(main, "Source ZIP failed", "The ZIP could not be created: " + e.getMessage());
        }
    }

    private void downloadLatestApk() {
        BuildRecord record = main.buildRecord();
        if (record == null || record.getApkPath() == null) {
            Confirmations.info(main, "No APK available",
                    "No successful build has produced an APK in this session. Run Build APK first.\n\n"
                            + "Real APK download from a build service is not configured in version 1.");
            return;
        }
        File apk = new File(record.getApkPath());
        if (!apk.exists()) {
            Confirmations.info(main, "APK missing", "The recorded APK path no longer exists: "
                    + record.getApkPath());
            return;
        }
        if (record.isDemo()) {
            Confirmations.info(main, "Demo artifact",
                    "This is a [DEMO] placeholder file, not a real APK:\n" + apk.getAbsolutePath());
            return;
        }
        Confirmations.info(main, "Latest APK", apk.getAbsolutePath());
    }

    private void openInOpenHands() {
        Result<String> result = main.services().openHands().openRepositoryLink(main.selectedRepo());
        log("repo", "Open in OpenHands (" + result.display() + ")", false);
        if (!result.ok || result.data == null || result.data.trim().isEmpty()) {
            Confirmations.info(main, "OpenHands unavailable", result.display());
            return;
        }

        // Launch the configured OpenHands URL directly in the device browser.
        // The old implementation only displayed the URL and required the user
        // to copy/paste it manually.
        try {
            android.content.Intent intent = new android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(result.data));
            intent.addCategory(android.content.Intent.CATEGORY_BROWSABLE);
            main.startActivity(intent);
        } catch (Exception e) {
            Confirmations.info(main, "Could not open OpenHands",
                    "The browser could not open the prepared link.\n\n" + result.data
                            + "\n\nError: " + e.getMessage());
        }
    }

    private void deleteRepository() {
        Repo repo = main.selectedRepo();
        Confirmations.confirmRepositoryDeletion(main, repo.getFullName(), () ->
                TaskRunner.run(main, "GitHub", "Deleting repository\u2026",
                        () -> main.services().gitHub().deleteRepository(repo),
                        result -> {
                            log("repo", "Delete repository " + repo.getFullName() + " (" + result.display()
                                    + ")", true);
                            Confirmations.info(main,
                                    result.ok ? "Repository deleted" : "Repository not deleted",
                                    result.display() + "\n\nProtected artifacts (latest APK, handoff ZIP, "
                                            + "handoff instructions) were not deleted.");
                            if (result.ok) {
                                main.repos().remove(repo);
                                main.setSelectedRepo(null);
                                main.replace(new RepositoryDashboardScreen(main));
                            }
                        }));
    }

    private void rebuild() {
        main.replace(new RepoActionScreen(main));
    }

    private void log(String category, String message, boolean confirmed) {
        main.services().activityLog().record(category, message, confirmed);
    }
}
