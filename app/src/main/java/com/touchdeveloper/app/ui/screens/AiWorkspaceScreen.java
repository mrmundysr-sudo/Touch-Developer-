package com.touchdeveloper.app.ui.screens;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.touchdeveloper.app.MainActivity;
import com.touchdeveloper.app.ui.Screen;
import com.touchdeveloper.app.ui.Ui;

/** Provider-aware workspace entry point. No simulated agent sessions. */
public final class AiWorkspaceScreen extends Screen {
    private String engine = "Claude Code";
    public AiWorkspaceScreen(MainActivity main) { super(main); }
    @Override public String title() { return "AI Development"; }
    @Override public View create() {
        LinearLayout column = main.column();
        column.addView(Ui.sectionLabel(main, "Choose your development engine"));
        addEngine(column, "Claude Code");
        addEngine(column, "OpenHands");
        addEngine(column, "DeepSeek (via OpenHands)");
        column.addView(Ui.sectionLabel(main, engine + " Workspace"));
        column.addView(Ui.body(main, "Repository: " + (main.selectedRepo() == null ? "Select a repository on the dashboard" : main.selectedRepo().getFullName())));
        column.addView(Ui.body(main, "Connection status: Not connected. No assignment has been started."));
        column.addView(Ui.sectionLabel(main, "Live activity"));
        column.addView(Ui.body(main, "Activity will appear here after a supported agent backend is connected. This is not a simulated session."));
        column.addView(Ui.sectionLabel(main, "Controls"));
        for (String label : new String[]{"Send message", "Pause / Resume", "Stop assignment", "View changes", "Download APK"}) {
            Button button = Ui.button(main, label + " (not connected)");
            button.setEnabled(false);
            column.addView(button);
        }
        column.addView(Ui.body(main, "GitHub Actions remains the source of verified APK builds. Configure authenticated agent backends before enabling remote execution."));
        return column;
    }
    private void addEngine(LinearLayout column, String name) {
        Button button = Ui.button(main, (engine.equals(name) ? "✓ " : "") + name);
        button.setOnClickListener(v -> { engine = name; main.refreshCurrent(); });
        column.addView(button);
    }
}
