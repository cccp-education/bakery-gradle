package bakery.dns

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * OVH DNS record wire representation (EPIC BKY-DNS).
 *
 * Pure DDD DTO mapping [DnsRecord] (domain type: type/name/value/ttl,
 * apex `@`) to the OVH API payload (fieldType/subDomain/target/ttl) and
 * back. An empty [subDomain] means the apex (`@`).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class OvhRecord(
    /** Record type, e.g. `A`, `CNAME`, `TXT`. */
    val fieldType: String = "",
    /** Record name; empty string means the apex (`@`). */
    val subDomain: String = "",
    /** Record value, e.g. `185.199.108.153` or `pages-content.github.io.`. */
    val target: String = "",
    /** Time to live in seconds. */
    val ttl: Int = 3600,
    /** Record id, present in API responses, never sent in request bodies. */
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val id: Long? = null,
) {
    /** Maps back to the domain [DnsRecord] (apex `@` for empty subDomain). */
    fun toDnsRecord(): DnsRecord =
        DnsRecord(
            type = fieldType,
            name = if (subDomain.isBlank()) "@" else subDomain,
            value = target,
            ttl = ttl,
        )

    companion object {
        /** Maps a domain [DnsRecord] to the OVH wire representation. */
        fun fromDnsRecord(record: DnsRecord): OvhRecord =
            OvhRecord(
                fieldType = record.type,
                subDomain = if (record.name == "@") "" else record.name,
                target = record.value,
                ttl = record.ttl,
            )
    }
}
