# Touch Developer repository notes

## What this repo is

This is the master Touch Development infrastructure repository. Its root now also
contains the Touch Developer V1 Android app, built from the verified
`TouchDeveloper-V1-HANDOFF.zip` specification.

- Authoritative spec for the app: `TouchDeveloper-V1-HANDOFF.zip`
  (see `TOUCH-DEVELOPER-V1-BUILD-PROMPT.md` inside it).
- Universal workflow rules: `CHATGPT_DEVELOPMENT_WORKFLOW.md`,
  `TOUCH_DEVELOPMENT_STANDARD.md`.

## Build commands

```bash
# The environment may not have an SDK at the default location.
echo "sdk.dir=$HOME/android-sdk" > local.properties   # git-ignored

./gradlew --no-daemon assembleDebug          # debug APK
./gradlew --no-daemon testDebugUnitTest      # JVM unit tests
./gradlew --no-daemon lintDebug              # Android lint
```

Artifacts land in `app/build/outputs/apk/debug/app-debug.apk`. Copies for
handoff live in `docs/build-output/`.

## Conventions

- Package `com.touchdeveloper.app`, Java 17, single activity, portrait only.
- `minSdk 26`, `compileSdk`/`targetSdk` 34, AGP 8.4.2, Gradle wrapper 8.7.
- No AndroidX: the app uses framework views only (`android.useAndroidX=false` in
  `gradle.properties`). Keep new UI on framework views unless the owner approves
  adding AndroidX.
- Do not use `java.net.URLEncoder.encode(String, Charset)` or other API 33+
  overloads; lint enforces minSdk 26.
- Integrations must stay behind the service interfaces and must use a clearly
  labelled demo implementation when unconfigured. Demo results are never `ok`.
- Never log, export, or commit credential values.

## Safety invariants

- Confirmations are required before destructive or public actions
  (`com.touchdeveloper.app.safety.Confirmations`).
- Credentials are stored only via `SecureCredentialService` (Keystore AES-GCM).
- A build or API action is never reported as successful unless the result
  confirmed it (`com.touchdeveloper.app.util.Result`).
