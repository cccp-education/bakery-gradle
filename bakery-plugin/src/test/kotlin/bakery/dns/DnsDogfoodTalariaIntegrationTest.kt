package bakery.dns

import bakery.FileSystemManager.yamlMapper
import bakery.SiteConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * BKY-DNS-6 — Dogfooding réel talaria.school.
 *
 * End-to-end reconciliation against the live OVH zone of talaria.school,
 * proving the desired state declared in site.yml already matches the zone
 * (noop) and that the idempotence contract holds on production data.
 *
 * Safety: dry-run only — no mutation ever reaches the live zone. The test
 * is skipped (assumeTrue) when:
 *  - the real site.yml is absent (CI, fresh checkout)
 *  - the `dns:` section is missing
 *  - OVH credentials are incomplete (no secret to leak)
 *
 * Credentials are read from site.yml (gitignored, never printed). The
 * [JavaOvhHttp] transport signs every request but never logs the secret.
 */
class DnsDogfoodTalariaIntegrationTest {
    private val talariaSiteYml =
        File("/home/cheroliv/workspace/office/sites/talaria/site.yml")

    @Test
    fun `reconcile talaria_school zone is a dry-run no-op`() {
        assumeTrue(talariaSiteYml.isFile) {
            "talaria site.yml not found — skip dogfooding (CI/fresh checkout)"
        }

        val site = yamlMapper.readValue(talariaSiteYml, SiteConfiguration::class.java)
        val dns =
            site.dns
                ?: return assumeTrue(false) { "talaria site.yml has no dns: section — skip" }

        assumeTrue(dns.credentials.isComplete()) {
            "OVH credentials incomplete in site.yml — skip dogfooding"
        }

        val client = OvhDnsClient(dns.credentials, JavaOvhHttp(dns.credentials))
        val provider = OvhDnsProvider(client)
        val provisioner = DnsProvisioner(provider, dns.domain)

        val liveRecords = provider.listRecords(dns.domain)
        println("[dogfood] live zone records (${liveRecords.size}):")
        liveRecords.forEach { existing ->
            val r = existing.record
            println("[dogfood]   id=${existing.id} ${r.type} ${r.name} -> ${r.value} (ttl ${r.ttl})")
        }

        val firstRun =
            provisioner.reconcile(
                desired = dns.records,
                dryRun = true,
                allowDelete = false,
            )

        println("[dogfood] desired records (${dns.records.size}):")
        dns.records.forEach { r ->
            println("[dogfood]   ${r.type} ${r.name} -> ${r.value} (ttl ${r.ttl})")
        }
        println("[dogfood] reconcile plan (${firstRun.plan.size}):")
        firstRun.plan.forEach { change ->
            val action =
                when (change) {
                    is DnsChange.Create -> "create"
                    is DnsChange.Update -> "update"
                    is DnsChange.Delete -> "delete"
                }
            val r = change.record
            println("[dogfood]   $action ${r.type} ${r.name} -> ${r.value} (ttl ${r.ttl})")
        }
        println("[dogfood] noop=${firstRun.noop} applied=${firstRun.applied.size} skipped=${firstRun.skipped.size}")

        assertThat(firstRun.noop)
            .`as`("zone talaria.school must already match the desired state — 4 A + 1 CNAME www")
            .isTrue()
        assertThat(firstRun.plan)
            .`as`("reconciliation plan must be empty (zone already correct)")
            .isEmpty()
        assertThat(firstRun.applied)
            .`as`("dry-run — nothing applied")
            .isEmpty()

        val secondRun =
            provisioner.reconcile(
                desired = dns.records,
                dryRun = true,
                allowDelete = false,
            )

        assertThat(secondRun.noop).isTrue()
        assertThat(secondRun.plan).isEmpty()
        assertThat(secondRun.applied).isEmpty()

        assertThat(liveRecords)
            .`as`("live zone must expose at least 4 A + 1 CNAME www")
            .hasSizeGreaterThanOrEqualTo(5)
    }

    private fun OvhCredentials.isComplete(): Boolean =
        listOf(applicationKey, applicationSecret, consumerKey).all { it.isNotBlank() }
}