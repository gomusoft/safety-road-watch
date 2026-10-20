# Roadmap and suggestions

Ordered by value for the owner. Nothing here is decided; each item needs the owner's go-ahead.
Feedback from the owner's real phone beats guessing: ask what they saw before changing behaviour.

## Next (after the owner tests v0.1 on a real phone)

1. **Fix what the field test finds.** Most likely spots: transcription on the owner's phone, Bluetooth key delivery,
   merge output playing in LINE. Ask for the on-screen message and the phone model / Android version.
2. **Edit plate and violations in the library.** The voice can be misheard; the share text should be correct
   without editing it in LINE.
3. **Plate reading from a frame (on-device OCR).** The owner's finance app reads Thai on-device with Tesseract
   (`tha` + `eng` traineddata from tessdata_fast). For native Kotlin, use a Tesseract Android binding with the same
   data files. Flow: user picks the clearest frame, crops the plate, OCR suggests the plate, the app compares it
   with the spoken plate. Expect weak results on blurry or night frames; keep the voice as the main source.
4. **Keep recording in the background** with a foreground service (camera type) so opening the library or
   locking the screen does not stop the dashcam.
5. **`MediaSession`** so Bluetooth media buttons always reach the app.

## Later

- Stop the voice note by saying "บันทึกเลย" (needs streaming recognition, which opens the mic through the recogniser).
- Choose which agency to send to per violation type, with the right report template (police vs DLT).
- Export and backup of events.
- Optional wake word with an on-device engine (e.g. an English keyword such as "Hey Road Watch"); check the
  engine's licence against CC BY-NC-SA before adding it.
- F-Droid-style reproducible release signing with a real release key (kept as a GitHub secret).

## Will not do (unless the owner changes the principles)

- Automatic violation detection from video.
- Any upload, server, account or public posting.
