package bakery.dns

/**
 * OVH HTTP transport port (EPIC BKY-DNS).
 *
 * Injectable so tests use a fake and the domain stays free of real I/O.
 * [JavaOvhHttp] is the production adapter (java.net.http + signature).
 */
interface OvhHttp {
    fun call(method: String, url: String, body: String?): OvhHttpResponse
}

/** Raw HTTP response — status code + body. */
data class OvhHttpResponse(val status: Int, val body: String)
