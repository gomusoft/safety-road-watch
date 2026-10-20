# Contributing

Thank you for helping make Thai roads safer. ขอบคุณที่ช่วยกันทำให้ถนนปลอดภัยขึ้น

Before you start, read [docs/product.md](docs/product.md) (why, principles, requirements) and
[docs/architecture.md](docs/architecture.md). Changes that break a principle (safety, privacy, no public shaming,
small and simple) will not be merged.

## Build
- JDK 17, Android SDK 35, Gradle 8.14: `gradle testDebugUnitTest assembleDebug`
- Or push a branch: CI builds the APK and attaches it to the workflow run.

## Workflow
- Branch from `main`: `feature/…`, `fix/…`, `docs/…`, `chore/…`. Never push to `main` directly.
- Commits: [Conventional Commits](https://www.conventionalcommits.org/) with a scope, header ≤ 50 characters,
  e.g. `feat(capture): keep recording in background`.
- One purpose per PR. CI must be green. Add a line under `## [Unreleased]` in `CHANGELOG.md` for anything a user notices,
  in English and Thai.
- Never commit real recordings, voice notes, plates, locations or keys (`.gitignore` blocks media files).

## Releasing
Versions follow [SemVer](https://semver.org/): **patch** = fixes only, **minor** = new features, **major** = a change
that breaks existing events or behaviour users rely on.

1. In a PR, move the `[Unreleased]` entries of `CHANGELOG.md` into a new `## [X.Y.Z] - YYYY-MM-DD` section
   (English + Thai) and update the compare links at the bottom. Merge it.
2. Tag the merge commit on `main` and push the tag:
   ```
   git tag -a vX.Y.Z -m "vX.Y.Z" && git push origin vX.Y.Z
   ```
3. CI builds the APK and publishes the GitHub release `vX.Y.Z` with that changelog section as its notes.
4. Every push to `main` also refreshes the `latest` pre-release, the always-newest test build.

## License
By contributing you agree your contribution is licensed under [CC BY-NC-SA 4.0](LICENSE).
