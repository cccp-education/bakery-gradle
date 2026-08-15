package bakery.dns

import bakery.FileSystemManager
import bakery.SiteConfiguration
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-DNS-1 — Unit tests for the DNS domain (bakery.dns).
 *
 * Covers DnsConfig / DnsRecord / OvhCredentials defaults, secret masking
 * and the Jackson mapping of the `dns:` section in site.yml.
 *
 * Methodology: DDD/TDD baby steps — each test compiles AND passes before
 * moving to the next one.
 */
class DnsConfigTest {

    @Nested
    @DisplayName("DnsConfig defaults")
    inner class DnsConfigDefaults {
        @Test
        @DisplayName("defaults: provider ovh, dry-run true, empty domain/records")
        fun `defaults are provider ovh dry run true`() {
            val config = DnsConfig()

            assertThat(config.provider).isEqualTo("ovh")
            assertThat(config.domain).isEqualTo("")
            assertThat(config.records).isEmpty()
            assertThat(config.dryRun).isTrue
        }
    }

    @Nested
    @DisplayName("DnsRecord defaults")
    inner class DnsRecordDefaults {
        @Test
        @DisplayName("default ttl is 3600")
        fun `default ttl is 3600`() {
            val record = DnsRecord(type = "A", name = "@", value = "185.199.108.153")

            assertThat(record.ttl).isEqualTo(3600)
        }

        @Test
        @DisplayName("apex A record and www CNAME record shapes")
        fun `apex A and www CNAME record shapes`() {
            val apexA = DnsRecord(type = "A", name = "@", value = "185.199.108.153", ttl = 3600)
            val wwwCname = DnsRecord(type = "CNAME", name = "www", value = "pages-content.github.io.")

            assertThat(apexA.type).isEqualTo("A")
            assertThat(apexA.name).isEqualTo("@")
            assertThat(wwwCname.type).isEqualTo("CNAME")
            assertThat(wwwCname.name).isEqualTo("www")
        }
    }

    @Nested
    @DisplayName("OvhCredentials secret masking")
    inner class CredentialsMasking {
        @Test
        @DisplayName("toString never leaks applicationKey/secret/consumerKey")
        fun `toString masks all secrets`() {
            val creds =
                OvhCredentials(
                    applicationKey = "super-secret-application-key",
                    applicationSecret = "super-secret-application-secret",
                    consumerKey = "super-secret-consumer-key",
                )

            val rendered = creds.toString()

            assertThat(rendered).doesNotContain("super-secret-application-key")
            assertThat(rendered).doesNotContain("super-secret-application-secret")
            assertThat(rendered).doesNotContain("super-secret-consumer-key")
        }

        @Test
        @DisplayName("empty credentials render as not set")
        fun `empty credentials render as not set`() {
            val rendered = OvhCredentials().toString()

            assertThat(rendered).contains("(not set)")
            assertThat(rendered).doesNotContain("***[0 chars]")
        }
    }

    @Nested
    @DisplayName("Jackson mapping — dns: section in site.yml")
    inner class JacksonMapping {
        @Test
        @DisplayName("dns section with records is parsed into DnsConfig")
        fun `dns section is parsed`() {
            val yaml =
                """
                language: fr
                supportedLanguages: [fr, en]
                dns:
                  provider: ovh
                  domain: talaria.school
                  dryRun: true
                  records:
                    - type: A
                      name: "@"
                      value: "185.199.108.153"
                      ttl: 3600
                    - type: CNAME
                      name: www
                      value: "pages-content.github.io."
                """.trimIndent()

            val config = FileSystemManager.yamlMapper.readValue<SiteConfiguration>(yaml)

            assertThat(config.dns).isNotNull
            val dns = config.dns!!
            assertThat(dns.provider).isEqualTo("ovh")
            assertThat(dns.domain).isEqualTo("talaria.school")
            assertThat(dns.dryRun).isTrue
            assertThat(dns.records).hasSize(2)
            assertThat(dns.records[0].type).isEqualTo("A")
            assertThat(dns.records[0].name).isEqualTo("@")
            assertThat(dns.records[0].value).isEqualTo("185.199.108.153")
            assertThat(dns.records[0].ttl).isEqualTo(3600)
            assertThat(dns.records[1].type).isEqualTo("CNAME")
            assertThat(dns.records[1].name).isEqualTo("www")
            assertThat(dns.records[1].value).isEqualTo("pages-content.github.io.")
        }

        @Test
        @DisplayName("dns section with credentials is parsed and masked on toString")
        fun `dns section with credentials is parsed`() {
            val yaml =
                """
                dns:
                  provider: ovh
                  domain: talaria.school
                  credentials:
                    applicationKey: my-app-key
                    applicationSecret: my-app-secret
                    consumerKey: my-consumer-key
                """.trimIndent()

            val config = FileSystemManager.yamlMapper.readValue<SiteConfiguration>(yaml)

            assertThat(config.dns).isNotNull
            assertThat(config.dns!!.credentials.applicationKey).isEqualTo("my-app-key")
            assertThat(config.dns!!.credentials.applicationSecret).isEqualTo("my-app-secret")
            assertThat(config.dns!!.credentials.consumerKey).isEqualTo("my-consumer-key")
            assertThat(config.dns.toString()).doesNotContain("my-app-secret")
        }

        @Test
        @DisplayName("absent dns section maps to null (backward compat, no provisioning)")
        fun `absent dns section maps to null`() {
            val yaml =
                """
                language: fr
                bake:
                  cname: talaria.school
                """.trimIndent()

            val config = FileSystemManager.yamlMapper.readValue<SiteConfiguration>(yaml)

            assertThat(config.dns).isNull()
        }

        @Test
        @DisplayName("unknown fields inside dns section are ignored")
        fun `unknown fields inside dns section are ignored`() {
            val yaml =
                """
                dns:
                  provider: ovh
                  domain: talaria.school
                  unexpectedField: whatever
                """.trimIndent()

            val config = FileSystemManager.yamlMapper.readValue<SiteConfiguration>(yaml)

            assertThat(config.dns).isNotNull
            assertThat(config.dns!!.domain).isEqualTo("talaria.school")
        }
    }
}