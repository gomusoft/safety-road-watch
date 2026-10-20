# ADR-0001: Native Kotlin app, not a Capacitor web shell

- **Status:** Accepted
- **Date:** 2026-10-20

## Context
The owner has a finance app (Capacitor + React + Tesseract.js, built with `gradlew assembleDebug`) and asked to reuse
what fits. This app's core is a rolling camera buffer (30 s pre-roll), a mic that is on only after a press,
hardware key handling and lossless clip merging. All of these are native Android APIs.

## Decision
Write the app in native Kotlin with Android Views and CameraX. No WebView, no Compose, no extra libraries.
Reused from the finance app: the build-an-APK approach (debug APK, sideload) and the plan to use Tesseract with
`tha` traineddata for later plate OCR.

## Alternatives considered
- **Capacitor + one native plugin:** almost everything would live in the plugin anyway; the WebView adds a layer and weight.
- **Compose:** nicer UI code, but more build setup and dependencies for two simple screens.

## Consequences
- Smallest dependency set and APK (~4.6 MB).
- UI code is plain and a bit verbose (library screen built in code).
- Tesseract will need a native Android binding instead of Tesseract.js when OCR is added.
