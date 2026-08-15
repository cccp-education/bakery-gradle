package bakery.dns

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-DNS-3 — Unit tests for [OvhDnsProvider].
 *
 * The adapter is a thin delegation over [OvhDnsClient] (tested in
 * BKY-DNS-2): it maps the OVH zone to [ExistingDnsRecord] domain objects
 * and forwards mutations. Transport is injected via a fake [OvhHttp].
 *
 * Methodology: DDD/TDD baby steps.
 */
class OvhDnsProviderTest {

    private val credentials = OvhCredentials("ak", "as", "ck")

    private fun provider(http: OvhHttp = StubOvhHttp()): OvhDnsProvider =
        OvhDnsProvider(OvhDnsClient(credentials, http))

    @Nested
    @DisplayName("identity")
    inner class Identity {
        @Test
        @DisplayName("identifies as the ovh provider and is available")
        fun `identifies itself`() {
            val provider = provider()
            assertThat(provider.name()).isEqualTo("ovh")
            assertThat(provider.isAvailable()).isTrue()
        }
    }

    @Nested
    @DisplayName("listRecords")
    inner class ListRecords {
        @Test
        @DisplayName("maps ids and OVH wire records to domain records")
        fun `maps zone records`() {
            val records = provider().listRecords("talaria.school")
            assertThat(records).hasSize(2)
            assertThat(records[0].id).isEqualTo(123L)
            assertThat(records[0].record).isEqualTo(DnsRecord("CNAME", "www", "pages-content.github.io.", 3600))
            assertThat(records[1].id).isEqualTo(124L)
            assertThat(records[1].record).isEqualTo(DnsRecord("A", "@", "185.199.108.153", 3600))
        }
    }

    @Nested
    @DisplayName("mutations")
    inner class Mutations {
        @Test
        @DisplayName("create delegates and returns the new id")
        fun `create delegates`() {
            val id = provider().createRecord("talaria.school", DnsRecord("A", "@", "185.199.108.153", 3600))
            assertThat(id).isEqualTo(500L)
        }

        @Test
        @DisplayName("update, delete and refresh delegate without throwing")
        fun `update delete refresh delegate`() {
            val p = provider()
            val record = DnsRecord("A", "@", "185.199.108.153", 3600)
            p.updateRecord("talaria.school", 123L, record)
            p.deleteRecord("talaria.school", 124L)
            p.refreshZone("talaria.school")
        }
    }

    /**
     * Canned OVH wire responses keyed on method + path shape.
     */
    private class StubOvhHttp : OvhHttp {
        override fun call(method: String, url: String, body: String?): OvhHttpResponse =
            when {
                method == "GET" && url.endsWith("/record") ->
                    OvhHttpResponse(200, "[123, 124]")
                method == "GET" && url.contains("/record/123") ->
                    OvhHttpResponse(
                        200,
                        """{"fieldType":"CNAME","subDomain":"www","target":"pages-content.github.io.","ttl":3600,"id":123}""",
                    )
                method == "GET" && url.contains("/record/124") ->
                    OvhHttpResponse(
                        200,
                        """{"fieldType":"A","subDomain":"","target":"185.199.108.153","ttl":3600,"id":124}""",
                    )
                method == "POST" && url.endsWith("/record") ->
                    OvhHttpResponse(
                        200,
                        """{"fieldType":"A","subDomain":"","target":"185.199.108.153","ttl":3600,"id":500}""",
                    )
                else -> OvhHttpResponse(200, "{}")
            }
    }
}
