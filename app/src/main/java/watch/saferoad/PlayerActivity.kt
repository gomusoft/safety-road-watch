package watch.saferoad

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

/**
 * Plays one clip or the merged video inside the app. The file path comes in as the "path" extra.
 * Pinch to zoom (to read a plate), and "จับภาพ" saves the frame on screen as a still picture of the event.
 */
class PlayerActivity : AppCompatActivity() {
    private var mp: MediaPlayer? = null
    private lateinit var stage: FrameLayout
    private lateinit var tv: TextureView
    private lateinit var seek: SeekBar
    private lateinit var time: TextView
    private lateinit var play: Button
    private lateinit var file: File
    private var videoW = 0
    private var videoH = 0
    private var dragging = false
    private var ready = false
    private val main = Handler(Looper.getMainLooper())

    private val tick = object : Runnable {
        override fun run() {
            val p = mp
            if (p != null && ready && !dragging) {
                seek.progress = p.currentPosition
                time.text = clock(p.currentPosition) + " / " + clock(p.duration)
            }
            main.postDelayed(this, 250)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun clock(ms: Int): String = "%d:%02d".format(ms / 60000, ms / 1000 % 60)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val path = intent.getStringExtra("path")
        if (path == null) {
            finish()
            return
        }
        file = File(path)

        tv = TextureView(this)
        stage = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(tv, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT, Gravity.CENTER))
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> fit() }
        }
        Zoomable(stage, tv)

        play = Button(this).apply {
            text = "หยุด"
            isAllCaps = false
            setOnClickListener { togglePlay() }
        }
        seek = SeekBar(this).apply {
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) mp?.seekTo(progress.toLong(), MediaPlayer.SEEK_CLOSEST)
                }
                override fun onStartTrackingTouch(s: SeekBar) {
                    dragging = true
                }
                override fun onStopTrackingTouch(s: SeekBar) {
                    dragging = false
                }
            })
        }
        time = TextView(this).apply {
            setTextColor(Color.WHITE)
            text = "0:00"
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0x99000000.toInt())
            setPadding(dp(8), dp(4), dp(8), dp(4))
            addView(play, LinearLayout.LayoutParams(WRAP_CONTENT, dp(48)))
            addView(seek, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            addView(time, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        }
        val capture = Button(this).apply {
            text = "จับภาพ"
            isAllCaps = false
            minHeight = dp(48)
            setOnClickListener { captureStill() }
        }
        val root = FrameLayout(this).apply {
            addView(stage, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(bar, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM))
            addView(capture, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.TOP or Gravity.END).apply { setMargins(dp(12), dp(12), dp(12), dp(12)) })
        }
        setContentView(root)

        tv.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) = open(st)
            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                release()
                return true
            }
            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
        }
        main.post(tick)
    }

    private fun open(st: SurfaceTexture) {
        try {
            mp = MediaPlayer().apply {
                setDataSource(file.path)
                setSurface(Surface(st))
                setOnVideoSizeChangedListener { _, w, h ->
                    videoW = w
                    videoH = h
                    fit()
                }
                setOnPreparedListener {
                    ready = true
                    seek.max = duration
                    start()
                }
                setOnCompletionListener { play.text = "เล่น" }
                setOnErrorListener { _, _, _ ->
                    Toast.makeText(this@PlayerActivity, "เล่นวิดีโอไม่ได้", Toast.LENGTH_SHORT).show()
                    finish()
                    true
                }
                prepareAsync()
            }
        } catch (ex: Exception) {
            Toast.makeText(this, "เล่นวิดีโอไม่ได้", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /** Sizes the video view to the video's aspect ratio inside the stage. */
    private fun fit() {
        if (videoW == 0 || videoH == 0 || stage.width == 0 || stage.height == 0) return
        val s = minOf(stage.width.toFloat() / videoW, stage.height.toFloat() / videoH)
        val w = (videoW * s).toInt()
        val h = (videoH * s).toInt()
        val lp = tv.layoutParams as FrameLayout.LayoutParams
        if (lp.width != w || lp.height != h) {
            lp.width = w
            lp.height = h
            tv.layoutParams = lp
        }
    }

    private fun togglePlay() {
        val p = mp ?: return
        if (p.isPlaying) {
            p.pause()
            play.text = "เล่น"
        } else {
            p.start()
            play.text = "หยุด"
        }
    }

    /** Saves the frame at the current position, at full video resolution, as still_<time>.jpg in the event folder. */
    private fun captureStill() {
        val p = mp ?: return
        if (!ready) return
        p.pause()
        play.text = "เล่น"
        val posMs = p.currentPosition
        val dir = file.parentFile ?: return
        Thread {
            val r = MediaMetadataRetriever()
            val ok = try {
                r.setDataSource(file.path)
                val frame: Bitmap = r.getFrameAtTime(posMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST) ?: error("no frame")
                FileOutputStream(File(dir, "still_${System.currentTimeMillis()}.jpg")).use { frame.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                true
            } catch (ex: Exception) {
                false
            } finally {
                r.release()
            }
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                val n = dir.listFiles { f -> f.name.startsWith("still_") }?.size ?: 0
                Toast.makeText(this, if (ok) "บันทึกภาพแล้ว (มี $n ภาพในเหตุการณ์นี้)" else "จับภาพไม่ได้", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun release() {
        ready = false
        mp?.release()
        mp = null
    }

    override fun onPause() {
        super.onPause()
        mp?.takeIf { ready && it.isPlaying }?.pause()
        play.text = "เล่น"
    }

    override fun onDestroy() {
        main.removeCallbacks(tick)
        release()
        super.onDestroy()
    }
}
