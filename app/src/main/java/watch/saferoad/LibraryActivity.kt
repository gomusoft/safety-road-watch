package watch.saferoad

import android.content.ContentValues
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.util.concurrent.Executors

class LibraryActivity : AppCompatActivity() {
    private lateinit var store: EventStore
    private lateinit var list: LinearLayout
    private var player: MediaPlayer? = null
    private val skipped = HashSet<String>() // clips the user unticked; kept so they stay unticked after watching a clip
    private val pool = Executors.newFixedThreadPool(2) // video frames are decoded one or two at a time

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
        if (Settings.lookupOn(this)) autoFetch()
    }

    /** Fills the place of events that have GPS but no address yet. Quietly gives up when offline. */
    private fun autoFetch() {
        val todo = store.list().filter { it.lat != null && it.lon != null && it.place.src.isEmpty() && it.place.text().isEmpty() }
        if (todo.isEmpty()) return
        Thread {
            var any = false
            for (e in todo) {
                val p = try {
                    store.lookupPlace(e.lat!!, e.lon!!)
                } catch (ex: Exception) {
                    null
                } ?: break // offline or no service: stop, try again next time
                store.savePlace(e.dir, Place(p.road, null, p.tambon, p.amphoe, p.changwat, p.src))
                any = true
            }
            if (any) runOnUiThread { if (!isFinishing) render() }
        }.start()
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

    private fun button(label: String, icon: Int? = null, onClick: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        minHeight = dp(48)
        if (icon != null) {
            setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
            compoundDrawableTintList = textColors
        }
        setOnClickListener { onClick() }
    }

    /** A row whose buttons all have the same width. */
    private fun row(vararg views: View) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        for (v in views) addView(v, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
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
        val clips = e.dir.listFiles { f -> f.name.startsWith("clip_") && f.name.endsWith(".mp4") }
            ?.sortedBy { it.name } ?: emptyList()
        if (clips.isNotEmpty()) {
            val pic = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(0xFFCCCCCC.toInt())
                setOnClickListener { playClip(File(e.dir, "merged.mp4").takeIf { it.exists() } ?: clips.first()) }
            }
            card.addView(pic, LinearLayout.LayoutParams(MATCH_PARENT, dp(180)).apply { topMargin = dp(4); bottomMargin = dp(8) })
            pool.execute {
                val bmp = Thumb.load(e)
                if (bmp != null) runOnUiThread { if (!isFinishing) pic.setImageBitmap(bmp) }
            }
        }
        val who = listOfNotNull(e.plate, e.province).joinToString(" ")
        card.addView(text(who.ifEmpty { "ไม่ได้ยินทะเบียน" }, 20f, true))
        if (e.violations.isNotEmpty()) card.addView(text("ความผิด: " + e.violations.joinToString(", ")))
        if (e.transcript.isNotEmpty()) card.addView(text("“${e.transcript}”"))
        if (e.note.isNotEmpty()) card.addView(text(e.note, 13f))

        val hasGps = e.lat != null && e.lon != null
        card.addView(text(if (hasGps) "พิกัด GPS: %.5f, %.5f".format(e.lat, e.lon) else "ไม่มีพิกัด GPS", 13f))
        val p = e.place
        val names = listOf("road" to "ถนน", "km" to "หลักกิโล", "tambon" to "ตำบล", "amphoe" to "อำเภอ", "changwat" to "จังหวัด")
        val from = mapOf("gps" to "จาก GPS", "voice" to "จากเสียง", "manual" to "แก้เอง")
        for ((k, label) in names) {
            val v = p.value(k) ?: continue
            card.addView(text("$label: $v  (${from[p.src[k]] ?: "-"})", 14f))
        }
        if (names.all { p.value(it.first) == null }) card.addView(text("ยังไม่มีที่อยู่: กด \"ดึงที่อยู่\" (จาก GPS) หรือแก้เอง", 14f))
        val placeRow = row(
            button("ดึงที่อยู่", R.drawable.ic_place) {
                if (Settings.lookupOn(this)) fetchPlace(e) else Settings.askLookupConsent(this) { fetchPlace(e) }
            }.apply { isEnabled = hasGps },
            button("แก้ที่อยู่ / กม.", R.drawable.ic_edit) { editPlace(e) },
        )
        card.addView(placeRow)

        val wav = File(e.dir, "voice.wav")
        if (wav.exists()) {
            val b = button("ฟังเสียง", R.drawable.ic_play) {}
            b.setOnClickListener { toggleVoice(wav, b) }
            card.addView(row(b))
        }

        val checks = ArrayList<Pair<CheckBox, File>>()
        for (f in clips) {
            val start = f.name.removePrefix("clip_").removeSuffix(".mp4").toLongOrNull() ?: e.time
            val off = Math.round((start - e.time) / 1000.0).toInt()
            val label = if (off >= 0) "+${off}s" else "${off}s"
            val cb = CheckBox(this).apply {
                isChecked = f.path !in skipped
                setTextColor(Color.BLACK)
                text = label
                setOnCheckedChangeListener { _, on -> if (on) skipped.remove(f.path) else skipped.add(f.path) }
            }
            checks.add(cb to f)
            val line = SwipeRow(this) { confirmDeleteClip(f, label) }.apply { orientation = LinearLayout.HORIZONTAL }
            line.addView(stillOf(f), LinearLayout.LayoutParams(dp(96), dp(54)).apply { marginEnd = dp(8) })
            line.addView(cb, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            line.addView(button("ดู", R.drawable.ic_play) { playClip(f) }, LinearLayout.LayoutParams(dp(110), WRAP_CONTENT))
            card.addView(line)
        }
        if (clips.isNotEmpty()) card.addView(text("ปัดคลิปไปทางซ้ายหรือขวาเพื่อลบคลิปนั้น", 12f).apply { setTextColor(0xFF555555.toInt()) })
        val selected = { checks.filter { it.first.isChecked }.map { it.second } }

        card.addView(row(
            button("รวมคลิป", R.drawable.ic_merge) { merge(e, selected()) },
            button("แชร์", R.drawable.ic_share) { share(e, selected()) },
            button("ลบ", R.drawable.ic_delete) { confirmDelete(e) },
        ))

        val merged = File(e.dir, "merged.mp4")
        if (merged.exists()) {
            card.addView(text("คลิปที่รวมแล้ว: ${"%.1f".format(merged.length() / 1048576.0)} MB (อยู่ในแอป)", 13f))
            card.addView(stillOf(merged), LinearLayout.LayoutParams(dp(160), dp(90)).apply { bottomMargin = dp(4) })
            val open = button("เปิดดู", R.drawable.ic_play) { playClip(merged) }
            card.addView(
                if (Build.VERSION.SDK_INT >= 29) row(open, button("บันทึกลงแกลเลอรี", R.drawable.ic_save) { saveToGallery(e, merged) })
                else row(open),
            )
        }
        return card
    }

    /** Small picture of the first frame of a video file; tapping it plays the video. */
    private fun stillOf(f: File) = ImageView(this).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        setBackgroundColor(0xFFCCCCCC.toInt())
        setOnClickListener { playClip(f) }
        pool.execute {
            val bmp = Thumb.first(f)
            if (bmp != null) runOnUiThread { if (!isFinishing) setImageBitmap(bmp) }
        }
    }

    override fun onDestroy() {
        pool.shutdownNow()
        super.onDestroy()
    }

    private fun uri(f: File): Uri = FileProvider.getUriForFile(this, "$packageName.files", f)

    private fun playClip(f: File) = startActivity(Intent(this, PlayerActivity::class.java).putExtra("path", f.path))

    private fun toggleVoice(f: File, b: Button) {
        if (player != null) {
            stopPlayer()
            b.text = "ฟังเสียง"
            return
        }
        try {
            player = MediaPlayer().apply {
                setDataSource(f.path)
                setOnCompletionListener {
                    stopPlayer()
                    b.text = "ฟังเสียง"
                }
                prepare()
                start()
            }
            b.text = "หยุด"
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
                if (isFinishing) return@runOnUiThread
                if (!ok) {
                    Toast.makeText(this, "รวมคลิปไม่สำเร็จ", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                render()
                mergedDialog(e)
            }
        }.start()
    }

    private fun mergedDialog(e: EventInfo) {
        val merged = File(e.dir, "merged.mp4")
        val b = AlertDialog.Builder(this)
            .setTitle("รวมคลิปเสร็จแล้ว")
            .setPositiveButton("เปิดดู") { _, _ -> playClip(merged) }
            .setNegativeButton("ปิด", null)
        if (Build.VERSION.SDK_INT >= 29) {
            b.setMessage("ไฟล์อยู่ในแอปนี้ ยังไม่อยู่ในแกลเลอรี\nกด \"บันทึกลงแกลเลอรี\" เพื่อเก็บไว้ในโฟลเดอร์ Movies/SafetyRoadWatch " +
                "(ถ้าเปิดสำรองรูปขึ้นคลาวด์ ไฟล์นี้อาจถูกอัปโหลดด้วย)")
            b.setNeutralButton("บันทึกลงแกลเลอรี") { _, _ -> saveToGallery(e, merged) }
        } else {
            b.setMessage("ไฟล์อยู่ในแอปนี้ กด \"แชร์\" เพื่อส่งออก")
        }
        b.show()
    }

    private fun saveToGallery(e: EventInfo, f: File) {
        if (Build.VERSION.SDK_INT < 29) return
        Thread {
            val ok = try {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, "safety_road_watch_${e.dir.name}.mp4")
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/SafetyRoadWatch")
                }
                val dst = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)!!
                contentResolver.openOutputStream(dst)!!.use { out -> f.inputStream().use { it.copyTo(out) } }
                true
            } catch (ex: Exception) {
                false
            }
            runOnUiThread {
                Toast.makeText(
                    this,
                    if (ok) "บันทึกแล้ว: แกลเลอรี > Movies/SafetyRoadWatch" else "บันทึกลงแกลเลอรีไม่สำเร็จ",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }.start()
    }

    private fun fetchPlace(e: EventInfo) {
        val lat = e.lat ?: return
        val lon = e.lon ?: return
        Toast.makeText(this, "กำลังดึงที่อยู่ (ส่งพิกัดให้บริการแผนที่ของเครื่อง ต้องใช้เน็ต)…", Toast.LENGTH_LONG).show()
        Thread {
            val p = try {
                store.lookupPlace(lat, lon)
            } catch (ex: Exception) {
                null
            }
            // Keep the kilometer marker (from the voice note or typed by hand).
            if (p != null) {
                // Keep what the user typed or said; fill the rest from GPS.
                val old = e.place
                val keep = { k: String -> old.src[k] == "manual" || (k == "km") }
                val pick = { k: String -> if (keep(k) && old.value(k) != null) old.value(k) else p.value(k) }
                val src = HashMap(p.src)
                for (k in listOf("road", "km", "tambon", "amphoe", "changwat"))
                    if (keep(k) && old.value(k) != null) src[k] = old.src[k] ?: "voice" else if (k == "km") src.remove(k)
                store.savePlace(e.dir, Place(pick("road"), pick("km"), pick("tambon"), pick("amphoe"), pick("changwat"), src))
            }
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                if (p == null) Toast.makeText(this, "ดึงที่อยู่ไม่ได้ (ไม่มีเน็ตหรือไม่มีบริการแผนที่) ใช้งานอย่างอื่นต่อได้ หรือกดแก้เอง", Toast.LENGTH_LONG).show() else render()
            }
        }.start()
    }

    private fun editPlace(e: EventInfo) {
        val p = e.place
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        fun field(hint: String, v: String?) = EditText(this).apply {
            this.hint = hint
            setText(v ?: "")
            setSingleLine()
            box.addView(this)
        }
        val road = field("ถนน / ทางหลวง", p.road)
        val km = field("หลักกิโลเมตร (เช่น 45 หรือ 12+500)", p.km)
        val tambon = field("ตำบล / แขวง", p.tambon)
        val amphoe = field("อำเภอ / เขต", p.amphoe)
        val changwat = field("จังหวัด", p.changwat)
        AlertDialog.Builder(this)
            .setTitle("ที่อยู่เหตุการณ์")
            .setView(ScrollView(this).apply { addView(box) })
            .setPositiveButton("บันทึก") { _, _ ->
                val t = { v: EditText -> v.text.toString().trim() }
                val new = listOf("road" to t(road), "km" to t(km), "tambon" to t(tambon), "amphoe" to t(amphoe), "changwat" to t(changwat))
                val src = HashMap(p.src)
                for ((k, v) in new) if (v != (p.value(k) ?: "")) src[k] = "manual"
                val g = { k: String -> new.first { it.first == k }.second }
                store.savePlace(e.dir, Place(g("road"), g("km"), g("tambon"), g("amphoe"), g("changwat"), src))
                render()
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    private fun share(e: EventInfo, files: List<File>) {
        val merged = File(e.dir, "merged.mp4")
        val send = if (merged.exists()) listOf(merged) else files
        if (send.isEmpty()) {
            Toast.makeText(this, "ยังไม่ได้เลือกคลิป", Toast.LENGTH_SHORT).show()
            return
        }
        val map = if (e.lat != null && e.lon != null) "https://maps.google.com/?q=${e.lat},${e.lon}" else ""
        val place = (e.place.text() + " " + map).trim().ifEmpty { "-" }
        val report = "แจ้งการกระทำผิดกฎจราจร\n" +
            "วันเวลา: ${e.iso.replace('T', ' ')}\n" +
            "ทะเบียน: ${e.plate ?: "-"} ${e.province ?: ""}\n" +
            "ความผิด: ${e.violations.joinToString(", ")}\n" +
            "สถานที่: $place"
        val still = File(e.dir, "thumb.jpg").takeIf { it.exists() }
        val i = Intent(Intent.ACTION_SEND_MULTIPLE).setType(if (still != null) "*/*" else "video/mp4")
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList((listOfNotNull(still) + send).map { uri(it) }))
            .putExtra(Intent.EXTRA_TEXT, report)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(i, "แชร์"))
    }

    private fun confirmDeleteClip(f: File, label: String) {
        AlertDialog.Builder(this)
            .setMessage("ลบคลิป $label นี้? กู้คืนไม่ได้ (วิดีโอที่รวมไว้แล้วไม่หาย)")
            .setPositiveButton("ลบ") { _, _ ->
                f.delete()
                skipped.remove(f.path)
                render()
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
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
