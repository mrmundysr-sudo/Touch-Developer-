# Placeholder / not-yet-implemented features (version 1)

These behaviors are deliberate placeholders. None of them report success, and
none of them send data anywhere. Each either shows an explicit message or falls
back to a clearly labelled demo result, following the handoff instruction to
"show a clear 'Coming in next version' message instead of pretending it worked."

## Build and APK

- Real remote build execution requires a configured OpenHands endpoint. Without
  one, **Build APK** runs a simulated demo build that writes a labelled
  `app-debug-DEMO.apk` placeholder file. The placeholder is not an APK and the UI
  says so.
- Real APK download from a build service is not implemented. The app never claims
  to have downloaded an APK it did not receive.
- Polling a remote build returns the endpoint's status text verbatim; a build is
  only marked Successful when the endpoint reports a successful status.

## OpenHands

- Opening the web UI is presented as a manual step (the link is prepared, not
  launched). No browser deep-link contract has been verified.
- The `/instructions`, `/builds`, and `/builds/{id}` endpoint paths are an
  assumed contract. Responses are reported exactly as received and are labelled
  as "endpoint contract is unverified".

## GitHub file operations

- **Compare versions** and **Restore previous version**: "Coming in next version".
- **Download as ZIP** for a single file: "Coming in next version". Use
  Repository actions → Create source ZIP instead.
- **Rename** is staged locally, but publishing a rename in one push is not
  implemented. The app states this instead of silently dropping it.
- Binary file staging (for example APK or ZIP uploads) is not implemented. Binary
  files are imported into private storage but not staged to GitHub.
- Files larger than roughly 1 MB cannot be read through the GitHub contents API
  path used here; the app reports this rather than failing silently.

## Handoff package

- The "full Android source project" section is built from locally staged and
  uploaded files. A complete archive of the remote GitHub tree is not
  implemented; when no local files exist the section is listed as unavailable.
- If an upload fails, the missing sections are listed in the manifest rather than
  omitted silently.
- Test results are recorded from the build record. Automated on-device tests are
  not part of version 1.

## Demo mode

- With no credentials, all repositories, files, previews, and builds are demo
  data. Demo results are marked `[DEMO]` and are never reported as success.
- The demo build's APK placeholder and the demo source ZIP are local-only.
