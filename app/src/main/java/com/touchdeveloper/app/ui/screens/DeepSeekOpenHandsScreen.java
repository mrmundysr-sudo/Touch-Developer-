package com.touchdeveloper.app.ui.screens;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.touchdeveloper.app.MainActivity;
import com.touchdeveloper.app.build.BuildRequest;
import com.touchdeveloper.app.ui.Screen;
import com.touchdeveloper.app.ui.Ui;
import com.touchdeveloper.app.util.Result;

/** OpenHands workspace. Uses the already-saved OpenHands connection; never asks for tokens. */
public final class DeepSeekOpenHandsScreen extends Screen {
    private String jobReference = "";
    private String latestStatus = "No OpenHands session started.";

    public DeepSeekOpenHandsScreen(MainActivity main) { super(main); }
    @Override public String title() { return "DeepSeek / OpenHands"; }

    @Override public View create() {
        LinearLayout column = main.column();
        SharedPreferences prefs = main.getSharedPreferences("openhands_draft", 0);
        column.addView(Ui.heading(main, "AI Developer — DeepSeek / OpenHands"));
        column.addView(Ui.body(main, "Uses your saved OpenHands connection. OpenHands chooses the model under your account settings; free DeepSeek API access is not yet verified. GitHub remains the source of truth."));
        EditText repo = new EditText(main);
        repo.setSingleLine(true);
        repo.setHint("GitHub owner/repository");
        repo.setText(prefs.getString("repo", "mrmundysr-sudo/TouchV"));
        column.addView(repo);
        EditText branch = new EditText(main);
        branch.setSingleLine(true);
        branch.setHint("Existing work branch (never main)");
        branch.setText(prefs.getString("branch", "deepseek-premium-pass"));
        column.addView(branch);
        EditText task = new EditText(main);
        task.setHint("Coding assignment / premium pass");
        task.setMinLines(5);
        task.setGravity(android.view.Gravity.TOP);
        task.setText(prefs.getString("task", ""));
        column.addView(task);
        TextView status = Ui.body(main, latestStatus);
        Button launch = Ui.button(main, "Start OpenHands coding session");
        column.addView(launch);
        column.addView(status);
        launch.setOnClickListener(v -> {
            String repository = repo.getText().toString().trim();
            String working = branch.getText().toString().trim();
            String assignment = task.getText().toString().trim();
            if (!repository.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")
                    || !working.matches("[A-Za-z0-9_./-]+") || working.contains("..")
                    || working.equalsIgnoreCase("main") || working.equalsIgnoreCase("master")
                    || assignment.isEmpty()) {
                main.toast("Select a repository, a safe working branch, and instructions.");
                return;
            }
            if (!main.services().openHands().isConfigured()) {
                main.toast("OpenHands is not connected. Check Setup."); return;
            }
            prefs.edit().putString("repo", repository).putString("branch", working)
                    .putString("task", assignment).apply();
            new AlertDialog.Builder(main)
                .setTitle("Start OpenHands session?")
                .setMessage("This submits a real coding task to your OpenHands account. Model choice, free access, limits, and possible charges are controlled by OpenHands and have not been verified here. Continue only after checking your account settings.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Start", (d, w) -> {
                    launch.setEnabled(false);
                    status.setText("Requesting OpenHands session...");
                    BuildRequest request = new BuildRequest().setRepoFullName(repository)
                            .setBranch(working).setAiInstructions(assignment);
                    new Thread(() -> {
                        Result<String> result = main.services().openHands().sendInstructions(request);
                        main.runOnUiThread(() -> {
                            launch.setEnabled(true);
                            latestStatus = result.display();
                            status.setText(latestStatus);
                            if (result.ok && result.data != null) {
                                jobReference = result.data;
                                prefs.edit().putString("job_reference", jobReference).apply();
                            }
                        });
                    }).start();
                }).show();
        });
        Button refresh = Ui.button(main, "Refresh OpenHands session status");
        column.addView(refresh);
        refresh.setOnClickListener(v -> {
            String ref = jobReference.isEmpty() ? prefs.getString("job_reference", "") : jobReference;
            if (ref.isEmpty()) { main.toast("No session reference saved yet."); return; }
            refresh.setEnabled(false);
            status.setText("Checking OpenHands...");
            new Thread(() -> {
                Result<String> result = main.services().openHands().buildStatus(ref);
                main.runOnUiThread(() -> {
                    refresh.setEnabled(true);
                    latestStatus = result.display();
                    status.setText(latestStatus);
                    if (result.ok && result.data != null
                            && result.data.startsWith("conversation:")) {
                        jobReference = result.data;
                        prefs.edit().putString("job_reference", jobReference).apply();
                    }
                });
            }).start();
        });
        column.addView(Ui.body(main, "This is a coding session, not an APK build. Verify commits on your chosen GitHub branch before using GitHub Actions to build. Session messages, completion checks, and automatic branch synchronization still require integration testing."));
        return column;
    }
}
