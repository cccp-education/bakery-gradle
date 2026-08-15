package bakery.dns

/**
 * A record currently present in the live zone (EPIC BKY-DNS-3).
 *
 * Pairs the OVH record id (needed to update/delete) with the domain
 * [DnsRecord] it maps to. Returned by [DnsProvider.listRecords].
 */
data class ExistingDnsRecord(
    val id: Long,
    val record: DnsRecord,
)
