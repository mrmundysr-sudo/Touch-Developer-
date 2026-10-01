package com.touchdeveloper.app.ui.screens;

import android.text.InputType;
import android.app.AlertDialog;
import com.touchdeveloper.app.safety.SetupBackup;
import com.touchdeveloper.app.ui.TaskRunner;
import java.util.Properties;
import java.util.Arrays;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.touchdeveloper.app.MainActivity;
import com.touchdeveloper.app.ServiceLocator;
import com.touchdeveloper.app.safety.Confirmations;
import com.touchdeveloper.app.ui.Screen;
import com.touchdeveloper.app.ui.Ui;

/**
 * Setup screen: connection status and secure credential entry, plus the activity
 * history required by the safety rules.
 *
 * Credentials are written straight into encrypted Android storage. They are never
 * displayed back in full, never logged, and never placed in a handoff ZIP.
 */
public class SettingsScreen extends Screen {

    private LinearLayout column;
    private EditText githubToken;
    private EditText openHandsEndpoint;
    private EditText openHandsToken;

    public SettingsScreen(MainActivity main) {
        super(main);
    }

    @Override
    public String title() {
        return "Setup and history";
    }

    @Override
    public View create() {
        column = main.column();

        column.addView(Ui.sectionLabel(main, "Connection mode"));
        column.addView(Ui.body(main, "GitHub: " + main.services().gitHub().modeDescription()));
        column.addView(Ui.body(main, "OpenHands: " + main.services().openHands().modeDescription()));
        column.addView(Ui.body(main, "Build: " + main.services().build().modeDescription()));

        column.addView(Ui.sectionLabel(main, "Stored credentials (never shown in full)"));
        column.addView(Ui.body(main, "GitHub token: "
                + main.services().credentials().maskedSummary(ServiceLocator.KEY_GITHUB_TOKEN)));
        column.addView(Ui.body(main, "OpenHands endpoint: "
                + main.services().credentials().maskedSummary(ServiceLocator.KEY_OPENHANDS_ENDPOINT)));
        column.addView(Ui.body(main, "OpenHands token: "
                + main.services().credentials().maskedSummary(ServiceLocator.KEY_OPENHANDS_TOKEN)));

        column.addView(Ui.sectionLabel(main, "Connect GitHub"));
        githubToken = passwordField("Paste a GitHub personal access token");
        column.addView(githubToken);
        Button saveGithub = Ui.button(main, "Save GitHub token securely");
        saveGithub.setOnClickListener(v -> saveCredential(ServiceLocator.KEY_GITHUB_TOKEN,
                githubToken, "GitHub token"));
        column.addView(saveGithub);
        Button clearGithub = Ui.button(main, "Remove GitHub token");
        clearGithub.setOnClickListener(v -> removeCredential(ServiceLocator.KEY_GITHUB_TOKEN,
                "GitHub token"));
        column.addView(clearGithub);

        column.addView(Ui.sectionLabel(main, "Connect OpenHands / build workspace"));
        openHandsEndpoint = textField("Endpoint URL, for example https://your-workspace.example");
        column.addView(openHandsEndpoint);
        openHandsToken = passwordField("Paste a workspace token (optional)");
        column.addView(openHandsToken);
        Button saveOpenHands = Ui.button(main, "Save OpenHands settings securely");
        saveOpenHands.setOnClickListener(v -> saveOpenHands());
        column.addView(saveOpenHands);
        Button clearOpenHands = Ui.button(main, "Remove OpenHands settings");
        clearOpenHands.setOnClickListener(v -> {
            removeCredential(ServiceLocator.KEY_OPENHANDS_ENDPOINT, "OpenHands endpoint");
            removeCredential(ServiceLocator.KEY_OPENHANDS_TOKEN, "OpenHands token");
        });
        column.addView(clearOpenHands);

        column.addView(Ui.sectionLabel(main, "Keep setup after reinstalling"));
        column.addView(Ui.body(main, "Save your connections in a password-protected file. Keep the file and password so you can restore after reinstalling. Expired or revoked tokens still need replacing."));
        Button backup = Ui.button(main, "Save Setup");
        backup.setOnClickListener(v -> setupPassword(false));
        column.addView(backup);
        Button restore = Ui.button(main, "Restore Setup");
        restore.setOnClickListener(v -> setupPassword(true));
        column.addView(restore);

        column.addView(Ui.sectionLabel(main, "Safety notes"));
        column.addView(Ui.body(main, "Credentials are encrypted with a key held in the Android Keystore. "
                + "They are never written to logs, source ZIPs, or handoff packages. Save Setup exports only a separately password-encrypted connection file."));

        column.addView(Ui.sectionLabel(main, "Activity history"));
        TextView history = Ui.mono(main, main.services().activityLog().asText());
        column.addView(history);

        Button refresh = Ui.button(main, "Refresh history");
        refresh.setOnClickListener(v -> main.replace(new SettingsScreen(main)));
        column.addView(refresh);
        return column;
    }

