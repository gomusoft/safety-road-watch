# Architecture

One Android app module, no backend. Kotlin, Android Views (no Compose), CameraX 1.4.
Package `watch.saferoad`, minSdk 26, target/compile SDK 35, JDK 17.

## Components

| File | Responsibility |
|---|---|
| `MainActivity.kt` | Dashcam screen. Permissions, capture button and hardware keys (`onKeyDown`), status text, TTS confirmation, saves the parsed result into the event |
| `Recorder.kt` (class `Rolling`) | Rolling video: back camera, `VideoCapture<Recorder>` at HD, **no audio**. Records 10 s segments to `filesDir/ring/seg_<startMillis>.mp4`, keeps the newest 4. On trigger, moves recent segments into the event and keeps routing new segments there until trigger + 30 s |
| `Voice.kt` | Voice note: `AudioRecord` 16 kHz mono PCM16 on a background thread, writes `voice.wav`; then transcribes the PCM with `SpeechRecognizer` + `RecognizerIntent.EXTRA_AUDIO_SOURCE` (API 33+), on-device recognizer if available |
| `Parse.kt` | Pure Kotlin (no Android imports, unit tested in `app/src/test`). Normalises spoken letter names and Thai digits, extracts plate, province (77 + aliases) and violation keywords |
| `EventStore.kt` | Creates event folders, reads/writes `event.json`, tracks location (GPS + network listeners while the camera screen is visible, last-known fallback) |
| `LibraryActivity.kt` | Library UI built in code: cards per event, voice playback (`MediaPlayer`), clip playback via `FileProvider` + `ACTION_VIEW`, merge, share (`ACTION_SEND_MULTIPLE`), delete |
| `Merge.kt` | Concatenates MP4 clips without re-encoding: `MediaExtractor` → `MediaMuxer`, shifting timestamps |

## Flow

```
camera ──10 s segments──▶ filesDir/ring/  (newest 4 kept, video only)
                │
press / BT key ─┤ trigger
                ▼
filesDir/events/<yyyyMMdd_HHmmss>/
    clip_<startMillis>.mp4   ← ring segments from the last ~35 s, then new ones until +30 s
    voice.wav                ← mic on only from the press until the second press or 12 s
    event.json               ← time, lat/lon, transcript, plate, province, violations, note
    merged.mp4               ← optional, made in the library
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
`lat`, `lon`, `plate`, `province` may be `null`. `note` holds a human-readable problem (e.g. transcription failed).
Clip offsets shown in the library are computed from the clip file name against the folder time.

## Storage and privacy

- Everything lives in app-private storage (`filesDir`). Uninstalling the app deletes all events.
- The only way data leaves the phone is the user pressing **Share**.
- The repo is public: `.gitignore` blocks `*.mp4`, `*.wav`, `*.pcm`, `*.m4a`. Never commit real evidence.

## Build, CI and releases

- `gradle assembleDebug` (no wrapper; CI pins Gradle 8.14.3 via `gradle/actions/setup-gradle`).
- `app/debug.keystore` is committed on purpose (public, debug only) so every build signs with the same key
  and installs over the previous one without losing data. It is **not** a release key.
- `.github/workflows/apk.yml`:
  - every push and PR: unit tests + debug APK, uploaded as a workflow artifact;
  - push to `main`: replaces the `latest` pre-release (one stable download link);
  - push of a tag `vX.Y.Z`: creates a versioned release whose notes are the matching section of `CHANGELOG.md`.
- `versionCode` = CI run number; `versionName` = the tag without `v`, or `dev.<run>` for untagged builds.
- Process: [CONTRIBUTING.md](../CONTRIBUTING.md#releasing).

## Known risks and limits

| Risk | Effect | Mitigation |
|---|---|---|
| `EXTRA_AUDIO_SOURCE` needs API 33 and a recogniser that honours it | No transcript on older phones or some recognisers | `voice.wav` is always kept; note explains why |
| ~100 ms gap between 10 s segments | Tiny jumps in merged video | Acceptable for evidence; could switch to one long recording + trimming later |
| Recording stops when the app is not in the foreground (library open, screen off) | No pre-roll right after returning | Keep the camera screen open while driving; a foreground service is on the roadmap |
| Bluetooth media keys may be taken by another app's media session | Remote does nothing | Volume keys work more reliably; a `MediaSession` is on the roadmap |
| Phone heat and storage | Throttling, full storage | HD only; ring keeps ~40 s; events are deleted by the user |
| Not compiled locally in the original session (no Android SDK) | Only CI compiles | CI must be green before merge |
