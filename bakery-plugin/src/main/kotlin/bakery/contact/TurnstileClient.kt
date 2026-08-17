package bakery.contact

/**
 * Turnstile verification request and result (EPIC BKY-CONTACT-SEC).
 *
 * Pure DDD domain type — the actual HTTP call to Cloudflare is isolated in
 * a port (the [TurnstileVerifier] object acts as a stub/decision gate here;
 * the real adapter lives outside the domain).
 */

data class TurnstileVerifyRequest(
    val secret: String,
    val response: String,
    val remoteIp: String? = null,
) {
    fun siteverifyUrl(): String = "https://challenges.cloudflare.com/turnstile/v0/siteverify"
}

sealed interface VerifyResult {
    val success: Boolean

    data object Success : VerifyResult {
        override val success: Boolean = true
    }

    data class Failure(val code: String) : VerifyResult {
        override val success: Boolean = false
    }
}

/**
 * Stub verifier — validates that secret and response are non-empty.
 *
 * The real HTTP adapter (UrlFetchApp in Apps Script) lives outside this domain.
 * This object provides the pure decision logic testable without network.
 */
object TurnstileVerifier {
    fun verify(request: TurnstileVerifyRequest): VerifyResult {
        if (request.secret.isBlank()) return VerifyResult.Failure("missing-secret")
        if (request.response.isBlank()) return VerifyResult.Failure("missing-response")
        return VerifyResult.Success
    }
}