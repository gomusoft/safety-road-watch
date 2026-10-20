package watch.saferoad

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/** Concatenates video-only MP4 files that share the same encoder settings. Blocking: call off the main thread. */
object Merge {
    fun concat(files: List<File>, out: File) {
        require(files.isNotEmpty())
        val retriever = MediaMetadataRetriever()
        val rotation = try {
            retriever.setDataSource(files[0].path)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        } finally {
            retriever.release()
        }

        val buf = ByteBuffer.allocate(2 * 1024 * 1024)
        val info = MediaCodec.BufferInfo()
        var muxer: MediaMuxer? = null
        var track = -1
        var offset = 0L
        try {
            muxer = MediaMuxer(out.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer.setOrientationHint(rotation)
            for (f in files) {
                val ex = MediaExtractor()
                try {
                    ex.setDataSource(f.path)
                    val idx = (0 until ex.trackCount).firstOrNull {
                        ex.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
                    } ?: continue
                    ex.selectTrack(idx)
                    if (track < 0) {
                        track = muxer.addTrack(ex.getTrackFormat(idx))
                        muxer.start()
                    }
                    var last = 0L
                    while (true) {
                        buf.clear()
                        val n = ex.readSampleData(buf, 0)
                        if (n < 0) break
                        val t = ex.sampleTime
                        val key = (ex.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0
                        info.set(0, n, t + offset, if (key) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
                        muxer.writeSampleData(track, buf, info)
                        if (t > last) last = t
                        ex.advance()
                    }
                    offset += last + 33_333L
                } finally {
                    ex.release()
                }
            }
            check(track >= 0) { "no video track" }
        } catch (e: Exception) {
            out.delete()
            throw e
        } finally {
            try {
                if (track >= 0) muxer?.stop()
            } catch (e: Exception) {
            }
            muxer?.release()
        }
    }
}
