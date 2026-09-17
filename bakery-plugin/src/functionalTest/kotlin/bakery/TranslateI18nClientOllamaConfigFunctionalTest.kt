package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * T-I18N-BAKERY US-2 — the consumer declares its LLM pool in `site.yml`
 * (`ollama:` section) and bakery activates it, without the caller touching the
 * shared runner build. The dry-run keeps the check free: no metered call is
 * ever made, only the effective configuration is observed.
 */
class TranslateI18nClientOllamaConfigFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `an ollama section in site yml activates the pool`() {
        createClientSite(ollamaSection = true)

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(
                    "translateI18nClient",
                    "--i18nClientSource=maquette/js",
                    "--i18nClientTargetLangs=en",
                    "--i18nClientDryRun=true",
                    "--info",
                ).build()

        assertThat(result.output).contains("translateI18nClient IA activé")
    }

    @Test
    fun `without an ollama section the pool stays disabled`() {
        createClientSite(ollamaSection = false)

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(
                    "translateI18nClient",
                    "--i18nClientSource=maquette/js",
                    "--i18nClientTargetLangs=en",
                    "--i18nClientDryRun=true",
                    "--info",
                ).build()

        assertThat(result.output).contains("translateI18nClient IA désactivé")
    }

    private fun createClientSite(ollamaSection: Boolean) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "talaria-i18n-client-ollama"
            """.trimIndent(),
        )

        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("education.cccp.bakery") }

            bakery {
                configPath = "site.yml"
            }
            """.trimIndent(),
        )

        val ollamaLines =
            if (ollamaSection) {
                listOf(
                    "ollama:",
                    "  model: nemotron-3-super:cloud",
                    "  portStart: 11437",
                    "  portEnd: 11465",
                    "  timeoutSeconds: 300",
                    "  deviceKeys:",
                    "    - keyName: ollama-11437",
                    "      privateKey: ssh-ed25519-fake-key",
                )
            } else {
                emptyList()
            }

        val siteYml =
            listOf(
                "bake:",
                "  srcPath: \"jbake\"",
                "  destDirPath: \"bake\"",
                "  cname: \"talaria.school\"",
            ) + ollamaLines

        projectDir.resolve("site.yml").writeText(siteYml.joinToString("\n", postfix = "\n"))

        projectDir.resolve("jbake").mkdirs()

        projectDir.resolve("maquette/js/i18n.js").also {
            it.parentFile.mkdirs()
            it.writeText(
                buildString {
                    appendLine("(function (ns) {")
                    appendLine("  var DICT = {")
                    appendLine("    fr: {")
                    appendLine("      \"nav.home\": \"Accueil\",")
                    appendLine("      \"nav.cart\": \"Panier\"")
                    appendLine("    },")
                    appendLine("    en: {")
                    appendLine("      \"nav.home\": \"Home\"")
                    appendLine("    }")
                    appendLine("  };")
                    appendLine("})(window.TALARIA);")
                },
            )
        }
    }
}
