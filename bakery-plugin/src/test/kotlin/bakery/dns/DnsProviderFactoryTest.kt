package bakery.dns

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-DNS-3 — Unit tests for [DnsProviderFactory].
 *
 * Pattern capsule `resolve*Provider` (CapsuleManager.resolveAudioPostProcessor):
 * an unknown or unavailable provider degrades to [NoOpDnsProvider] rather
 * than throwing. The OVH adapter is only returned when a real
 * [OvhDnsClient] is supplied (i.e. credentials configured).
 *
 * Methodology: DDD/TDD baby steps.
 */
class DnsProviderFactoryTest {

    private class FakeOvhHttp : OvhHttp {
        override fun call(method: String, url: String, body: String?): OvhHttpResponse =
            OvhHttpResponse(200, "[]")
    }

    @Nested
    @DisplayName("degraded resolution")
    inner class Degraded {
        @Test
        @DisplayName("the noop name yields the noop provider")
        fun `noop name`() {
            assertThat(DnsProviderFactory.resolve("noop")).isInstanceOf(NoOpDnsProvider::class.java)
        }

        @Test
        @DisplayName("an unknown provider degrades to noop")
        fun `unknown provider`() {
            assertThat(DnsProviderFactory.resolve("cloudflare")).isInstanceOf(NoOpDnsProvider::class.java)
        }

        @Test
        @DisplayName("ovh without a client degrades to noop")
        fun `ovh without client`() {
            assertThat(DnsProviderFactory.resolve("ovh")).isInstanceOf(NoOpDnsProvider::class.java)
        }
    }

    @Nested
    @DisplayName("ovh")
    inner class Ovh {
        @Test
        @DisplayName("ovh with a client yields the ovh adapter")
        fun `ovh with client`() {
            val client = OvhDnsClient(OvhCredentials("ak", "as", "ck"), FakeOvhHttp())
            val provider = DnsProviderFactory.resolve("ovh", client)
            assertThat(provider).isInstanceOf(OvhDnsProvider::class.java)
            assertThat(provider.name()).isEqualTo("ovh")
            assertThat(provider.isAvailable()).isTrue()
        }
    }
}
