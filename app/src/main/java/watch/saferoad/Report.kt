package watch.saferoad

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

/** Everything about sending a report. The app never sends anything itself: it opens the browser, mail app or dialer. */
object Report {
    const val ISSUES = "https://github.com/gomusoft/safety-road-watch/issues/new"

    /** The text of the report: what happened, where, and (if the user filled it in) who is reporting. */
    fun text(c: Context, e: EventInfo): String {
        val map = if (e.lat != null && e.lon != null) "https://maps.google.com/?q=${e.lat},${e.lon}" else ""
        val place = (e.place.text() + " " + map).trim().ifEmpty { "-" }
        val sb = StringBuilder()
        sb.append("แจ้งการกระทำผิดกฎจราจร\n")
        sb.append("วันเวลา: ${e.iso.replace('T', ' ')}\n")
        sb.append("ทะเบียน: ${e.plate ?: "-"} ${e.province ?: ""}\n")
        sb.append("ความผิด: ${e.violations.joinToString(", ")}\n")
        sb.append("สถานที่: $place")
        val who = listOf("ผู้แจ้ง" to Settings.profileName(c), "โทร" to Settings.profilePhone(c), "อีเมล" to Settings.profileEmail(c))
            .filter { it.second.isNotBlank() }
        if (who.isNotEmpty()) sb.append("\n").append(who.joinToString("\n") { "${it.first}: ${it.second}" })
        return sb.toString()
    }

    /** The pictures of an event: the picture of the press first, then every captured still. */
    fun pictures(e: EventInfo): List<File> {
        val still = File(e.dir, "thumb.jpg").takeIf { it.exists() }
        val captured = e.dir.listFiles { f -> f.name.startsWith("still_") && f.name.endsWith(".jpg") }?.sortedBy { it.name } ?: emptyList()
        return listOfNotNull(still) + captured
    }

    /** The merged video if there is one, else every clip. */
    fun videos(e: EventInfo): List<File> {
        val merged = File(e.dir, "merged.mp4")
        if (merged.exists()) return listOf(merged)
        return e.dir.listFiles { f -> f.name.startsWith("clip_") && f.name.endsWith(".mp4") }?.sortedBy { it.name } ?: emptyList()
    }

    fun uri(c: Context, f: File): Uri = FileProvider.getUriForFile(c, "${c.packageName}.files", f)

    /** Opens the mail app with the report text and the files attached. The user presses send there. */
    fun mail(a: AppCompatActivity, e: EventInfo, to: String) {
        val files = pictures(e) + videos(e)
        val i = Intent(Intent.ACTION_SEND_MULTIPLE).setType("*/*")
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            .putExtra(Intent.EXTRA_SUBJECT, "แจ้งการกระทำผิดกฎจราจร ${e.plate ?: ""} ${e.iso.replace('T', ' ')}".trim())
            .putExtra(Intent.EXTRA_TEXT, text(a, e))
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(files.map { uri(a, it) }))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        a.startActivity(Intent.createChooser(i, "ส่งอีเมลถึง $to"))
    }

    /** Copies the merged video and the pictures to the gallery so a web form can pick them. Android 10+. */
    fun saveToGallery(c: Context, e: EventInfo): Int {
        if (Build.VERSION.SDK_INT < 29) return 0
        var n = 0
        fun put(f: File, name: String, mime: String, collection: Uri, dir: String) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, dir)
            }
            val dst = c.contentResolver.insert(collection, values) ?: return
            c.contentResolver.openOutputStream(dst)!!.use { out -> f.inputStream().use { it.copyTo(out) } }
            n++
        }
        val tag = e.dir.name
        File(e.dir, "merged.mp4").takeIf { it.exists() }?.let {
            put(it, "safety_road_watch_$tag.mp4", "video/mp4", MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "Movies/SafetyRoadWatch")
        }
        for (f in pictures(e)) {
            put(f, "safety_road_watch_${tag}_${f.nameWithoutExtension}.jpg", "image/jpeg", MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "Pictures/SafetyRoadWatch")
        }
        return n
    }

    /** Shows the address first, then opens it in the browser. The user can see where it goes before it goes there. */
    fun openUrl(a: AppCompatActivity, url: String) {
        AlertDialog.Builder(a)
            .setTitle("เปิดเว็บนี้ในเบราว์เซอร์")
            .setMessage("$url\n\nตรวจว่าเป็นเว็บของหน่วยงานรัฐ (.go.th) อย่าเปิดลิงก์ที่ส่งมาทาง SMS หรือ LINE อ้างว่าเป็นกรมขนส่งหรือตำรวจ")
            .setPositiveButton("เปิด") { _, _ ->
                try {
                    a.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (ex: Exception) {
                    Toast.makeText(a, "ไม่พบแอปเปิดเว็บ", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    /** The first time a report is sent, the user reads and accepts the rules. */
    fun gate(a: AppCompatActivity, onYes: () -> Unit) {
        if (Settings.reportAccepted(a)) {
            onYes()
            return
        }
        AlertDialog.Builder(a)
            .setTitle("กติกาการส่งรายงาน")
            .setMessage(
                "• แอปไม่ส่งข้อมูลเอง คุณเป็นคนส่งผ่านเว็บ อีเมล หรือโทรศัพท์ของคุณ\n" +
                    "• ทุกครั้งแอปจะแสดงที่อยู่เว็บ (URL) ให้เห็นก่อนเปิด ตรวจว่าเป็นเว็บของหน่วยงานรัฐ (.go.th)\n" +
                    "• อย่าเปิดลิงก์จาก SMS หรือ LINE ที่อ้างว่ามาจากกรมขนส่งหรือตำรวจ ปลอมกันเยอะ\n" +
                    "• ส่งเฉพาะเหตุการณ์ที่เกิดขึ้นจริง การแจ้งเท็จมีโทษ\n" +
                    "• ส่งให้หน่วยงานเท่านั้น อย่าโพสต์สาธารณะ เสี่ยงหมิ่นประมาทและผิด PDPA\n" +
                    "• ชื่อ เบอร์ และอีเมลในโปรไฟล์ จะอยู่ในข้อความรายงานที่คุณส่งเท่านั้น\n" +
                    "• ขั้นตอนในคู่มืออาจเปลี่ยน ถ้าหน้าเว็บไม่ตรงกับคู่มือ กด \"แจ้งว่าไม่ตรง\" ให้ผู้ดูแลแก้",
            )
            .setPositiveButton("อ่านแล้ว ยอมรับ") { _, _ ->
                Settings.setReportAccepted(a, true)
                onYes()
            }
            .setNegativeButton("ไม่ยอมรับ", null)
            .show()
    }

    /** A pre-filled "the guide is wrong" issue on GitHub; shown to the user before it opens. No personal data goes in it. */
    fun problemUrl(r: Recipient, version: String): String {
        val title = "Guide out of date: ${r.id}"
        val body = "Channel: ${r.name} (${r.id})\nGuide last checked: ${r.checked}\nApp version: $version\n\n" +
            "What looks different now (page, step, field)?\n\n"
        return Uri.parse(ISSUES).buildUpon().appendQueryParameter("title", title).appendQueryParameter("body", body).build().toString()
    }
}
