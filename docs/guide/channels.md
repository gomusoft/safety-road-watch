# Official reporting channels: how-to (Thailand)

Checked on **2026-10-20**. Written for the Safety Road Watch app (no internet permission; it only opens a
browser, mail app or LINE with a URL the user sees first).

## How this was checked (read first)

- The build container's egress proxy **blocked every Thai government host** (HTTP 403 on CONNECT: dlt.go.th,
  trafficpolice.go.th, royalthaipolice.go.th, pcd.go.th, ecap.pcd.go.th, thaipoliceonline.go.th, line.me).
  A real browser could not open any official form. **No screenshots were taken. `docs/guide/img/` is empty on
  purpose; nothing was mocked.**
- Page text was read through a third-party fetch/search service (Parallel Search `web_fetch`). That gives the
  text of the official pages as the service cached or fetched them, not a live phone view. Form fields and
  upload limits that need a logged-in session could not be seen.
- Confidence: **verified** = seen on an official .go.th page or an official document today; **unverified** =
  only news/blog, old (2021-2022), or not seen at all. Nothing here was confirmed by phone.
- Warning: fake SMS/LINE messages impersonating DLT and police (traffic fines) are circulating (DE ministry
  notice via Bangkokbiznews, 2026-08-27). DLT does not send links by SMS or LINE. The app must only open the
  URLs below.

## Summary

| # | Channel | Agency | Video | Reward | Confidence |
|---|---|---|---|---|---|
| 1 | DLT reward-claim system `recompense.dlt.go.th` (ระบบรับเรื่องร้องเรียน กรณีขอรับเงินสินบน) | DLT | Evidence required (clip/photo); size limit unknown | Yes, 50% | login page verified; rules from DLT document |
| 2 | DLT complaint web `ins.dlt.go.th/cmpweb/` (รับและตรวจสอบเรื่องร้องเรียนรถโดยสารสาธารณะ) | DLT | Unknown | See note | page verified, may be superseded by #1 |
| 3 | DLT hotline 1584 | DLT | No (phone) | Via #1 | verified number |
| 4 | DLT LINE `@1584DLT`, e-mail `dlt_1584complain@hotmail.com`, Facebook "1584 ร้องเรียนรถโดยสารสาธารณะ" | DLT | Probably yes | Unclear | unverified (news 2021-2022) |
| 5 | Police hotline 1197 and Traffic Police `trafficpolice.go.th` | RTP | No | No regular share found | unverified (site unreachable) |
| 6 | Police clip programme "อาสาตาจราจร" (via ศจร.ตร. and partner Facebook pages) | RTP | Yes | Monthly contest prizes, not 50% | partly verified |
| 7 | Police apps "Police i lert u", "Thai Police Online" | RTP | Photos (i lert u) | No | verified they exist; not meant for traffic reports |
| 8 | PCD hotline 1650 and `ecap.pcd.go.th/ecap/` | PCD | Unknown | No | verified link and hotline |
| 9 | Traffy Fondue (LINE OA `@traffyfondue`, `fonduereport.traffy.in.th`) | NSTDA | Photo | No | verified site; not a police/DLT channel |

---

## 1. DLT reward-claim system (ระบบรับเรื่องร้องเรียน กรณีขอรับเงินสินบน)

- **URL:** https://recompense.dlt.go.th/ (page title "DLT 1584"). Seen today: login by 13-digit ID number +
  password, buttons "ลงทะเบียน" (register), "ลืมรหัสผ่าน" (forgot password), "เข้าสู่ระบบด้วย ThaID".
- **Why it matters:** a DLT internal circular (OIC infocenter PDF,
  https://infocenter.oic.go.th/upload/cms/1771991504_6467.pdf; the reader's OCR is garbled, the date is not
  legible; regulation "ฉบับที่ ๕", effective 5 July, year unreadable, probably B.E. 2568 = 2025 - unverified)
  says the DLT regulation on rewards now lets a reporter file a tip for a reward through **the single website
  DLT designates**, and that offices check `recompense.dlt.go.th/admin` each working day. Older channels
  (LINE, e-mail, Facebook, hotline) were described in 2019-2022 news as ways to file with reward; the 2025+
  circular suggests the reward claim must go through this site. **Treat this site as the primary channel for
  anything where the user wants the 50% reward. Confirm by calling 1584.**
