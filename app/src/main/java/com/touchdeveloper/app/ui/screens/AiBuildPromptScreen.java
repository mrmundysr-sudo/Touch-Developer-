package com.touchdeveloper.app.ui.screens;

import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.touchdeveloper.app.MainActivity;
import com.touchdeveloper.app.build.BuildRequest;
import com.touchdeveloper.app.build.DemoBuildService;
import com.touchdeveloper.app.model.BuildRecord;
import com.touchdeveloper.app.safety.Confirmations;
import com.touchdeveloper.app.ui.Screen;
import com.touchdeveloper.app.ui.TaskRunner;
import com.touchdeveloper.app.ui.Ui;
import com.touchdeveloper.app.util.Result;

import java.io.File;

/**
 * AI Build Prompt Screen: source code, AI instructions, optional project ZIP and
 * assets, selected repository and branch, and the Build / Send / Handoff actions.
 */
public class AiBuildPromptScreen extends Screen {

    private static final String[] EXAMPLE_PROMPTS = {
            "Build the latest version.",
            "Fix the Gradle errors.",
            "Replace this image with the new one.",
            "Show me what changed.",
            "Create a complete source handoff."
    };

    private EditText sourceField;
    private EditText instructionsField;
    private TextView attachmentSummary;
    private CheckBox simulateFailure;

    public AiBuildPromptScreen(MainActivity main) {
        super(main);
    }

    @Override
    public String title() {
        return "AI Build Prompt";
    }

    @Override
    public View create() {
        LinearLayout column = main.column();
        BuildRequest request = main.pendingRequest();

        String repoName = main.selectedRepo() == null ? request.getRepoFullName()
                : main.selectedRepo().getFullName();
        column.addView(Ui.body(main, "Selected repository: "
                + (repoName == null || repoName.isEmpty() ? "none" : repoName)));
        column.addView(Ui.body(main, "Selected branch: " + request.getBranch()));
        column.addView(Ui.body(main, "Build route: " + main.services().build().modeDescription()));

        column.addView(Ui.sectionLabel(main, "Source code"));
        sourceField = new EditText(main);
        sourceField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        sourceField.setMinLines(6);
        sourceField.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        sourceField.setHint("Paste source code here");
        sourceField.setText(request.getSourceCode());
        column.addView(sourceField);

        column.addView(Ui.sectionLabel(main, "AI instructions"));
        instructionsField = new EditText(main);
        instructionsField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        instructionsField.setMinLines(6);
        instructionsField.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        instructionsField.setHint("Describe what to build or change");
        instructionsField.setText(request.getAiInstructions());
        column.addView(instructionsField);

        column.addView(Ui.sectionLabel(main, "Example prompts"));
        for (String prompt : EXAMPLE_PROMPTS) {
            Button example = Ui.button(main, prompt);
            example.setOnClickListener(v -> instructionsField.setText(prompt));
            column.addView(example);
        }

        column.addView(Ui.sectionLabel(main, "Optional uploads"));
        attachmentSummary = Ui.body(main, attachmentText(request));
        column.addView(attachmentSummary);

        Button projectZip = Ui.button(main, "Upload project ZIP");
        projectZip.setOnClickListener(v -> pickProjectZip(request));
        column.addView(projectZip);

        Button asset = Ui.button(main, "Upload asset");
        asset.setOnClickListener(v -> pickAsset(request));
        column.addView(asset);

        simulateFailure = new CheckBox(main);
        simulateFailure.setText("Demo build only: simulate a failure so the error screen can be tested");
        column.addView(simulateFailure);

        column.addView(Ui.sectionLabel(main, "Actions"));
        Button build = Ui.button(main, "Build APK");
        build.setOnClickListener(v -> buildApk(request));
        column.addView(build);

        Button send = Ui.button(main, "Send to OpenHands");
        send.setOnClickListener(v -> sendToOpenHands(request));
        column.addView(send);

        Button handoff = Ui.button(main, "Create Handoff ZIP");
        handoff.setOnClickListener(v -> {
            captureFields(request);
            main.show(new HandoffPackageScreen(main));
        });
        column.addView(handoff);
        return column;
    }

