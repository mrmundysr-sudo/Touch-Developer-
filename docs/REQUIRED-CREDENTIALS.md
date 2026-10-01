# Required credentials and URLs

The app runs without any credentials and shows labelled demo mode. The entries
below switch individual integrations to live behavior. All credentials are stored
with the Android Keystore using AES-256-GCM and are never shown in full, logged,
or included in a ZIP.

## Enter them

Open the app, tap **Setup** in the header, and use the matching section.

| Credential | Purpose | Required for |
| --- | --- | --- |
| GitHub personal access token | Live repository listing, file read/write, branch and commit operations | Real GitHub features |
| OpenHands endpoint URL | Remote build and instruction submission | Real builds |
| OpenHands token (optional) | Authenticates the endpoint calls when the endpoint requires it | Protected endpoints |

## GitHub token

- Create a fine-grained personal access token in GitHub.
- Minimum scopes depend on the actions you use. Read access is enough to list
  repositories and read files; write access to repository contents is needed for
  commit and push.
- The app sends it only in the `Authorization: Bearer <token>` header of GitHub
  API requests. It is not written to any file.

## OpenHands endpoint

- Provide the base URL of your OpenHands/agent workspace.
- The app calls `<endpoint>/instructions`, `<endpoint>/builds`, and
  `<endpoint>/builds/{id}`.
- This contract has not been verified against a running service in this project.
  If your deployment uses different paths, adjust `OpenHandsApiService`.

## What is never needed

- No hardcoded token, password, API key, or private URL is included in the source.
- No account or login is required just to open the app or use demo mode.
