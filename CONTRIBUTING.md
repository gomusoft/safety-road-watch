# Contributing

Thank you for helping make Thai roads safer. ขอบคุณที่ช่วยกันทำให้ถนนปลอดภัยขึ้น

Before you start, read [docs/product.md](docs/product.md) (why, principles, requirements) and
[docs/architecture.md](docs/architecture.md). Changes that break a principle (safety, privacy, no public shaming,
small and simple) will not be merged.

## Build
- JDK 17, Android SDK 35, Gradle 8.14: `gradle testDebugUnitTest assembleDebug`
- Or let CI build it: open a PR, or run the `apk` workflow by hand on your branch; the APK is attached to the run.

## Workflow
- Branches: `dev` (default, integration) and `main` (promoted releases). Branch from the latest `dev`:
  `feature/<slug>`, `fix/<slug>`, `docs/<slug>`, `chore/<slug>`, then open a PR into `dev` (squash merge).
  Never push to `dev` or `main` directly.
- Every merge into `dev` publishes the test APK at the `dev-latest` release. To get an APK from any other branch,
  open a PR or run the `apk` workflow by hand on that branch.
- Commits: [Conventional Commits](https://www.conventionalcommits.org/) with a scope, header ≤ 50 characters,
  e.g. `feat(capture): keep recording in background`.
- One purpose per PR. CI must be green. Add a line under `## [Unreleased]` in `CHANGELOG.md` for anything a user notices,
  in English and Thai.
- Never commit real recordings, voice notes, plates, locations or keys (`.gitignore` blocks media files).

## Releasing
Versions follow [SemVer](https://semver.org/): **patch** = fixes only, **minor** = new features, **major** = a change
that breaks existing events or behaviour users rely on.

Interim process until the dev-pose release workflow is added (it will promote `dev` → `main` and tag automatically):

1. In a PR into `dev`, move the `[Unreleased]` entries of `CHANGELOG.md` into a new `## [X.Y.Z] - YYYY-MM-DD` section
   (English + Thai) and update the compare links at the bottom. Merge it.
2. Promote: open a PR from `dev` into `main` and merge it with a **merge commit**. Tag that merge commit on `main`:
   ```
   git tag -a vX.Y.Z -m "vX.Y.Z" && git push origin vX.Y.Z
   ```
3. CI builds the APK and publishes the GitHub release `vX.Y.Z` with that changelog section as its notes.
4. Pushes to `dev` refresh `dev-latest`; pushes to `main` refresh `latest`.

## Reporting channels and the guide
The "send report" guide (Library → "ส่งรายงานให้หน่วยงาน", and Settings → "คู่มือการส่งรายงาน") reads
`app/src/main/assets/recipients.json`. Anyone can add or fix a channel with a normal PR:

- One entry per way of sending: `how` is `web` (an https URL the user sees before it opens), `mail` (opens the mail app with
  the report and files attached) or `phone` (opens the dialer). Only list **official** channels, and never a link that
  arrived by SMS or LINE: fake DLT and police messages circulate. Write `verified` honestly (what you saw, when) and
  set `checked` to today's date. Unverified details stay marked as unverified.
- The research notes live in `docs/guide/channels.md` and `docs/reporting-channels.md`; update them with the same PR.
- Screenshots (optional but wanted): open the real page on a phone-sized screen (about 390 x 844), take one picture per
  step, put it in `app/src/main/assets/guide/<id>-<step>.png`, and list the file names in the entry's `images` array
  (same order as `steps`, `""` for a step without a picture). **Never include personal data** (names, ID or plate numbers,
  phone numbers, cookies, logged-in pages) and never submit a real or fake report to take a picture.
- The app has no internet permission and never sends data itself. Keep it that way: the guide only opens the browser, the mail app or the dialer.
- Users can press "แจ้งว่าไม่ตรง" in the guide: it opens a prefilled GitHub issue. Fix the entry and close the issue.

## License
By contributing you agree your contribution is licensed under [CC BY-NC-SA 4.0](LICENSE).
