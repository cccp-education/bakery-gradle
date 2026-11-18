package bakery.netlify

/**
 * HTTP transport port for the Netlify API (US-4).
 *
 * Injectable so the domain and the client stay testable against an in-memory
 * fake; [JavaNetlifyHttp] is the production adapter (`java.net.http`).
 */
interface NetlifyHttp {
    fun postZip(
        url: String,
        token: String,
        zip: ByteArray,
    ): NetlifyHttpResponse

    fun get(
        url: String,
        token: String,
    ): NetlifyHttpResponse
}

/** Raw response — status + body, no Netlify semantics. */
data class NetlifyHttpResponse(
    val status: Int,
    val body: String,
)
