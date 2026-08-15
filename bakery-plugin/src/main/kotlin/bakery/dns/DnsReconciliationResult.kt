package bakery.dns

/**
 * Outcome of a reconciliation run (EPIC BKY-DNS-3).
 *
 * - [plan] — every change computed by [DnsDiff] against the zone.
 * - [applied] — changes actually executed (empty on dry run).
 * - [skipped] — planned changes not executed (dry run, or orphans
 *   blocked because the purge flag was not set).
 * - [noop] — true when the zone already matches the desired state
 *   (no mutation, no refresh).
 */
data class DnsReconciliationResult(
    val plan: List<DnsChange>,
    val applied: List<DnsChange>,
    val skipped: List<DnsChange>,
    val noop: Boolean,
)
