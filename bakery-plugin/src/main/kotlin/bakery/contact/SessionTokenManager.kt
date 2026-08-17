package bakery.contact

import java.security.SecureRandom

/**
 * Single-use session token (EPIC BKY-CONTACT-SEC).
 *
 * Issued with a TTL (default 10 min), validated against expiry, and
 * consumed exactly once (anti-replay). Storage is in-memory — the real
 * adapter (Apps Script CacheService) lives outside the domain.
 */
data class SessionToken(val value: String, val issuedAt: Long)

class SessionTokenManager(private val ttlMs: Long = 600000L) {
    private val issued = mutableSetOf<String>()
    private val consumed = mutableSetOf<String>()
    private val random = SecureRandom()

    fun issue(now: Long): SessionToken {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        val value = bytes.joinToString("") { "%02x".format(it) }
        issued.add(value)
        return SessionToken(value, issuedAt = now)
    }

    fun validate(token: SessionToken, now: Long): Boolean {
        if (token.value.isBlank()) return false
        if (!issued.contains(token.value)) return false
        if (consumed.contains(token.value)) return false
        if (now - token.issuedAt > ttlMs) return false
        return true
    }

    fun consume(token: SessionToken): Boolean {
        if (!validate(token, now = token.issuedAt + 1)) return false
        consumed.add(token.value)
        return true
    }
}