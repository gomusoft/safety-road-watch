package watch.saferoad

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Records the voice note (mic only while recording), writes voice.wav, then transcribes it. */
class Voice(context: Context) {
    private val ctx = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    @Volatile
    var isRecording = false
        private set

    fun stop() {
        isRecording = false
    }

    /** onRecorded: mic released (main thread). onDone(transcript, note): after transcription (main thread). */
    @SuppressLint("MissingPermission")
    fun start(dir: File, onRecorded: () -> Unit, onDone: (String, String) -> Unit) {
        if (isRecording) return
        isRecording = true
        Thread {
            val pcm = File(dir, "voice.pcm")
            var ar: AudioRecord? = null
            try {
                val size = maxOf(
                    AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT),
                    4096,
                )
                ar = AudioRecord(
                    MediaRecorder.AudioSource.MIC, RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, size * 2,
                )
                check(ar.state == AudioRecord.STATE_INITIALIZED)
                val buf = ByteArray(size)
                val end = SystemClock.elapsedRealtime() + 12_000
                ar.startRecording()
                FileOutputStream(pcm).use { out ->
                    while (isRecording && SystemClock.elapsedRealtime() < end) {
                        val n = ar.read(buf, 0, buf.size)
                        if (n > 0) out.write(buf, 0, n) else if (n < 0) break
                    }
                }
            } catch (e: Exception) {
                // handled below: empty pcm
            } finally {
                try {
                    ar?.stop()
                } catch (e: Exception) {
                }
                ar?.release()
                isRecording = false
            }
            if (pcm.length() == 0L) {
                pcm.delete()
                main.post { onRecorded(); onDone("", "บันทึกเสียงไม่สำเร็จ") }
                return@Thread
            }
            try {
                writeWav(pcm, File(dir, "voice.wav"))
            } catch (e: Exception) {
            }
            main.post {
                onRecorded()
                transcribe(pcm, onDone)
            }
        }.start()
    }

    private fun writeWav(pcm: File, wav: File) {
        val len = pcm.length().toInt()
        val h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        h.put("RIFF".toByteArray()).putInt(36 + len).put("WAVE".toByteArray())
        h.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
        h.putInt(RATE).putInt(RATE * 2).putShort(2).putShort(16)
        h.put("data".toByteArray()).putInt(len)
        FileOutputStream(wav).use { out ->
            out.write(h.array())
            pcm.inputStream().use { it.copyTo(out) }
        }
    }

    private fun transcribe(pcm: File, onDone: (String, String) -> Unit) {
        if (Build.VERSION.SDK_INT < 33) {
            pcm.delete()
            onDone("", "เครื่องนี้แปลงเสียงจากไฟล์ไม่ได้ ฟังเสียงในคลัง")
            return
        }
        transcribe33(pcm, onDone)
    }

    @RequiresApi(33)
    private fun transcribe33(pcm: File, onDone: (String, String) -> Unit) {
        var finished = false
        var sr: SpeechRecognizer? = null
        var pfd: ParcelFileDescriptor? = null
        fun end(text: String, note: String) {
            if (finished) return
            finished = true
            try {
                sr?.destroy()
                pfd?.close()
            } catch (e: Exception) {
            }
            pcm.delete()
            onDone(text, note)
        }
        try {
            val rec = if (SpeechRecognizer.isOnDeviceRecognitionAvailable(ctx)) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(ctx)
            } else {
                SpeechRecognizer.createSpeechRecognizer(ctx)
            }
            sr = rec
            val fd = ParcelFileDescriptor.open(pcm, ParcelFileDescriptor.MODE_READ_ONLY)
            pfd = fd
            rec.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
                override fun onError(error: Int) =
                    end("", "แปลงเสียงไม่สำเร็จ (code $error) ฟังเสียงในคลังได้")

                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (text.isNullOrBlank()) end("", "แปลงเสียงไม่สำเร็จ (ไม่มีผลลัพธ์) ฟังเสียงในคลังได้") else end(text, "")
                }
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH")
                .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, fd)
                .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
                .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, RATE)
            rec.startListening(intent)
            main.postDelayed({ end("", "แปลงเสียงไม่สำเร็จ (หมดเวลา) ฟังเสียงในคลังได้") }, 25_000)
        } catch (e: Exception) {
            end("", "แปลงเสียงไม่สำเร็จ (${e.javaClass.simpleName}) ฟังเสียงในคลังได้")
        }
    }

    private companion object {
        const val RATE = 16000
    }
}
