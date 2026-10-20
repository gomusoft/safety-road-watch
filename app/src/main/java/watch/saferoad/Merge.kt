package watch.saferoad

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/** A spoken note (16 kHz mono WAV written by [pcmToWav]) placed [offsetMs] after the start of the merged video. */
class Narration(val wav: File, val offsetMs: Long)

/** Concatenates video-only MP4 files that share the same encoder settings. Blocking: call off the main thread. */
object Merge {
    /**
     * Writes [files] one after the other into [out], and lays [voice] over them as an AAC track when given.
     * Returns false when the voice could not be added (the video is still written, without sound).
     */
    fun concat(files: List<File>, out: File, voice: Narration? = null): Boolean {
        require(files.isNotEmpty())
        val encoded = try {
            voice?.let { encodeAac(it.wav) }
        } catch (e: Exception) {
            null
        }
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
            val audioTrack = encoded?.let { muxer.addTrack(it.format) } ?: -1
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
            if (encoded != null && voice != null) {
                for ((bytes, i) in encoded.samples) {
                    info.set(0, bytes.size, i.presentationTimeUs + voice.offsetMs * 1000L, i.flags)
                    muxer.writeSampleData(audioTrack, ByteBuffer.wrap(bytes), info)
                }
            }
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
        return voice == null || encoded != null
    }

    private class Encoded(val format: MediaFormat, val samples: List<Pair<ByteArray, MediaCodec.BufferInfo>>)

    /** Encodes a 16 kHz mono WAV (44-byte header) to AAC-LC in memory: a note is at most a minute, so it stays small. */
    private fun encodeAac(wav: File): Encoded {
        val rate = Voice.RATE
        val all = wav.readBytes()
        val pcm = all.copyOfRange(minOf(44, all.size), all.size)
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, rate, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 64_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16_384)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val samples = ArrayList<Pair<ByteArray, MediaCodec.BufferInfo>>()
        var outFormat: MediaFormat? = null
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            val info = MediaCodec.BufferInfo()
            var pos = 0
            var inputDone = false
            var done = false
            while (!done) {
                if (!inputDone) {
                    val i = codec.dequeueInputBuffer(10_000)
                    if (i >= 0) {
                        val b = codec.getInputBuffer(i)!!
                        b.clear()
                        val n = minOf(b.remaining(), pcm.size - pos)
                        val us = pos / 2L * 1_000_000L / rate
                        if (n <= 0) {
                            codec.queueInputBuffer(i, 0, 0, us, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            b.put(pcm, pos, n)
                            codec.queueInputBuffer(i, 0, n, us, 0)
                            pos += n
                        }
                    }
                }
                val o = codec.dequeueOutputBuffer(info, 10_000)
                if (o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    outFormat = codec.outputFormat
                } else if (o >= 0) {
                    val ob = codec.getOutputBuffer(o)!!
                    if (info.size > 0 && (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        val bytes = ByteArray(info.size)
                        ob.position(info.offset)
                        ob.limit(info.offset + info.size)
                        ob.get(bytes)
                        samples.add(bytes to MediaCodec.BufferInfo().apply { set(0, info.size, info.presentationTimeUs, info.flags) })
                    }
                    codec.releaseOutputBuffer(o, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) done = true
                }
            }
        } finally {
            try {
                codec.stop()
            } catch (e: Exception) {
            }
            codec.release()
        }
        check(samples.isNotEmpty()) { "no audio" }
        return Encoded(outFormat ?: error("no audio format"), samples)
    }
}
