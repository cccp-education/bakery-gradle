package bakery.dns

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-DNS-2 — Unit tests for OvhDnsClient.
 *
 * The HTTP transport is injected via the [OvhHttp] port, so tests use a
 * fake and assert the exact request (method/url/body) and the response
 * mapping. Zero real I/O.
 *
 * Methodology: DDD/TDD baby steps.
 */
class OvhDnsClientTest {

    private val credentials =
        OvhCredentials(
            applicationKey = "application-key",
            applicationSecret = "application-secret",
            consumerKey = "consumer-key",
        )

    private class FakeOvhHttp(
        private val handler: (method: String, url: String, body: String?) -> OvhHttpResponse,
    ) : OvhHttp {
        val requests = mutableListOf<OvhHttpRequest>()

        override fun call(method: String, url: String, body: String?): OvhHttpResponse {
            requests.add(OvhHttpRequest(method, url, body))
            return handler(method, url, body)
        }
    }

    private data class OvhHttpRequest(
        val method: String,
        val url: String,
        val body: String?,
    )

    @Nested
    @DisplayName("listRecordIds")
    inner class ListRecordIds {
        @Test
        @DisplayName("GETs the record list path and parses the array of ids")
        fun `gets record list and parses ids`() {
            val fake = FakeOvhHttp { _, _, _ -> OvhHttpResponse(200, "[123456, 123457]") }
            val client = OvhDnsClient(credentials, fake)

            val ids = client.listRecordIds("talaria.school")

            assertThat(ids).containsExactly(123456L, 123457L)
            assertThat(fake.requests).hasSize(1)
            assertThat(fake.requests[0].method).isEqualTo("GET")
            assertThat(fake.requests[0].url)
                .isEqualTo("https://eu.api.ovh.com/1.0/domain/zone/talaria.school/record")
            assertThat(fake.requests[0].body).isNull()
        }

        @Test
        @DisplayName("fieldType is appended as a query parameter")
        fun `fieldType is appended as query parameter`() {
            val fake = FakeOvhHttp { _, _, _ -> OvhHttpResponse(200, "[]") }
            val client = OvhDnsClient(credentials, fake)

            client.listRecordIds("talaria.school", fieldType = "A")

            assertThat(fake.requests[0].url)
                .isEqualTo("https://eu.api.ovh.com/1.0/domain/zone/talaria.school/record?fieldType=A")
        }

        @Test
        @DisplayName("throws OvhDnsException on non-2xx response")
        fun `throws on non 2xx`() {
            val fake = FakeOvhHttp { _, _, _ -> OvhHttpResponse(404, "{\"message\":\"Not found\"}") }
            val client = OvhDnsClient(credentials, fake)

            assertThatThrownBy { client.listRecordIds("talaria.school") }
                .isInstanceOf(OvhDnsException::class.java)
                .hasMessageContaining("404")
        }
    }

    @Nested
    @DisplayName("getRecord")
    inner class GetRecord {
        @Test
        @DisplayName("GETs the record path and returns a normalized DnsRecord")
        fun `gets record and returns normalized DnsRecord`() {
            val fake =
                FakeOvhHttp { _, _, _ ->
                    OvhHttpResponse(
                        200,
                        """{"id":123456,"fieldType":"A","subDomain":"","target":"185.199.108.153","ttl":3600}""",
                    )
                }
            val client = OvhDnsClient(credentials, fake)

            val record = client.getRecord("talaria.school", 123456L)

            assertThat(fake.requests[0].url)
                .isEqualTo("https://eu.api.ovh.com/1.0/domain/zone/talaria.school/record/123456")
            assertThat(record.type).isEqualTo("A")
            assertThat(record.name).isEqualTo("@")
            assertThat(record.value).isEqualTo("185.199.108.153")
            assertThat(record.ttl).isEqualTo(3600)
        }
    }

