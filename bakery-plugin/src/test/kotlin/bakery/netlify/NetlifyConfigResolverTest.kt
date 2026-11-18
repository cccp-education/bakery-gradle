package bakery.netlify

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-NETLIFY (US-2, D12) — the cascade is CLI > gradle.properties >
 * DSL > site.yml > defaults. Every layer is proved, not claimed.
 */
class NetlifyConfigResolverTest {
    private val yaml =
        NetlifyConfig(
            siteId = "yaml-site",
            tokenEnv = "YAML_TOKEN",
            verifyUrl = "https://yaml.example/",
            deploy = NetlifyDeployOptions(exclude = listOf("yaml.bak"), message = "yaml message", force = true, dryRun = true),
            toml = NetlifyTomlOptions(enabled = true, publishDir = "yaml-publish", redirects = true),
        )

    @Test
    @DisplayName("defaults when nothing is configured")
    fun defaults() {
        val resolved = NetlifyConfigResolver.resolve(emptyMap(), NetlifyDsl(), yaml = null)

        assertThat(resolved.siteId).isEmpty()
        assertThat(resolved.isConfigured).isFalse
        assertThat(resolved.tokenEnv).isEqualTo("NETLIFY_AUTH_TOKEN")
        assertThat(resolved.deploy.message).isEqualTo("bakery deploy")
        assertThat(resolved.deploy.dryRun).isFalse
        assertThat(resolved.toml.enabled).isFalse
    }

    @Test
    @DisplayName("site.yml wins over defaults")
    fun yamlWinsOverDefaults() {
        val resolved = NetlifyConfigResolver.resolve(emptyMap(), NetlifyDsl(), yaml = yaml)

        assertThat(resolved.siteId).isEqualTo("yaml-site")
        assertThat(resolved.tokenEnv).isEqualTo("YAML_TOKEN")
        assertThat(resolved.deploy.exclude).containsExactly("yaml.bak")
        assertThat(resolved.deploy.force).isTrue
        assertThat(resolved.deploy.dryRun).isTrue
        assertThat(resolved.toml.publishDir).isEqualTo("yaml-publish")
    }

    @Test
    @DisplayName("DSL wins over site.yml")
    fun dslWinsOverYaml() {
        val dsl =
            NetlifyDsl().apply {
                siteId = "dsl-site"
                message = "dsl message"
                force = false
                exclude = "one.bak, two.bak"
            }

        val resolved = NetlifyConfigResolver.resolve(emptyMap(), dsl, yaml = yaml)

        assertThat(resolved.siteId).isEqualTo("dsl-site")
        assertThat(resolved.deploy.message).isEqualTo("dsl message")
        assertThat(resolved.deploy.force).isFalse
        assertThat(resolved.deploy.exclude).containsExactly("one.bak", "two.bak")
    }

    @Test
    @DisplayName("gradle.properties / CLI win over the DSL")
    fun propertiesWinOverDsl() {
        val dsl = NetlifyDsl().apply { siteId = "dsl-site" }
        val props =
            mapOf(
                "bakery.netlify.siteId" to "cli-site",
                "bakery.netlify.deploy.dryRun" to "true",
                "bakery.netlify.toml.enabled" to "true",
            )

        val resolved = NetlifyConfigResolver.resolve(props, dsl, yaml = yaml)

        assertThat(resolved.siteId).isEqualTo("cli-site")
        assertThat(resolved.deploy.dryRun).isTrue
        assertThat(resolved.toml.enabled).isTrue
    }

    @Test
    @DisplayName("an empty exclusion CSV is an empty list")
    fun emptyExcludeList() {
        val resolved = NetlifyConfigResolver.resolve(emptyMap(), NetlifyDsl(), yaml = null)

        assertThat(resolved.deploy.exclude).isEmpty()
    }
}
