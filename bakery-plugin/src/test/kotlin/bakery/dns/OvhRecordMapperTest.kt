package bakery.dns

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-DNS-2 — Unit tests for the OVH wire record mapping.
 *
 * Maps the domain [DnsRecord] (type/name/value/ttl, apex `@`) to the OVH
 * wire representation (fieldType/subDomain/target/ttl) and back.
 *
 * Methodology: DDD/TDD baby steps — pure object, no I/O.
 */
class OvhRecordMapperTest {

    private val json = jacksonObjectMapper()

    @Nested
    @DisplayName("DnsRecord -> OvhRecord wire mapping")
    inner class ToWire {
        @Test
        @DisplayName("apex A record maps to empty subDomain")
        fun `apex A record maps to empty subDomain`() {
            val wire =
                OvhRecord.fromDnsRecord(
                    DnsRecord(type = "A", name = "@", value = "185.199.108.153", ttl = 3600),
                )

            assertThat(wire.fieldType).isEqualTo("A")
            assertThat(wire.subDomain).isEqualTo("")
            assertThat(wire.target).isEqualTo("185.199.108.153")
            assertThat(wire.ttl).isEqualTo(3600)
        }

        @Test
        @DisplayName("www CNAME record maps to www subDomain")
        fun `www CNAME record maps to www subDomain`() {
            val wire =
                OvhRecord.fromDnsRecord(
                    DnsRecord(type = "CNAME", name = "www", value = "pages-content.github.io."),
                )

            assertThat(wire.fieldType).isEqualTo("CNAME")
            assertThat(wire.subDomain).isEqualTo("www")
            assertThat(wire.target).isEqualTo("pages-content.github.io.")
        }

        @Test
        @DisplayName("request body omits id and serializes the exact OVH payload")
        fun `request body omits id`() {
            val body =
                json.writeValueAsString(
                    OvhRecord.fromDnsRecord(
                        DnsRecord(type = "A", name = "@", value = "185.199.108.153", ttl = 3600),
                    ),
                )

            assertThat(body)
                .isEqualTo(
                    "{\"fieldType\":\"A\",\"subDomain\":\"\",\"target\":\"185.199.108.153\",\"ttl\":3600}",
                )
        }
    }

    @Nested
    @DisplayName("OvhRecord wire -> DnsRecord")
    inner class FromWire {
        @Test
        @DisplayName("empty subDomain maps to apex name")
        fun `empty subDomain maps to apex name`() {
            val record =
                OvhRecord(
                    fieldType = "A",
                    subDomain = "",
                    target = "185.199.108.153",
                    ttl = 3600,
                    id = 123456L,
                ).toDnsRecord()

            assertThat(record.type).isEqualTo("A")
            assertThat(record.name).isEqualTo("@")
            assertThat(record.value).isEqualTo("185.199.108.153")
            assertThat(record.ttl).isEqualTo(3600)
        }

        @Test
        @DisplayName("named subDomain is preserved")
        fun `named subDomain is preserved`() {
            val record =
                OvhRecord(
                    fieldType = "CNAME",
                    subDomain = "www",
                    target = "pages-content.github.io.",
                ).toDnsRecord()

            assertThat(record.name).isEqualTo("www")
        }

        @Test
        @DisplayName("get response with id and zone is parsed")
        fun `get response with id and zone is parsed`() {
            val response =
                """{"id":123456,"fieldType":"A","subDomain":"","target":"185.199.108.153","ttl":3600,"zone":"talaria.school"}"""
            val wire = json.readValue<OvhRecord>(response)

            assertThat(wire.id).isEqualTo(123456L)
            assertThat(wire.fieldType).isEqualTo("A")
            assertThat(wire.subDomain).isEqualTo("")
            assertThat(wire.target).isEqualTo("185.199.108.153")
        }
    }
}
