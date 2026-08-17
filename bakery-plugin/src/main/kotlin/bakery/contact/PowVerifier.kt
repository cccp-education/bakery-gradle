package bakery.contact

import java.security.MessageDigest

/**
 * Proof-of-Work verifier (EPIC BKY-CONTACT-SEC).
 *
 * Verifies that `sha256(challenge + nonce)` starts with N zero chars.
 * Difficulty 0 = no PoW required (any nonce accepted).
 */
object PowVerifier {
    fun verify(challenge: String, nonce: String, difficulty: Int): Boolean {
        if (difficulty == 0) return true
        val hash = sha256(challenge + nonce)
        return hash.startsWith("0".repeat(difficulty))
    }

    fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Content heuristics for spam detection (EPIC BKY-CONTACT-SEC).
 *
 * Pure functions — no I/O, no external services.
 */
object ContentHeuristics {
    private val URL_REGEX = Regex("https?://[^\\s]+")

    private val DISPOSABLE_DOMAINS =
        setOf(
            "mailinator.com",
            "guerrillamail.com",
            "10minutemail.com",
            "tempmail.com",
            "throwaway.email",
            "yopmail.com",
        )

    fun countUrls(text: String): Int = URL_REGEX.findAll(text).count()

    fun isDuplicateSubject(subject: String, message: String): Boolean = subject == message

    fun isDisposableDomain(email: String): Boolean {
        val domain = email.substringAfter("@", "").lowercase()
        return DISPOSABLE_DOMAINS.contains(domain)
    }

    fun isGibberish(text: String): Boolean {
        if (text.isBlank()) return false
        val nonLatin = text.count { it.code > 0x2E7F }
        val ratio = nonLatin.toDouble() / text.length
        return ratio > 0.5 && !text.contains(" ")
    }
}