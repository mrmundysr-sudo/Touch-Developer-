# Touch Developer — Unified AI Development Workspace
Status: Approved design / implementation pending. Branch: feature/ai-development-workspaces

## Objective
Extend existing Android Touch Developer app without breaking repository refresh, GitHub authentication, 10MB resumable chunk uploads, existing file picker, or GitHub Actions APK download. GitHub remains source of truth.

## User experience
Add AI Development navigation screen with engine choices:
1. Claude Code (coding agent)
2. OpenHands (agent runtime, optionally configured with DeepSeek)
3. DeepSeek (explicitly labeled OpenHands + DeepSeek until a separate agent runtime is configured)

Common workspace: repo and branch picker, assignment prompt, live activity timeline, message composer, status, questions requiring response, progress, spending budget, history, GitHub commit/diff view, GitHub Actions build results and verified APK download. Bottom swipe-up tray: Pause/Resume, Stop, View Changes, Download APK. Show truthful disabled/unsupported states rather than simulated activity.

## Architecture
Use provider adapter interface: connect, capabilities, startAssignment, sendMessage, streamEvents, pause, resume, stop, getChanges, getCosts. Backend service must hold credentials securely and manage long-running sessions; Android app should not ship API keys or use GitHub tokens as AI-provider credentials. DeepSeek API is a model, not an independent coding agent; route through OpenHands runtime and identify this clearly. Provider capabilities govern control enablement. Poll/reconnect with persistent job IDs; never mark assignment complete merely because app disconnects.

## Isolation and safety
Each assignment gets an explicit repo, source SHA, unique feature branch and isolated workspace. Two agents must not push to same branch. Changes reviewed through PR/diff before merging; no automatic overwrite of main. Record agent, model, task, commits, test outcomes and cost. Require confirmation for destructive operations and spending above cap.

## Notifications
Push notifications for questions, errors, completed review, and verified builds; require opt-in, avoid sensitive code or credentials in notification body. Provide in-app fallback if push backend is not configured.

## Build workflow
GitHub Actions is canonical build path. Download APK only after successful workflow run and validated nonempty APK artifact; display provenance (repository, branch, SHA, run ID). Never call a mock or a tiny HTML/error response an APK.

## Delivery phases
1. Inspect existing app and preserve existing functions; implement common navigation/UI and persisted assignment records. Provider availability must be honest.
2. Connect Claude Code through an authenticated backend with live messages/events and supported controls.
3. Connect OpenHands; add DeepSeek model configuration inside OpenHands.
4. Parallel review assignments on separate branches, side-by-side results, push notifications, spend caps and production hardening.

## Acceptance criteria
Existing GitHub setup, repository refresh, upload and APK flows still work. Engine switching preserves per-engine sessions. Offline/reconnect restores history. Unsupported pause/resume controls visibly disabled. Agent cannot overwrite another branch. Secrets absent from logs and APK. Android build succeeds in GitHub Actions and actual APK installs on target Android phone.

This file is a design and implementation contract, NOT a claim that any provider is connected or that an APK has been built.
