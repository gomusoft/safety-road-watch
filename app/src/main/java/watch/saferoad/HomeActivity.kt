package watch.saferoad

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/** First screen: start recording, open the library or the settings, and see at a glance whether the phone is ready. */
class HomeActivity : AppCompatActivity() {
    private lateinit var library: Button
    private lateinit var readiness: TextView

    private val askPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        library = findViewById(R.id.btnLibrary)
        readiness = findViewById(R.id.readiness)
        findViewById<Button>(R.id.btnStart).setOnClickListener { startRecording() }
        library.setOnClickListener { startActivity(Intent(this, LibraryActivity::class.java)) }
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        readiness.setOnClickListener { if (missing().isNotEmpty()) askPermissions.launch(PERMISSIONS) }
        // Only on a fresh start, so coming back from the camera or a rotation does not reopen it.
        if (savedInstanceState == null && Settings.autoStart(this)) startRecording()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun startRecording() = startActivity(Intent(this, MainActivity::class.java))

    private fun has(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    private fun missing(): List<String> = buildList {
        if (!has(Manifest.permission.CAMERA)) add("กล้อง")
        if (!has(Manifest.permission.RECORD_AUDIO)) add("ไมค์")
        if (!has(Manifest.permission.ACCESS_FINE_LOCATION) && !has(Manifest.permission.ACCESS_COARSE_LOCATION)) add("GPS")
    }

    private fun refresh() {
        val n = EventStore(this).list().size
        library.text = if (n == 0) "คลัง" else "คลัง ($n)"
        val m = missing()
        if (m.isEmpty()) {
            readiness.setTextColor(0xFFB0BEC5.toInt())
            readiness.text = "กล้อง ✓   ไมค์ ✓   GPS ✓"
        } else {
            readiness.setTextColor(Color.parseColor("#FFCA28"))
            readiness.text = "ยังไม่ได้อนุญาต: ${m.joinToString(", ")}\nแตะที่นี่เพื่ออนุญาต"
        }
    }

    private companion object {
        val PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
}
