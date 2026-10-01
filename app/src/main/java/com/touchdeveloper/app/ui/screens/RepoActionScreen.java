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
                Result<String> staged = main.services().gitHub()
                        .stageFileBytes(main.selectedRepo(), selectedName, bytes);
                log("repo", "Upload staged " + selectedName + " (" + staged.display() + ")", false);
                Confirmations.info(main, "Upload staged",
                        staged.display() + "\n\nSource: " + selectedName
                                + "\nCommit and push to publish it.");
            }
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
        Confirmations.info(main, "Open in OpenHands",
                result.display() + "\n\nLink: " + (result.data == null ? "not available" : result.data)
                        + "\n\nOpening the link in a browser is a manual step in version 1.");
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
