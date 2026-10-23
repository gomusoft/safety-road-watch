package watch.saferoad

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import java.io.File
import java.io.FileOutputStream

/** The still picture of an event: the video frame at the moment of the press, saved once as thumb.jpg. */
object Thumb {
    private val cache = LruCache<String, Bitmap>(12)
    private val small = LruCache<String, Bitmap>(80)

    /** The small picture if it is already in memory, so the library can show it at once without a grey flash. */
    fun peek(f: File): Bitmap? = small.get("${f.path}:${f.lastModified()}")

    /** The card picture of an event if it is already in memory. */
    fun peekCard(e: EventInfo): Bitmap? = cache.get(e.dir.name)

    /**
     * A frame of a video. The phone's video decoder can be busy (right after the player or a merge) and then gives
     * nothing, so try a few times before giving up. Blocking.
     */
    private fun frame(f: File, us: Long, option: Int): Bitmap? {
        for (i in 0..2) {
            val r = MediaMetadataRetriever()
            try {
                r.setDataSource(f.path)
                r.getFrameAtTime(us, option)?.let { return it }
            } catch (ex: Exception) {
            } finally {
                r.release()
            }
            if (i < 2) {
                try {
                    Thread.sleep(400L * (i + 1))
                } catch (ex: InterruptedException) {
                    return null
                }
            }
        }
        return null
    }

    /**
     * First frame of any clip or merged video, small. Kept in memory and as a small file next to the video (so it is
     * there again after the app was closed and while the decoder is busy). Blocking: call off the main thread.
     */
    fun first(f: File): Bitmap? {
        val key = "${f.path}:${f.lastModified()}"
        small.get(key)?.let { return it }
        val disk = File(f.path + ".jpg")
        if (disk.exists() && disk.lastModified() >= f.lastModified()) {
            BitmapFactory.decodeFile(disk.path)?.let {
                small.put(key, it)
                return it
            }
        }
        val raw = frame(f, 0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
        val bmp = if (raw.width > 320) Bitmap.createScaledBitmap(raw, 320, raw.height * 320 / raw.width, true) else raw
        try {
            FileOutputStream(disk).use { bmp.compress(Bitmap.CompressFormat.JPEG, 80, it) }
        } catch (ex: Exception) {
        }
        small.put(key, bmp)
        return bmp
    }

    /** A captured still picture, shrunk for the library strip. Blocking: call off the main thread. */
    fun still(f: File): Bitmap? {
        val key = "${f.path}:${f.lastModified()}"
        small.get(key)?.let { return it }
        val bmp = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = 4 }) ?: return null
        small.put(key, bmp)
        return bmp
    }

    private fun startOf(f: File) = f.name.removePrefix("clip_").removeSuffix(".mp4").toLongOrNull()

    /** Blocking (decodes video): call off the main thread. Null when there is no clip yet or it cannot be read. */
    fun load(e: EventInfo): Bitmap? {
        cache.get(e.dir.name)?.let { return it }
        val saved = File(e.dir, "thumb.jpg")
        if (saved.exists()) {
            BitmapFactory.decodeFile(saved.path)?.let {
                cache.put(e.dir.name, it)
                return it
            }
        }
        val clips = (e.dir.listFiles { f -> f.name.startsWith("clip_") && f.name.endsWith(".mp4") } ?: return null)
            .filter { startOf(it) != null }.sortedBy { startOf(it) }
        // The clip that was recording at the press; until it has arrived, show the first one but do not keep it.
        val exact = clips.lastOrNull { startOf(it)!! <= e.time }
        val clip = exact ?: clips.firstOrNull() ?: return null
        val us = if (exact != null) (e.time - startOf(clip)!!) * 1000 else 0L
        val raw = frame(clip, us, MediaMetadataRetriever.OPTION_CLOSEST) ?: return null
        val bmp = if (raw.width > 720) Bitmap.createScaledBitmap(raw, 720, raw.height * 720 / raw.width, true) else raw
        if (exact != null) {
            try {
                FileOutputStream(saved).use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                cache.put(e.dir.name, bmp)
            } catch (ex: Exception) {
            }
        }
        return bmp
    }
}
