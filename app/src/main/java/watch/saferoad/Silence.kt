package watch.saferoad

/**
 * Decides when a voice note is over: [silenceMs] without speech. Speech is a level clearly above the quietest
 * recent level (engine and road noise count as the floor, which creeps up slowly if the car gets louder).
 * Silence is counted from the first frame, so a press with no speech at all also ends after [silenceMs].
 */
class SilenceDetector(private val silenceMs: Long = 8_000, private val minLevel: Double = 500.0) {
    private var floor = Double.MAX_VALUE
    private var lastVoice = -1L
    private var lastNow = 0L

    /** [level] is the RMS of one audio frame (0..32768), [now] a monotonic time in ms. True when the note should end. */
    fun update(level: Double, now: Long): Boolean {
        if (lastVoice < 0) lastVoice = now
        floor = if (floor == Double.MAX_VALUE) level else minOf(level, floor * (1.0 + 0.0001 * (now - lastNow)))
        lastNow = now
        if (level > maxOf(floor * 2.5, minLevel)) lastVoice = now
        return now - lastVoice >= silenceMs
    }
}

/** RMS of 16-bit little-endian mono PCM in [buf] (first [n] bytes). */
fun rms(buf: ByteArray, n: Int): Double {
    val samples = n / 2
    if (samples == 0) return 0.0
    var sum = 0.0
    for (i in 0 until samples) {
        val s = (buf[2 * i].toInt() and 0xFF) or (buf[2 * i + 1].toInt() shl 8)
        sum += s.toDouble() * s
    }
    return Math.sqrt(sum / samples)
}
