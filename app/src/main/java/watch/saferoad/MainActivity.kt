package watch.saferoad

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
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
    private lateinit var catchBtn: Button
    private val handler = Handler(Looper.getMainLooper())
    private var pulse: ObjectAnimator? = null
    private var showingResult = false
    private val clearResult = Runnable {
        showingResult = false
        if (!voice.isRecording) status.setText(R.string.status_ready)
    }

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
        rec.onCollectEnd = { if (!voice.isRecording && !showingResult) status.setText(R.string.status_ready) }
        rec.onProblem = { status.text = "กล้องมีปัญหา (code $it)" }
        catchBtn = findViewById(R.id.btnCatch)
        catchBtn.setOnClickListener { onCatch() }
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
        pulse?.cancel()
        handler.removeCallbacks(clearResult)
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
        val dir = rec.trigger()
        if (dir == null) {
            status.text = "กล้องยังไม่พร้อม"
            return
        }
        catchBtn.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS,
        )
        if (!has(Manifest.permission.RECORD_AUDIO)) {
            saveEvent(dir, "", "ไม่ได้อนุญาตไมโครโฟน")
            return
        }
        handler.removeCallbacks(clearResult)
        showingResult = false
        showRecording(true)
        voice.start(dir, {
            showRecording(false)
            status.text = "⏳ กำลังแปลงเสียง… กดบันทึกเหตุการณ์ใหม่ได้เลย"
        }) { transcript, note -> saveEvent(dir, transcript, note) }
    }

    /** Yellow, pulsing button and a large yellow banner while the microphone is on. */
    private fun showRecording(on: Boolean) {
        pulse?.cancel()
        pulse = null
        catchBtn.alpha = 1f
        if (on) {
            catchBtn.setText(R.string.btn_recording)
            catchBtn.backgroundTintList = ColorStateList.valueOf(0xE6FFC107.toInt())
            catchBtn.setTextColor(Color.BLACK)
            catchBtn.setShadowLayer(0f, 0f, 0f, 0)
            catchBtn.textSize = 28f
            status.text = "🎙️ พูดได้เลย: ทะเบียน จังหวัด ความผิด\nเงียบ 8 วินาทีจะจบเอง หรือกดอีกครั้งเพื่อจบ"
            status.setBackgroundColor(0xE6FFC107.toInt())
            status.setTextColor(Color.BLACK)
            status.textSize = 20f
            pulse = ObjectAnimator.ofFloat(catchBtn, "alpha", 1f, 0.55f).apply {
                duration = 600
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                start()
            }
        } else {
            catchBtn.setText(R.string.btn_catch)
            catchBtn.backgroundTintList = ColorStateList.valueOf(0x8043A047.toInt())
            catchBtn.setTextColor(Color.WHITE)
            catchBtn.setShadowLayer(4f, 0f, 0f, 0x80000000.toInt())
            catchBtn.textSize = 40f
            status.setBackgroundColor(0x99000000.toInt())
            status.setTextColor(Color.WHITE)
            status.textSize = 16f
        }
    }

    private fun saveEvent(dir: File, transcript: String, note: String) {
        val p = parse(transcript)
        store.update(dir) {
            put("transcript", transcript)
            put("plate", p.plate ?: JSONObject.NULL)
            put("province", p.province ?: JSONObject.NULL)
            put("violations", JSONArray(p.violations))
            put("km", p.km ?: JSONObject.NULL)
            if (p.km != null) put("place_src", JSONObject().put("km", "voice"))
            put("note", note)
        }
        val parts = listOfNotNull(p.plate, p.province) + p.violations
        val shown = if (parts.isEmpty()) "บันทึกแล้ว" else "บันทึก " + parts.joinToString(" ")
        if (!voice.isRecording) {
            status.text = "✅ $shown" + if (note.isNotEmpty()) "\n$note" else ""
            showingResult = true
            handler.removeCallbacks(clearResult)
            handler.postDelayed(clearResult, 6_000)
            // Never speak while the next note is being recorded: the mic would pick it up.
            if (ttsOk) tts?.speak(shown, TextToSpeech.QUEUE_FLUSH, null, "saved")
        }
    }
}
