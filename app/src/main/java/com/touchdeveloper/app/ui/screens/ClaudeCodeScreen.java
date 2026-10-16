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
import org.json.JSONArray;
import org.json.JSONObject;
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
                        String payload = "{\"ref\":\"main\",\"inputs\":{\"target_repo\":\""
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
        column.addView(Ui.heading(main, "Claude Workspace — live job status"));
        TextView live = Ui.body(main, "Choose Refresh to retrieve the latest GitHub Actions run. Activity details are available in GitHub logs.");
        column.addView(live);
        Button refresh = Ui.button(main, "Refresh Claude status");
        column.addView(refresh);
        refresh.setOnClickListener(v -> {
            String token = main.services().credentials().retrieve(ServiceLocator.KEY_GITHUB_TOKEN);
            if (token == null || token.trim().isEmpty()) { main.toast("Connect GitHub in Setup."); return; }
            refresh.setEnabled(false);
            live.setText("Checking GitHub Actions...");
            new Thread(() -> {
                Http.Response response = Http.get("https://api.github.com/repos/mrmundysr-sudo/Touch-Developer-/actions/workflows/claude-assignment.yml/runs?per_page=10", token);
                String message;
                try {
                    if (!response.ok()) throw new IllegalStateException(Http.statusText(response));
                    JSONArray runs = new JSONObject(response.body).getJSONArray("workflow_runs");
                    if (runs.length() == 0) message = "No Claude assignments found.";
                    else {
                        JSONObject run = runs.getJSONObject(0);
                        String state = run.optString("status", "unknown");
                        String conclusion = run.optString("conclusion", "");
                        String id = run.optString("id", "");
                        String created = run.optString("created_at", "");
                        message = "Latest run #" + run.optString("run_number", "?")
                                + "\\nStatus: " + state
                                + (conclusion.isEmpty() || conclusion.equals("null") ? "" : " (" + conclusion + ")")
                                + "\\nStarted: " + created
                                + "\\nRun ID: " + id
                                + "\\nUse View Jobs to inspect the detailed log.";
                    }
                } catch (Exception e) { message = "Unable to read Claude jobs: " + e.getMessage(); }
                String result = message;
                main.runOnUiThread(() -> { live.setText(result); refresh.setEnabled(true); });
            }).start();
        });
        EditText liveMessage = new EditText(main);
        liveMessage.setHint("Message Claude — instructions for a running assignment");
        liveMessage.setMinLines(2);
        column.addView(liveMessage);
        Button messageHelp = Ui.button(main, "Live messaging — setup required");
        messageHelp.setOnClickListener(v -> new AlertDialog.Builder(main)
            .setTitle("Interactive messages not connected yet")
            .setMessage("The current GitHub Actions Claude job is noninteractive. Sending text now would not reach Claude. The next runner milestone is authenticated queued instructions with delivery and acknowledgment. Your message stays in this box; it has not been sent.")
            .setPositiveButton("OK", null).show());
        column.addView(messageHelp);
        Button tools = Ui.button(main, "▲ Swipe-up tools (tap to expand)");
        column.addView(tools);
        LinearLayout drawer = main.column();
        drawer.setVisibility(View.GONE);
        column.addView(drawer);
        tools.setOnClickListener(v -> {
            boolean show = drawer.getVisibility() != View.VISIBLE;
            drawer.setVisibility(show ? View.VISIBLE : View.GONE);
            tools.setText(show ? "▼ Hide tools" : "▲ Swipe-up tools (tap to expand)");
        });
        Button pause = Ui.button(main, "Pause / Resume — requires session runner");
        pause.setOnClickListener(v -> main.toast("Pause/resume requires a checkpoint-capable Claude runner; no command sent."));
        drawer.addView(pause);
        Button stop = Ui.button(main, "Stop Assignment — open GitHub run");
        stop.setOnClickListener(v -> new AlertDialog.Builder(main)
            .setTitle("Stop assignment")
            .setMessage("Cancel the selected running workflow from its GitHub Actions page. This app does not cancel a run automatically yet.")
            .setNegativeButton("Back", null)
            .setPositiveButton("Open Actions", (d,w) -> open("https://github.com/mrmundysr-sudo/Touch-Developer-/actions/workflows/claude-assignment.yml")).show());
        drawer.addView(stop);
        Button changes = Ui.button(main, "View Changes — GitHub branch");
        changes.setOnClickListener(v -> {
            String target = repo.getText().toString().trim();
            String working = branch.getText().toString().trim();
            if (!target.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+") || !working.matches("[A-Za-z0-9_./-]+") || working.contains("..")) {
                main.toast("Choose a valid repository and branch."); return;
            }
            open("https://github.com/" + target + "/commits/" + Uri.encode(working, "/"));
        });
        drawer.addView(changes);
        Button apk = Ui.button(main, "Download APK — verified build artifacts");
        apk.setOnClickListener(v -> open("https://github.com/" + repo.getText().toString().trim() + "/actions"));
        drawer.addView(apk);
        column.addView(Ui.body(main, "Push alerts, questions from Claude, cost metering, and interactive control require a backend update. No messages or pause commands are sent by this preview."));
        Button runs = Ui.button(main, "View Claude jobs and logs on GitHub");
        runs.setOnClickListener(v -> open("https://github.com/mrmundysr-sudo/Touch-Developer-/actions/workflows/claude-assignment.yml"));
        column.addView(runs);
        column.addView(Ui.body(main, "Claude can edit code in approved repositories. Cross-repository commits require the TOUCH_TARGET_REPOS_TOKEN Actions secret and a successful Claude run. A submitted job is not a completed job. Check Actions for results."));
        return column;
    }
    private void open(String url) { main.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
}
