package watch.saferoad

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
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

/**
 * Records the voice note (mic only while recording), writes voice.wav, then transcribes it.
 * The note ends when the user presses again, after 8 s without speech, or after 45 s.
 */
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
            try {
                val silence = SilenceDetector()
                Mic.record(pcm, MAX_MS, { isRecording }) { buf, n -> silence.update(rms(buf, n), SystemClock.elapsedRealtime()) }
            } catch (e: Exception) {
                // handled below: empty pcm
            } finally {
                isRecording = false
            }
            if (pcm.length() == 0L) {
                pcm.delete()
                main.post { onRecorded(); onDone("", "บันทึกเสียงไม่สำเร็จ") }
                return@Thread
            }
            try {
                pcmToWav(pcm, File(dir, "voice.wav"))
            } catch (e: Exception) {
            }
            main.post {
                onRecorded()
                enqueue(pcm, onDone)
            }
        }.start()
    }

    // One recogniser at a time: a new note can be recorded while an earlier one is still being transcribed.
    private val jobs = ArrayDeque<() -> Unit>()
    private var transcribing = false

    private fun enqueue(pcm: File, onDone: (String, String) -> Unit) {
        jobs.addLast { transcribe(pcm) { text, note -> onDone(text, note); next() } }
        if (!transcribing) next()
    }

    private fun next() {
        val job = jobs.removeFirstOrNull()
        transcribing = job != null
        job?.invoke()
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
        const val MAX_MS = 45_000L
    }
}
