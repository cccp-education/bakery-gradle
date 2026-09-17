package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * CHE-I18N-22 US-7 — `translateTemplates` produces the full-template copy that
 * the CI swaps in place of `jbake/` (JBake has no MessageResolver).
 *
 * The dry-run keeps the check free: no metered call is ever made, only the
 * effective configuration and the skip behaviour are observed.
 */
class TranslateTemplatesFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `the task is registered in the transform group`() {
        createSite()

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("tasks", "--group", "transform")
                .build()

        assertThat(result.output).contains("translateTemplates")
    }

    @Test
    fun `an ollama section activates the template translation pool`() {
        createSite(ollamaSection = true)

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(
                    "translateTemplates",
                    "--templateTargetLangs=de",
                    "--templateSourceLang=fr",
                    "--templateDryRun=true",
                    "--info",
                ).build()

        assertThat(result.output).contains("translateTemplates IA activé")
    }

    @Test
    fun `without an ollama section the template translation pool stays disabled`() {
        createSite(ollamaSection = false)

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(
                    "translateTemplates",
                    "--templateTargetLangs=de",
                    "--templateSourceLang=fr",
                    "--templateDryRun=true",
                    "--info",
                ).build()

        assertThat(result.output).contains("translateTemplates IA désactivé")
    }

    private fun createSite(ollamaSection: Boolean = false) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "cheroliv-templates"
            """.trimIndent(),
        )

        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("education.cccp.bakery") }
            bakery { configPath = "site.yml" }
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
                "  cname: \"cheroliv.com\"",
            ) + ollamaLines

        projectDir.resolve("site.yml").writeText(siteYml.joinToString("\n", postfix = "\n"))

        projectDir.resolve("jbake/templates").mkdirs()
        projectDir.resolve("jbake/templates/hero.thyme").writeText(
            """<h1 class="hero">Développeur</h1>""",
        )
    }
}
