# Implemented features (version 1)

Everything below is implemented in source and compiles into the debug APK. Items
that depend on credentials or an unverified endpoint behave as described in
`PLACEHOLDER-FEATURES.md` when those are missing.

## Repository Dashboard

- Repository name, current branch, and latest commit (short SHA + message)
- Build status label using the required status set
  (Local, Committed, Pushed, Building, Successful, Failed)
- Latest APK and latest source ZIP names
- Refresh button that calls `GitHubService.listRepositories()`
- Every repository row is clickable and opens the Repository File Screen
- `[DEMO]` label on demo repositories

## Repository File Screen

- Lists files and folders for the selected repository at the current path
- Folder rows navigate into the folder; an "Up one level" button walks back up
- Flags on rows: protected artifact, staged locally, excluded from build
- File action menu with all twelve required items:
  Download, Open or preview, Copy, Paste or replace, Rename, Delete,
  Upload replacement, Compare versions, Restore previous version, Add to build,
  Exclude from build, Download as ZIP

## Repository Action Menu

- Pull latest changes (GitHub commit lookup)
- Upload files (stages text files at a chosen repository path)
- Create branch (confirmed, real GitHub ref creation)
- Commit changes (shows exactly what will change, then asks for a commit message)
- Push to GitHub (separate public-push confirmation)
- Create source ZIP (real ZIP built from locally staged/uploaded files)
- Build APK (opens the AI Build Prompt screen)
- Download latest APK
- Download complete project (opens the Handoff Package screen)
- Open in OpenHands
- Delete repository (two-step confirmation)

## AI Build Prompt Screen

- Large multiline source-code field
- Large multiline AI-instructions field
- Optional project ZIP upload
- Optional asset upload
- Selected repository and selected branch display
- All five example prompts, each loadable with one tap
- Build APK, Send to OpenHands, and Create Handoff ZIP buttons

## Build Status Screen

- Project name, status label, start time, completion time
- Live progress messages and Gradle output
- Error output
- Copy Errors, Send Errors to OpenHands, Rebuild
- Download APK, Download Source ZIP, Download Complete Handoff
- View activity history

## Handoff Package Screen

- Creates a real ZIP containing: source folders, APK (when present), README,
  AI instructions, pasted source code, assets, build log, error log, test
  results, version info, commit info, and a SHA-256 checksum
- Filename follows `TouchDeveloper-[project]-handoff-v001.zip`
- A manifest lists included entries and explicitly lists unavailable sections

## Services

- `GitHubService` — live REST API implementation and labelled demo implementation
- `OpenHandsService` — configured-endpoint implementation and labelled demo
- `BuildService` — OpenHands-routed builds and a labelled simulated demo build
- `ZipService` — real ZIP creation and SHA-256 checksums
- `CredentialService` — Android Keystore AES-256-GCM encrypted storage
- `ActivityLogService` — in-memory + private-file activity history

## Safety

- Confirmation before deleting a file
- Separate, two-step confirmation before deleting a repository
- Protected artifacts blocked from deletion by default
  (current source, latest APK, handoff ZIP, handoff instructions)
- "Show exactly what will change" review before commits
- Separate confirmation before pushing publicly to GitHub
- Activity history of actions, including whether the user confirmed
- Status is never shown as successful unless the API or build result confirmed it
- No plaintext credentials: values are encrypted with a Keystore key and masked
  in the UI; they are never logged or included in a ZIP

## Tests

- 14 JVM unit tests covering demo labelling, safety protections, staging,
  ZIP/handoff creation, checksum generation, and JSON parsing
