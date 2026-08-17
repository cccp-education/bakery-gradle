package bakery.dns

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-DNS-3 — Unit tests for [DnsProvisioner] reconciliation.
 *
 * Idempotence contract (backlog note "delete must require a double flag"):
 * - dry-run is the default — without it nothing is ever applied.
 * - orphans are only deleted when [DnsProvisioner.reconcile]'s
 *   [allowDelete] is set (purge flag); otherwise they are skipped.
 * - a zone that already matches the desired state is a noop (no
 *   mutation, no refresh).
 * - a second reconcile after an apply is a noop (idempotence).
 *
 * Methodology: DDD/TDD baby steps.
 */
class DnsProvisionerTest {

    private val a1 = DnsRecord("A", "@", "185.199.108.153", 3600)
    private val cname = DnsRecord("CNAME", "www", "pages-content.github.io.", 3600)

    private fun provisioner(provider: DnsProvider, domain: String = "talaria.school") =
        DnsProvisioner(provider, domain)

    @Nested
    @DisplayName("noop")
    inner class Noop {
        @Test
        @DisplayName("noop when the zone already matches the desired state")
        fun `noop when identical`() {
            val provider = RecordingProvider(listOf(ExistingDnsRecord(1L, a1), ExistingDnsRecord(2L, cname)))
            val result = provisioner(provider).reconcile(listOf(a1, cname))
            assertThat(result.noop).isTrue()
            assertThat(result.plan).isEmpty()
            assertThat(provider.calls).containsExactly("list")
        }

        @Test
        @DisplayName("second reconcile after an apply is a noop (idempotence)")
        fun `second reconcile is a noop`() {
            val provider = RecordingProvider()
            val p = provisioner(provider)
            val first = p.reconcile(listOf(a1, cname), dryRun = false)
            assertThat(first.applied).hasSize(2)
            val second = p.reconcile(listOf(a1, cname), dryRun = false)
            assertThat(second.noop).isTrue()
            assertThat(second.plan).isEmpty()
            assertThat(second.applied).isEmpty()
        }
    }

    @Nested
    @DisplayName("dry run")
    inner class DryRun {
        @Test
        @DisplayName("dry run is the default and never mutates")
        fun `dry run is the default`() {
            val provider = RecordingProvider()
            val result = provisioner(provider).reconcile(listOf(cname))
            assertThat(result.noop).isFalse()
            assertThat(result.applied).isEmpty()
            assertThat(result.skipped).hasSize(1)
            assertThat(provider.calls).containsExactly("list")
        }

        @Test
        @DisplayName("dry run reports would-be deletes as skipped even with the purge flag")
        fun `dry run blocks deletes even with purge`() {
            val provider =
                RecordingProvider(
                    listOf(
                        ExistingDnsRecord(1L, a1),
                        ExistingDnsRecord(2L, DnsRecord("A", "@", "203.0.113.9", 3600)),
                    ),
                )
            val result = provisioner(provider).reconcile(listOf(a1), dryRun = true, allowDelete = true)
            assertThat(result.applied).isEmpty()
            assertThat(result.skipped.filterIsInstance<DnsChange.Delete>()).hasSize(1)
            assertThat(provider.calls).containsExactly("list")
        }
    }

    @Nested
    @DisplayName("apply")
    inner class Apply {
        @Test
        @DisplayName("apply creates a missing record and refreshes the zone")
        fun `apply creates missing record`() {
            val provider = RecordingProvider()
            val result = provisioner(provider).reconcile(listOf(cname), dryRun = false)
            assertThat(result.applied).hasSize(1)
            assertThat(result.skipped).isEmpty()
            assertThat(provider.calls).contains("create:CNAME:www:pages-content.github.io.")
            assertThat(provider.calls).contains("refresh")
        }

        @Test
        @DisplayName("apply updates a drifted record keeping its id")
        fun `apply updates drifted record keeping id`() {
            val provider = RecordingProvider(listOf(ExistingDnsRecord(5L, DnsRecord("CNAME", "www", "pages-content.github.io.", 7200))))
            val result = provisioner(provider).reconcile(listOf(cname), dryRun = false)
            assertThat(result.applied).hasSize(1)
            val update = result.applied[0] as DnsChange.Update
            assertThat(update.id).isEqualTo(5L)
            assertThat(provider.calls).contains("update:5:pages-content.github.io.")
        }

        @Test
        @DisplayName("apply never refreshes when nothing was applied")
        fun `no refresh when nothing applied`() {
            val provider = RecordingProvider(
                listOf(
                    ExistingDnsRecord(1L, a1),
                    ExistingDnsRecord(2L, DnsRecord("A", "@", "203.0.113.9", 3600)),
                ),
            )
            val result = provisioner(provider).reconcile(listOf(a1), dryRun = false, allowDelete = false)
            assertThat(result.applied).isEmpty()
            assertThat(result.skipped.filterIsInstance<DnsChange.Delete>()).hasSize(1)
            assertThat(provider.calls).containsExactly("list")
        }
    }

    @Nested
    @DisplayName("purge")
    inner class Purge {
        @Test
        @DisplayName("an orphan in a desired slot is skipped without the purge flag")
        fun `orphan skipped without purge`() {
            val provider = RecordingProvider(
                listOf(
                    ExistingDnsRecord(1L, a1),
                    ExistingDnsRecord(2L, DnsRecord("A", "@", "203.0.113.9", 3600)),
                ),
            )
            val result = provisioner(provider).reconcile(listOf(a1), dryRun = false, allowDelete = false)
            assertThat(result.applied).isEmpty()
            assertThat(result.skipped.filterIsInstance<DnsChange.Delete>()).hasSize(1)
            assertThat(provider.calls).doesNotContain("delete:2")
        }

        @Test
        @DisplayName("an orphan in a desired slot is deleted and the zone refreshed with the purge flag")
        fun `orphan deleted with purge`() {
            val provider = RecordingProvider(
                listOf(
                    ExistingDnsRecord(1L, a1),
                    ExistingDnsRecord(2L, DnsRecord("A", "@", "203.0.113.9", 3600)),
                ),
            )
            val result = provisioner(provider).reconcile(listOf(a1), dryRun = false, allowDelete = true)
            assertThat(result.applied.filterIsInstance<DnsChange.Delete>()).hasSize(1)
            assertThat(provider.calls).contains("delete:2")
            assertThat(provider.calls).contains("refresh")
        }
    }

    /**
     * In-memory provider that records every call and keeps a mutable zone,
     * so reconciliation can be replayed to prove idempotence.
     */
    private class RecordingProvider(initial: List<ExistingDnsRecord> = emptyList()) : DnsProvider {
        val calls = mutableListOf<String>()
        var current = initial.toMutableList()
        private var nextId = 1000L

        override fun listRecords(domain: String): List<ExistingDnsRecord> {
            calls += "list"
            return current.toList()
        }

        override fun createRecord(domain: String, record: DnsRecord): Long {
            calls += "create:${record.type}:${record.name}:${record.value}"
            val id = nextId++
            current += ExistingDnsRecord(id, record)
            return id
        }

        override fun updateRecord(domain: String, id: Long, record: DnsRecord) {
            calls += "update:$id:${record.value}"
            current = current.map { if (it.id == id) ExistingDnsRecord(id, record) else it }.toMutableList()
        }

        override fun deleteRecord(domain: String, id: Long) {
            calls += "delete:$id"
            current = current.filterNot { it.id == id }.toMutableList()
        }

        override fun refreshZone(domain: String) {
            calls += "refresh"
        }

        override fun isAvailable(): Boolean = true

        override fun name(): String = "recording"
    }
}
