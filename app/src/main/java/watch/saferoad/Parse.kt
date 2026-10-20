package watch.saferoad

data class Parsed(val plate: String?, val province: String?, val violations: List<String>)

private val CONSONANTS = listOf(
    "กอไก่" to "ก", "ขอไข่" to "ข", "ฃอขวด" to "ฃ", "คอควาย" to "ค", "ฅอคน" to "ฅ",
    "ฆอระฆัง" to "ฆ", "งองู" to "ง", "จอจาน" to "จ", "ฉอฉิ่ง" to "ฉ", "ชอช้าง" to "ช",
    "ซอโซ่" to "ซ", "ฌอเฌอ" to "ฌ", "ยอหญิง" to "ญ", "ดอชฎา" to "ฎ", "ตอปฏัก" to "ฏ",
    "ฐอฐาน" to "ฐ", "ฑอมณโฑ" to "ฑ", "ฒอผู้เฒ่า" to "ฒ", "ณอเณร" to "ณ", "ดอเด็ก" to "ด",
    "ตอเต่า" to "ต", "ถอถุง" to "ถ", "ทอทหาร" to "ท", "ธอธง" to "ธ", "นอหนู" to "น",
    "บอใบไม้" to "บ", "ปอปลา" to "ป", "ผอผึ้ง" to "ผ", "ฝอฝา" to "ฝ", "พอพาน" to "พ",
    "ฟอฟัน" to "ฟ", "ภอสำเภา" to "ภ", "มอม้า" to "ม", "ยอยักษ์" to "ย", "รอเรือ" to "ร",
    "ลอลิง" to "ล", "วอแหวน" to "ว", "ศอศาลา" to "ศ", "ษอฤๅษี" to "ษ", "สอเสือ" to "ส",
    "หอหีบ" to "ห", "ฬอจุฬา" to "ฬ", "อออ่าง" to "อ", "ฮอนกฮูก" to "ฮ",
)

// Each name may contain spaces between any characters ("กอ ไก่").
private val CONSONANT_RES = CONSONANTS.map { (name, c) ->
    Regex(name.map { it.toString() }.joinToString("\\s*")) to c
}

private val PLATE = Regex("""(?<![ก-๛])(\d?[ก-ฮ]{1,2})\s*(\d{1,4})""")
private val GLUE = Regex("""(?<=[ก-ฮ0-9])\s+(?=[ก-ฮ0-9])""")
private val FILLER = Regex("บันทึก(เลย)?")

// First line item is the canonical name, the rest are aliases.
private val PROVINCES: List<List<String>> = """
กรุงเทพมหานคร|กรุงเทพ|กทม
สมุทรปราการ
นนทบุรี
ปทุมธานี
พระนครศรีอยุธยา|อยุธยา
อ่างทอง
ลพบุรี
สิงห์บุรี
ชัยนาท
สระบุรี
ชลบุรี
ระยอง
จันทบุรี
ตราด
ฉะเชิงเทรา
ปราจีนบุรี
นครนายก
สระแก้ว
นครราชสีมา|โคราช
บุรีรัมย์
สุรินทร์
ศรีสะเกษ
อุบลราชธานี
ยโสธร
ชัยภูมิ
อำนาจเจริญ
บึงกาฬ
หนองบัวลำภู
ขอนแก่น
อุดรธานี
เลย
หนองคาย
มหาสารคาม
ร้อยเอ็ด
กาฬสินธุ์
สกลนคร
นครพนม
มุกดาหาร
เชียงใหม่
ลำพูน
ลำปาง
อุตรดิตถ์
แพร่
น่าน
พะเยา
เชียงราย
แม่ฮ่องสอน
นครสวรรค์
อุทัยธานี
กำแพงเพชร
ตาก
สุโขทัย
พิษณุโลก
พิจิตร
เพชรบูรณ์
ราชบุรี
กาญจนบุรี
สุพรรณบุรี
นครปฐม
สมุทรสาคร
สมุทรสงคราม
เพชรบุรี
ประจวบคีรีขันธ์
นครศรีธรรมราช
กระบี่
พังงา
ภูเก็ต
สุราษฎร์ธานี
ระนอง
ชุมพร
สงขลา
สตูล
ตรัง
พัทลุง
ปัตตานี
ยะลา
นราธิวาส
""".trim().lines().map { it.trim().split('|') }

// keywords (comma separated) to label
private val VIOLATIONS = listOf(
    "ฝ่าไฟแดง,ไฟแดง" to "ฝ่าไฟแดง",
    "เส้นทึบ,คร่อมเส้น,ปาดเส้นทึบ" to "เปลี่ยนเลนคร่อมเส้นทึบ",
    "ควันดำ" to "ควันดำ",
    "ล้อล้น,ล้อเกินตัวถัง,ล้อเกิน" to "ล้อล้นตัวถัง",
    "ย้อนศร" to "ย้อนศร",
    "ทางเท้า,ฟุตบาท" to "ขับบนทางเท้า",
    "ไม่สวมหมวก,หมวกกันน็อค,หมวกกันน็อก" to "ไม่สวมหมวกนิรภัย",
    "โทรศัพท์,ใช้มือถือ" to "ใช้โทรศัพท์ขณะขับ",
    "แซงซ้าย,ไหล่ทาง" to "ขับบนไหล่ทาง",
    "ป้ายทะเบียน,ไม่มีป้าย,บังป้าย" to "ป้ายทะเบียนผิดกฎหมาย",
    "บรรทุกเกิน,น้ำหนักเกิน" to "บรรทุกเกิน",
    "จอดในที่ห้าม,จอดซ้อนคัน" to "จอดในที่ห้ามจอด",
)

fun parse(text: String): Parsed {
    // Thai digits to ASCII
    val t = text.map { if (it in '๐'..'๙') '0' + (it - '๐') else it }.joinToString("")

    // Plate: spell-out names to letters, drop the word "ทะเบียน", glue tokens together.
    var p = t.replace("ทะเบียน", " ")
    for ((re, c) in CONSONANT_RES) p = re.replace(p, c)
    p = GLUE.replace(p, "")
    val m = PLATE.find(p)
    val plate = m?.let { it.groupValues[1] + " " + it.groupValues[2] }

    // Province and violations: spaces removed, filler words dropped.
    val s = FILLER.replace(t, " ").replace(Regex("\\s+"), "")

    var best: Triple<Int, Int, String>? = null // index, alias length, canonical
    for (names in PROVINCES) {
        for (alias in names) {
            val i = s.indexOf(alias)
            if (i < 0) continue
            val b = best
            if (b == null || i < b.first || (i == b.first && alias.length > b.second)) {
                best = Triple(i, alias.length, names[0])
            }
        }
    }

    val found = ArrayList<Pair<Int, String>>()
    for ((keys, label) in VIOLATIONS) {
        for (k in keys.split(',')) {
            val i = s.indexOf(k)
            if (i >= 0) found.add(i to label)
        }
    }
    val violations = found.sortedBy { it.first }.map { it.second }.distinct()

    return Parsed(plate, best?.third, violations)
}
