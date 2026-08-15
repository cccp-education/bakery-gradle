package bakery.dns

import java.security.MessageDigest

/**
 * OVH API request signature (EPIC BKY-DNS).
 *
 * Pure DDD object — no I/O. Computes the OVH HMAC-style SHA-1 signature
 * over `applicationSecret + "+" + consumerKey + "+" + method + "+" + url
 * + "+" + body + "+" + timestamp`, prefixed with `$1$` (mirrors the
 * reference `ovh.py` script).
 */
object OvhSignature {

    /**
     * Computes the OVH request signature for the given call parameters.
     *
     * @param applicationSecret OVH application secret.
     * @param consumerKey OVH consumer key.
     * @param method HTTP method (e.g. `GET`, `POST`, `PUT`, `DELETE`).
     * @param url the full request URL (base + path + query).
     * @param body the raw request body (empty string when absent).
     * @param timestamp the request epoch-second timestamp.
     * @return `$1$` + lowercase hex SHA-1 of the signed payload.
     */
    fun sign(
        applicationSecret: String,
        consumerKey: String,
        method: String,
        url: String,
        body: String,
        timestamp: String,
    ): String {
        val toSign = "$applicationSecret+$consumerKey+$method+$url+$body+$timestamp"
        val digest = MessageDigest.getInstance("SHA-1").digest(toSign.toByteArray(Charsets.UTF_8))
        return "\$1\$" + digest.toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xFF) }
}
