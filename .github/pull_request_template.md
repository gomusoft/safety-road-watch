## Summary
<1–3 sentences in plain language: what changes for the user>

## Context / Why
<the problem or goal; link the task, roadmap item or decision>
<!-- Bug PRs: Problem → Root cause → Fix, plus the regression test that fails before the fix. -->

## Changes
- <grouped by area; what and why, not a line-by-line diff>

## Screenshots / Demo
| Before | After |
|---|---|
| <phone screenshot> | <phone screenshot> |

## How to Test
1. Install the APK from this PR's `apk` run (artifact), or from `dev-latest` after merge.
2. <step>
3. <expected result>

## Changelog
- [ ] Added under `[Unreleased]` in `CHANGELOG.md` (English + Thai), or not user-visible

## Risk & Rollback
- **Risk:** low / medium / high: <what could break, who is affected>
- **Rollback:** revert the PR; reinstall the previous APK (same signing key, data is kept)

## Privacy & Safety Checklist
- [ ] No real recordings, voice notes, plates, locations, secrets or keys committed
- [ ] The rolling video still records no audio; the mic is only on after a capture press
- [ ] Nothing leaves the phone without the user pressing Share
- [ ] Anything used while driving works with one big tap or a hardware key

## Quality Checklist
- [ ] CI green (unit tests + APK build)
- [ ] Tests added or updated for new logic or the regression
- [ ] Tried on a real phone (say which model / Android version), or say it was not
- [ ] ADR added if an architecture or technology decision was made; owner decisions recorded in `docs/product.md`

## Notes for Reviewer / Follow-ups
<open questions, known limits, follow-ups>