- **Fields (from the circular, what DLT staff verify):** first name, last name, 13-digit ID number, address,
  phone, e-mail, **copy of ID card**, **copy of bank book** (for payout), date, time and place of the offence,
  and the evidence of the offence (photo/video). The older 2019-2022 rules also list the vehicle plate and
  details. Exact web field labels and the upload size/format limit: **not seen** (login required).
- **Steps (inferred, unverified in the UI):**
  1. On the phone, open https://recompense.dlt.go.th/ in the browser.
  2. First time: tap ลงทะเบียน, enter personal data, upload ID-card and bank-book copies (or log in with ThaID).
  3. Log in with ID number and password (or ThaID).
  4. Create a new report: date, time, place, plate, province, description, attach the clip and photos.
  5. Submit. Expect an SMS confirming receipt, later an SMS when the case closes.
- **Phone:** yes, a normal web page; mobile layout not checked.
- **Reward:** 50% of the fine after the state's share is deducted, paid only after the offender pays the
  fine, within 15 working days of the closing SMS (DLT notice reproduced by JS100, 2021-04-02; TCIJ
  2021-10-05). False or malicious reports are a criminal offence.
- **Scope:** offences under the Motor Vehicle Act and Land Transport Act B.E. 2522: public-transport and
  commercial vehicles, and for private cars only vehicle-condition offences (illegal modifications, black smoke,
  etc.), not driving behaviour such as red lights.
- **Privacy:** the reporter must give ID data and bank data to a government site; the app should warn the user
  before opening it. DLT says reporter data is kept confidential (Springnews 2022).
- **Checked:** 2026-10-20. **Confidence:** login page = verified; field list and steps = unverified.
- **Screenshots:** none (host blocked).

## 2. DLT complaint web (ins.dlt.go.th/cmpweb)

- **URL:** https://ins.dlt.go.th/cmpweb/ - title "รับและตรวจสอบเรื่องร้องเรียนรถโดยสารสาธารณะ". Text seen
  today: DLT accepts complaints about vehicles under the Motor Vehicle Act and Land Transport Act, and you can
  check progress by entering the 13-digit ID number. Contact shown: phone 1584, e-mail **1584@dlt.go.th**.
- **Fields and attachments:** not seen. Older news (2021) says: reporter name, address, phone, bank account if
  the reward is wanted, plus date, time, place, plate and a photo or VDO clip.
