package bakery.dns

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-DNS-1 — Unit tests for DnsConfigMerger.
 *
 * 4 sources with precedence: CLI > YAML (site.yml dns:) > Props
 * (gradle.properties) > ENV (defaults). Pattern capsule
 * mergeAudioPostConfig.
 *
 * Methodology: DDD/TDD baby steps — each test compiles AND passes before
 * moving to the next one.
 */
class DnsConfigMergerTest {

    private val envConfig = DnsConfig(provider = "ovh", domain = "env.example.com", dryRun = true)
    private val propsConfig = DnsConfig(provider = "ovh", domain = "props.example.com", dryRun = true)
    private val yamlConfig = DnsConfig(provider = "ovh", domain = "yaml.example.com", dryRun = false)

    @Nested
    @DisplayName("scalar precedence CLI > YAML > Props > ENV")
    inner class Precedence {
        @Test
        @DisplayName("cli wins over yaml")
        fun `cli wins over yaml`() {
            val merged =
                DnsConfigMerger.merge(
                    env = envConfig,
                    props = propsConfig,
                    yaml = yamlConfig,
                    cli = mapOf("dns.domain" to "cli.example.com"),
                )

            assertThat(merged.domain).isEqualTo("cli.example.com")
        }

        @Test
        @DisplayName("yaml wins over props")
        fun `yaml wins over props`() {
            val merged =
                DnsConfigMerger.merge(
                    env = envConfig,
                    props = propsConfig,
                    yaml = yamlConfig,
                    cli = emptyMap(),
                )

            assertThat(merged.domain).isEqualTo("yaml.example.com")
        }

        @Test
        @DisplayName("props win over env")
        fun `props win over env`() {
            val merged =
                DnsConfigMerger.merge(
                    env = envConfig,
                    props = propsConfig,
                    yaml = null,
                    cli = emptyMap(),
                )

            assertThat(merged.domain).isEqualTo("props.example.com")
        }

        @Test
        @DisplayName("env is the fallback when nothing else is set")
        fun `env is the fallback`() {
            val merged =
                DnsConfigMerger.merge(
                    env = envConfig,
                    props = DnsConfig(),
                    yaml = null,
                    cli = emptyMap(),
                )

            assertThat(merged.domain).isEqualTo("env.example.com")
        }

        @Test
        @DisplayName("defaults when all sources are empty")
        fun `defaults when all sources empty`() {
            val merged =
                DnsConfigMerger.merge(
                    env = DnsConfig(),
                    props = DnsConfig(),
                    yaml = null,
                    cli = emptyMap(),
                )

            assertThat(merged.provider).isEqualTo("ovh")
            assertThat(merged.domain).isEqualTo("")
            assertThat(merged.records).isEmpty()
            assertThat(merged.dryRun).isTrue
        }
    }

    @Nested
    @DisplayName("boolean precedence")
    inner class BooleanPrecedence {
        @Test
        @DisplayName("cli boolean wins over yaml")
        fun `cli boolean wins over yaml`() {
            val merged =
                DnsConfigMerger.merge(
                    env = envConfig,
                    props = propsConfig,
                    yaml = yamlConfig, // dryRun=false
                    cli = mapOf("dns.dryRun" to "true"),
                )

            assertThat(merged.dryRun).isTrue
        }

        @Test
        @DisplayName("yaml boolean wins over props/env")
        fun `yaml boolean wins over props`() {
            val merged =
                DnsConfigMerger.merge(
                    env = envConfig,
                    props = propsConfig, // dryRun=true
                    yaml = yamlConfig, // dryRun=false
                    cli = emptyMap(),
                )

            assertThat(merged.dryRun).isFalse
        }
    }

    @Nested
    @DisplayName("records merge")
    inner class RecordsMerge {
        @Test
        @DisplayName("yaml records win over props/env records")
        fun `yaml records win over props`() {
            val yamlRecords =
                listOf(DnsRecord(type = "A", name = "@", value = "185.199.108.153"))
            val propsRecords =
                listOf(DnsRecord(type = "CNAME", name = "www", value = "pages-content.github.io."))

            val merged =
                DnsConfigMerger.merge(
                    env = DnsConfig(),
                    props = propsConfig.copy(records = propsRecords),
                    yaml = yamlConfig.copy(records = yamlRecords),
                    cli = emptyMap(),
                )

            assertThat(merged.records).isEqualTo(yamlRecords)
        }

        @Test
        @DisplayName("props records used when yaml section absent")
        fun `props records used when yaml absent`() {
            val propsRecords =
                listOf(DnsRecord(type = "A", name = "@", value = "185.199.108.153"))

            val merged =
                DnsConfigMerger.merge(
                    env = DnsConfig(),
                    props = propsConfig.copy(records = propsRecords),
                    yaml = null,
                    cli = emptyMap(),
                )

            assertThat(merged.records).isEqualTo(propsRecords)
        }
    }

    @Nested
    @DisplayName("credentials merge")
    inner class CredentialsMerge {
        @Test
        @DisplayName("credentials fields merge independently per source")
        fun `credentials merge independently`() {
            val yamlCreds = OvhCredentials(applicationKey = "yaml-key")
            val propsCreds = OvhCredentials(applicationSecret = "props-secret")
            val envCreds = OvhCredentials(consumerKey = "env-consumer")

            val merged =
                DnsConfigMerger.merge(
                    env = DnsConfig(credentials = envCreds),
                    props = DnsConfig(credentials = propsCreds),
                    yaml = yamlConfig.copy(credentials = yamlCreds),
                    cli = emptyMap(),
                )

            assertThat(merged.credentials.applicationKey).isEqualTo("yaml-key")
            assertThat(merged.credentials.applicationSecret).isEqualTo("props-secret")
            assertThat(merged.credentials.consumerKey).isEqualTo("env-consumer")
        }

        @Test
        @DisplayName("blank cli never overrides a non-blank lower-priority source")
        fun `blank cli does not override`() {
            val merged =
                DnsConfigMerger.merge(
                    env = envConfig,
                    props = propsConfig,
                    yaml = yamlConfig,
                    cli = mapOf("dns.domain" to ""),
                )

            assertThat(merged.domain).isEqualTo("yaml.example.com")
        }
    }
}