# Touch Development

Private master infrastructure repository for the Touch app family.

This repository is the durable source of truth for development workflow shared across Touch projects. Individual app repositories contain app-specific source and specifications; universal development behavior belongs here.

## Required workflow

Before working on a Touch project, read `CHATGPT_DEVELOPMENT_WORKFLOW.md` and `TOUCH_DEVELOPMENT_STANDARD.md`.

GitHub is the source of truth for source code. GitHub Actions is the standard Android build path. OpenHands/DeepSeek is an optional alternate development path chosen by the owner, not an automatic fallback.

## Project model

Touch Development (this repository)
- universal development rules
- GitHub/ChatGPT workflow
- Android CI/build templates
- handoff standards
- reusable standards and components

Individual repositories
- app-specific source
- app-specific assets
- app-specific specifications
- a pointer back to this master repository

## Touch Developer app (version 1)

The root of this repository now contains the buildable Touch Developer V1 Android
app (`com.touchdeveloper.app`), built from the verified `TouchDeveloper-V1-HANDOFF.zip`
specification. See `docs/README-APP.md` for setup and run instructions, and the
other files in `docs/` for the implemented/placeholder feature lists, required
credentials, known limitations, testing steps, and the build log.
