package com.touchdeveloper.app.ui.screens;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.app.AlertDialog;
import com.touchdeveloper.app.MainActivity;
import com.touchdeveloper.app.ServiceLocator;
import com.touchdeveloper.app.ui.Screen;
import com.touchdeveloper.app.ui.Ui;
import com.touchdeveloper.app.util.Http;
import com.touchdeveloper.app.util.Json;

/** Phone-first GitHub Actions Claude job launcher. No Anthropic key is stored in the APK. */
public final class ClaudeCodeScreen extends Screen {
    private static final String WORKFLOW = "claude-assignment.yml";
    public ClaudeCodeScreen(MainActivity main) { super(main); }
    @Override public String title() { return "AI Developer"; }

    @Override public View create() {
        LinearLayout column = main.column();
        SharedPreferences prefs = main.getSharedPreferences("claude_code_draft", 0);
        column.addView(Ui.heading(main, "Claude Code — GitHub Actions"));
        column.addView(Ui.body(main, "Run Claude without Codespaces browser login. The Anthropic API key stays centrally in Touch Developer Actions. Select an approved app repository and its working branch."));
        EditText repo = new EditText(main);
        repo.setSingleLine(true);
        repo.setHint("owner/repository");
        repo.setText(prefs.getString("repo", "mrmundysr-sudo/Touch-Developer-"));
        column.addView(repo);
        EditText branch = new EditText(main);
        branch.setSingleLine(true);
        branch.setHint("Working branch (not main)");
        branch.setText(prefs.getString("branch", "claude-code-mobile-control"));
        column.addView(branch);
        EditText assignment = new EditText(main);
        assignment.setHint("What should Claude work on?");
        assignment.setMinLines(5);
        assignment.setGravity(android.view.Gravity.TOP);
        assignment.setText(prefs.getString("assignment", ""));
        column.addView(assignment);
        Button launch = Ui.button(main, "Submit Claude assignment");
        column.addView(launch);
        TextView status = Ui.body(main, "Status: no job submitted. No API usage started.");
        column.addView(status);
        launch.setOnClickListener(v -> {
            String target = repo.getText().toString().trim();
            String working = branch.getText().toString().trim();
            String task = assignment.getText().toString().trim();
            if (!target.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")
                    || working.isEmpty() || working.equals("main") || working.equals("master")
                    || task.isEmpty()) {
                main.toast("Enter a repository, non-default branch, and assignment.");
                return;
            }
            if (!target.equals("mrmundysr-sudo/Touch-Developer-") && !target.equals("mrmundysr-sudo/TouchV")) {
                main.toast("Choose an approved repository: Touch Developer or TouchV.");
                return;
            }
            if (task.length() > 10000) { main.toast("Assignment must be under 10,000 characters."); return; }
            prefs.edit().putString("repo", target).putString("branch", working)
                    .putString("assignment", task).apply();
            String token = main.services().credentials().retrieve(ServiceLocator.KEY_GITHUB_TOKEN);
            if (token == null || token.trim().isEmpty()) {
                main.toast("Connect GitHub in Setup before submitting a job.");
                return;
            }
            new AlertDialog.Builder(main).setTitle("Run Claude Code?")
                .setMessage("This starts a GitHub Actions job that may consume paid Anthropic API credits. The workflow allows up to 6 hours and caps Claude API usage at $20 per assignment. Continue?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Run", (d, w) -> {
                    launch.setEnabled(false);
                    status.setText("Submitting to GitHub...");
                    new Thread(() -> {
                        String url = "https://api.github.com/repos/mrmundysr-sudo/Touch-Developer-/actions/workflows/" + WORKFLOW + "/dispatches";
                        String payload = "{\"ref\":\"claude-code-mobile-control\",\"inputs\":{\"target_repo\":\""
                                + Json.escape(target) + "\",\"target_branch\":\"" + Json.escape(working)
                                + "\",\"assignment\":\"" + Json.escape(task) + "\"}}";
                        Http.Response response = Http.post(url, token, payload);
                        main.runOnUiThread(() -> {
                            launch.setEnabled(true);
                            if (response.code == 204) {
                                status.setText("GitHub accepted the request. Open Actions to see whether Claude ran successfully.");
                            } else {
                                status.setText("GitHub did not accept the job (HTTP " + response.code + "). Check workflow permissions and Actions configuration.");
                            }
                        });
                    }).start();
                }).show();
        });
        Button runs = Ui.button(main, "View Claude jobs and logs on GitHub");
        runs.setOnClickListener(v -> open("https://github.com/mrmundysr-sudo/Touch-Developer-/actions/workflows/claude-assignment.yml"));
        column.addView(runs);
        column.addView(Ui.body(main, "Claude can edit code in approved repositories. TouchV changes are returned as a patch artifact until cross-repository GitHub write permission is configured; Anthropic needs no additional API key. Check Actions for results."));
        return column;
    }
    private void open(String url) { main.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
}
