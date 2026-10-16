# Touch Developer — Living App Specification
Updated 2026-10-16. Source of truth: GitHub, branch `claude-code-mobile-control`.

## Product principle
**One Interface. Multiple AI Engines. One GitHub Source of Truth.** Android phone-first, consistent UX regardless of engine. Never change a user's main branch automatically. Never claim a build or AI task succeeded without verification. Keep existing Touch Developer GitHub/file upload/build functions.

## Phase 1 — Claude Code workspace (active development)
Claude Code is the only AI engine being implemented now. Use existing `ANTHROPIC_API_KEY` and `TOUCH_TARGET_REPOS_TOKEN` GitHub Actions repository secrets; no API keys in APK, no new key requests. Current runner is GitHub Actions workflow `.github/workflows/claude-assignment.yml` on main, targeting approved working branches. Avoid additional hosting costs until necessary.

### Main workspace screen
- Assignment identity: target repository, branch, current run ID, status (queued/running/waiting/completed/failed/cancelled), elapsed time.
- Live activity window: stream observable Claude actions, edits, test/build events and explicit progress summaries; do not claim to show private chain-of-thought. Current implementation can query GitHub Actions run status; detailed live event streaming requires runner instrumentation.
- Statistics: changed files, API cost only if verified, runner time; use `—` rather than fabricated values.
- **Message Claude** composer always visible below activity feed. Messages must be associated with a specific authenticated run, show queued/delivered/acknowledged/failed states, and persist across app restarts. Never pretend an unsent message was delivered.
- When Claude asks a question, show it immediately above the composer; allow response without switching apps. Support autonomous and ask-before-major-changes modes, with safe default autonomous for routine edits.
- Collapsible **swipe-up bottom tools**: Pause/Resume, Stop Assignment, View Changes, Download APK. Prefer a native bottom sheet with gesture and handle; temporary tap-to-expand drawer is acceptable until native swipe is built.
- Pause/Resume must actually checkpoint or control a persistent session; do not label a simple workflow cancel as pause. Stop requires confirmation and must show result. View Changes shows commits/diffs, distinguish uncommitted work from committed work. Download APK only enabled for successfully built, verified Android APK artifact; show source branch and build run.
- Show recent job history and exact errors, not generic accepted-dispatch messages. GitHub 204 only means request accepted.

### Notifications
- Push notification with Android sound/vibration when Claude asks for input; tapping deep-links to the correct session and question.
- Additional optional alerts: completed assignment, failure/blocker, APK ready.
- Android 13+ notification permission, user preferences, Android notification channels, Do Not Disturb behavior, privacy-preserving text previews.
- Firebase Cloud Messaging is a candidate free notification transport; a secure authenticated backend/bridge must deliver runner events and route responses. Never embed server keys in APK. No promise of background push before backend is configured.
- Prevent duplicate alerts and notification storms. Question must remain visible in workspace even if push is denied.

### Runner and reliability
- Current one-shot GitHub Actions `claude -p` cannot accept messages mid-execution or pause/resume. Plan a staged/checkpoint-based job queue first; consider persistent isolated runner only if truly interactive behavior is needed.
- Stream structured, sanitized observable events (Claude stream-json or equivalent) to authenticated progress store; redact secrets, limit retention. GitHub logs can provide coarse fallback but may lag.
- Authenticated, run-scoped message queue with delivery acknowledgments and safe points; no false real-time claims.
- Explicit stop/cancel using GitHub Actions API where supported; distinguish cancelling, cancelled and failed.
- GitHub Actions builds must run tests/lint/assemble and verify APK before surfacing download. Keep source/branch/commit provenance.
- Limit concurrent assignments, rate-limit messages, and enforce spending/time budgets with clearly separated Claude API and GitHub runner charges.
- First paid Touch V run 38864498412 reached Claude execution but failed; root cause requires report/log inspection. Do not treat as successful.

### Delivery milestones
1. Show genuine GitHub run status and failures in app; accessible expandable controls and visible message composer with honest unavailable states.
2. Add run-scoped status history, live event reporting, file diffs, verified APK linking, and actual cancel control.
3. Add queued instructions and question/answer checkpoints; ensure acknowledgments and safe resume.
4. Add FCM push notifications with sound/vibration, session deep links, and settings.
5. If justified, implement persistent interactive Claude sessions and true pause/resume. Test on Android hardware and via GitHub Actions before claiming complete.

## Phase 2 — OpenHands + DeepSeek (planned; do not implement now)
- Reuse user's existing **free-use OpenHands + DeepSeek arrangement** if its terms and technical interface permit; verify actual endpoint, authentication, model routing, licensing and cost before wiring. Do not silently replace it with paid DeepSeek API calls.
- DeepSeek is a model provider, not by itself an autonomous coding environment. OpenHands can be the coding agent using DeepSeek.
- Reuse **exact same Touch Developer UX**: repository/branch, assignment, activity, always-visible messages, question prompts, bottom sheet, notifications, diffs, verified APK, job history.
- Build provider adapter contract: create assignment, get status/events, send instruction, answer question, pause/resume if supported, stop, retrieve changes, trigger/verify APK build, cost reporting when available.
- Label unsupported controls clearly rather than fake parity; backend implementations may differ while front-end stays consistent.
- Keep GitHub source of truth; separate branches/worktrees and isolated commits per engine; never let concurrent engines overwrite each other.
- Potential future independent review mode: Claude premium pass, OpenHands+DeepSeek audit, consolidated reports and conflict-aware review; user chooses when to run and whether to merge.
- No automatic engine switching or paid fallback.

## Status honesty (2026-10-16)
Implemented in app development branch: Claude assignment dispatch; latest Actions run status refresh; message composer preview (not delivered); expandable tool drawer; GitHub links for changes/builds. Not yet implemented: true swipe gesture, per-run live stream, interactive message delivery, push, pause/resume, in-app cancellation, verified direct APK downloads, multi-engine integration. Keep UI and release notes aligned with actual capabilities.
