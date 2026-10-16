# Touch Developer — Claude Code Mobile Control

Status: architecture and implementation plan (not yet implemented).
Branch: `claude-code-mobile-control`. Preserve existing features and main branch.

## Objective
Phone-first Android control center to launch and monitor Claude Code jobs in a GitHub Codespace, inspect results, and retrieve verified GitHub Actions APK artifacts. GitHub remains source of truth.

## Architecture
- Android app: repository/branch picker, assignment composer, job timeline, approval queue, build status, APK download, usage dashboard, billing links.
- Secure backend: authenticated job/session API, server-side GitHub and Anthropic credentials, Codespaces lifecycle and Claude Code process management. Never embed API keys or GitHub privileged tokens in the APK.
- Codespace: repository workspace with Claude Code and Android tooling (or Actions builds); isolate jobs by branch; log actions; require confirmation for destructive operations.
- GitHub Actions: canonical Gradle build; only offer APK after success and artifact verification.

## Initial milestone
1. Audit Android source, package namespace, current screens, GitHub authentication, chunked-upload flow, and build workflow.
2. Add an AI Developer screen without changing existing functionality: repo/branch, prompt, session status, stop, progress log, approvals, build artifact link.
3. Build authenticated backend and test a no-cost mock session before enabling paid API usage.
4. Add real Codespaces/Claude Code integration; protect against runaway costs and repeated jobs.
5. Compile via GitHub Actions and test APK installation.

## Billing
- 'Add Credits' opens the official Anthropic Console billing page in browser; payments are never processed inside Touch Developer.
- Report only API usage/cost supported by verified metering. Don't present an estimated wallet balance as authoritative.
- Show Codespaces compute/storage costs separately; session cap is an app-enforced guard, not a guaranteed Anthropic account billing cap.
- Auto-recharge off by default where configured in Anthropic account.

## Security and operational safeguards
- Codespaces port forwarding is not a substitute for authenticated backend access.
- Short-lived credentials, encryption, minimum permissions, secret rotation and log redaction.
- No automatic pushes to main, force-pushes, secret exposure, or destructive changes.
- Explicit user approval for billing/account changes and destructive operations.
- Keep app functional when Codespaces sleeps, API credits run out, or network drops.

## Acceptance criteria
- User can select a repo/branch and submit an assignment from Android.
- Can see real job status and stop a running job.
- Can view Claude Code output and approve required actions.
- Can trigger/track GitHub Actions, and download only a verified APK.
- Existing GitHub file upload, branch management, and APK functions remain operational.