    @Nested
    @DisplayName("createRecord")
    inner class CreateRecord {
        @Test
        @DisplayName("POSTs the OVH payload and returns the created id")
        fun `posts payload and returns created id`() {
            val fake =
                FakeOvhHttp { _, _, _ ->
                    OvhHttpResponse(
                        200,
                        """{"id":999001,"fieldType":"A","subDomain":"","target":"185.199.108.153","ttl":3600}""",
                    )
                }
            val client = OvhDnsClient(credentials, fake)

            val id =
                client.createRecord(
                    "talaria.school",
                    DnsRecord(type = "A", name = "@", value = "185.199.108.153", ttl = 3600),
                )

            assertThat(id).isEqualTo(999001L)
            assertThat(fake.requests[0].method).isEqualTo("POST")
            assertThat(fake.requests[0].url)
                .isEqualTo("https://eu.api.ovh.com/1.0/domain/zone/talaria.school/record")
            assertThat(fake.requests[0].body)
                .isEqualTo(
                    "{\"fieldType\":\"A\",\"subDomain\":\"\",\"target\":\"185.199.108.153\",\"ttl\":3600}",
                )
        }

        @Test
        @DisplayName("throws when the created record has no id")
        fun `throws when created record has no id`() {
            val fake = FakeOvhHttp { _, _, _ -> OvhHttpResponse(200, "{}") }
            val client = OvhDnsClient(credentials, fake)

            assertThatThrownBy {
                client.createRecord("talaria.school", DnsRecord(type = "A", name = "@", value = "1.2.3.4"))
            }.isInstanceOf(OvhDnsException::class.java)
        }
    }

    @Nested
    @DisplayName("updateRecord")
    inner class UpdateRecord {
        @Test
        @DisplayName("PUTs the OVH payload on the record path")
        fun `puts payload on record path`() {
            val fake = FakeOvhHttp { _, _, _ -> OvhHttpResponse(200, "{}") }
            val client = OvhDnsClient(credentials, fake)

            client.updateRecord(
                "talaria.school",
                123456L,
                DnsRecord(type = "A", name = "@", value = "185.199.108.153"),
            )

            assertThat(fake.requests[0].method).isEqualTo("PUT")
            assertThat(fake.requests[0].url)
                .isEqualTo("https://eu.api.ovh.com/1.0/domain/zone/talaria.school/record/123456")
            assertThat(fake.requests[0].body)
                .isEqualTo(
                    "{\"fieldType\":\"A\",\"subDomain\":\"\",\"target\":\"185.199.108.153\",\"ttl\":3600}",
                )
        }
    }

    @Nested
    @DisplayName("deleteRecord")
    inner class DeleteRecord {
        @Test
        @DisplayName("DELETEs the record path")
        fun `deletes the record path`() {
            val fake = FakeOvhHttp { _, _, _ -> OvhHttpResponse(200, "") }
            val client = OvhDnsClient(credentials, fake)

            client.deleteRecord("talaria.school", 123456L)

            assertThat(fake.requests[0].method).isEqualTo("DELETE")
            assertThat(fake.requests[0].url)
                .isEqualTo("https://eu.api.ovh.com/1.0/domain/zone/talaria.school/record/123456")
            assertThat(fake.requests[0].body).isNull()
        }
    }

    @Nested
    @DisplayName("refreshZone")
    inner class RefreshZone {
        @Test
        @DisplayName("POSTs an empty json object to the refresh path")
        fun `posts empty json to refresh path`() {
            val fake = FakeOvhHttp { _, _, _ -> OvhHttpResponse(200, "") }
            val client = OvhDnsClient(credentials, fake)

            client.refreshZone("talaria.school")

            assertThat(fake.requests[0].method).isEqualTo("POST")
            assertThat(fake.requests[0].url)
                .isEqualTo("https://eu.api.ovh.com/1.0/domain/zone/talaria.school/refresh")
            assertThat(fake.requests[0].body).isEqualTo("{}")
        }
    }
}
