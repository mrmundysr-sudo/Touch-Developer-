package com.touchdeveloper.app;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import android.app.Activity;

import com.touchdeveloper.app.build.BuildRequest;
import com.touchdeveloper.app.files.HandoffResult;
import com.touchdeveloper.app.model.BuildRecord;
import com.touchdeveloper.app.model.Repo;
import com.touchdeveloper.app.ui.Screen;
import com.touchdeveloper.app.ui.Ui;
import com.touchdeveloper.app.ui.screens.AiBuildPromptScreen;
import com.touchdeveloper.app.ui.screens.BuildStatusScreen;
import com.touchdeveloper.app.ui.screens.FileScreen;
import com.touchdeveloper.app.ui.screens.HandoffPackageScreen;
import com.touchdeveloper.app.ui.screens.RepoActionScreen;
import com.touchdeveloper.app.ui.screens.RepositoryDashboardScreen;
import com.touchdeveloper.app.ui.screens.SettingsScreen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Single-activity host for every Touch Developer screen.
 *
 * The activity owns shared state (connected repositories, the selected repository,
 * the pending build request, the latest build record, and the latest handoff) and
 * provides simple stack navigation between screens.
 */
public class MainActivity extends Activity {

    private ServiceLocator services;

    private final List<Repo> repos = new ArrayList<>();
    private Repo selectedRepo;
    private BuildRequest pendingRequest = new BuildRequest();
    private BuildRecord buildRecord;
    private HandoffResult lastHandoff;
    private boolean lastBuildForcedFailure;
    private String lastBuildLog = "";
    private String lastError = "";

    private FrameLayout root;
    private LinearLayout header;
    private TextView headerTitle;
    private Deque<Screen> backStack = new ArrayDeque<>();
    private Screen current;

    /** Result callback for a system file picker started by a screen. */
    public interface PickerCallback {
        void onPicked(android.net.Uri uri);
        void onCancelled();
    }

    private static final int REQUEST_PICK_FILE = 4001;
    private PickerCallback pickerCallback;

    /** Opens a document picker and delivers the chosen URI to the callback. */
    public void pickFile(PickerCallback callback) {
        this.pickerCallback = callback;
        android.content.Intent intent = new android.content.Intent(
                android.content.Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        try {
            startActivityForResult(intent, REQUEST_PICK_FILE);
        } catch (Exception e) {
            pickerCallback = null;
            toast("No file picker is available on this device.");
        }
    }

    /** Save a user-owned document outside the application's uninstallable storage. */
    public void saveSetupFile(PickerCallback callback) {
        pickerCallback = callback;
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(android.content.Intent.EXTRA_TITLE, "touch-developer-setup.tds");
        try { startActivityForResult(intent, REQUEST_PICK_FILE); }
        catch (Exception e) { pickerCallback = null; toast("No file picker is available on this device."); callback.onCancelled(); }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_FILE) {
            return;
        }
        PickerCallback callback = pickerCallback;
        pickerCallback = null;
        if (callback == null) {
            return;
        }
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            callback.onPicked(data.getData());
        } else {
            callback.onCancelled();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        services = new ServiceLocator(this);

        root = new FrameLayout(this);
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);

        header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setBackgroundColor(0xFF12283C);
        header.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button back = new Button(this);
        back.setText("<");
        back.setAllCaps(false);
        back.setOnClickListener(v -> goBack());
        header.addView(back);

        headerTitle = new TextView(this);
        headerTitle.setTextColor(0xFFFFFFFF);
        headerTitle.setTextSize(17f);
        headerTitle.setPadding(Ui.dp(this, 10), 0, 0, 0);
        header.addView(headerTitle, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button settings = new Button(this);
        settings.setText("Setup");
        settings.setAllCaps(false);
        settings.setOnClickListener(v -> show(new SettingsScreen(this)));
        header.addView(settings);

        column.addView(header);
        column.addView(root, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(column);

        show(new RepositoryDashboardScreen(this));
    }

    public ServiceLocator services() {
        return services;
    }

    // ---- shared state -------------------------------------------------------

    public List<Repo> repos() {
        return repos;
    }

    public Repo selectedRepo() {
        return selectedRepo;
    }

    public void setSelectedRepo(Repo repo) {
        this.selectedRepo = repo;
    }

    public BuildRequest pendingRequest() {
        return pendingRequest;
    }

    public void setPendingRequest(BuildRequest request) {
        this.pendingRequest = request;
    }

    public BuildRecord buildRecord() {
        return buildRecord;
    }

    public void setBuildRecord(BuildRecord record) {
        this.buildRecord = record;
    }

    public HandoffResult lastHandoff() {
        return lastHandoff;
    }

    public void setLastHandoff(HandoffResult result) {
        this.lastHandoff = result;
    }

    public boolean isLastBuildForcedFailure() {
        return lastBuildForcedFailure;
    }

    public void setLastBuildForcedFailure(boolean value) {
        this.lastBuildForcedFailure = value;
    }

    public String lastBuildLog() {
        return lastBuildLog;
    }

    public void setLastBuildLog(String log) {
        this.lastBuildLog = log;
    }

    /** Last error text to show on the dashboard, or "" when there is none. */
    public String lastError() {
        return lastError;
    }

    public void setLastError(String error) {
        this.lastError = error == null ? "" : error;
    }

    // ---- navigation ---------------------------------------------------------

    public void show(Screen screen) {
        if (current != null && current != screen) {
            backStack.push(current);
        }
        display(screen);
    }

    /** Shows a screen without growing the back stack (used for tab-like moves). */
    public void replace(Screen screen) {
        display(screen);
    }

    private void display(Screen screen) {
        render(screen);
        screen.onShown();
    }

    /**
     * Re-renders the current screen in place without calling {@code onShown()}.
     * Used to show an updated screen after an asynchronous result, so a screen that
     * loads data in {@code onShown()} cannot start another load from its own refresh.
     */
    public void refreshCurrent() {
        if (current != null) {
            render(current);
        }
    }

    /** The screen currently displayed, used to drop results from a screen left behind. */
    public Screen currentScreen() {
        return current;
    }

    private void render(Screen screen) {
        current = screen;
        headerTitle.setText(screen.title());
        View content = screen.create();
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        root.removeAllViews();
        root.addView(scroll);
    }

    public void goBack() {
        if (backStack.isEmpty()) {
            show(new RepositoryDashboardScreen(this));
            return;
        }
        Screen previous = backStack.pop();
        display(previous);
    }

    @Override
    public void onBackPressed() {
        if (backStack.isEmpty()) {
            super.onBackPressed();
        } else {
            goBack();
        }
    }

    // ---- helpers ------------------------------------------------------------

    public void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    public LinearLayout column() {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 24));
        return column;
    }
}
