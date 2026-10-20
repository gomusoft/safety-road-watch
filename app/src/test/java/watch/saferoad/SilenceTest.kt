package watch.saferoad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SilenceTest {
    /** Feeds [levels] as 100 ms frames starting at [from]; returns the time it first said "stop", or null. */
    private fun run(d: SilenceDetector, from: Long, vararg levels: Pair<Double, Int>): Long? {
        var t = from
        for ((level, frames) in levels) repeat(frames) {
            t += 100
            if (d.update(level, t)) return t
        }
        return null
    }

    @Test
    fun stopsEightSecondsAfterSpeech() {
        val t = run(SilenceDetector(), 0, 200.0 to 5, 3000.0 to 30, 200.0 to 200)
        assertEquals(11_500L, t) // last speech frame at 3500 ms + 8 s
    }

    @Test
    fun keepsGoingWhenSpeechResumesBeforeEightSeconds() {
        val t = run(SilenceDetector(), 0, 200.0 to 5, 3000.0 to 20, 200.0 to 70, 3000.0 to 20, 200.0 to 70)
        assertEquals(null, t)
    }

    @Test
    fun noSpeechAtAllEndsAfterEightSeconds() {
        assertEquals(8100L, run(SilenceDetector(), 0, 200.0 to 200))
    }

    @Test
    fun steadyLoudNoiseIsNotSpeech() {
        val t = run(SilenceDetector(), 0, 1500.0 to 40, 6000.0 to 20, 1500.0 to 200)
        assertTrue(t != null)
    }

    @Test
    fun rmsOfPcm() {
        val buf = byteArrayOf(0x10, 0x00, 0xF0.toByte(), 0xFF.toByte()) // +16, -16
        assertEquals(16.0, rms(buf, 4), 0.001)
        assertFalse(rms(ByteArray(0), 0) > 0)
    }
}
