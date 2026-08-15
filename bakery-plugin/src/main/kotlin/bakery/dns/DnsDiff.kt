package bakery.dns

/**
 * Pure reconciliation diff (EPIC BKY-DNS-3).
 *
 * Compares the desired records ([DnsConfig.records]) against the actual
 * zone ([ExistingDnsRecord]) and computes the create/update/delete plan.
 *
 * Semantics (never-duplicate, update-if-different):
 * - The addressing slot is (type, name). The apex may legitimately carry
 *   several A records with distinct values (GitHub Pages round-robin),
 *   so a slot is addressed by key and the *value set* decides the actions.
 * - A desired value already present in the zone is never re-created.
 * - An actual value with no desired counterpart is an orphan (delete).
 *
 * Pure DDD object — no I/O, no Gradle coupling, fully unit-testable.
 */
object DnsDiff {

    private data class Slot(val type: String, val name: String)

    /**
     * Returns the ordered plan of [DnsChange] actions. Order: all
     * creates/updates grouped by desired slot, then all deletes.
     */
    fun compute(desired: List<DnsRecord>, actual: List<ExistingDnsRecord>): List<DnsChange> {
        val desiredBySlot = desired.groupBy { Slot(it.type, it.name) }
        val actualBySlot = actual.groupBy { Slot(it.record.type, it.record.name) }
        val changes = mutableListOf<DnsChange>()

        for ((slot, desiredRecords) in desiredBySlot) {
            val actualRecords = actualBySlot[slot] ?: emptyList()
            for (d in desiredRecords) {
                val sameValue = actualRecords.firstOrNull { it.record.value == d.value }
                when {
                    sameValue == null -> changes += DnsChange.Create(d)
                    sameValue.record.ttl != d.ttl -> changes += DnsChange.Update(sameValue.id, d, sameValue.record)
                }
            }
        }

        for ((slot, actualRecords) in actualBySlot) {
            val desiredValues = desiredBySlot[slot]?.map { it.value }?.toSet() ?: emptySet()
            for (a in actualRecords) {
                if (a.record.value !in desiredValues) {
                    changes += DnsChange.Delete(a.id, a.record)
                }
            }
        }

        return changes
    }
}
