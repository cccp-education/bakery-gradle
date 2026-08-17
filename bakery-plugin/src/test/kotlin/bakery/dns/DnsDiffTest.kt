package bakery.dns

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-DNS-3 — Unit tests for the pure diff [DnsDiff].
 *
 * Reconciliation semantics (never-duplicate, update-if-different):
 * - Matching slot is (type, name) — the apex may carry several A records
 *   with distinct values (GitHub Pages round-robin), so a slot is
 *   addressed by key and the *value* set decides create/update/delete.
 * - A desired value already present in the actual zone is never
 *   re-created (never-duplicate).
 * - An actual value absent from the desired set is an orphan (delete).
 *
 * Methodology: DDD/TDD baby steps.
 */
class DnsDiffTest {

    private val a1 = DnsRecord("A", "@", "185.199.108.153", 3600)
    private val a2 = DnsRecord("A", "@", "185.199.109.153", 3600)
    private val cname = DnsRecord("CNAME", "www", "pages-content.github.io.", 3600)

    @Nested
    @DisplayName("noop")
    inner class Noop {
        @Test
        @DisplayName("empty desired and empty actual yield no changes")
        fun `empty both sides`() {
            assertThat(DnsDiff.compute(emptyList(), emptyList())).isEmpty()
        }

        @Test
        @DisplayName("identical records yield no changes")
        fun `identical records`() {
            val changes = DnsDiff.compute(listOf(a1), listOf(ExistingDnsRecord(1L, a1)))
            assertThat(changes).isEmpty()
        }

        @Test
        @DisplayName("full apex set with distinct values under the same key is a noop")
        fun `full apex set`() {
            val desired = listOf(a1, a2)
            val actual = listOf(ExistingDnsRecord(1L, a1), ExistingDnsRecord(2L, a2))
            assertThat(DnsDiff.compute(desired, actual)).isEmpty()
        }

        @Test
        @DisplayName("a desired value listed twice is never duplicated against the zone")
        fun `duplicate desired value not duplicated`() {
            val changes = DnsDiff.compute(listOf(a1, a1), listOf(ExistingDnsRecord(1L, a1)))
            assertThat(changes.filterIsInstance<DnsChange.Create>()).isEmpty()
            assertThat(changes).isEmpty()
        }
    }

    @Nested
    @DisplayName("create")
    inner class Create {
        @Test
        @DisplayName("a missing desired record yields a create")
        fun `missing desired yields create`() {
            val changes = DnsDiff.compute(listOf(cname), emptyList())
            assertThat(changes).hasSize(1)
            val change = changes[0] as DnsChange.Create
            assertThat(change.record).isEqualTo(cname)
        }

        @Test
        @DisplayName("apex records with different values under the same key do not collide")
        fun `apex distinct values do not collide`() {
            val changes = DnsDiff.compute(listOf(a1, a2), listOf(ExistingDnsRecord(1L, a1)))
            assertThat(changes).hasSize(1)
            val change = changes[0] as DnsChange.Create
            assertThat(change.record).isEqualTo(a2)
        }
    }

    @Nested
    @DisplayName("update")
    inner class Update {
        @Test
        @DisplayName("a ttl drift yields an update keeping the record id")
        fun `ttl drift yields update keeping id`() {
            val actual = listOf(ExistingDnsRecord(7L, DnsRecord("CNAME", "www", "pages-content.github.io.", 7200)))
            val changes = DnsDiff.compute(listOf(cname), actual)
            assertThat(changes).hasSize(1)
            val change = changes[0] as DnsChange.Update
            assertThat(change.id).isEqualTo(7L)
            assertThat(change.record).isEqualTo(cname)
            assertThat(change.previous.ttl).isEqualTo(7200)
        }

        @Test
        @DisplayName("a live ttl of 0 (zone default) is not a drift — BKY-DNS-6 dogfooding")
        fun `live ttl zero is zone default not a drift`() {
            val actual =
                listOf(
                    ExistingDnsRecord(1L, DnsRecord("A", "@", "185.199.108.153", 0)),
                    ExistingDnsRecord(2L, DnsRecord("A", "@", "185.199.109.153", 0)),
                    ExistingDnsRecord(3L, DnsRecord("A", "@", "185.199.110.153", 0)),
                    ExistingDnsRecord(4L, DnsRecord("A", "@", "185.199.111.153", 0)),
                    ExistingDnsRecord(5L, DnsRecord("CNAME", "www", "pages-content.github.io.", 60)),
                )
            val desired =
                listOf(
                    DnsRecord("A", "@", "185.199.108.153", 3600),
                    DnsRecord("A", "@", "185.199.109.153", 3600),
                    DnsRecord("A", "@", "185.199.110.153", 3600),
                    DnsRecord("A", "@", "185.199.111.153", 3600),
                    DnsRecord("CNAME", "www", "pages-content.github.io.", 60),
                )
            val changes = DnsDiff.compute(desired, actual)
            assertThat(changes).isEmpty()
        }
    }

    @Nested
    @DisplayName("delete")
    inner class Delete {
        @Test
        @DisplayName("a value drift in a desired slot yields a create for the new value and a delete for the old")
        fun `value drift yields create and delete`() {
            val actual = listOf(ExistingDnsRecord(1L, DnsRecord("A", "@", "203.0.113.1", 3600)))
            val changes = DnsDiff.compute(listOf(a1), actual)
            assertThat(changes.filterIsInstance<DnsChange.Create>()).hasSize(1)
            assertThat(changes.filterIsInstance<DnsChange.Delete>()).hasSize(1)
        }

        @Test
        @DisplayName("out-of-scope records (NS, MX, SPF, ftp CNAME) are never deleted — BKY-DNS-6 dogfooding")
        fun `out of scope records are ignored`() {
            val ns = ExistingDnsRecord(10L, DnsRecord("NS", "@", "dns109.ovh.net.", 0))
            val mx = ExistingDnsRecord(11L, DnsRecord("MX", "@", "1 mx1.mail.ovh.net.", 0))
            val spf = ExistingDnsRecord(12L, DnsRecord("SPF", "@", "v=spf1 include:mx.ovh.com -all", 0))
            val ftp = ExistingDnsRecord(13L, DnsRecord("CNAME", "ftp", "talaria.school.", 0))
            val desired = listOf(a1, a2, cname)
            val actual =
                listOf(
                    ExistingDnsRecord(1L, a1),
                    ExistingDnsRecord(2L, a2),
                    ExistingDnsRecord(3L, cname),
                    ns,
                    mx,
                    spf,
                    ftp,
                )
            val changes = DnsDiff.compute(desired, actual)
            assertThat(changes).isEmpty()
        }
    }

    @Nested
    @DisplayName("combined")
    inner class Combined {
        @Test
        @DisplayName("one compute returns create, update and delete at once")
        fun `combined create update delete`() {
            val desired = listOf(a1, a2, cname)
            val actual = listOf(
                ExistingDnsRecord(1L, a1),
                ExistingDnsRecord(2L, DnsRecord("CNAME", "www", "pages-content.github.io.", 7200)),
                ExistingDnsRecord(3L, DnsRecord("A", "@", "203.0.113.9", 3600)),
            )
            val changes = DnsDiff.compute(desired, actual)
            assertThat(changes.filterIsInstance<DnsChange.Create>()).hasSize(1)
            assertThat(changes.filterIsInstance<DnsChange.Update>()).hasSize(1)
            assertThat(changes.filterIsInstance<DnsChange.Delete>()).hasSize(1)
        }
    }
}
