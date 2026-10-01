# Touch Developer — App README (setup and run)

Touch Developer is a phone-first Android control center for managing small Android
projects through GitHub, AI instructions, and OpenHands.

Version 1 focuses on this working path:

`Select repository → paste source/instructions → build → download APK and complete handoff ZIP`

This app was built strictly from the verified handoff package
`TouchDeveloper-V1-HANDOFF.zip` and the standards in
`CHATGPT_DEVELOPMENT_WORKFLOW.md` and `TOUCH_DEVELOPMENT_STANDARD.md`.

## Project facts

| Item | Value |
| --- | --- |
| Language | Java |
| Structure | Single activity (`MainActivity`) |
| Package | `com.touchdeveloper.app` |
| minSdk | 26 |
| targetSdk / compileSdk | 34 |
| Orientation | Portrait (locked in the manifest) |
| Gradle | 8.7 (wrapper included) |
| Android Gradle Plugin | 8.4.2 |
| Java | 17 |

## Requirements

- Java 17 (JDK)
- Android SDK platform 34 and build-tools 34.0.0
- Android Studio (optional, for IDE use)

## Build and run

Command line:

```bash
# Point Gradle at your SDK (local.properties is git-ignored)
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

./gradlew --no-daemon assembleDebug
```

The debug APK is written to:

```
app/build/outputs/apk/debug/app-debug.apk
```

Install it on a phone with ADB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Run the unit tests:

```bash
./gradlew --no-daemon testDebugUnitTest
```

Android Studio: open the repository root as a Gradle project and run the `app`
configuration.

## Continuous integration

`.github/workflows/android-debug-apk.yml` builds the debug APK on push, pull
request, and manual dispatch, then uploads the APK as a workflow artifact. It is
the project's `templates/android-debug-apk.yml` template with the build step set
to `assembleDebug`.

## First-run behavior

With no credentials stored, every integration runs in clearly labelled demo mode.
Demo results are prefixed with `[DEMO]`, are never reported as success, and never
contact the network. Open **Setup** to store a GitHub token and an OpenHands
endpoint to switch to the live integrations.

## Documentation

- `docs/IMPLEMENTED-FEATURES.md`
- `docs/PLACEHOLDER-FEATURES.md`
- `docs/REQUIRED-CREDENTIALS.md`
- `docs/KNOWN-LIMITATIONS.md`
- `docs/TESTING-STEPS.md`
- `docs/build-output/build-log.txt`
- `docs/build-output/app-debug.apk`
- `docs/build-output/TouchDeveloper-touch-development-source-v001.zip`

## Verified build record

`./gradlew --no-daemon --no-build-cache clean assembleDebug testDebugUnitTest lintDebug`
completed with `BUILD SUCCESSFUL` (see `docs/build-output/build-log.txt`).

- Unit tests: 14 passed, 0 failed
- Lint: 0 errors, 16 warnings
- Debug APK SHA-256: `eb6ac401e01ad07d87bf3ea47d9d85023254abd911daa7c62d1d06721034eaab`
- Source ZIP: see `docs/build-output/SHA256SUMS` for the checksum. (The ZIP
  contains this README, so its own hash cannot be printed inside it.)

The APK has been built and inspected (`aapt dump badging` confirms package
`com.touchdeveloper.app`, minSdk 26, portrait). It has not been run on a physical
device; see `docs/KNOWN-LIMITATIONS.md`.
