package watch.saferoad

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * The how-to for each way of sending a report. Without extras it lists the ways; with "id" it shows one, and with
 * "event" (an event folder) its buttons also work on that event. The data is assets/recipients.json.
 */
class GuideActivity : AppCompatActivity() {
    private lateinit var box: LinearLayout
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun text(s: String, sp: Float = 15f, bold: Boolean = false, color: Int = Color.BLACK) = TextView(this).apply {
        text = s
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        minHeight = dp(48)
        setOnClickListener { onClick() }
    }

    private fun add(v: android.view.View) = box.addView(v, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(6) })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.WHITE)
            addView(box)
        })
        val all = Recipients.load(this)
        val one = intent.getStringExtra("id")?.let { id -> all.firstOrNull { it.id == id } }
        val event = intent.getStringExtra("event")?.let { path -> EventStore(this).list().firstOrNull { it.dir.path == path } }
        if (one == null) list(all, event) else detail(one, event)
    }

    private fun openGuide(r: Recipient, e: EventInfo?) = startActivity(
        Intent(this, GuideActivity::class.java).putExtra("id", r.id).apply { if (e != null) putExtra("event", e.dir.path) },
    )

    private fun list(all: List<Recipient>, e: EventInfo?) {
        add(text("คู่มือการส่งรายงาน", 24f, true))
        val guide = Recipients.guide(this)
        if (e != null && e.violations.isNotEmpty()) add(text("ความผิดที่บันทึกไว้: ${e.violations.joinToString(", ")}", 15f, true))
        add(text("ความผิดแต่ละแบบส่งคนละหน่วยงาน เลือกตามความผิด ข้อมูลตรวจล่าสุดดูในแต่ละช่องทาง ช่องทางใหม่เพิ่มได้ที่ไฟล์ recipients.json ในโปรเจกต์", 13f, color = 0xFF555555.toInt()))
        if (guide.note.isNotEmpty()) add(text(guide.note, 12f, color = 0xFF555555.toInt()))
        if (all.isEmpty()) add(text("อ่านรายการช่องทางไม่ได้", 16f))
        val byId = all.associateBy { it.id }
        val matched = guide.groups.filter { e != null && it.matches(e.violations) }
        add(text("ส่งที่ไหนดี (แยกตามความผิด)", 18f, true))
        for (g in matched + guide.groups.filter { it !in matched }) {
            val hit = g in matched
            add(text((if (hit) "★ " else "") + g.title + if (hit) "  (ตรงกับเหตุการณ์นี้)" else "", 16f, true, if (hit) 0xFF2E7D32.toInt() else Color.BLACK))
            if (g.items.isNotEmpty()) add(text(g.items, 13f, color = 0xFF555555.toInt()))
            add(text(g.note, 14f))
            for (id in g.channels) byId[id]?.let { r -> add(button("ส่งที่: ${r.name}") { openGuide(r, e) }) }
        }
        add(text("ช่องทางทั้งหมด", 18f, true))
        for (r in all) add(button(r.name) { openGuide(r, e) })
    }

    private fun detail(r: Recipient, e: EventInfo?) {
        add(text(r.name, 22f, true))
        add(text("ใช้กับ: ${r.scope}", 14f))
        add(text("ตรวจล่าสุด ${r.checked}: ${r.verified}", 13f, color = 0xFF555555.toInt()))
        r.warn?.let { add(text("ระวัง: $it", 14f, true, 0xFFB00020.toInt())) }
        r.url?.let { add(text("เว็บ: $it", 15f, true)) }
        r.email?.let { add(text("อีเมล: $it", 15f, true)) }
        r.phone?.let { add(text("โทร: $it", 15f, true)) }

        add(text("ขั้นตอน", 18f, true))
        r.steps.forEachIndexed { i, step ->
            add(text("${i + 1}. $step", 15f))
            val img = r.images.getOrNull(i).orEmpty()
            if (img.isNotEmpty()) {
                val bmp = try {
                    assets.open("guide/$img").use { BitmapFactory.decodeStream(it) }
                } catch (ex: Exception) {
                    null
                }
                if (bmp != null) add(ImageView(this).apply { adjustViewBounds = true; setImageBitmap(bmp) })
            }
        }
        if (r.images.isEmpty()) add(text("ยังไม่มีภาพประกอบหน้าจอ ผู้ร่วมพัฒนาเพิ่มได้ (ดู CONTRIBUTING.md)", 12f, color = 0xFF555555.toInt()))

        if (e != null) {
            add(button("คัดลอกข้อความรายงาน") {
                getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("report", Report.text(this, e)))
                Toast.makeText(this, "คัดลอกแล้ว วางในแบบฟอร์มได้", Toast.LENGTH_SHORT).show()
            })
            if (r.how == "web") add(button("บันทึกวิดีโอและรูปลงแกลเลอรี (เพื่อแนบในเว็บ)") { saveGallery(e) })
        }
        r.url?.let { url -> add(button("เปิดเว็บ") { Report.gate(this) { Report.openUrl(this, url) } }) }
        r.email?.let { to ->
            if (e != null) add(button("ส่งอีเมล") { Report.gate(this) { Report.mail(this, e, to) } })
            else add(text("เปิดคู่มือนี้จากปุ่ม \"ส่งรายงาน\" ของเหตุการณ์ เพื่อแนบข้อความและไฟล์", 12f, color = 0xFF555555.toInt()))
        }
        r.phone?.let { n -> add(button("โทร $n") { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$n"))) }) }
        add(button("แจ้งว่าไม่ตรง (หน้าเว็บหรือขั้นตอนเปลี่ยนไป)") {
            val version = packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
            Report.openUrl(this, Report.problemUrl(r, version))
        })
    }

    private fun saveGallery(e: EventInfo) {
        Thread {
            val n = try {
                Report.saveToGallery(this, e)
            } catch (ex: Exception) {
                -1
            }
            runOnUiThread {
                Toast.makeText(
                    this,
                    when {
                        n < 0 -> "บันทึกลงแกลเลอรีไม่สำเร็จ"
                        n == 0 -> "ไม่มีไฟล์ให้บันทึก (รวมคลิปก่อน) หรือเครื่องเก่ากว่า Android 10"
                        else -> "บันทึก $n ไฟล์แล้ว: แกลเลอรี > Movies หรือ Pictures/SafetyRoadWatch"
                    },
                    Toast.LENGTH_LONG,
                ).show()
            }
        }.start()
    }
}
