package com.touchdeveloper.app.ui.screens;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.touchdeveloper.app.MainActivity;
import com.touchdeveloper.app.model.RepoFile;
import com.touchdeveloper.app.safety.Confirmations;
import com.touchdeveloper.app.safety.Protection;
import com.touchdeveloper.app.ui.FileListController;
import com.touchdeveloper.app.ui.Screen;
import com.touchdeveloper.app.ui.TaskRunner;
import com.touchdeveloper.app.ui.Ui;
import com.touchdeveloper.app.util.Result;

import java.io.File;
import java.util.List;

/**
 * Repository File Screen: lists the selected repository's files and folders and
 * opens a file action menu for each entry.
 *
 * The screen owns one {@link FileListController}, so a single automatic load runs
 * on first show and every later load is user-initiated. The list is rendered once
 * per load; the screen is never rebuilt from inside a load, which is what previously
 * caused an endless reload loop.
 *
 * Destructive actions are confirmed, protected artifacts are guarded, and actions
 * that version 1 does not implement report "Coming in next version" instead of
 * pretending to work.
 */
public class FileScreen extends Screen {

    private static final String COMING_SOON = "Coming in next version";

    private String currentPath = "";
    private final FileListController controller = new FileListController();

    public FileScreen(MainActivity main) {
        super(main);
    }

    @Override
    public String title() {
        if (main.selectedRepo() == null) {
            return "Repository";
        }
        return main.selectedRepo().getName();
    }

    @Override
    public View create() {
        LinearLayout column = main.column();

        if (main.selectedRepo() == null) {
            column.addView(Ui.body(main, "No repository is selected. Go back and choose one."));
            return column;
        }

        column.addView(Ui.body(main, "Repository: " + main.selectedRepo().getFullName()
                + "   Branch: " + main.selectedRepo().getCurrentBranch()));
        column.addView(Ui.body(main, main.services().gitHub().modeDescription()));

        LinearLayout actions = new LinearLayout(main);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button repoActions = Ui.button(main, "Repository actions");
        repoActions.setOnClickListener(v -> main.show(new RepoActionScreen(main)));
        actions.addView(repoActions);

        Button up = Ui.button(main, "Up one level");
        up.setOnClickListener(v -> {
            int slash = currentPath.lastIndexOf('/');
            currentPath = slash < 0 ? "" : currentPath.substring(0, slash);
            load();
        });
        actions.addView(up);

        Button refresh = Ui.button(main, "Refresh");
        refresh.setOnClickListener(v -> load());
        actions.addView(refresh);

        column.addView(actions);
        column.addView(Ui.body(main, "Path: /" + currentPath));

        column.addView(Ui.sectionLabel(main, "Files and folders"));
        renderEntries(column);
        return column;
    }

    @Override
    public void onShown() {
        // One automatic load per screen instance. A completed load, successful or
        // not, is never retried automatically.
        if (main.selectedRepo() != null && controller.shouldAutoLoad()) {
            load();
        }
    }

    private void load() {
        if (!controller.beginRequest()) {
            return;
        }
        TaskRunner.run(main, "GitHub", "Loading files\u2026",
                () -> main.services().gitHub().listFiles(main.selectedRepo(), currentPath),
                result -> {
                    controller.onResult(result);
                    // Re-render in place; do not replace the screen, or onShown()
                    // would start another load and the screen would never settle.
                    // If the user navigated away, drop the result instead of
                    // painting it over the screen they moved to.
                    if (main.currentScreen() == this) {
                        main.refreshCurrent();
                    }
                });
    }

    /**
     * Renders the current state. An empty, successfully loaded list is reported as
     * an empty folder; a failure is reported once and Refresh stays available.
     */
    private void renderEntries(LinearLayout column) {
        List<RepoFile> entries = controller.entries();
        if (!entries.isEmpty()) {
            for (RepoFile entry : entries) {
                column.addView(fileRow(entry));
            }
            return;
        }
        if (controller.isLoading()) {
            column.addView(Ui.body(main, "Loading files\u2026"));
        } else if (!controller.error().isEmpty()) {
            column.addView(Ui.error(main, controller.error()));
            column.addView(Ui.body(main, "Tap Refresh to try again."));
        } else if (controller.isLoaded()) {
            column.addView(Ui.body(main, "This folder is empty."));
        } else {
            column.addView(Ui.body(main, "Loading files\u2026"));
        }
    }

