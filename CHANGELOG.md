# Changelog

All notable changes to this project. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
versions: [Semantic Versioning](https://semver.org/). Each release has English and Thai notes.
The section for a tag `vX.Y.Z` becomes that GitHub release's notes, so write it for users.

## [Unreleased]

### Added
- Library: the GPS point plus road, kilometer marker, tambon, amphoe and province per event. "Fetch address" looks them up from the GPS point (needs internet, only when pressed); the kilometer marker is also picked out of the voice note; each value is labelled "from GPS", "from voice" or "edited"; all fields can be edited by hand. The share text includes them.
- After merging clips the app says where the file is, shows it on the event card, and can save it to the gallery (Movies/SafetyRoadWatch, Android 10+).

### Changed
- Library buttons are equal in size and have icons.
- The capture button is now a soft, 50% transparent green with a shield instead of solid red.

### เพิ่ม
- คลัง: แสดงพิกัด GPS พร้อมถนน หลักกิโลเมตร ตำบล อำเภอ จังหวัด ของแต่ละเหตุการณ์ กด "ดึงที่อยู่" เพื่อหาจากพิกัด GPS (ต้องใช้เน็ต และดึงเมื่อกดเท่านั้น) หลักกิโลเมตรดึงจากเสียงที่พูดได้ด้วย บอกที่มาของแต่ละค่า (จาก GPS / จากเสียง / แก้เอง) และแก้เองได้ทุกช่อง ข้อความแชร์ใส่ที่อยู่ให้
- หลังรวมคลิป แอปบอกว่าไฟล์อยู่ที่ไหน แสดงในการ์ดเหตุการณ์ และบันทึกลงแกลเลอรีได้ (Movies/SafetyRoadWatch, Android 10 ขึ้นไป)

### เปลี่ยน
- ปุ่มในคลังขนาดเท่ากันและมีไอคอน
- ปุ่มบันทึกหน้ากล้องเป็นสีเขียวโปร่งแสง 50% มีรูปโล่ แทนสีแดงทึบ

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
