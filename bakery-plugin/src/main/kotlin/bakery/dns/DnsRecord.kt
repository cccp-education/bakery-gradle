package bakery.dns

/**
 * A single desired DNS record (EPIC BKY-DNS).
 *
 * Pure DDD domain type. Apex records use the `@` name; TTL defaults to
 * 3600 (1 hour) for GitHub Pages-style records.
 */
data class DnsRecord(
    /** Record type, e.g. `A`, `CNAME`, `TXT`. */
    val type: String = "",
    /** Record name, `@` for the apex (zone root). */
    val name: String = "",
    /** Record value, e.g. `185.199.108.153` or `pages-content.github.io.`. */
    val value: String = "",
    /** Time to live in seconds. */
    val ttl: Int = 3600,
)