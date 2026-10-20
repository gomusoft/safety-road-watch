# AGENTS.md: safety-road-watch

Rules for every agent working in this repo.

- **Public repo.** Never commit a real recording, voice note, photo, plate number, location, secret or key other than the committed debug keystore (public by design, debug only). `.gitignore` blocks media files; do not force-add them.
- **Privacy rule:** the rolling dashcam video never records audio. The microphone is on only after the user presses the capture button, and only until they stop or 12 s pass. Data stays on the device; nothing is uploaded by the app.
- **Scope:** KISS and YAGNI. Native Kotlin, Android Views, CameraX only. Add a library only when the feature cannot be done reasonably without it.
- **Branch model: `main` only.** Work on short branches (`feature/`, `fix/`, `chore/`, `docs/`, `claude/`) merged into `main` by PR. Never push directly to `main`.
- **Commits:** Conventional Commits with a scope (`feat(capture): ...`), header 50 chars or less.
- **CI:** `.github/workflows/apk.yml` runs unit tests and builds the debug APK on every push and PR, and publishes it as the `latest` release on pushes.
