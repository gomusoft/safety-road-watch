package watch.saferoad

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

class LibraryActivity : AppCompatActivity() {
    private lateinit var store: EventStore
    private lateinit var list: LinearLayout
    private var player: MediaPlayer? = null

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = EventStore(this)
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.WHITE)
            addView(list)
        })
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun onPause() {
        super.onPause()
        stopPlayer()
    }

    private fun text(s: String, sp: Float = 15f, bold: Boolean = false) = TextView(this).apply {
        text = s
        setTextColor(Color.BLACK)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { onClick() }
    }

    private fun render() {
        stopPlayer()
        list.removeAllViews()
        val events = store.list()
        if (events.isEmpty()) list.addView(text("ยังไม่มีเหตุการณ์ที่บันทึกไว้", 18f))
        for (e in events) list.addView(card(e))
    }

    private fun card(e: EventInfo): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFEEEEEE.toInt())
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(12) }
        }
        card.addView(text(e.iso.replace('T', ' '), 14f, true))
        val who = listOfNotNull(e.plate, e.province).joinToString(" ")
        card.addView(text(who.ifEmpty { "ไม่ได้ยินทะเบียน" }, 20f, true))
        if (e.violations.isNotEmpty()) card.addView(text("ความผิด: " + e.violations.joinToString(", ")))
        if (e.transcript.isNotEmpty()) card.addView(text("“${e.transcript}”"))
        if (e.note.isNotEmpty()) card.addView(text(e.note, 13f))

        val wav = File(e.dir, "voice.wav")
        if (wav.exists()) {
            val b = button("▶ ฟังเสียง") {}
            b.setOnClickListener { toggleVoice(wav, b) }
            card.addView(b)
        }

        val checks = ArrayList<Pair<CheckBox, File>>()
        val clips = e.dir.listFiles { f -> f.name.startsWith("clip_") && f.name.endsWith(".mp4") }
            ?.sortedBy { it.name } ?: emptyList()
        for (f in clips) {
            val start = f.name.removePrefix("clip_").removeSuffix(".mp4").toLongOrNull() ?: e.time
            val off = Math.round((start - e.time) / 1000.0).toInt()
            val cb = CheckBox(this).apply {
                isChecked = true
                setTextColor(Color.BLACK)
                text = if (off >= 0) "+${off}s" else "${off}s"
            }
            checks.add(cb to f)
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(cb, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            row.addView(button("▶") { playClip(f) })
            card.addView(row)
        }
        val selected = { checks.filter { it.first.isChecked }.map { it.second } }

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(button("รวมคลิปที่เลือก") { merge(e, selected()) }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        actions.addView(button("แชร์") { share(e, selected()) }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        actions.addView(button("ลบ") { confirmDelete(e) }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        card.addView(actions)
        return card
    }

    private fun uri(f: File): Uri = FileProvider.getUriForFile(this, "$packageName.files", f)

    private fun playClip(f: File) {
        val i = Intent(Intent.ACTION_VIEW).setDataAndType(uri(f), "video/mp4")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(i)
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(this, "ไม่มีแอปเล่นวิดีโอ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleVoice(f: File, b: Button) {
        if (player != null) {
            stopPlayer()
            b.text = "▶ ฟังเสียง"
            return
        }
        try {
            player = MediaPlayer().apply {
                setDataSource(f.path)
                setOnCompletionListener {
                    stopPlayer()
                    b.text = "▶ ฟังเสียง"
                }
                prepare()
                start()
            }
            b.text = "■ หยุด"
        } catch (ex: Exception) {
            stopPlayer()
            Toast.makeText(this, "เล่นเสียงไม่ได้", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopPlayer() {
        player?.release()
        player = null
    }

    private fun merge(e: EventInfo, files: List<File>) {
        if (files.isEmpty()) {
            Toast.makeText(this, "ยังไม่ได้เลือกคลิป", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "กำลังรวมคลิป…", Toast.LENGTH_SHORT).show()
        Thread {
            val ok = try {
                Merge.concat(files, File(e.dir, "merged.mp4"))
                true
            } catch (ex: Exception) {
                false
            }
            runOnUiThread {
                Toast.makeText(this, if (ok) "รวมคลิปเสร็จแล้ว (merged.mp4)" else "รวมคลิปไม่สำเร็จ", Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    private fun share(e: EventInfo, files: List<File>) {
        val merged = File(e.dir, "merged.mp4")
        val send = if (merged.exists()) listOf(merged) else files
        if (send.isEmpty()) {
            Toast.makeText(this, "ยังไม่ได้เลือกคลิป", Toast.LENGTH_SHORT).show()
            return
        }
        val place = if (e.lat != null && e.lon != null) "https://maps.google.com/?q=${e.lat},${e.lon}" else "-"
        val report = "แจ้งการกระทำผิดกฎจราจร\n" +
            "วันเวลา: ${e.iso.replace('T', ' ')}\n" +
            "ทะเบียน: ${e.plate ?: "-"} ${e.province ?: ""}\n" +
            "ความผิด: ${e.violations.joinToString(", ")}\n" +
            "สถานที่: $place"
        val i = Intent(Intent.ACTION_SEND_MULTIPLE).setType("video/mp4")
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(send.map { uri(it) }))
            .putExtra(Intent.EXTRA_TEXT, report)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(i, "แชร์"))
    }

    private fun confirmDelete(e: EventInfo) {
        AlertDialog.Builder(this)
            .setMessage("ลบเหตุการณ์นี้ทั้งหมด (วิดีโอ เสียง ข้อมูล)?")
            .setPositiveButton("ลบ") { _, _ ->
                e.dir.deleteRecursively()
                render()
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }
}