    private String attachmentText(BuildRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Project ZIP: ").append(request.getProjectZip() == null ? "none"
                : request.getProjectZip().getName()).append('\n');
        sb.append("Assets: ").append(request.getAssets().isEmpty() ? "none"
                : String.valueOf(request.getAssets().size()));
        return sb.toString();
    }

    private void captureFields(BuildRequest request) {
        request.setSourceCode(sourceField.getText().toString());
        request.setAiInstructions(instructionsField.getText().toString());
    }

    private void pickProjectZip(BuildRequest request) {
        main.pickFile(new MainActivity.PickerCallback() {
            @Override
            public void onPicked(android.net.Uri uri) {
                Result<File> imported = main.services().fileTransfer().importUri(uri, "project.zip");
                log("prompt", "Imported project ZIP (" + imported.display() + ")");
                if (imported.ok && imported.data != null) {
                    request.setProjectZip(imported.data);
                    attachmentSummary.setText(attachmentText(request));
                } else {
                    main.toast(imported.display());
                }
            }

            @Override
            public void onCancelled() {
                main.toast("Project ZIP selection cancelled.");
            }
        });
    }

    private void pickAsset(BuildRequest request) {
        main.pickFile(new MainActivity.PickerCallback() {
            @Override
            public void onPicked(android.net.Uri uri) {
                Result<File> imported = main.services().fileTransfer().importUri(uri, "asset.bin");
                log("prompt", "Imported asset (" + imported.display() + ")");
                if (imported.ok && imported.data != null) {
                    request.getAssets().add(imported.data);
                    attachmentSummary.setText(attachmentText(request));
                } else {
                    main.toast(imported.display());
                }
            }

            @Override
            public void onCancelled() {
                main.toast("Asset selection cancelled.");
            }
        });
    }

    private void buildApk(BuildRequest request) {
        captureFields(request);
        if (main.selectedRepo() == null) {
            main.toast("Select a repository on the dashboard before building.");
            return;
        }
        request.setRepoFullName(main.selectedRepo().getFullName());
        request.setBranch(main.selectedRepo().getCurrentBranch());
        request.setProjectName(main.selectedRepo().getName());

        if (main.services().build().isConfigured()) {
            // A configured build routes through OpenHands, so it must not run on
            // the main thread.
            TaskRunner.run(main, "Build", "Requesting build\u2026",
                    () -> main.services().build().startBuild(request),
                    record -> {
                        main.setBuildRecord(record);
                        main.selectedRepo().setBuildStatus(record.getStatus());
                        log("build", "Started build via configured service");
                        Confirmations.info(main, "Build requested", record.progressText()
                                + "\n\nA build was requested. Success is only reported when the build "
                                + "service confirms it.");
                        main.show(new BuildStatusScreen(main));
                    });
            return;
        }

        boolean forceFailure = simulateFailure != null && simulateFailure.isChecked();
        main.setLastBuildForcedFailure(forceFailure);
        BuildRecord record = main.services().demoBuild().startBuild(request);
        main.setBuildRecord(record);
        main.selectedRepo().setBuildStatus(record.getStatus());
        log("build", "Started demo build" + (forceFailure ? " (simulated failure)" : ""));
        main.toast("[DEMO] Demo build started. No real build is running.");
        main.show(new BuildStatusScreen(main));
        main.services().demoBuild().runSimulation(record, forceFailure, new DemoBuildService.Listener() {
            @Override
            public void onProgress(BuildRecord updated) {
                main.setBuildRecord(updated);
                main.replace(new BuildStatusScreen(main));
            }

            @Override
            public void onComplete(BuildRecord updated) {
                main.setBuildRecord(updated);
                main.setLastBuildLog(updated.progressText() + "\n" + updated.getGradleOutput()
                        + (updated.hasErrors() ? "\n" + updated.getErrorOutput() : ""));
                main.selectedRepo().setBuildStatus(updated.getStatus());
                if (updated.isDemo()) {
                    main.toast("[DEMO] Demo build finished: " + updated.getStatus().display()
                            + " (no real build ran)");
                } else {
                    main.toast("Build finished: " + updated.getStatus().display());
                }
                main.replace(new BuildStatusScreen(main));
            }
        });
    }

    private void sendToOpenHands(BuildRequest request) {
        captureFields(request);
        TaskRunner.run(main, "OpenHands", "Sending to OpenHands\u2026",
                () -> main.services().openHands().sendInstructions(request),
                result -> {
                    log("prompt", "Send to OpenHands (" + result.display() + ")");
                    Confirmations.info(main, result.ok ? "Sent to OpenHands" : "Not sent to OpenHands",
                            result.display());
                });
    }

    private void log(String category, String message) {
        main.services().activityLog().record(category, message, false);
    }
}
