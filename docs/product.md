# Product: why, requirements, decisions

Read this first. It is the source of truth for what Safety Road Watch is and is not.
When the owner decides something durable, record it in the decision log at the end, in the same PR.

## Why

Thai roads carry a lot of everyday violations: running red lights, changing lanes across solid lines,
black smoke, wheels sticking out past the body, riding on footpaths, going the wrong way.
Most drivers see them and do nothing, because collecting evidence is awkward and holding a phone
while driving is itself illegal and dangerous.

Thailand lets citizens report violations with video evidence, and in many cases the reporter
receives a share of the fine (see [reporting-channels.md](reporting-channels.md)). The money is small;
the real goal is **safer roads through laws that are actually enforced**.

Safety Road Watch lets an ordinary driver capture usable evidence **without taking their eyes off the road**:
one press, a short spoken note, keep driving. Review and send it after parking.

## Users

- **Primary:** the owner, a driver in Thailand with an Android phone mounted on the dashboard.
- **Later:** any Thai driver who wants to report violations. The app is free and non-commercial.

## Principles (non-negotiable)

1. **Safety first.** Every in-car action works with one tap anywhere on a big button, or a Bluetooth/volume key.
   No typing, no small targets while driving.
2. **Privacy by design.**
   - The rolling dashcam video **never records audio**.
   - The microphone is on **only** after the user presses capture, until they press again or 12 s pass.
   - All data stays on the device. The app has no backend, no analytics, no account.
3. **Report to authorities, never shame.** The app shares evidence to official channels. It never posts publicly.
4. **Smallest thing that works (KISS, YAGNI).** The owner had a ~3M-token budget for the whole project.
   Prefer a manual step over a fragile automatic one.

## Requirements: MVP (v0.1.0, built)

| ID | Requirement | Status |
|---|---|---|
| R1 | Dashcam screen: back camera preview, screen stays on, video recorded continuously in a rolling buffer, **video only** | Built |
| R2 | Capture by tapping the big button, or a Bluetooth remote / volume / play-pause / enter key | Built |
| R3 | One capture = about **30 s before** and **30 s after** the press, kept as 10 s clips | Built |
| R4 | After a capture press, record a voice note; stop on the next press or after 12 s | Built |
| R5 | Keep the voice note as a separate audio file (`voice.wav`) the user can replay | Built |
| R6 | Transcribe the voice note (Thai, `th-TH`), on-device when available | Built (Android 13+ only) |
| R7 | Extract plate (e.g. `8กค 3692`), province (77, with aliases like โคราช, กทม) and violation types from the transcript; letters spoken by name (กอไก่) are understood | Built, unit tested |
| R8 | Spoken confirmation (TTS) of what was saved | Built |
| R9 | Library: list events, replay voice, play each clip, choose clips, **merge** chosen clips into one MP4, share clips + a ready-made Thai report text with a Google Maps link, delete | Built |
| R10 | Record time and GPS location with each event | Built |
| R11 | CI builds an installable APK on every push; the phone downloads it from one link | Built |

## Non-goals (for now)

- Automatic detection of violations from video (red-light, solid line, smoke). Hard, power-hungry,
  error-prone. Humans decide; the app only does the boring parts.
- A wake word ("Hey …"). Thai custom wake words have no good on-device library; an always-on mic conflicts
  with principle 2. Revisit only with an on-device engine (see [roadmap.md](roadmap.md)).
- Any server, account, cloud sync or public feed.
- iOS.

## Decision log

| Date | Decision | By |
|---|---|---|
| 2026-10-20 | Build a minimal Android app; reporting is manual via share to official LINE/Facebook/web channels | Owner |
| 2026-10-20 | Trigger = tap or Bluetooth button (option "A"). Wake word deferred | Owner |
| 2026-10-20 | No audio in the rolling video. Mic only on press. Voice note kept as a separate file **and** transcribed | Owner |
| 2026-10-20 | Capture window 30 s before + 30 s after, user chooses clips to merge later | Owner |
| 2026-10-20 | Native Kotlin + Android Views + CameraX, not the Capacitor shell from the finance app (see [ADR-0001](adr/0001-native-kotlin.md)) | Lead agent, owner accepted |
| 2026-10-20 | Plate OCR from video frames deferred: plate comes from the voice note in the MVP | Lead agent |
| 2026-10-20 | Public repo, license **CC BY-NC-SA 4.0**: free, no commercial use, derivatives must stay open under the same license | Owner |
| 2026-10-20 | Releases: rolling `latest` build from `main` plus versioned tags `vX.Y.Z` with notes from `CHANGELOG.md` | Owner |
| 2026-10-20 | Agent team: an Opus-class lead designs and reviews; cheaper models (Sonnet) write the code, to save tokens | Owner |
