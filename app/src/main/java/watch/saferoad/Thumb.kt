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

    /** First frame of any clip or merged video, small, kept in memory only. Blocking: call off the main thread. */
    fun first(f: File): Bitmap? {
        val key = "${f.path}:${f.lastModified()}"
        small.get(key)?.let { return it }
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(f.path)
            val raw = r.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
            val bmp = if (raw.width > 320) Bitmap.createScaledBitmap(raw, 320, raw.height * 320 / raw.width, true) else raw
            small.put(key, bmp)
            bmp
        } catch (ex: Exception) {
            null
        } finally {
            r.release()
        }
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
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(clip.path)
            val us = if (exact != null) (e.time - startOf(clip)!!) * 1000 else 0L
            val raw = r.getFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST) ?: return null
            val bmp = if (raw.width > 720) Bitmap.createScaledBitmap(raw, 720, raw.height * 720 / raw.width, true) else raw
            if (exact != null) {
                FileOutputStream(saved).use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                cache.put(e.dir.name, bmp)
            }
            bmp
        } catch (ex: Exception) {
            null
        } finally {
            r.release()
        }
    }
}
