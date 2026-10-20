package watch.saferoad

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.SystemClock
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** The one place the microphone is opened. Callers only call it after the user pressed a button. */
object Mic {
    /**
     * Records 16 kHz mono PCM16 into [pcm] until [keepGoing] says no, [maxMs] pass, or [onBuffer] returns true.
     * VOICE_RECOGNITION tunes the mic for speech; the phone's own noise suppression and gain control are added when it
     * has them, and a high-pass filter cuts engine rumble. Throws if the mic cannot be opened. Blocking.
     */
    @SuppressLint("MissingPermission")
    fun record(pcm: File, maxMs: Long, keepGoing: () -> Boolean, onBuffer: (ByteArray, Int) -> Boolean = { _, _ -> false }) {
        val rate = Voice.RATE
        val size = maxOf(AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT), 4096)
        val ar = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION, rate, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, size * 2,
        )
        var ns: NoiseSuppressor? = null
        var agc: AutomaticGainControl? = null
        try {
            check(ar.state == AudioRecord.STATE_INITIALIZED)
            if (NoiseSuppressor.isAvailable()) ns = NoiseSuppressor.create(ar.audioSessionId)?.also { it.enabled = true }
            if (AutomaticGainControl.isAvailable()) agc = AutomaticGainControl.create(ar.audioSessionId)?.also { it.enabled = true }
            val highPass = HighPass(rate)
            val buf = ByteArray(size)
            val end = SystemClock.elapsedRealtime() + maxMs
            ar.startRecording()
            FileOutputStream(pcm).use { out ->
                while (keepGoing() && SystemClock.elapsedRealtime() < end) {
                    val n = ar.read(buf, 0, buf.size)
                    if (n < 0) break
                    if (n > 0) {
                        highPass.apply(buf, n)
                        out.write(buf, 0, n)
                        if (onBuffer(buf, n)) break
                    }
                }
            }
        } finally {
            try {
                ar.stop()
            } catch (e: Exception) {
            }
            ns?.release()
            agc?.release()
            ar.release()
        }
    }
}

/** Wraps raw 16 kHz mono PCM16 in a 44-byte WAV header. */
fun pcmToWav(pcm: File, wav: File) {
    val rate = Voice.RATE
    val len = pcm.length().toInt()
    val h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
    h.put("RIFF".toByteArray()).putInt(36 + len).put("WAVE".toByteArray())
    h.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
    h.putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
    h.put("data".toByteArray()).putInt(len)
    FileOutputStream(wav).use { out ->
        out.write(h.array())
        pcm.inputStream().use { it.copyTo(out) }
    }
}
