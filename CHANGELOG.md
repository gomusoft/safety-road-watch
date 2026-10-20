# Changelog

All notable changes to this project. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
versions: [Semantic Versioning](https://semver.org/). Each release has English and Thai notes.
The section for a tag `vX.Y.Z` becomes that GitHub release's notes, so write it for users.

## [Unreleased]

## [0.1.0] - 2026-10-20

First public build. / รุ่นแรกที่เปิดให้ใช้

### Added
- Dashcam screen that keeps a rolling, video-only buffer (no audio is recorded while driving).
- One-press capture with the big button or a Bluetooth / volume key: about 30 s before and 30 s after.
- Voice note after the press, kept as `voice.wav` and transcribed in Thai on Android 13+.
- Plate, province and violation types picked out of the voice note, confirmed out loud.
- Library: replay the voice note, play clips, merge chosen clips into one video, share with a ready-made report, delete.
- Time and GPS location saved with every event.

### เพิ่ม
- หน้ากล้องหน้ารถที่อัดวนเฉพาะภาพ ไม่อัดเสียงระหว่างขับ
- กดปุ่มใหญ่หรือรีโมท Bluetooth / ปุ่มเสียง ครั้งเดียว เก็บคลิปก่อนกดประมาณ 30 วินาที และหลังกด 30 วินาที
- บันทึกเสียงหลังกดปุ่ม เก็บเป็นไฟล์ `voice.wav` และแปลงเป็นข้อความภาษาไทย (Android 13 ขึ้นไป)
- แยกทะเบียน จังหวัด และประเภทความผิดจากเสียง แล้วพูดยืนยันกลับ
- คลัง: ฟังเสียง ดูคลิป รวมคลิปที่เลือกเป็นคลิปเดียว แชร์พร้อมข้อความรายงาน และลบ
- บันทึกเวลาและตำแหน่ง GPS ทุกเหตุการณ์

[Unreleased]: https://github.com/gomusoft/safety-road-watch/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/gomusoft/safety-road-watch/releases/tag/v0.1.0
