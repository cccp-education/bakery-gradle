package bakery.dns

/**
 * OVH API interaction failure (EPIC BKY-DNS).
 *
 * Carries the HTTP status and body when known. Never contains credentials.
 */
class OvhDnsException(
    message: String,
    val status: Int? = null,
    val body: String? = null,
) : RuntimeException(message)
