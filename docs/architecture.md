# Architecture

One Android app module, no backend. Kotlin, Android Views (no Compose), CameraX 1.4.
Package `watch.saferoad`, minSdk 26, target/compile SDK 35, JDK 17.

## Components

| File | Responsibility |
|---|---|
| `HomeActivity.kt` | Launcher screen (dark): big start button, library with event count, settings, and a one-line readiness check for camera / mic / GPS permissions (tap to ask). Opens the camera at once when the "open camera on start" setting is on |
| `SettingsActivity.kt`, `Settings.kt` | Two choices kept in `SharedPreferences`, both off by default: address lookup from GPS (asks for consent first) and open the camera on start. Also shows the version |
| `MainActivity.kt` | Dashcam screen. Permissions, capture button and hardware keys (`onKeyDown`), status text, TTS confirmation, saves the parsed result into the event |
| `Recorder.kt` (class `Rolling`) | Rolling video: back camera, `VideoCapture<Recorder>` at HD, **no audio**. Records 20 s segments to `filesDir/ring/seg_<startMillis>.mp4`, keeps the newest 4. On trigger, moves recent segments into the event and keeps routing new segments there until trigger + 30 s |
| `Mic.kt` | The only place the microphone is opened (`Mic.record`) and `pcmToWav`; used by the voice note and by the player's narration. Source `VOICE_RECOGNITION`, plus `NoiseSuppressor` and `AutomaticGainControl` when the phone has them, and a 150 Hz high-pass filter `HighPass` against engine rumble), 16 kHz mono PCM16 |
| `Voice.kt` | Voice note: records through `Mic`, writes `voice.wav`; then transcribes the PCM with `SpeechRecognizer` + `RecognizerIntent.EXTRA_AUDIO_SOURCE` (API 33+), on-device recognizer if available |
| `Parse.kt` | Pure Kotlin (no Android imports, unit tested in `app/src/test`). Normalises spoken letter names and Thai digits, extracts plate, province (77 + aliases) and violation keywords |
| `EventStore.kt` | Creates event folders, reads/writes `event.json`, tracks location (GPS + network listeners while the camera screen is visible, last-known fallback) |
| `Report.kt`, `Recipients.kt`, `GuideActivity.kt` | Sending a report: report text (with the user's profile), attachments, mail intent, gallery copy, URL confirm dialog and the first-use rules; the agencies come from `assets/recipients.json`; the guide screen shows steps for one agency (see CONTRIBUTING.md) |
| `LibraryActivity.kt` | Library UI built in code: cards per event, voice playback (`MediaPlayer`), clip playback via `FileProvider` + `ACTION_VIEW`, merge, share (`ACTION_SEND_MULTIPLE`), delete |
| `PlayerActivity.kt` | In-app player: `VideoView` + `MediaController` for one clip or `merged.mp4` (path in the intent, no external app) |
| `Thumb.kt` | The still picture of an event: the frame at the press moment (`MediaMetadataRetriever`), saved once as `thumb.jpg`, cached in memory. Library draws it on a background thread; share attaches it |
| `Merge.kt` | Concatenates MP4 clips without re-encoding: `MediaExtractor` → `MediaMuxer`, shifting timestamps |

## Flow

```
camera ──20 s segments──▶ filesDir/ring/  (newest 4 kept, video only)
                │
press / BT key ─┤ trigger
                ▼
filesDir/events/<yyyyMMdd_HHmmss>/
    clip_<startMillis>.mp4   ← ring segments from the last ~35 s, then new ones until +30 s
    voice.wav                ← mic on only from the press until the second press, 8 s of silence or 45 s
    event.json               ← time, lat/lon, transcript, plate, province, violations, km, road, tambon, amphoe, changwat, note
    merged.mp4               ← optional, made in the library
    thumb.jpg                ← still picture at the press moment, made by the library on first view
```

The mic and the camera never compete: the video has no audio track, so `AudioRecord` owns the mic during the note.
Transcription runs after the note is recorded, from the PCM file, so the recogniser never opens the mic itself.

## `event.json`

```json
{
  "time": "2026-10-20T08:15:30",
  "lat": 14.97, "lon": 102.10,
  "transcript": "ทะเบียน 8กค3692 นครราชสีมา ล้อล้น ควันดำ",
  "plate": "8กค 3692",
  "province": "นครราชสีมา",
  "violations": ["ล้อล้นตัวถัง", "ควันดำ"],
  "note": ""
}
```
`lat`, `lon`, `plate`, `province` and the place fields (`km`, `road`, `tambon`, `amphoe`, `changwat`) may be `null`. The place fields are filled by the library's "fetch address" button (`Geocoder`, needs internet), by hand, or `km` from the voice note. `note` holds a human-readable problem (e.g. transcription failed).
Clip offsets shown in the library are computed from the clip file name against the folder time.

## Storage and privacy

- Everything lives in app-private storage (`filesDir`). Uninstalling the app deletes all events.
- Data leaves the phone only when the user presses **Share**, or after they accept the opt-in address lookup (the phone's geocoder sends the coordinate to its map service). Lookup failures (offline) are ignored; nothing else depends on the internet.
- The repo is public: `.gitignore` blocks `*.mp4`, `*.wav`, `*.pcm`, `*.m4a`. Never commit real evidence.

## Build, CI and releases

- `gradle assembleDebug` (no wrapper; CI pins Gradle 8.14.3 via `gradle/actions/setup-gradle`).
- `app/debug.keystore` is committed on purpose (public, debug only) so every build signs with the same key
  and installs over the previous one without losing data. It is **not** a release key.
- `.github/workflows/apk.yml` (code changes only; docs-only changes skip it):
  - every PR, and a manual run on any branch (Actions → apk → Run workflow): unit tests + debug APK as a 7-day artifact;
  - push to `dev`: replaces the `dev-latest` pre-release (the owner's test build, one fixed link);
  - push to `main`: replaces the `latest` pre-release;
  - push of a tag `vX.Y.Z`: creates a versioned release whose notes are the matching section of `CHANGELOG.md`.
- Branch model `dev-main`: work → `dev` (squash PR) → `main` (promotion PR, merge commit). The standard dev-pose
  release workflow (standard 10) is not built yet; see the roadmap.
- `.github/workflows/sync-main.yml`: after any push to `main`, opens a PR `main` → `dev` (skipped when the trees are equal or a PR is open). Needs "Allow GitHub Actions to create and approve pull requests" in repo settings. PRs made with the built-in token do not start other workflows, so the `apk` check does not run on them.
- `versionCode` = CI run number; `versionName` = the tag without `v`, or `dev.<run>` for untagged builds.
- Process: [CONTRIBUTING.md](../CONTRIBUTING.md#releasing).

## Known risks and limits

| Risk | Effect | Mitigation |
|---|---|---|
| `EXTRA_AUDIO_SOURCE` needs API 33 and a recogniser that honours it | No transcript on older phones or some recognisers | `voice.wav` is always kept; note explains why |
| ~100 ms gap between 20 s segments (was 10 s: twice as many joins) | Tiny jumps in merged video, about 2 to 3 per event | Acceptable for evidence. Zero gap needs our own `MediaCodec` encoder with an in-memory ring of encoded frames (a rewrite of `Recorder.kt`, and CameraX would have to feed it); only if the joins still bother in the field |
| Recording stops when the app is not in the foreground (library open, screen off) | No pre-roll right after returning | Keep the camera screen open while driving; a foreground service is on the roadmap |
| Bluetooth media keys may be taken by another app's media session | Remote does nothing | Volume keys work more reliably; a `MediaSession` is on the roadmap |
| Phone heat and storage | Throttling, full storage | HD only; ring keeps ~40 s; events are deleted by the user |
| Not compiled locally in the original session (no Android SDK) | Only CI compiles | CI must be green before merge |
