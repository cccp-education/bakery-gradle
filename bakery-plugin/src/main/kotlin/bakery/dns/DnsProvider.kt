package bakery.dns

/**
 * DNS zone mutation port (EPIC BKY-DNS-3).
 *
 * Pattern capsule `AudioPostProcessor` port: the reconciler
 * ([DnsProvisioner]) depends on this interface, not on a concrete
 * provider. Low-level operations so the pure diff/reconcile logic stays
 * outside the adapter.
 *
 * Implementations: [OvhDnsProvider] (real API via [OvhDnsClient]) and
 * [NoOpDnsProvider] (degraded/CI fallback). Resolved via
 * [DnsProviderFactory].
 */
interface DnsProvider {

    /** Lists the records currently present in the zone. */
    fun listRecords(domain: String): List<ExistingDnsRecord>

    /** Creates a record and returns its id. */
    fun createRecord(domain: String, record: DnsRecord): Long

    /** Updates an existing record (by id) to the desired one. */
    fun updateRecord(domain: String, id: Long, record: DnsRecord)

    /** Deletes an existing record (by id). */
    fun deleteRecord(domain: String, id: Long)

    /** Triggers a zone refresh so pending changes propagate. */
    fun refreshZone(domain: String)

    /** Returns true when the provider is usable. */
    fun isAvailable(): Boolean

    /** Provider handle for logging, e.g. `ovh` or `noop`. */
    fun name(): String
}
