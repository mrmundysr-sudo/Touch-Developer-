package com.touchdeveloper.app.ui.screens;

import android.content.Intent;
import android.content.SharedPreferences;
import android.text.InputType;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.touchdeveloper.app.MainActivity;
import com.touchdeveloper.app.ui.Screen;
import com.touchdeveloper.app.ui.Ui;

/** First-stage phone UI. Does not launch paid jobs without a secure backend. */
public final class ClaudeCodeScreen extends Screen {
    public ClaudeCodeScreen(MainActivity main) { super(main); }
    @Override public String title() { return "AI Developer"; }
    @Override public View create() {
        LinearLayout column = main.column();
        column.addView(Ui.heading(main, "Claude Code"));
        column.addView(Ui.body(main, "SETUP REQUIRED — no Claude session is running. No API credits will be spent from this screen."));
        String selected = main.selectedRepo() == null ? "Choose a repository on the dashboard first"
                : main.selectedRepo().getFullName() + " / " + main.selectedRepo().getCurrentBranch();
        column.addView(Ui.sectionLabel(main, "Selected project"));
        column.addView(Ui.body(main, selected));
        column.addView(Ui.sectionLabel(main, "Development assignment"));
        EditText prompt = new EditText(main);
        prompt.setHint("Describe what Claude should improve...");
        SharedPreferences prefs = main.getSharedPreferences("claude_code_draft", 0);
        prompt.setText(prefs.getString("assignment", ""));
        prompt.setMinLines(5);
        prompt.setGravity(android.view.Gravity.TOP);
        column.addView(prompt);
        Button save = Ui.button(main, "Save assignment draft");
        save.setOnClickListener(v -> {
            prefs.edit().putString("assignment", prompt.getText().toString()).apply();
            main.toast("Assignment saved on this phone. No job started.");
        });
        column.addView(save);
        column.addView(Ui.sectionLabel(main, "Maximum planned spend (USD)"));
        EditText budget = new EditText(main);
        budget.setSingleLine(true);
        budget.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        budget.setHint("Example: 5.00");
        budget.setText(prefs.getString("budget", "5.00"));
        column.addView(budget);
        Button saveBudget = Ui.button(main, "Save planned budget");
        saveBudget.setOnClickListener(v -> {
            String amount = budget.getText().toString().trim();
            try {
                double value = Double.parseDouble(amount);
                if (!Double.isFinite(value) || value <= 0 || value > 10000) throw new NumberFormatException();
                prefs.edit().putString("budget", amount).apply();
                main.toast("Planned budget saved. Not yet enforced by a backend.");
            } catch (NumberFormatException ex) {
                main.toast("Enter a valid amount greater than $0 and no more than $10,000.");
            }
        });
        column.addView(saveBudget);
        Button copy = Ui.button(main, "Copy assignment for Claude Code");
        copy.setOnClickListener(v -> {
            String assignment = prompt.getText().toString().trim();
            if (assignment.isEmpty()) { main.toast("Enter an assignment first."); return; }
            String repoName = main.selectedRepo() == null ? "" : main.selectedRepo().getFullName();
            String branchName = main.selectedRepo() == null ? "" : main.selectedRepo().getCurrentBranch();
            String instruction = "Repository: " + repoName + "\nBranch: " + branchName
                    + "\nUse GitHub as the source of truth. Preserve existing approved artwork and behavior. "
                    + "Inspect the project before editing, work only on the selected branch, "
                    + "run tests and build through GitHub Actions, and report verified artifacts only.\n\n"
                    + assignment;
            ClipboardManager clipboard = (ClipboardManager) main.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) { main.toast("Clipboard unavailable."); return; }
            clipboard.setPrimaryClip(ClipData.newPlainText("Claude Code assignment", instruction));
            main.toast("Assignment copied. Paste it into Claude Code in Codespaces.");
        });
        column.addView(copy);
        Button start = Ui.button(main, "Start Claude Code — connection required");
        start.setEnabled(false);
        column.addView(start);
        column.addView(Ui.sectionLabel(main, "Session"));
        column.addView(Ui.body(main, "Status: Not connected\nProgress: No active job\nAPK: No verified artifact"));
        Button billing = Ui.button(main, "Add Anthropic API credits (official website)");
        billing.setOnClickListener(v -> open("https://console.anthropic.com/settings/billing"));
        column.addView(billing);
        Button codespaces = Ui.button(main, "Open GitHub Codespaces");
        codespaces.setOnClickListener(v -> open("https://github.com/codespaces"));
        column.addView(codespaces);
        column.addView(Ui.body(main, "Manual mode: copy the assignment, open Codespaces, and paste it into Claude Code. This does not start a job automatically."));
        column.addView(Ui.body(main, "Drafts and planned budgets are saved locally. A planned budget is NOT a billing limit until backend enforcement is implemented."));
        column.addView(Ui.body(main, "Payments take place on Anthropic's website. API usage, credit balance, job execution and stop controls require a secure backend before they can be enabled."));
        return column;
    }
    private void open(String url) {
        main.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
}
