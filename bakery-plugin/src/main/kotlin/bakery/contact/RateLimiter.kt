package bakery.contact

/**
 * Rate limiter — pure decision logic (EPIC BKY-CONTACT-SEC).
 *
 * Tracks per-key counters with sliding windows (perHour, perDay) and a
 * global daily cap. Storage is in-memory (mutable Map) — the real adapter
 * (Apps Script CacheService) lives outside the domain.
 */
class RateLimiter(
    private val perHour: Int,
    private val perDay: Int,
    private val globalCap: Int,
) {
    private data class Counter(var hourCount: Int, var dayCount: Int, var firstSeen: Long)

    private val counters = mutableMapOf<String, Counter>()
    private var globalDayCount = 0
    private var globalFirstSeen = 0L

    fun allow(key: String, now: Long): Boolean {
        val c = counters.getOrPut(key) { Counter(0, 0, now) }
        if (now - c.firstSeen >= HOUR_MS) c.hourCount = 0
        if (now - c.firstSeen >= DAY_MS) {
            c.dayCount = 0
            c.firstSeen = now
        }
        if (globalFirstSeen == 0L) globalFirstSeen = now
        if (now - globalFirstSeen >= DAY_MS) {
            globalDayCount = 0
            globalFirstSeen = now
        }
        if (c.hourCount >= perHour) return false
        if (c.dayCount >= perDay) return false
        if (globalDayCount >= globalCap) return false
        c.hourCount++
        c.dayCount++
        globalDayCount++
        return true
    }

    fun reset(key: String) {
        counters.remove(key)
    }

    companion object {
        private const val HOUR_MS = 3600000L
        private const val DAY_MS = 86400000L
    }
}