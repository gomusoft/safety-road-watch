# AGENTS.md: safety-road-watch

Rules for every agent working in this repo (Claude, Gemini, Codex, Copilot, Cursor or any other).
`CLAUDE.md` only points here.

## Start here (new session)
1. [docs/product.md](docs/product.md): why the app exists, principles, requirements, **decision log**.
2. [docs/architecture.md](docs/architecture.md): components, data layout, CI and releases, known risks.
3. [docs/roadmap.md](docs/roadmap.md): suggested next work (needs the owner's go-ahead).
4. [docs/reporting-channels.md](docs/reporting-channels.md): Thai reporting channels and rewards (research, verify).
5. [CONTRIBUTING.md](CONTRIBUTING.md): build, workflow, how to release. [CHANGELOG.md](CHANGELOG.md): what shipped.

## Rules
- **Talk to the owner in Thai.** Code, commits and docs in English; user-facing notes in English + Thai.
- **Public repo.** Never commit a real recording, voice note, photo, plate number, location, secret or key other than
  the committed debug keystore (public by design, debug only). `.gitignore` blocks media files; do not force-add them.
- **Privacy rule:** the rolling dashcam video never records audio. The microphone is on only after the user presses
  capture, and only until they stop or 12 s pass. Data stays on the device; the app uploads nothing.
- **Safety rule:** anything used while driving must work with one big tap or a hardware key.
- **Scope:** KISS and YAGNI. Native Kotlin, Android Views, CameraX only. Add a library only when the feature cannot be
  done reasonably without it, and check its licence is compatible with CC BY-NC-SA 4.0.
- **Branch model: `main` only.** Work on short branches (`feature/`, `fix/`, `docs/`, `chore/`, `claude/`) merged
  into `main` by squash PR. Never push directly to `main`.
- **Commits:** Conventional Commits with a scope (`feat(capture): …`), header 50 characters or less.
- **CI must be green** before merge. CI is the only place the app is compiled when a session has no Android SDK.
- **Changelog:** every user-visible change adds a line under `[Unreleased]` in `CHANGELOG.md` (English + Thai).
- **Durable decisions** from the owner go into the decision log in `docs/product.md` in the same PR; architecture
  decisions get an ADR in `docs/adr/`.
- **Agent team:** a strong lead model designs, reviews and writes docs; cheaper models write code from a precise
  brief. The owner works to a token budget, so keep changes small.
