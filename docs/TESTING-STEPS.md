# Testing steps

## A. Build verification (no device needed)

```bash
cd <repository root>
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
./gradlew --no-daemon clean assembleDebug testDebugUnitTest
```

Expected results:

- `BUILD SUCCESSFUL`
- 14 unit tests pass
- APK at `app/build/outputs/apk/debug/app-debug.apk`

Optional APK identity check:

```bash
$ANDROID_HOME/build-tools/34.0.0/aapt dump badging \
  app/build/outputs/apk/debug/app-debug.apk
```

Confirm `package: name='com.touchdeveloper.app'`, `sdkVersion:'26'`, and that the
launchable activity is `com.touchdeveloper.app.MainActivity`.

## B. Install on a phone

1. Enable Developer Options and USB debugging on the phone.
2. Connect the phone and run:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

3. Launch **Touch Developer**. The app opens in portrait mode.

Alternatively, download the APK artifact from the GitHub Actions run named
`Touch-debug-<run number>`.

## C. First workflow test — demo mode (no credentials)

This exercises the whole path without any account.

1. On the dashboard, confirm the `[DEMO]` banner appears and tap
   **Refresh repositories**.
2. Two demo repositories appear. Confirm each shows branch, latest commit, build
   status, latest APK, and latest source ZIP.
3. Tap the first repository. The Repository File Screen lists demo files and
   folders, and shows `Path: /`.
4. Tap a file to open the action menu. Choose:
   - **Open or preview** — a `[DEMO]` preview appears.
   - **Copy** — a "Copied to clipboard" toast appears.
   - **Paste or replace** — edit the text and tap **Stage**. Confirm the dialog
     says nothing has been sent to GitHub.
   - **Delete** — a confirmation appears first.
   - **Compare versions** — confirm the "Coming in next version" message.
   - **Add to build** / **Exclude from build** — the row flag changes.
5. Go back and open **Repository actions**:
   - **Commit changes** — a review dialog lists the staged change, then a commit
     message dialog appears. Confirm the result says the commit is local-only.
   - **Push to GitHub** — confirm it fails with the explicit "unavailable in demo
     mode" message. No network request is made.
   - **Create source ZIP** — a real ZIP is created from local files (if any were
     uploaded/staged) and the manifest is shown.
   - **Delete repository** — confirm two confirmations appear. In demo mode the
     result states nothing was deleted on GitHub.
6. Open **AI Build Prompt**:
   - Confirm the selected repository and branch are shown.
   - Paste text in both fields and tap an example prompt button.
   - Leave the "simulate a failure" checkbox off; tap **Build APK**. The Build
     Status Screen shows a live demo transcript ending in `Successful`, marked
     `[DEMO]`.
   - Go back, tick the simulate-failure checkbox, and tap **Build APK** again.
     Status ends in `Failed` with error output.
   - Tap **Copy Errors** and confirm the clipboard message.
   - Tap **Send Errors to OpenHands**. In demo mode confirm it reports the
     endpoint is not configured.
7. Open **Handoff Package**:
   - Tap **Create handoff ZIP**. A manifest lists included entries and any
     unavailable sections.
   - Tap **Show last handoff manifest** to re-display it.
8. Tap **Setup** and confirm the activity history lists every action above, with
   destructive actions marked `(confirmed)` where applicable.

## D. Live GitHub test (requires a token)

1. In **Setup**, paste a GitHub personal access token and tap
   **Save GitHub token securely**. Confirm the masked summary shows only the last
   four characters.
2. Return to the dashboard and tap **Refresh repositories**. Confirm real
   repositories load and the `[DEMO]` label is gone.
3. Open a repository and a small text file:
   - **Open or preview** shows real content.
   - **Paste or replace** with a small edit, then **Commit changes**, then
     **Push to GitHub**.
   - Confirm the public-push dialog lists the changed path before you confirm.
   - Refresh the file on GitHub to confirm the push.
4. Optionally test **Create branch** and confirm the branch appears on GitHub.
5. Remove the token in **Setup** and confirm the app returns to demo mode.

## E. Safety checks

1. Try to delete a file named like `README.md` or an `.apk` file. Confirm the app
   blocks it as a protected artifact.
2. Confirm deleting a repository always asks twice.
3. Confirm the clipboard and any exported ZIP contain no token text.
4. Turn off Wi-Fi/mobile data and confirm a live action reports a clear network
   error instead of claiming success.
