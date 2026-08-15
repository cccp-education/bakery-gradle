package bakery.dns

/**
 * Degraded [DnsProvider] fallback (EPIC BKY-DNS-3).
 *
 * Pattern capsule `NoOpAudioPostProcessor`: used in CI and whenever no
 * real provider is configured (missing/unknown provider, no OVH
 * credentials). Reports an empty zone and every mutation is a harmless
 * no-op — combined with the dry-run default, provisioning is a
 * guaranteed no-op and the real zone is never touched.
 */
class NoOpDnsProvider : DnsProvider {

    override fun listRecords(domain: String): List<ExistingDnsRecord> = emptyList()

    override fun createRecord(domain: String, record: DnsRecord): Long = 0L

    override fun updateRecord(domain: String, id: Long, record: DnsRecord) = Unit

    override fun deleteRecord(domain: String, id: Long) = Unit

    override fun refreshZone(domain: String) = Unit

    override fun isAvailable(): Boolean = true

    override fun name(): String = "noop"
}
