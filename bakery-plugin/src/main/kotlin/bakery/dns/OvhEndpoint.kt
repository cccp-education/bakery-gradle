package bakery.dns

/**
 * OVH API endpoint path builders (EPIC BKY-DNS).
 *
 * Pure DDD object — no I/O. Base URL: EU API v1
 * (`https://eu.api.ovh.com/1.0`).
 */
object OvhEndpoint {

    const val BASE_URL: String = "https://eu.api.ovh.com/1.0"

    /** Zone record collection path, e.g. `/domain/zone/{domain}/record`. */
    fun recordListPath(domain: String): String = "/domain/zone/$domain/record"

    /** Single record path, e.g. `/domain/zone/{domain}/record/{id}`. */
    fun recordPath(domain: String, id: Long): String = "/domain/zone/$domain/record/$id"

    /** Zone refresh path, e.g. `/domain/zone/{domain}/refresh`. */
    fun refreshPath(domain: String): String = "/domain/zone/$domain/refresh"
}
