# ChatGPT / GitHub Development Workflow — MASTER

## Purpose

These instructions are durable project infrastructure. They exist so a new ChatGPT/OpenAI coding session can recover the established Touch workflow from GitHub without relying on conversation memory.

## Authority

GitHub repositories are the source of truth for Touch application source code. This private repository is the source of truth for universal Touch development workflow.

## Standard ChatGPT path

When the owner asks ChatGPT/OpenAI to work on a Touch project:

1. Read this master workflow and the Touch Development Standard.
2. Inspect the target application's repository and current source before proposing a handoff.
3. Work directly against GitHub using available GitHub access.
4. Prefer a branch/PR for substantial changes unless the owner explicitly requests another approach.
5. Make the requested source changes.
6. Let the repository's GitHub Actions workflow build/test the change.
7. A push or pull request can initiate the build when CI is configured for push/PR.
8. Absence of a standalone workflow_dispatch/start-workflow connector command is NOT a blocker when push/PR triggers are available.
9. Inspect Actions results and logs when a build fails.
10. Correct the source/build configuration and rebuild as needed.
11. Retrieve the successful APK/workflow artifact for testing when connector capabilities permit.

## Important fallback rule

Do NOT default to OpenHands or DeepSeek merely because ChatGPT lacks a local Android/Gradle build environment or a standalone workflow-dispatch command.

Before suggesting an external coding handoff, use the available GitHub path:
source -> edit -> commit/push or PR -> GitHub Actions -> logs/fix -> APK artifact.

OpenHands/DeepSeek remains an OPTIONAL alternate development path. Use it when the owner chooses it or when it provides a specific needed capability.

## Permissions and interruptions

Repository instructions cannot guarantee external authorization forever. GitHub/plugin authorization can expire, be revoked, or change. If that happens, identify the specific connection problem. Do not reinterpret it as proof that the Touch workflow is impossible or require the owner to redesign the system.

## New Touch projects

Every new Touch app repository should:
- point back to this repository as the master workflow;
- include Android CI that builds a test APK on push/PR;
- follow the Touch Development Standard;
- preserve app-specific specifications in the app repository;
- avoid duplicating universal rules unless necessary for execution.

## Handoffs

When corrected source is handed to OpenHands or another coding agent, include a concise change note:
- intentional changes;
- removed/replaced behaviors;
- locked behaviors that must not be restored;
- known test changes.

Bundled tests may be stale after intentional refactors. Update stale tests to the locked specification rather than reverting intended behavior merely to satisfy them.
