package watch.saferoad

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class HighPassTest {
    private val rate = 16000

    /** 16-bit PCM of a sine of [hz] and [amp] lasting one second, run through the filter; returns the RMS of the second half. */
    private fun levelAfterFilter(hz: Double, amp: Double): Double {
        val pcm = ByteArray(rate * 2)
        for (i in 0 until rate) {
            val v = (amp * sin(2 * PI * hz * i / rate)).toInt()
            pcm[2 * i] = v.toByte()
            pcm[2 * i + 1] = (v shr 8).toByte()
        }
        HighPass(rate).apply(pcm, pcm.size)
        return rms(pcm.copyOfRange(rate, pcm.size), rate)
    }

    @Test
    fun cutsEngineRumble() {
        val before = 8000 / Math.sqrt(2.0)
        assertTrue(levelAfterFilter(30.0, 8000.0) < before * 0.25)
    }

    @Test
    fun keepsSpeech() {
        val before = 8000 / Math.sqrt(2.0)
        assertTrue(levelAfterFilter(1000.0, 8000.0) > before * 0.95)
    }

    @Test
    fun removesConstantOffset() {
        val pcm = ByteArray(rate * 2)
        for (i in 0 until rate) {
            pcm[2 * i] = 1000.toByte()
            pcm[2 * i + 1] = (1000 shr 8).toByte()
        }
        HighPass(rate).apply(pcm, pcm.size)
        assertTrue(rms(pcm.copyOfRange(rate, pcm.size), rate) < 20.0)
    }
}
