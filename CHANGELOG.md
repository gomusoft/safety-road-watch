# Changelog

All notable changes to this project. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
versions: [Semantic Versioning](https://semver.org/). Each release has English and Thai notes.
The section for a tag `vX.Y.Z` becomes that GitHub release's notes, so write it for users.

## [Unreleased]

### Added
- Home screen: a big start button, the library (with the number of events), settings, and a quick check that camera, microphone and GPS are allowed. Settings has the address lookup choice (moved from the library) and "open camera on start", which skips the home screen (off by default).
- Library: the GPS point plus road, kilometer marker, tambon, amphoe and province per event. "Fetch address" looks them up from the GPS point (needs internet and your consent); the kilometer marker is also picked out of the voice note; each value is labelled "from GPS", "from voice" or "edited"; all fields can be edited by hand. The share text includes them.
- Address lookup is an opt-in mode (off by default, asks for your consent first, because it sends the coordinate to the phone's map service). When on, the library fills in missing addresses by itself; offline it quietly waits and everything else keeps working.
- After merging clips the app says where the file is, shows it on the event card, and can save it to the gallery (Movies/SafetyRoadWatch, Android 10+).

### Changed
- The library works in both portrait and landscape and turns with the phone, even if auto-rotate is locked, so you no longer have to rotate back and forth after capturing.
- The voice note now ends 8 seconds after you stop talking (up to 45 s) instead of a fixed 12 s; speak again before that and it keeps recording, or press the button to end it.
- While the voice note is recording the button turns yellow and pulses, with a big banner and a light vibration.
- You can press capture for the next event straight away, even while the previous one is still collecting video or being transcribed. The result message disappears by itself after a few seconds.
- Library buttons are equal in size and have icons.
- The capture button is now a soft, 50% transparent green with a shield instead of solid red.

### Fixed
- The camera screen now keeps the screen on and shows over the lock screen, and recording restarts cleanly after the screen was off or after the library.

### เพิ่ม
- หน้าแรก: ปุ่มเริ่มบันทึกใหญ่ คลัง (บอกจำนวนเหตุการณ์) ตั้งค่า และแถบเช็กว่าอนุญาตกล้อง ไมค์ GPS แล้วหรือยัง ในตั้งค่ามีตัวเลือกดึงที่อยู่ (ย้ายมาจากหัวคลัง) และ "เปิดกล้องทันทีเมื่อเปิดแอป" ที่ข้ามหน้าแรก (ปิดไว้ก่อน)
- คลัง: แสดงพิกัด GPS พร้อมถนน หลักกิโลเมตร ตำบล อำเภอ จังหวัด ของแต่ละเหตุการณ์ กด "ดึงที่อยู่" เพื่อหาจากพิกัด GPS (ต้องใช้เน็ตและกดยอมรับก่อน) หลักกิโลเมตรดึงจากเสียงที่พูดได้ด้วย บอกที่มาของแต่ละค่า (จาก GPS / จากเสียง / แก้เอง) และแก้เองได้ทุกช่อง ข้อความแชร์ใส่ที่อยู่ให้
- การดึงที่อยู่เป็นโหมดที่เลือกเปิดเอง (ปิดไว้ก่อน และขอให้กดยอมรับก่อน เพราะส่งพิกัดให้บริการแผนที่ของเครื่อง) เมื่อเปิด คลังจะเติมที่อยู่ให้เอง ถ้าไม่มีเน็ตก็ข้ามไปเงียบๆ และใช้อย่างอื่นได้ตามปกติ
- หลังรวมคลิป แอปบอกว่าไฟล์อยู่ที่ไหน แสดงในการ์ดเหตุการณ์ และบันทึกลงแกลเลอรีได้ (Movies/SafetyRoadWatch, Android 10 ขึ้นไป)

### เปลี่ยน
- คลังใช้ได้ทั้งแนวตั้งและแนวนอน หมุนตามเครื่อง แม้ปิดการหมุนอัตโนมัติไว้ ไม่ต้องหมุนเครื่องกลับไปมาหลังบันทึก
- บันทึกเสียงจบเองเมื่อเงียบไป 8 วินาที (สูงสุด 45 วินาที) แทน 12 วินาทีตายตัว ถ้าพูดต่อก่อนครบ 8 วินาทีก็อัดต่อ หรือกดปุ่มเพื่อจบเอง
- ระหว่างอัดเสียง ปุ่มเปลี่ยนเป็นสีเหลืองกะพริบ มีแถบข้อความตัวใหญ่ และเครื่องสั่นเบาๆ
- กดบันทึกเหตุการณ์ถัดไปได้ทันที แม้เหตุการณ์ก่อนยังเก็บวิดีโอหรือแปลงเสียงอยู่ ข้อความผลลัพธ์หายเองหลังไม่กี่วินาที
- ปุ่มในคลังขนาดเท่ากันและมีไอคอน
- ปุ่มบันทึกหน้ากล้องเป็นสีเขียวโปร่งแสง 50% มีรูปโล่ แทนสีแดงทึบ

### แก้ไข
- หน้ากล้องเปิดจอค้างไว้และแสดงทับหน้าจอล็อกได้ และอัดต่อได้ถูกต้องหลังจอดับหรือกลับจากคลัง

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
