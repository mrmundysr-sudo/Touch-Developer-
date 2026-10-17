package com.touchdeveloper.app.ui.screens;

import android.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.touchdeveloper.app.MainActivity;
import com.touchdeveloper.app.model.Repo;
import com.touchdeveloper.app.ui.Screen;
import com.touchdeveloper.app.ui.TaskRunner;
import com.touchdeveloper.app.ui.Ui;
import com.touchdeveloper.app.safety.Confirmations;
import java.util.ArrayList;
import java.util.List;

/** Branch operations are scoped to the currently selected repository. */
public class BranchManagerScreen extends Screen {
    private List<String> branches = new ArrayList<>();
    private boolean loaded;
    private String error = "";

    public BranchManagerScreen(MainActivity main) { super(main); }
    @Override public String title() { return "Branches"; }

    @Override public View create() {
        LinearLayout column = main.column();
        Repo repo = main.selectedRepo();
        if (repo == null) { column.addView(Ui.error(main, "Select a repository first.")); return column; }
        column.addView(Ui.body(main, "Repository: " + repo.getFullName()));
        column.addView(Ui.body(main, "ACTIVE BRANCH: " + repo.getCurrentBranch()));
        Button refresh = Ui.button(main, "Refresh branches");
        refresh.setOnClickListener(v -> load());
        column.addView(refresh);
        Button create = Ui.button(main, "Create new branch");
        create.setOnClickListener(v -> createBranch());
        column.addView(create);
        if (!error.isEmpty()) column.addView(Ui.error(main, error));
        if (!loaded) column.addView(Ui.body(main, "Loading branches..."));
        for (String name : branches) {
            Button row = Ui.button(main, (name.equals(repo.getCurrentBranch()) ? "✓ ACTIVE  " : "Switch to  ") + name);
            row.setOnClickListener(v -> switchBranch(name));
            column.addView(row);
            if (!name.equals(repo.getCurrentBranch())) {
                Button delete = Ui.button(main, "Delete branch: " + name);
                delete.setOnClickListener(v -> Confirmations.confirmDestructive(main,
                        "Delete GitHub branch?", "Permanently delete " + name + " from " + repo.getFullName()
                                + "? This cannot be undone.", () -> deleteBranch(name)));
                column.addView(delete);
            }
        }
        return column;
    }

    @Override public void onShown() { if (!loaded) load(); }

    private void load() {
        Repo repo = main.selectedRepo();
        if (repo == null) return;
        TaskRunner.run(main, "GitHub", "Loading branches...",
                () -> main.services().gitHub().listBranches(repo), result -> {
                    loaded = true;
                    error = result.ok ? "" : result.display();
                    if (result.ok && result.data != null) branches = new ArrayList<>(result.data);
                    if (main.currentScreen() == this) main.refreshCurrent();
                });
    }

    private void switchBranch(String name) {
        Repo repo = main.selectedRepo();
        if (name.equals(repo.getCurrentBranch())) return;
        new AlertDialog.Builder(main).setTitle("Switch branch?")
                .setMessage("Switch " + repo.getFullName() + " to " + name
                        + "? Subsequent file browsing, uploads and builds will target this branch.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Switch", (d, w) -> {
                    repo.setCurrentBranch(name);
                    main.pendingRequest().setBranch(name);
                    TaskRunner.run(main, "GitHub", "Refreshing selected branch...",
                            () -> main.services().gitHub().refresh(repo), result -> {
                                if (!result.ok) error = result.display();
                                main.refreshCurrent();
                            });
                }).show();
    }

    private void createBranch() {
        EditText input = new EditText(main);
        input.setSingleLine(true);
        input.setHint("feature/my-branch");
        new AlertDialog.Builder(main).setTitle("New branch from " + main.selectedRepo().getCurrentBranch())
                .setView(input).setNegativeButton("Cancel", null)
                .setPositiveButton("Create", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.matches("[A-Za-z0-9][A-Za-z0-9._/-]*") || name.contains("..")
                            || name.contains("//") || name.endsWith(".") || name.endsWith("/")) {
                        Confirmations.info(main, "Invalid branch name", "Enter a valid GitHub branch name.");
                        return;
                    }
                    TaskRunner.run(main, "GitHub", "Creating branch...",
                            () -> main.services().gitHub().createBranch(main.selectedRepo(), name),
                            result -> {
                                Confirmations.info(main, result.ok ? "Branch created" : "Creation failed", result.display());
                                if (result.ok) load();
                            });
                }).show();
    }

    private void deleteBranch(String name) {
        TaskRunner.run(main, "GitHub", "Deleting branch...",
                () -> main.services().gitHub().deleteBranch(main.selectedRepo(), name),
                result -> {
                    Confirmations.info(main, result.ok ? "Branch deleted" : "Deletion failed", result.display());
                    if (result.ok) load();
                });
    }
}
