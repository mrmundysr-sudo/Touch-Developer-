#!/usr/bin/env bash
set -euo pipefail
if ! command -v node >/dev/null; then echo 'Node.js is missing' >&2; exit 1; fi
npm install -g @anthropic-ai/claude-code
claude --version
printf '\nClaude Code installed. Run: claude\nAuthenticate using the supported Claude Code login flow; do not commit tokens or API keys.\n'
