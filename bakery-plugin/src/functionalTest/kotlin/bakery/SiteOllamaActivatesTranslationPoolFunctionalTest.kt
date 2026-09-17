package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * CHE-I18N-22 US-1 — the `ollama:` section of `site.yml` activates the rotating
 * LLM pool for *every* translation task, not only `translateI18nClient`.
 *
 * Before this US only `translateI18nClient` resolved `site.ollama` through
 * `IaConfigResolver`; `migrateContentI18n` (AsciiDoc articles) and
 * `migrateToI18n` (Thymeleaf message bundles) consumed the raw DSL [bakery.llm.IaConfig]
 * and stayed disabled — the pool was unreachable for the very tasks that
 * translate a site. The dry-run keeps the check free: no metered call is ever
 * made, only the effective configuration is observed.
 */
class SiteOllamaActivatesTranslationPoolFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `an ollama section activates the content migration pool`() {
        createSite(ollamaSection = true)

        val result = runMigration("migrateContentI18n")

        assertThat(result.output).contains("migrateContentI18n IA activé")
    }

    @Test
    fun `without an ollama section the content migration pool stays disabled`() {
        createSite(ollamaSection = false)

        val result = runMigration("migrateContentI18n")

        assertThat(result.output).contains("migrateContentI18n IA désactivé")
    }

    @Test
    fun `an ollama section activates the template migration pool`() {
        createSite(ollamaSection = true)

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(
                    "migrateToI18n",
                    "--i18nSite=jbake",
                    "--i18nLangs=en",
                    "--i18nDryRun=true",
                    "--info",
                ).build()

        assertThat(result.output).contains("migrateToI18n IA activé")
    }

    private fun runMigration(task: String) =
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                task,
                "--contentI18nSource=content/blog",
                "--contentI18nOutput=content-i18n",
                "--contentI18nSourceLang=fr",
                "--contentI18nTargetLangs=en",
                "--contentI18nDryRun=true",
                "--info",
            ).build()

    private fun createSite(ollamaSection: Boolean) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "cheroliv-i18n-pool"
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
            """<h1 class="hero">Bienvenue</h1>""",
        )

        projectDir.resolve("jbake/content/blog/2026").mkdirs()
        projectDir.resolve("jbake/content/blog/2026/0001_post.adoc").writeText(
            """= Titre
:jbake-type: post
:jbake-status: published

Un article en français.
""",
        )
    }
}
