package com.touchdeveloper.app.build;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.touchdeveloper.app.model.BuildRecord;
import com.touchdeveloper.app.model.StatusLabel;
import com.touchdeveloper.app.safety.DemoMode;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Labelled demo build used when no real build backend is configured.
 *
 * The simulation runs entirely on the device: it writes a clearly labelled
 * placeholder file where a real APK would be, emits a realistic progress
 * transcript, and can simulate a failure so the error path can be exercised.
 * The record is always marked as demo and no real build ever runs.
 */
public class DemoBuildService implements BuildService {

    /** Receives progress and completion callbacks for a running simulation. */
    public interface Listener {
        void onProgress(BuildRecord record);

        void onComplete(BuildRecord record);
    }

    private static final String[] STEPS = {
            "Preparing workspace",
            "Resolving dependencies",
            "Compiling Java sources",
            "Packaging resources",
            "Running assembleDebug",
            "Packaging APK"
    };

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public DemoBuildService(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public String modeDescription() {
        return "Local simulated demo build (no real build runs). " + DemoMode.banner("Build service");
    }

    @Override
    public BuildRecord startBuild(BuildRequest request) {
        BuildRecord record = new BuildRecord("demo-" + System.currentTimeMillis(),
                request.getProjectName(), request.getRepoFullName(), request.getBranch());
        record.setDemo(true);
        record.setStartTime(System.currentTimeMillis());
        record.setStatus(StatusLabel.BUILDING);
        record.appendProgress("Demo build started for " + request.getRepoFullName()
                + " (" + request.getBranch() + ").");
        record.appendProgress("No real build is running. This transcript is simulated.");
        record.setApkPath(writePlaceholderApk(request.getProjectName()).getAbsolutePath());
        return record;
    }

    /**
     * Runs the simulated progress transcript, calling back on the main thread until
     * the record reaches {@link StatusLabel#SUCCESSFUL} or {@link StatusLabel#FAILED}.
     *
     * @param forceFailure when true the simulation ends in the failure path so the
     *                      error output and status can be tested.
     */
    public void runSimulation(BuildRecord record, boolean forceFailure, Listener listener) {
        if (record == null || listener == null) {
            return;
        }
        advance(record, forceFailure, listener, 0);
    }

    private void advance(BuildRecord record, boolean forceFailure, Listener listener, int step) {
        if (step < STEPS.length) {
            record.appendProgress(STEPS[step]);
            listener.onProgress(record);
            handler.postDelayed(() -> advance(record, forceFailure, listener, step + 1), 350);
            return;
        }
        finish(record, forceFailure, listener);
    }

    private void finish(BuildRecord record, boolean forceFailure, Listener listener) {
        record.appendGradle("> Task :app:assembleDebug");
        if (forceFailure) {
            record.appendGradle("> Task :app:compileDebugJavaWithJavac FAILED");
            record.appendError("[DEMO] Simulated compile error: cannot find symbol (simulated failure).");
            record.setStatus(StatusLabel.FAILED);
        } else {
            record.appendGradle("BUILD SUCCESSFUL (simulated)");
            record.setStatus(StatusLabel.SUCCESSFUL);
        }
        record.setEndTime(System.currentTimeMillis());
        listener.onComplete(record);
    }

    /** Writes a labelled placeholder in place of a real APK. */
    private File writePlaceholderApk(String projectName) {
        String base = (projectName == null || projectName.trim().isEmpty())
                ? "project" : projectName.trim().replaceAll("[^A-Za-z0-9._-]", "-");
        File dir = new File(context.getFilesDir(), "exports");
        dir.mkdirs();
        File placeholder = new File(dir, "app-debug-DEMO.apk");
        String text = DemoMode.label("This is a placeholder, not a real APK.") + "\n"
                + "Project: " + base + "\n"
                + "A real build requires a configured build service.\n"
                + "Created: " + new java.util.Date() + "\n";
        try (FileOutputStream out = new FileOutputStream(placeholder)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            // The status screen reports a missing APK rather than claiming success.
        }
        return placeholder;
    }
}
