# Touch Development Standard — MASTER

## Core principles

- GitHub is the authoritative source repository.
- Keep universal rules in Touch Development and app-specific rules in each app repository.
- Prefer complete, testable changes over disconnected snippets.
- Preserve known-good behavior while changing only requested behavior.
- Inspect current source before editing.
- Treat successful compilation as necessary but not sufficient; owner device testing remains important.

## Android build standard

Touch Android repositories should provide a GitHub Actions workflow that:
- checks out source;
- installs the project's required Java version;
- makes the Gradle wrapper executable;
- runs the appropriate Gradle APK build;
- uploads the APK as a workflow artifact;
- triggers on pull requests and pushes to the main branch;
- may additionally support manual workflow_dispatch when useful.

A missing connector command for manual workflow_dispatch does not prevent CI use when push/PR triggers exist.

## Source handoff standard

For corrected-source handoffs, provide a short handoff/change note documenting:
- intentional changes;
- removed/replaced behavior;
- behavior that must not be restored;
- affected tests or assumptions.

Before handoff, sanity-check dependent source references. Tests that encode superseded behavior should be updated rather than used as a reason to undo an intentional change.

## Touch product defaults

Unless an app specification overrides them:
- keep apps compact and efficient;
- prioritize clear phone-first interaction;
- preserve deliberate safety protections;
- avoid unnecessary backend/user-data dependencies;
- maintain consistent Touch branding and reusable components where appropriate.

App-specific interaction, orientation, artwork, safety, monetization, and feature rules belong in that application's specification.
