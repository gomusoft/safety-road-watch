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
   - The microphone is on **only** after the user presses capture, until they press again, 8 s pass without speech, or 45 s pass.
   - All data stays on the device. The app has no backend, no analytics, no account.
3. **Report to authorities, never shame.** The app shares evidence to official channels. It never posts publicly.
4. **Smallest thing that works (KISS, YAGNI).** The owner had a ~3M-token budget for the whole project.
   Prefer a manual step over a fragile automatic one.

## Requirements: MVP (v0.1.0, built)

| ID | Requirement | Status |
|---|---|---|
| R1 | Dashcam screen: back camera preview, screen stays on, video recorded continuously in a rolling buffer, **video only** | Built |
| R2 | Capture by tapping the big button, or a Bluetooth remote / volume / play-pause / enter key | Built |
| R3 | One capture = about **30 s before** and **30 s after** the press, kept as 20 s clips, so the "before" part is 30 to 50 s and the "after" part 30 to 50 s (whole clips) | Built |
| R4 | After a capture press, record a voice note; stop on the next press, after 8 s of silence (speech resumes the clock), or after 45 s. A new press is allowed at any time, even while the previous event still collects its 30 s or is being transcribed | Built |
| R5 | Keep the voice note as a separate audio file (`voice.wav`) the user can replay | Built |
| R6 | Transcribe the voice note (Thai, `th-TH`), on-device when available | Built (Android 13+ only) |
| R7 | Extract plate (e.g. `8กค 3692`), province (77, with aliases like โคราช, กทม) and violation types from the transcript; letters spoken by name (กอไก่) are understood | Built, unit tested |
| R8 | Spoken confirmation (TTS) of what was saved | Built |
| R9 | Library: list events, replay voice, play each clip, choose clips, **merge** chosen clips into one MP4, share clips + a ready-made Thai report text with a Google Maps link, delete | Built |
| R10 | Record time and GPS location with each event | Built |
| R13 | Home screen (dark): start recording, library with count, settings, permission readiness. Settings: address lookup (opt-in) and open camera on start (off by default) | Built, not field-tested |
| R12 | Place per event: road, kilometer marker, tambon, amphoe, province. Fetched on demand from the GPS point (phone geocoder), km also from the voice note, all editable | Built, not field-tested |
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
| 2026-10-20 | Follows dev-pose standards v0.5.0, class `personal`. Branch model `dev-main`: the owner tests from `dev` before anything is promoted to `main` and released | Owner |
| 2026-10-20 | CI publishes an APK from `dev` automatically (`dev-latest`); any branch can be built by hand | Owner |
| 2026-10-20 | Discord reporting skipped for now (public project; owner will choose a personal or public server later) | Owner |
| 2026-10-20 | Address (road, tambon, amphoe, province) is fetched only when the user presses a button, never automatically: the phone geocoder sends the coordinate to the map service over the internet, which breaks "nothing leaves the phone" if done silently. Kilometer marker comes from the voice note or typing: the phone geocoder does not return it, and speaking while driving is not required (the owner does not always know where they are), so GPS fills the rest. The library shows the coordinate and labels each value as from GPS, voice or edited. Merged video stays in the app until the user saves it to the gallery | Lead agent from owner request, owner to confirm |
| 2026-10-20 | Supersedes the row above: the address lookup is an opt-in **mode** (off by default). Turning it on, or pressing "fetch address" while off, shows a consent dialog; accepting lets the library fill missing addresses automatically. Declining or having no internet never blocks anything else (recording, voice, merge, share, typing the address) | Owner |
| 2026-10-20 | Voice note ends 8 s after the last speech (cap 45 s, was a fixed 12 s that cut people off). While the mic is on the button turns yellow and pulses (no red). A new capture can start at any time; events overlap and the later one gets copies of the footage already collected | Owner |
| 2026-10-20 | `dev` is the default and a protected branch; `main` is protected. Everything reaches `main` only by a promotion PR from `dev`. A `sync-main` workflow opens a PR `main` → `dev` after any push to `main`, so the two never drift (a README edit went straight to `main` once and had to be merged back by hand) | Owner |
| 2026-10-20 | Fewer joins between clips first: record 20 s pieces instead of 10 s (option A). A gap-free recorder with our own `MediaCodec` ring (option B) only if the owner still sees jumps in the field | Owner |
| 2026-10-20 | Video plays inside the app (no external player), each event has a still picture from the moment of the press that is also attached when sharing, and settings are whole-card switches (field test on the owner's phone: the checkbox was not visibly tappable) | Owner |
| 2026-10-20 | Add a dark home screen as the launcher (start / library / settings). "Open camera on start" is off by default but recommended for use in the car. The address lookup choice moves from the library to settings | Owner |
| 2026-10-20 | Capture button on the camera screen: 50% transparent green with a shield, calm and not alarming | Owner |
| 2026-10-20 | Agent team: an Opus-class lead designs and reviews; cheaper models (Sonnet) write the code, to save tokens | Owner |
| 2026-10-20 | Library: ticks on clips survive watching another clip; a single clip is deleted by swiping it sideways, with a confirm dialog (the owner merges at home, not while driving). Video stabilization is on by default and greyed out with a reason on phones that cannot do it. Next: still pictures and zoom in the player, voice choice when merging, then sender profile and recipient settings; the release key and release workflow come after | Owner |
| 2026-10-20 | Voice note: cut engine rumble and vibration (speech-tuned mic source, the phone's noise suppression when present, 150 Hz high-pass) so speech is clearer and the 8 s silence detector is less fooled by car noise. Field test in a real car decides if more is needed | Owner |
| 2026-10-20 | Player: own controls on a `TextureView` so it can zoom (pinch, double tap, pan); "จับภาพ" saves the frame at the current position at full video resolution as `still_<time>.jpg` in the event folder (several per event, listed in the library, opened full screen with zoom, deletable, attached when sharing). Choosing which frame by scrubbing is therefore covered | Owner |
