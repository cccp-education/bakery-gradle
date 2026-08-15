package bakery.dns

/**
 * Idempotent DNS reconciler (EPIC BKY-DNS-3).
 *
 * Compares the desired records against the live zone through a
 * [DnsProvider] and applies create-if-missing, update-if-different,
 * delete-if-orphan.
 *
 * Safety (backlog note "dry-run non négociable" + "delete must require a
 * double flag"):
 * - [dryRun] defaults to true — without it nothing is ever applied.
 * - orphans are only deleted when [allowDelete] (purge flag) is set,
 *   otherwise they are reported as skipped.
 * - a zone that already matches the desired state is a noop: only the
 *   listing happens, no mutation and no refresh (economy of ink).
 *
 * Pure domain logic — the [DnsProvider] injects all I/O.
 */
class DnsProvisioner(
    private val provider: DnsProvider,
    private val domain: String,
) {

    fun reconcile(
        desired: List<DnsRecord>,
        dryRun: Boolean = true,
        allowDelete: Boolean = false,
    ): DnsReconciliationResult {
        val plan = DnsDiff.compute(desired, provider.listRecords(domain))
        val applied = mutableListOf<DnsChange>()
        val skipped = mutableListOf<DnsChange>()

        if (dryRun) {
            skipped += plan
        } else {
            for (change in plan) {
                when (change) {
                    is DnsChange.Create -> {
                        provider.createRecord(domain, change.record)
                        applied += change
                    }
                    is DnsChange.Update -> {
                        provider.updateRecord(domain, change.id, change.record)
                        applied += change
                    }
                    is DnsChange.Delete ->
                        if (allowDelete) {
                            provider.deleteRecord(domain, change.id)
                            applied += change
                        } else {
                            skipped += change
                        }
                }
            }
            if (applied.isNotEmpty()) {
                provider.refreshZone(domain)
            }
        }

        return DnsReconciliationResult(
            plan = plan,
            applied = applied,
            skipped = skipped,
            noop = plan.isEmpty(),
        )
    }
}
