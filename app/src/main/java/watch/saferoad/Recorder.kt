package watch.saferoad

import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import androidx.camera.video.Recorder as CxRecorder

/**
 * Rolling 10 s video-only segments. NEVER enables audio (the mic is only used by Voice after a button press).
 */
class Rolling(private val activity: AppCompatActivity, private val store: EventStore) {
    private val main = Handler(Looper.getMainLooper())
    private val ring = File(activity.filesDir, "ring").apply { mkdirs() }
    private val executor = ContextCompat.getMainExecutor(activity)

    private var started = false
    private var running = false
    private var generation = 0 // bumped by every start(), so a stale camera callback can tell it is stale
    private var videoCapture: VideoCapture<CxRecorder>? = null
    private var current: Recording? = null
    private var curStart = 0L
    private val stopSegment = Runnable { current?.stop() }

    /** Events still collecting "after" footage, each with the time its 30 s run out. Several can overlap. */
    private class Active(val dir: File, val deadline: Long)

    private val active = ArrayList<Active>()

    var onCollectEnd: (() -> Unit)? = null
    var onProblem: ((Int) -> Unit)? = null

    val collecting: Boolean get() = active.isNotEmpty()

    fun start(view: PreviewView) {
        if (started) return
        started = true
        val gen = ++generation
        val future = ProcessCameraProvider.getInstance(activity)
        future.addListener({
            if (!started || gen != generation) return@addListener
            val provider = future.get()
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(view.surfaceProvider)
            val selector = QualitySelector.from(
                Quality.HD,
                FallbackStrategy.lowerQualityOrHigherThan(Quality.HD),
            )
            val vc = VideoCapture.withOutput(CxRecorder.Builder().setQualitySelector(selector).build())
            provider.unbindAll()
            provider.bindToLifecycle(activity, CameraSelector.DEFAULT_BACK_CAMERA, preview, vc)
            videoCapture = vc
            ring.listFiles()?.forEach { it.delete() } // stale pre-roll is useless
            running = true
            startSegment()
        }, executor)
    }

    fun stop() {
        started = false
        running = false
        main.removeCallbacks(stopSegment)
        current?.stop()
    }

    /** Returns the new event folder, or null if the camera is not ready. Works while an earlier event is still collecting. */
    fun trigger(): File? {
        if (!running) return null
        val now = System.currentTimeMillis()
        val dir = store.create(now)
        // Footage that an earlier, still-collecting event already took out of the ring is copied, not shared.
        for (a in active) {
            a.dir.listFiles { f -> f.name.startsWith("clip_") && f.name.endsWith(".mp4") }?.forEach { f ->
                val s = f.name.removePrefix("clip_").removeSuffix(".mp4").toLongOrNull()
                if (s != null && s >= now - 35_000) f.copyTo(File(dir, f.name), true)
            }
        }
        ring.listFiles()?.forEach { f ->
            val s = startOf(f)
            if (s != null && s != curStart && s >= now - 35_000) moveTo(f, dir, s)
        }
        active.add(Active(dir, now + 30_000))
        return dir
    }

    private fun startOf(f: File) = f.name.removePrefix("seg_").removeSuffix(".mp4").toLongOrNull()

    private fun moveTo(f: File, dir: File, start: Long) {
        val dest = File(dir, "clip_$start.mp4")
        if (!f.renameTo(dest)) {
            f.copyTo(dest, true)
            f.delete()
        }
    }

    private fun startSegment() {
        val vc = videoCapture ?: return
        if (!running) return
        val start = System.currentTimeMillis()
        if (active.removeAll { start >= it.deadline } && active.isEmpty()) onCollectEnd?.invoke()
        val file = File(ring, "seg_$start.mp4")
        curStart = start
        current = vc.output.prepareRecording(activity, FileOutputOptions.Builder(file).build())
            .start(executor) { ev ->
                if (ev is VideoRecordEvent.Finalize) onFinalize(file, start, ev.error)
            }
        main.removeCallbacks(stopSegment)
        main.postDelayed(stopSegment, 10_000)
    }

    private fun onFinalize(file: File, start: Long, error: Int) {
        // After stop() + start() (screen off/on, library) the old segment's Finalize can arrive after the new
        // segment began. A stale one must not clear `current`, flip `running` or start a second recording.
        val mine = start == curStart
        if (mine) current = null
        val targets = active.filter { start < it.deadline }
        if (!file.exists() || file.length() == 0L) {
            file.delete()
            if (mine && error != VideoRecordEvent.Finalize.ERROR_NONE && running) {
                running = false // do not spin on a broken camera
                onProblem?.invoke(error)
                return
            }
        } else if (targets.isNotEmpty()) {
            // Every event still collecting gets this segment: copy to all but the last, move into the last.
            targets.dropLast(1).forEach { file.copyTo(File(it.dir, "clip_$start.mp4"), true) }
            moveTo(file, targets.last().dir, start)
        }
        if (mine && running) startSegment()
        // keep only the newest 4 finished segments
        ring.listFiles()
            ?.filter { startOf(it).let { s -> s != null && s != curStart } }
            ?.sortedByDescending { startOf(it) }
            ?.drop(4)
            ?.forEach { it.delete() }
    }
}
