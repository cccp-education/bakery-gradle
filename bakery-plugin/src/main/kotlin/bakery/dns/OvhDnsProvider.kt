package bakery.dns

/**
 * OVH [DnsProvider] adapter (EPIC BKY-DNS-3).
 *
 * Thin delegation over [OvhDnsClient] (BKY-DNS-2): lists the zone as
 * [ExistingDnsRecord] domain objects (id + mapped [DnsRecord]) and
 * forwards every mutation. Zero logic here — the diff/reconcile lives in
 * [DnsDiff]/[DnsProvisioner].
 */
class OvhDnsProvider(
    private val client: OvhDnsClient,
) : DnsProvider {

    override fun listRecords(domain: String): List<ExistingDnsRecord> =
        client.listRecordIds(domain).map { id -> ExistingDnsRecord(id, client.getRecord(domain, id)) }

    override fun createRecord(domain: String, record: DnsRecord): Long =
        client.createRecord(domain, record)

    override fun updateRecord(domain: String, id: Long, record: DnsRecord) {
        client.updateRecord(domain, id, record)
    }

    override fun deleteRecord(domain: String, id: Long) {
        client.deleteRecord(domain, id)
    }

    override fun refreshZone(domain: String) {
        client.refreshZone(domain)
    }

    override fun isAvailable(): Boolean = true

    override fun name(): String = "ovh"
}
