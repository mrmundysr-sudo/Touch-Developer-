# Change note: repository management

## Added

- Dashboard action to create a GitHub repository by name, with optional description.
- New repositories default to private; the owner may choose public visibility.
- A README is initialized so the new repository has a default branch.
- Confirmation appears before the app sends the create request. The dashboard
  adds the repository only after GitHub confirms success.
- Missing fine-grained token permission errors identify the required repository
  creation or administration write permission.

## Existing behavior retained

- Repository deletion remains available from **Repository actions** and requires
  two confirmations.
- Demo mode never reports a real repository create operation as successful.

## Verification

- Added a JVM test ensuring demo mode never claims repository creation succeeded.
- GitHub Actions should run unit tests, lint, and assemble the debug APK for the
  pull request. Local Gradle execution was unavailable because the wrapper could
  not reach the Gradle distribution host in this environment.
