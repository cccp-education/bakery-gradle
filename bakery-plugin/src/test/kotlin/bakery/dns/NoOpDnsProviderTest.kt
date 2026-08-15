package bakery.dns

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-DNS-3 — Unit tests for [NoOpDnsProvider].
 *
 * The NoOp provider is the degraded/CI fallback (pattern capsule
 * `NoOpAudioPostProcessor`): it reports an empty zone and never mutates.
 * Combined with the dry-run default it makes provisioning a guaranteed
 * no-op when no real OVH credentials are configured.
 *
 * Methodology: DDD/TDD baby steps.
 */
class NoOpDnsProviderTest {

    private val record = DnsRecord("A", "@", "185.199.108.153", 3600)

    @Test
    @DisplayName("identifies itself as the noop provider and is available")
    fun `identifies itself`() {
        val provider = NoOpDnsProvider()
        assertThat(provider.name()).isEqualTo("noop")
        assertThat(provider.isAvailable()).isTrue()
    }

    @Test
    @DisplayName("reports an empty zone")
    fun `empty zone`() {
        assertThat(NoOpDnsProvider().listRecords("talaria.school")).isEmpty()
    }

    @Test
    @DisplayName("every mutation is a harmless no-op")
    fun `mutations are no-ops`() {
        val provider = NoOpDnsProvider()
        assertThat(provider.createRecord("talaria.school", record)).isEqualTo(0L)
        provider.updateRecord("talaria.school", 1L, record)
        provider.deleteRecord("talaria.school", 1L)
        provider.refreshZone("talaria.school")
        assertThat(provider.listRecords("talaria.school")).isEmpty()
    }

    @Test
    @DisplayName("reconciling through a noop provider is safe and never applies")
    fun `reconcile is safe`() {
        val result = DnsProvisioner(NoOpDnsProvider(), "talaria.school")
            .reconcile(listOf(record), dryRun = true, allowDelete = true)
        assertThat(result.applied).isEmpty()
        assertThat(result.skipped).hasSize(1)
    }
}
