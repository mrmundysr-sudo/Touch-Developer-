# Known limitations (version 1)

## Verification status

- The debug APK builds cleanly and the 14 unit tests pass on the build machine.
- The app has **not** been run on a physical Android device or emulator in this
  environment. Interactive flows are verified by building, by unit tests on the
  pure-Java logic, and by code review only. Owner device testing is required.
- No live GitHub token or OpenHands endpoint was available, so live network paths
  were not exercised against a real server.

## Build and toolchain

- Builds require Java 17 and Android SDK platform 34. Older SDKs will not compile
  the project as configured.
- The wrapper downloads Gradle 8.7 on first run; an offline machine needs Gradle
  already cached.

## Functional limitations

- Remote builds require a configured OpenHands endpoint; otherwise builds are
  simulated demos.
- Real APK download from a build service is not implemented.
- File rename publishing, version comparison, single-file ZIP download, and
  binary file staging are not implemented (see `PLACEHOLDER-FEATURES.md`).
- The OpenHands endpoint paths are assumed and unverified.
- Large files (over roughly 1 MB) cannot be read through the GitHub contents API
  path used.
- The handoff "full source project" is assembled from local staged/uploaded
  files, not from an archive of the remote GitHub tree.

## Platform notes

- The app is locked to portrait orientation.
- The UI uses lightweight programmatic views rather than a rich design system,
  matching the "simple functional placeholder visuals" requirement.
- `android:allowBackup="false"` is set so credentials and staged files are not
  backed up.

## Safety review notes

- The activity log records action descriptions only. Credential values are never
  passed to it.
- The GitHub API error messages surfaced to the user are GitHub's own `message`
  field; they do not contain the token.
