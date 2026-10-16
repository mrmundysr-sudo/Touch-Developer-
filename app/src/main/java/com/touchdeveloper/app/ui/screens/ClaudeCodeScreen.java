package com.touchdeveloper.app.ui.screens;

import android.content.Intent;
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
        prompt.setMinLines(5);
        prompt.setGravity(android.view.Gravity.TOP);
        column.addView(prompt);
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
        column.addView(Ui.body(main, "Payments take place on Anthropic's website. API usage, credit balance, job execution and stop controls require a secure backend before they can be enabled."));
        return column;
    }
    private void open(String url) {
        main.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
}