    private View fileRow(RepoFile file) {
        LinearLayout row = new LinearLayout(main);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundColor(Color.WHITE);
        row.setPadding(Ui.dp(main, 12), Ui.dp(main, 8), Ui.dp(main, 12), Ui.dp(main, 8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(Ui.dp(main, 12), Ui.dp(main, 3), Ui.dp(main, 12), Ui.dp(main, 3));
        row.setLayoutParams(params);

        TextView name = new TextView(main);
        name.setText(file.displayName() + (file.isDirectory() ? "" : "   " + file.getSize() + " B"));
        name.setTextSize(15f);
        row.addView(name);

        String flags = "";
        if (Protection.isProtectedName(file.getName())) {
            flags += "protected artifact  ";
        }
        if (file.isStagedLocally()) {
            flags += "staged locally  ";
        }
        if (!file.isIncludedInBuild()) {
            flags += "excluded from build  ";
        }
        if (!flags.isEmpty()) {
            row.addView(Ui.body(main, flags.trim()));
        }

        row.setClickable(true);
        row.setOnClickListener(v -> {
            if (file.isDirectory()) {
                currentPath = file.getPath();
                load();
            } else {
                showFileMenu(file);
            }
        });
        return row;
    }

    // ---- file action menu ---------------------------------------------------

    private void showFileMenu(RepoFile file) {
        final String[] items = {
                "Download",
                "Open or preview",
                "Copy",
                "Paste or replace",
                "Rename",
                "Delete",
                "Upload replacement",
                "Compare versions",
                "Restore previous version",
                "Add to build",
                "Exclude from build",
                "Download as ZIP"
        };
        new AlertDialog.Builder(main)
                .setTitle(file.displayName())
                .setItems(items, (dialog, which) -> handleFileAction(items[which], file))
                .show();
    }

    private void handleFileAction(String action, RepoFile file) {
        switch (action) {
            case "Download":
                doDownload(file);
                break;
            case "Open or preview":
                doPreview(file);
                break;
            case "Copy":
                doCopy(file);
                break;
            case "Paste or replace":
                doPasteReplace(file);
                break;
            case "Rename":
                doRename(file);
                break;
            case "Delete":
                doDelete(file);
                break;
            case "Upload replacement":
                doUploadReplacement(file);
                break;
            case "Add to build":
                file.setIncludedInBuild(true);
                log("file", "Added " + file.getPath() + " to build", false);
                main.toast(file.getPath() + " will be included in the build.");
                main.refreshCurrent();
                break;
            case "Exclude from build":
                file.setIncludedInBuild(false);
                log("file", "Excluded " + file.getPath() + " from build", false);
                main.toast(file.getPath() + " will be excluded from the build.");
                main.refreshCurrent();
                break;
            case "Compare versions":
            case "Restore previous version":
                main.toast(action + ": " + COMING_SOON + ". Nothing was changed.");
                log("file", action + " requested for " + file.getPath() + " (" + COMING_SOON + ")", false);
                break;
            case "Download as ZIP":
                doDownloadZip(file);
                break;
            default:
                main.toast(COMING_SOON);
        }
    }

    private void doDownload(final RepoFile file) {
        TaskRunner.run(main, "GitHub", "Downloading\u2026",
                () -> main.services().gitHub().downloadFile(main.selectedRepo(), file),
                result -> {
                    log("file", "Download " + file.getPath() + " (" + result.display() + ")", false);
                    if (result.data == null) {
                        main.toast(result.display());
                        return;
                    }
                    Result<File> saved = main.services().fileTransfer()
                            .writeExportBytes(shortName(file.getPath()), result.data);
                    if (!saved.ok) {
                        main.toast(saved.display());
                        return;
                    }
                    Confirmations.info(main, "Download complete",
                            "Saved to app storage:\n" + saved.data.getAbsolutePath()
                                    + "\n\nThis is a local copy. Pushing it to GitHub is a separate, "
                                    + "confirmed step.");
                });
    }

    private void doDownloadZip(RepoFile file) {
        main.toast("Download as ZIP for a single file: " + COMING_SOON
                + ". Use Create source ZIP in Repository actions for a project archive.");
        log("file", "Download ZIP requested for " + file.getPath() + " (" + COMING_SOON + ")", false);
    }

    private void doPreview(final RepoFile file) {
        TaskRunner.run(main, "GitHub", "Loading preview\u2026",
                () -> main.services().gitHub().readFile(main.selectedRepo(), file),
                result -> {
                    log("file", "Preview " + file.getPath() + " (" + result.display() + ")", false);
                    if (result.data == null) {
                        main.toast(result.display());
                        return;
                    }
                    String body = result.demo ? "[DEMO] preview data\n\n" + result.data : result.data;
                    new AlertDialog.Builder(main)
                            .setTitle("Preview: " + file.displayName())
                            .setMessage(body.length() > 4000
                                    ? body.substring(0, 4000) + "\n... [truncated]" : body)
                            .setPositiveButton("Close", (d, w) -> d.dismiss())
                            .show();
                });
    }

    private void doCopy(final RepoFile file) {
        TaskRunner.run(main, "GitHub", "Fetching file\u2026",
                () -> main.services().gitHub().readFile(main.selectedRepo(), file),
                result -> {
                    String content = result.data;
                    if (content == null) {
                        content = file.getPath();
                    }
                    ClipboardManager clipboard =
                            (ClipboardManager) main.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard == null) {
                        main.toast("Clipboard is unavailable on this device.");
                        return;
                    }
                    clipboard.setPrimaryClip(ClipData.newPlainText("TouchDeveloper", content));
                    log("file", "Copied " + file.getPath() + " to clipboard (" + result.display() + ")",
                            false);
                    main.toast("Copied to clipboard.");
                });
    }

    private void doPasteReplace(RepoFile file) {
        EditText input = new EditText(main);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(6);
        input.setText(file.getTextContent() == null ? "" : file.getTextContent());

        AlertDialog dialog = new AlertDialog.Builder(main)
                .setTitle("Paste or replace: " + file.displayName())
                .setView(input)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Stage", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String text = input.getText().toString();
                    Result<String> staged = main.services().gitHub()
                            .stageFile(main.selectedRepo(), file.getPath(), text);
                    file.setStagedLocally(true);
                    log("file", "Staged replace " + file.getPath() + " (" + staged.display() + ")", false);
                    dialog.dismiss();
                    Confirmations.info(main, "Staged locally",
                            staged.display() + "\n\nNothing has been sent to GitHub. Run Commit changes, "
                                    + "then Push to GitHub, to publish it.");
                    main.refreshCurrent();
                }));
        dialog.show();
    }

    private void doRename(RepoFile file) {
        EditText input = new EditText(main);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(file.getName());

        AlertDialog dialog = new AlertDialog.Builder(main)
                .setTitle("Rename " + file.displayName())
                .setView(input)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Stage rename", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) {
                        main.toast("Enter a new file name.");
                        return;
                    }
                    String parent = "";
                    int slash = file.getPath().lastIndexOf('/');
                    if (slash >= 0) {
                        parent = file.getPath().substring(0, slash + 1);
                    }
                    String newPath = parent + newName;
                    Result<String> result = main.services().gitHub()
                            .stageRename(main.selectedRepo(), file, newPath);
                    log("file", "Staged rename " + file.getPath() + " -> " + newPath, false);
                    dialog.dismiss();
                    Confirmations.info(main, "Rename staged", result.display());
                    main.refreshCurrent();
                }));
        dialog.show();
    }

    private void doDelete(RepoFile file) {
        if (Protection.isProtectedName(file.getName())) {
            Confirmations.info(main, "Protected artifact", Protection.reasonFor(file.getName())
                    + "\n\nDeleting protected artifacts is blocked by default in version 1. "
                    + "Remove the protection in a later version if you are certain.");
            log("file", "Blocked delete of protected artifact " + file.getPath(), false);
            return;
        }
        Confirmations.confirmDestructive(main, "Delete file?",
                "Delete " + file.getPath() + "?\n\nThis stages a deletion. It is only removed from GitHub "
                        + "when you Commit and Push.",
                () -> {
                    Result<String> result = main.services().gitHub().stageDelete(main.selectedRepo(), file);
                    log("file", "Staged delete of " + file.getPath() + " (" + result.display() + ")", true);
                    Confirmations.info(main, "Deletion staged", result.display());
                    main.refreshCurrent();
                });
    }

    private void doUploadReplacement(RepoFile file) {
        main.pickFile(new MainActivity.PickerCallback() {
            @Override
            public void onPicked(android.net.Uri uri) {
                Result<File> imported = main.services().fileTransfer().importUri(uri, file.getName());
                log("file", "Upload replacement for " + file.getPath() + " (" + imported.display() + ")", false);
                if (!imported.ok) {
                    main.toast(imported.display());
                    return;
                }
                Result<String> read = main.services().fileTransfer().readText(imported.data);
                if (!read.ok) {
                    main.toast(read.display() + " The file was imported and saved locally; "
                            + "staging binary replacements to GitHub is not implemented in version 1.");
                    return;
                }
                Result<String> staged = main.services().gitHub()
                        .stageFile(main.selectedRepo(), file.getPath(), read.data);
                file.setStagedLocally(true);
                Confirmations.info(main, "Replacement staged", staged.display()
                        + "\n\nSource: " + imported.data.getName()
                        + "\nNothing has been sent to GitHub yet.");
                main.refreshCurrent();
            }

            @Override
            public void onCancelled() {
                main.toast("Upload cancelled. Nothing was changed.");
            }
        });
    }

    private String shortName(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    private void log(String category, String message, boolean confirmed) {
        main.services().activityLog().record(category, message, confirmed);
    }
}
