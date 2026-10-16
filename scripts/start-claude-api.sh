#!/usr/bin/env bash
set -euo pipefail

# Run inside the Touch Developer GitHub Codespace.
# Uses the ANTHROPIC_API_KEY Codespaces secret; never prints the key.
if [[ -z "${ANTHROPIC_API_KEY:-}" ]]; then
  echo 'ANTHROPIC_API_KEY is not available in this Codespace.'
  echo 'Confirm the secret is authorized for Touch-Developer-, then stop and restart this Codespace.'
  exit 1
fi
if ! command -v claude >/dev/null 2>&1; then
  echo 'Claude Code CLI is not installed in this Codespace.'
  exit 1
fi
# Remove conflicting provider/auth configuration without touching the API key.
unset ANTHROPIC_AUTH_TOKEN CLAUDE_CODE_USE_BEDROCK CLAUDE_CODE_USE_VERTEX CLAUDE_CODE_USE_FOUNDRY
printf '%s\n' 'Anthropic API key found (value hidden). Launching Claude Code with API billing.'
exec claude "$@"
