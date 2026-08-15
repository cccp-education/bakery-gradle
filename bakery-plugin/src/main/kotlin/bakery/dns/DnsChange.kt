package bakery.dns

/**
 * A single reconciliation action (EPIC BKY-DNS-3).
 *
 * Produced by [DnsDiff.compute] and executed by [DnsProvisioner]:
 * - [Create] — a desired record is missing from the zone.
 * - [Update] — a desired value exists but drifts (ttl); keeps the id.
 * - [Delete] — an actual record is an orphan (no desired counterpart).
 *   Only applied when the provisioner receives the purge flag.
 */
sealed interface DnsChange {
    val record: DnsRecord

    /** The desired record is missing from the zone — create it. */
    data class Create(override val record: DnsRecord) : DnsChange

    /** An existing record must be updated to match the desired one. */
    data class Update(
        val id: Long,
        override val record: DnsRecord,
        val previous: DnsRecord,
    ) : DnsChange

    /** An orphan record should be removed (requires the purge flag). */
    data class Delete(
        val id: Long,
        override val record: DnsRecord,
    ) : DnsChange
}
