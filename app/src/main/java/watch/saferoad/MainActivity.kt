package watch.saferoad

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var store: EventStore
    private lateinit var rec: Rolling
    private lateinit var voice: Voice
    private lateinit var preview: PreviewView
    private lateinit var status: TextView
    private var tts: TextToSpeech? = null
    private var ttsOk = false
    private var busy = false

    private val askPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        startAll()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // If the screen is locked or switched off by accident, come back over the lock screen and light it up.
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        setContentView(R.layout.activity_main)
        preview = findViewById(R.id.preview)
        status = findViewById(R.id.status)
        store = EventStore(this)
        voice = Voice(this)
        rec = Rolling(this, store)
        rec.onCollectEnd = { if (!busy) status.setText(R.string.status_ready) }
        rec.onProblem = { status.text = "กล้องมีปัญหา (code $it)" }
        findViewById<Button>(R.id.btnCatch).setOnClickListener { onCatch() }
        findViewById<Button>(R.id.btnLibrary).setOnClickListener {
            startActivity(Intent(this, LibraryActivity::class.java))
        }
        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                ttsOk = tts?.setLanguage(Locale.forLanguageTag("th-TH")) != TextToSpeech.LANG_MISSING_DATA
            }
        }
        if (!has(Manifest.permission.CAMERA)) {
            askPermissions.launch(
                arrayOf(
                    Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    private fun has(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    private fun startAll() {
        if (has(Manifest.permission.CAMERA)) rec.start(preview) else status.text = "ต้องอนุญาตกล้องก่อน"
        store.startLocation()
    }

    override fun onResume() {
        super.onResume()
        if (has(Manifest.permission.CAMERA)) startAll()
    }

    override fun onPause() {
        super.onPause()
        rec.stop()
        store.stopLocation()
    }

    override fun onDestroy() {
        voice.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_ENTER,
            -> {
                if ((event?.repeatCount ?: 0) == 0) onCatch()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun onCatch() {
        if (voice.isRecording) {
            voice.stop() // finishes the voice note; Voice calls back when transcribed
            return
        }
        if (busy) return
        val dir = rec.trigger()
        if (dir == null) {
            if (!rec.collecting) status.text = "กล้องยังไม่พร้อม"
            return
        }
        if (!has(Manifest.permission.RECORD_AUDIO)) {
            saveEvent(dir, "", "ไม่ได้อนุญาตไมโครโฟน")
            return
        }
        busy = true
        status.text = "🎙️ พูดได้เลย: ทะเบียน จังหวัด ความผิด (กดอีกครั้งเพื่อจบ)"
        voice.start(dir, { status.text = "⏳ กำลังแปลงเสียง…" }) { transcript, note ->
            busy = false
            saveEvent(dir, transcript, note)
        }
    }

    private fun saveEvent(dir: File, transcript: String, note: String) {
        val p = parse(transcript)
        store.update(dir) {
            put("transcript", transcript)
            put("plate", p.plate ?: JSONObject.NULL)
            put("province", p.province ?: JSONObject.NULL)
            put("violations", JSONArray(p.violations))
            put("note", note)
        }
        val parts = listOfNotNull(p.plate, p.province) + p.violations
        val shown = if (parts.isEmpty()) "บันทึกแล้ว" else "บันทึก " + parts.joinToString(" ")
        status.text = "✅ $shown" + if (note.isNotEmpty()) "\n$note" else ""
        if (ttsOk) tts?.speak(shown, TextToSpeech.QUEUE_FLUSH, null, "saved")
    }
}
