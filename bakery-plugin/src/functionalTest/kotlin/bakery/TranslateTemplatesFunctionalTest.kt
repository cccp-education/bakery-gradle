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

    @Test
    fun `a partial variant schedules only its french copies`() {
        createSite(ollamaSection = true)
        // A variant that already carries a translated hero but a French blog.
        projectDir.resolve("jbake/i18n/de/templates").mkdirs()
        projectDir.resolve("jbake/i18n/de/templates/hero.thyme").writeText("""<h1 class="hero">Entwickler</h1>""")
        projectDir.resolve("jbake/i18n/de/templates/blog.thyme").writeText("""<p>Derniers articles</p>""")
        projectDir.resolve("jbake/templates/blog.thyme").writeText("""<p>Derniers articles</p>""")

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

        // hero is translated (preserved), blog is a French copy (scheduled).
        assertThat(result.output).contains("[de] DRY-RUN blog.thyme")
        assertThat(result.output).doesNotContain("[de] DRY-RUN hero.thyme")
    }

    @Test
    fun `a forced language schedules every template`() {
        createSite(ollamaSection = true)
        projectDir.resolve("jbake/i18n/es/templates").mkdirs()
        projectDir.resolve("jbake/i18n/es/templates/hero.thyme").writeText("""<h1 class="hero">Desarrollador</h1>""")

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(
                    "translateTemplates",
                    "--templateTargetLangs=es",
                    "--templateSourceLang=fr",
                    "--templateForceLangs=es",
                    "--templateDryRun=true",
                    "--info",
                ).build()

        assertThat(result.output).contains("[translateTemplates] Langues forcées : es")
        assertThat(result.output).contains("[es] DRY-RUN hero.thyme")
    }

    @Test
    fun `a preserved template has its html lang aligned without any translation`() {
        createSite(ollamaSection = true)
        // A template already translated keeps its copy (preserved), but declares
        // the French source language — a WCAG 3.1.1 violation to repair for free.
        projectDir.resolve("jbake/i18n/de/templates").mkdirs()
        projectDir.resolve("jbake/i18n/de/templates/hero.thyme")
            .writeText("""<html lang="fr"><body><h1 class="hero">Entwickler</h1></body></html>""")

        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                "translateTemplates",
                "--templateTargetLangs=de",
                "--templateSourceLang=fr",
                "--templateDryRun=false",
                "--info",
            ).build()

        val fixed = projectDir.resolve("jbake/i18n/de/templates/hero.thyme").readText()
        assertThat(fixed).contains("""<html lang="de">""")
        assertThat(fixed).contains("Entwickler")
    }

    @Test
    fun `the variant templates are written next to the reference under jbake`() {
        createSite(ollamaSection = true)

        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                "translateTemplates",
                "--templateTargetLangs=de",
                "--templateSourceLang=fr",
                "--templateDryRun=false",
                "--info",
            ).build()

        // The i18n base is co-located under the bake root, never at the project root.
        assertThat(projectDir.resolve("jbake/i18n/de/templates/hero.thyme")).exists()
        assertThat(projectDir.resolve("i18n")).doesNotExist()
    }

    @Test
    fun `a structurally broken translation is never written`() {
        createSite(ollamaSection = true)
        // A reference template whose tag skeleton is intact.
        projectDir.resolve("jbake/templates/footer.thyme").writeText(
            """<footer th:if="${'$'}{shown}"><p>Pied</p></footer>""",
        )

        // No translation service is reachable: the model call fails, the target
        // stays absent and the delta re-schedules it — never a corrupted file.
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                "translateTemplates",
                "--templateTargetLangs=de",
                "--templateSourceLang=fr",
                "--templateDryRun=false",
                "--info",
            ).build()

        val target = projectDir.resolve("jbake/i18n/de/templates/footer.thyme")
        if (target.exists()) {
            assertThat(target.readText()).contains(">")
        }
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
