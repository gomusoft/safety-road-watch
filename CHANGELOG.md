# Changelog

All notable changes to this project. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
versions: [Semantic Versioning](https://semver.org/). Each release has English and Thai notes.
The section for a tag `vX.Y.Z` becomes that GitHub release's notes, so write it for users.

## [Unreleased]

### Added
- Clips and the merged video now play inside the app (no other video app needed).
- Every clip and the merged video show a small picture of their first frame, tap it to play. Each event also shows a still picture: the video frame at the moment you pressed. It is saved with the event and sent along when you share.
- Home screen: a big start button, the library (with the number of events), settings, and a quick check that camera, microphone and GPS are allowed. Settings has the address lookup choice (moved from the library) and "open camera on start", which skips the home screen (off by default).
- Library: the GPS point plus road, kilometer marker, tambon, amphoe and province per event. "Fetch address" looks them up from the GPS point (needs internet and your consent); the kilometer marker is also picked out of the voice note; each value is labelled "from GPS", "from voice" or "edited"; all fields can be edited by hand. The share text includes them.
- Address lookup is an opt-in mode (off by default, asks for your consent first, because it sends the coordinate to the phone's map service). When on, the library fills in missing addresses by itself; offline it quietly waits and everything else keeps working.
- After merging clips the app says where the file is, shows it on the event card, and can save it to the gallery (Movies/SafetyRoadWatch, Android 10+).

- Swipe a clip sideways in the library to delete just that clip (asks first).
- Video stabilization setting, on by default; phones whose camera cannot do it show it greyed out with the reason.

- While watching a video you can zoom in with two fingers (or double tap) and drag, to read a plate. "จับภาพ" saves the frame on screen as a still picture of the event; you can take several. They appear under the event, open full screen with zoom (and can be deleted), and are sent along when you share.

- Merging clips asks whether to put your voice note into the merged video (placed where you pressed the button) or leave it silent. While watching the merged video, "อัดเสียงบรรยาย" records a new spoken note over it (say what you see as it plays) and can put it into the video; the microphone is on only between your two presses.

- Send a report: "ส่งรายงานให้หน่วยงาน" on each event opens a guide for the chosen agency (DLT reward system and complaint web, DLT e-mail, police 1197, PCD 1650) with the steps, the address to open (always shown and confirmed before the browser opens), a button to copy the report text, one to save the video and pictures to the gallery so a web form can attach them, and one to open the mail app with everything attached. The first time you accept the rules. Nothing is sent by the app itself. The guide is also in Settings, together with your sender profile (name, phone, e-mail, kept on the phone and added to the report text only when you send). If a page or step no longer matches the guide, "แจ้งว่าไม่ตรง" opens a prefilled GitHub issue (shown to you first). Agencies are listed in `recipients.json`, so contributors can add or fix them with a commit.

### Changed
- The in-app player has its own play/pause button and a seek bar.
- Clips you untick in the library stay unticked after you watch another clip and come back.
- The voice note filters out engine rumble and road vibration, and uses the phone's own noise suppression when it has it, so your voice is easier to hear and to transcribe in a noisy car.
- Video is recorded in 20-second pieces instead of 10, so there are about half as many tiny jumps between clips. The "before" and "after" parts now run 30 to 50 seconds (whole clips).
- Settings: each option is a card you can tap anywhere, with a clear on/off switch.
- The library works in both portrait and landscape and turns with the phone, even if auto-rotate is locked, so you no longer have to rotate back and forth after capturing.
- The voice note now ends 8 seconds after you stop talking (up to 45 s) instead of a fixed 12 s; speak again before that and it keeps recording, or press the button to end it.
- While the voice note is recording the button turns yellow and pulses, with a big banner and a light vibration.
- You can press capture for the next event straight away, even while the previous one is still collecting video or being transcribed. The result message disappears by itself after a few seconds.
- Library buttons are equal in size and have icons.
- The capture button is now a soft, 50% transparent green with a shield instead of solid red.

### Fixed
- The camera screen now keeps the screen on and shows over the lock screen, and recording restarts cleanly after the screen was off or after the library.

### เพิ่ม
- ดูคลิปและวิดีโอที่รวมแล้วในแอปได้เลย ไม่ต้องใช้แอปวิดีโออื่น
- ทุกคลิปและวิดีโอที่รวมแล้วมีภาพแรกของคลิปให้ดู กดที่ภาพเพื่อเล่น และการ์ดแต่ละเหตุการณ์มีภาพนิ่ง คือภาพวิดีโอ ณ วินาทีที่กดบันทึก เก็บไว้กับเหตุการณ์ และแนบไปตอนแชร์
- หน้าแรก: ปุ่มเริ่มบันทึกใหญ่ คลัง (บอกจำนวนเหตุการณ์) ตั้งค่า และแถบเช็กว่าอนุญาตกล้อง ไมค์ GPS แล้วหรือยัง ในตั้งค่ามีตัวเลือกดึงที่อยู่ (ย้ายมาจากหัวคลัง) และ "เปิดกล้องทันทีเมื่อเปิดแอป" ที่ข้ามหน้าแรก (ปิดไว้ก่อน)
- คลัง: แสดงพิกัด GPS พร้อมถนน หลักกิโลเมตร ตำบล อำเภอ จังหวัด ของแต่ละเหตุการณ์ กด "ดึงที่อยู่" เพื่อหาจากพิกัด GPS (ต้องใช้เน็ตและกดยอมรับก่อน) หลักกิโลเมตรดึงจากเสียงที่พูดได้ด้วย บอกที่มาของแต่ละค่า (จาก GPS / จากเสียง / แก้เอง) และแก้เองได้ทุกช่อง ข้อความแชร์ใส่ที่อยู่ให้
- การดึงที่อยู่เป็นโหมดที่เลือกเปิดเอง (ปิดไว้ก่อน และขอให้กดยอมรับก่อน เพราะส่งพิกัดให้บริการแผนที่ของเครื่อง) เมื่อเปิด คลังจะเติมที่อยู่ให้เอง ถ้าไม่มีเน็ตก็ข้ามไปเงียบๆ และใช้อย่างอื่นได้ตามปกติ
- หลังรวมคลิป แอปบอกว่าไฟล์อยู่ที่ไหน แสดงในการ์ดเหตุการณ์ และบันทึกลงแกลเลอรีได้ (Movies/SafetyRoadWatch, Android 10 ขึ้นไป)

- ปัดคลิปไปทางซ้ายหรือขวาในคลังเพื่อลบเฉพาะคลิปนั้น (ถามยืนยันก่อน)
- ตั้งค่ากันภาพสั่นของวิดีโอ เปิดไว้ให้เป็นค่าเริ่มต้น ถ้ากล้องของเครื่องทำไม่ได้จะเป็นสีจางพร้อมบอกเหตุผล

- ตอนดูวิดีโอ ซูมได้ด้วยสองนิ้ว (หรือแตะสองครั้ง) และลากดูส่วนอื่น เพื่ออ่านทะเบียนให้ชัด ปุ่ม "จับภาพ" เก็บภาพที่เห็นบนจอเป็นรูปนิ่งของเหตุการณ์ จับได้หลายภาพ รูปจะอยู่ใต้เหตุการณ์ กดดูเต็มจอและซูมได้ (ลบได้) และถูกส่งไปพร้อมตอนแชร์

- ตอนรวมคลิป แอปถามว่าจะใส่เสียงที่บันทึกไว้ลงในวิดีโอ (ตรงจุดที่กดปุ่ม) หรือไม่ใส่เสียง และตอนดูวิดีโอที่รวมแล้ว ปุ่ม "อัดเสียงบรรยาย" อัดเสียงพูดใหม่ทับไปกับภาพ (พูดสิ่งที่เห็นขณะเล่น) และใส่ลงในวิดีโอได้ ไมค์เปิดเฉพาะระหว่างที่กดปุ่มเริ่มจนกดหยุด

- ส่งรายงาน: ปุ่ม "ส่งรายงานให้หน่วยงาน" ในแต่ละเหตุการณ์ เปิดคู่มือของหน่วยงานที่เลือก (ระบบขอรับเงินสินบนและเว็บร้องเรียนของกรมการขนส่งทางบก อีเมลกรมฯ ตำรวจ 1197 กรมควบคุมมลพิษ 1650) มีขั้นตอน ที่อยู่เว็บที่ต้องเปิด (แสดงและให้ยืนยันก่อนเปิดเบราว์เซอร์ทุกครั้ง) ปุ่มคัดลอกข้อความรายงาน ปุ่มบันทึกวิดีโอและรูปลงแกลเลอรีเพื่อแนบในเว็บ และปุ่มเปิดแอปอีเมลพร้อมแนบไฟล์ ครั้งแรกต้องอ่านและยอมรับกติกา แอปไม่ส่งข้อมูลเอง คู่มืออยู่ในตั้งค่าด้วย พร้อมโปรไฟล์ผู้ส่ง (ชื่อ เบอร์ อีเมล เก็บในเครื่อง ใส่ในข้อความรายงานเมื่อคุณกดส่งเท่านั้น) ถ้าหน้าเว็บหรือขั้นตอนไม่ตรงกับคู่มือ ปุ่ม "แจ้งว่าไม่ตรง" เปิดหน้า GitHub ที่กรอกไว้ให้ (แสดงให้ดูก่อน) รายชื่อหน่วยงานอยู่ในไฟล์ `recipients.json` ผู้ร่วมพัฒนาเพิ่มหรือแก้ได้ด้วยคอมมิต

### เปลี่ยน
- เครื่องเล่นในแอปมีปุ่มเล่น/หยุดและแถบเลื่อนของตัวเอง
- คลิปที่เอาติ๊กออกในคลังยังคงไม่ติ๊กหลังไปดูคลิปอื่นแล้วกลับมา
- เสียงบันทึกตัดเสียงเครื่องยนต์และเสียงสั่นของถนนออก และใช้ตัวลดเสียงรบกวนของเครื่องถ้ามี ทำให้ได้ยินเสียงพูดชัดขึ้นและถอดเสียงได้ดีขึ้นในรถที่เสียงดัง
- อัดวิดีโอเป็นท่อนละ 20 วินาทีแทน 10 วินาที รอยต่อสะดุดเล็กๆ ระหว่างคลิปจึงลดลงประมาณครึ่งหนึ่ง ช่วงก่อนกดและหลังกดจะยาว 30 ถึง 50 วินาที (ครบทั้งคลิป)
- ตั้งค่า: แต่ละตัวเลือกเป็นแถบกดได้ทั้งแถบ มีสวิตช์เปิด/ปิดที่ชัดเจน
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