- **Reward:** older news says yes; the newer circular (see #1) points reward claims to recompense.dlt.go.th.
  Use this site for plain complaints.
- **Checked:** 2026-10-20. **Confidence:** exists = verified; form details = unverified.
- **Screenshots:** none.

## 3. DLT hotline 1584 (สายด่วน 1584)

- **Number:** 1584, 24 hours (DLT notices, news 2021-2026; Bangkokbiznews 2026-08-27 repeats 1584 as the
  verification number). Switchboard 0-2271-8888 (from dlt.go.th footer).
- **How:** call, give date, time, place, plate, description; they tell you which channel to use for evidence.
- **Video:** no. **Reward:** only through the formal route (#1).
- **Confidence:** number verified.

## 4. DLT LINE, e-mail, Facebook (older channels)

- **LINE:** `@1584DLT` (written `@1584dlt` elsewhere; LINE ids are case-insensitive). Seen only in news dated
  2021-2022 (JS100 2021-04-02, TCIJ 2021-10-05, Thairath 2021-11-18, Springnews 2022-01-26). The id
  `@dltthailand` given in the task was **not found** anywhere; do not use it.
- **E-mail:** dlt_1584complain@hotmail.com (same news). The ins.dlt.go.th page shows 1584@dlt.go.th today,
  which is the safer address.
- **Facebook:** https://www.facebook.com/dlt1584 ("1584 ร้องเรียนรถโดยสารสาธารณะ").
- **Attachments:** photos/clips accepted by these channels per the 2021 notice; sizes unknown.
- **Reward:** not guaranteed; see #1.
- **Confidence:** unverified (not seen on an official page today).

## 5. Police hotline 1197 and Traffic Police (ตำรวจทางหลวง/ตำรวจจราจร)

- **1197:** Traffic Police hotline named in an Amarin TV report and in older articles. Not confirmed from an
  official page today (trafficpolice.go.th unreachable). Website: https://www.trafficpolice.go.th/ (unverified).
- **Other numbers seen in news:** 191 (emergency), 1599 (police complaint line), 1356 (Transport Safety
  Centre, Thai PBS 2020) - all unverified for this use.
- **LINE:** no official police traffic LINE account or id was found. The Royal Thai Police "LINE @1197" and
  "ตำรวจไทย" accounts mentioned in the task could **not** be confirmed. Do not hard-code one.
- **Video:** not by phone call. **Reward:** no regular 50% share was found for police reports; some articles
  claim it under the Land Traffic Act - unverified (see docs/reporting-channels.md).
- **Confidence:** unverified.

## 6. Police clip programme "อาสาตาจราจร"

- Run by the Traffic Administration Centre (ศูนย์บริหารงานจราจร สำนักงานตำรวจแห่งชาติ, ศจร.ตร.).
  Siamrath, 2026-09-09: the programme (running since 2022) gave 20 prizes, 100,000 baht total, to owners of
  dashcam clips chosen for April and May 2026, sponsored by Viriyah Insurance. So it is **active in 2026**.
- Channels named in 2021 news (Thairath 2021-11-18, Springnews 2022-01-26), possibly changed since:
  Facebook pages of the RTP Social Media Centre
  (https://www.facebook.com/SocialMediaRoyalThaiPolice/), JS100 Radio, สวพ.91, and the "อาสาตาจราจร" page
  (https://www.facebook.com/อาสาตาจราจร-100472452158287). ศจร.ตร. checks the clip and sends it to the local
  police station for prosecution. Facebook needs a login, which the app cannot do for the user.
- **Video:** yes. **Reward:** contest prizes, not 50%. **Confidence:** the programme = verified (news 2026);
  the page list = unverified, 2021.

## 7. Police apps

- **Police i lert u** (Arunsawad Dot Com for Royal Thai Police; Google Play id `com.arunsawad.policeilu`,
  500K+ downloads, last update 2024-03-20): an emergency-help app. Sends personal data, photos, time and
  location to a 24-hour "War Room". Described for help requests, **not** for reporting traffic offences.
  Listed in the RTP app centre https://apps.police.go.th/ with "Police 4.0", "Police Care" etc.
- **Thai Police Online** (https://thaipoliceonline.go.th/): online reports for technology crime (fraud etc.).
  Login via ThaID. Not for traffic offences.
- **Confidence:** exist = verified; "not for traffic" = from store/site text, verified.

## 8. PCD 1650 (black smoke / pollution)

- **Hotline 1650:** Mon-Fri 08:30-18:00; outside hours it takes a voice message for a call back. Also
  0 2298 2000 (08:30-16:30), e-mail e-petition@pcd.go.th (PCD ECAP documents, 2025/2026 PDFs).
- **Web:** pcd.go.th front page "ร้องเรียนปัญหามลพิษ Hotline 1650 / ออนไลน์" links to
  http://ecap.pcd.go.th/ecap/ (verified link on the page; the form itself not opened).
- **LINE:** Traffy Fondue `@Traffyfondue`, said to take PCD pollution reports 24 h (PCD ECAP document, via
  search summary - unverified). Traffy Fondue lists "ควันดำ" (black smoke) as a report type.
- **Video:** unknown. **Reward:** none found. PCD does not issue fines to vehicles; for black smoke vehicles
  articles also point to DLT 1584 / police 1197.
- **Confidence:** hotline/link verified; the form unverified.

## 9. Traffy Fondue (traffy.in.th)

- Run by NSTDA (สวทช.) as a general city-problem tool; categories include traffic (จราจร) and black smoke.
  Web form https://fonduereport.traffy.in.th/ (fields seen: description, photo, agency, mobile number with
  OTP, consent). Not an official traffic-violation channel of police or DLT. No reward.
- **Confidence:** site verified; use for this purpose is not.

---

## Open questions for the owner (phone 1584 / 1197)

1. 1584: is `recompense.dlt.go.th` the only channel for a reward claim now? Do LINE @1584DLT and the e-mail
   still accept reports? Which offences does DLT take (private-car driving offences are not DLT's)?
2. 1584: attachment types and size limit on recompense.dlt.go.th; is a copy of ID and bank book required at
   registration? Can ThaID replace it?
3. 1197: is there an official traffic-police LINE or web form for clips today? Which one does a driver use?
   Is the 50% reward paid to a citizen for a police-handled offence?
4. 1197: does the อาสาตาจราจร Facebook route still work, and what size/length is accepted?
5. 1650: does the ecap form take video, and is black smoke handled by PCD or sent to DLT?
