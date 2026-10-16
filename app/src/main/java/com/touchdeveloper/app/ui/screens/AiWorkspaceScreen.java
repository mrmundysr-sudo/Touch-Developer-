package com.touchdeveloper.app.ui.screens;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import com.touchdeveloper.app.model.Repo;
import java.util.ArrayList;
import com.touchdeveloper.app.MainActivity;
import com.touchdeveloper.app.ui.Screen;
import com.touchdeveloper.app.ui.Ui;

/** Provider-aware workspace entry point. No simulated agent sessions. */
public final class AiWorkspaceScreen extends Screen {
    private String engine = "Claude Code";
    private String selectedRepoName;
    private String selectedBranch;
    public AiWorkspaceScreen(MainActivity main) { super(main); }
    @Override public String title() { return "AI Development"; }
    @Override public View create() {
        LinearLayout column = main.column();
        column.addView(Ui.sectionLabel(main, "Choose your development engine"));
        addEngine(column, "Claude Code");
        addEngine(column, "OpenHands");
        addEngine(column, "DeepSeek (via OpenHands)");
        column.addView(Ui.sectionLabel(main, "Repository and branch"));
        ArrayList<String> repoNames = new ArrayList<>();
        repoNames.add("Choose repository");
        for (Repo repo : main.repos()) if (!repo.isDemo()) repoNames.add(repo.getFullName());
        Spinner repoPicker = new Spinner(main);
        repoPicker.setAdapter(new ArrayAdapter<>(main, android.R.layout.simple_spinner_dropdown_item, repoNames));
        String initial = selectedRepoName != null ? selectedRepoName : (main.selectedRepo() == null ? null : main.selectedRepo().getFullName());
        if (initial != null && repoNames.contains(initial)) repoPicker.setSelection(repoNames.indexOf(initial));
        column.addView(repoPicker);
        Spinner branchPicker = new Spinner(main);
        column.addView(branchPicker);
        repoPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRepoName = position == 0 ? null : repoNames.get(position);
                Repo chosen = null;
                for (Repo repo : main.repos()) if (repo.getFullName().equals(selectedRepoName)) { chosen = repo; break; }
                ArrayList<String> names = new ArrayList<>();
                if (chosen != null) {
                    main.setSelectedRepo(chosen);
                    names.addAll(chosen.getBranches());
                    if (!names.contains(chosen.getCurrentBranch())) names.add(chosen.getCurrentBranch());
                }
                if (names.isEmpty()) names.add("No branch selected");
                branchPicker.setAdapter(new ArrayAdapter<>(main, android.R.layout.simple_spinner_dropdown_item, names));
                if (selectedBranch != null && names.contains(selectedBranch)) branchPicker.setSelection(names.indexOf(selectedBranch));
            }
        });
        branchPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String value = (String) parent.getItemAtPosition(position);
                selectedBranch = "No branch selected".equals(value) ? null : value;
            }
        });
        column.addView(Ui.body(main, "Showing locally known branches. Remote branch refresh is not yet connected."));
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