    private void setupPassword(boolean restore) {
        EditText password = passwordField(restore ? "Backup password" : "Choose a backup password (at least 8 characters)");
        AlertDialog dialog = new AlertDialog.Builder(main)
                .setTitle(restore ? "Restore Setup" : "Save Setup")
                .setMessage(restore ? "Choose your saved setup file. Connections in the file replace matching saved connections." : "This file contains your connection tokens, encrypted with this password. Save it in Downloads or another folder you keep. You will need the same password after reinstalling.")
                .setView(password).setNegativeButton("Cancel", null)
                .setPositiveButton("Choose file", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            char[] secret = password.getText().toString().toCharArray();
            if (secret.length < 8) { Arrays.fill(secret, '\0'); main.toast("Use at least 8 characters."); return; }
            password.setText(""); dialog.dismiss();
            MainActivity.PickerCallback callback = new MainActivity.PickerCallback() {
                public void onCancelled() { Arrays.fill(secret, '\0'); main.toast("Cancelled. Setup was not changed."); }
                public void onPicked(android.net.Uri uri) { transferSetup(uri, secret, restore); }
            };
            if (restore) main.pickFile(callback); else main.saveSetupFile(callback);
        }));
        dialog.show();
    }

    private void transferSetup(android.net.Uri uri, char[] password, boolean restore) {
        TaskRunner.run(main, restore ? "Restore Setup" : "Save Setup", "Processing encrypted setup…", () -> {
            try {
                if (restore) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    try (InputStream in = main.getContentResolver().openInputStream(uri)) {
                        if (in == null) return "Could not open the setup file.";
                        byte[] buffer = new byte[4096]; int count;
                        while ((count = in.read(buffer)) != -1) {
                            if (bytes.size() + count > 65536) return "The setup file is too large.";
                            bytes.write(buffer, 0, count);
                        }
                    }
                    Properties values = SetupBackup.decrypt(bytes.toByteArray(), password);
                    Properties old = new Properties();
                    for (String key : values.stringPropertyNames()) {
                        String value = main.services().credentials().retrieve(key);
                        if (value != null) old.setProperty(key, value);
                    }
                    for (String key : values.stringPropertyNames()) {
                        if (!main.services().credentials().store(key, values.getProperty(key))) {
                            for (String original : values.stringPropertyNames()) {
                                if (old.containsKey(original)) main.services().credentials().store(original, old.getProperty(original));
                                else main.services().credentials().remove(original);
                            }
                            return "Could not restore secure storage. Previous settings were restored where possible.";
                        }
                    }
                    return "Setup restored. Connections still depend on the saved tokens being valid.";
                } else {
                    Properties values = new Properties();
                    for (String key : SetupBackup.KEYS) {
                        String value = main.services().credentials().retrieve(key);
                        if (value != null && !value.isEmpty()) values.setProperty(key, value);
                    }
                    if (values.isEmpty()) return "There are no saved connections to back up.";
                    byte[] bytes = SetupBackup.encrypt(values, password);
                    try (OutputStream out = main.getContentResolver().openOutputStream(uri, "wt")) {
                        if (out == null) return "Could not write the setup file.";
                        out.write(bytes);
                    }
                    return "Setup saved. Keep this file and its password for future reinstalls.";
                }
            } catch (Exception e) {
                return restore ? "Restore failed. Check the password and setup file. Connection settings were not restored." : "Save failed. Choose another location and try again.";
            } finally { Arrays.fill(password, '\0'); }
        }, message -> {
            main.services().rebuild();
            Confirmations.info(main, restore ? "Restore Setup" : "Save Setup", message);
            main.replace(new SettingsScreen(main));
        });
    }

    private EditText textField(String hint) {
        EditText field = new EditText(main);
        field.setInputType(InputType.TYPE_CLASS_TEXT);
        field.setHint(hint);
        return field;
    }

    private EditText passwordField(String hint) {
        EditText field = new EditText(main);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        field.setHint(hint);
        return field;
    }

    private void saveCredential(String key, EditText field, String label) {
        String value = field.getText().toString().trim();
        if (value.isEmpty()) {
            main.toast("Enter a value for " + label + " first.");
            return;
        }
        boolean stored = main.services().credentials().store(key, value);
        field.setText("");
        main.services().activityLog().record("setup", label + (stored ? " stored securely" : " was not stored"),
                false);
        main.services().rebuild();
        if (stored) {
            Confirmations.info(main, "Stored securely", label + " was stored in encrypted storage. "
                    + "The value is not shown again and is not included in any exported file.");
        } else {
            Confirmations.info(main, "Not stored", label + " could not be stored on this device. "
                    + "Check that secure storage is available.");
        }
        main.replace(new SettingsScreen(main));
    }

    private void saveOpenHands() {
        String endpoint = openHandsEndpoint.getText().toString().trim();
        String token = openHandsToken.getText().toString().trim();
        if (endpoint.isEmpty()) {
            main.toast("Enter an OpenHands endpoint first.");
            return;
        }
        boolean endpointStored = main.services().credentials()
                .store(ServiceLocator.KEY_OPENHANDS_ENDPOINT, endpoint);
        boolean tokenStored = token.isEmpty() || main.services().credentials()
                .store(ServiceLocator.KEY_OPENHANDS_TOKEN, token);
        openHandsToken.setText("");
        main.services().activityLog().record("setup",
                "OpenHands settings " + (endpointStored && tokenStored ? "stored securely" : "not stored"),
                false);
        main.services().rebuild();
        Confirmations.info(main, endpointStored && tokenStored ? "Stored securely" : "Not stored",
                "OpenHands endpoint and token were saved to encrypted storage. "
                        + "The endpoint contract is unverified, so build success is never assumed.");
        main.replace(new SettingsScreen(main));
    }

    private void removeCredential(String key, String label) {
        Confirmations.confirmDestructive(main, "Remove " + label + "?",
                "Remove the stored " + label + " from this device? Unsaved work is not affected.",
                () -> {
                    main.services().credentials().remove(key);
                    main.services().activityLog().record("setup", label + " removed", true);
                    main.services().rebuild();
                    main.replace(new SettingsScreen(main));
                });
    }
}
