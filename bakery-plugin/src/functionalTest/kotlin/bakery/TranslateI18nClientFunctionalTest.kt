package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-3.
 *
 * Functional (Oven) tests for the `translateI18nClient` task through a real
 * Gradle build: registration, dry-run safety and delta reporting. The LLM is
 * disabled (`ia.enabled` absent) so no metered call is ever made — the delta is
 * still computed and reported (Ink Economy Law).
 */
class TranslateI18nClientFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `dry-run reports the missing keys and writes nothing`() {
        createClientSite()

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(
                    "translateI18nClient",
                    "--i18nClientSource=maquette/js",
                    "--i18nClientTargetLangs=en",
                    "--i18nClientSourceLang=fr",
                    "--i18nClientDryRun=true",
                ).build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(result.output).contains("[translateI18nClient] Delta : 1 cles manquantes")
        assertThat(result.output).contains("DRY-RUN")
        assertThat(result.output).contains("aucun fichier modifie")
        assertThat(englishBlock()).doesNotContain("nav.cart")
    }

    @Test
    fun `the task is registered with the transform group`() {
        createClientSite()

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("tasks", "--group=transform")
                .build()

        assertThat(result.output).contains("translateI18nClient")
    }

    @Test
    fun `without an IA service the source dictionary is left untouched`() {
        createClientSite()

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(
                    "translateI18nClient",
                    "--i18nClientSource=maquette/js",
                    "--i18nClientTargetLangs=en",
                    "--i18nClientSourceLang=fr",
                    "--i18nClientDryRun=false",
                ).build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(result.output).contains("Aucun TranslationService")
        assertThat(englishBlock()).doesNotContain("nav.cart")
    }

    @Test
    fun `a complete dictionary is reported as a no-op`() {
        createClientSite(complete = true)

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(
                    "translateI18nClient",
                    "--i18nClientSource=maquette/js",
                    "--i18nClientTargetLangs=en",
                    "--i18nClientSourceLang=fr",
                    "--i18nClientDryRun=true",
                ).build()

        assertThat(result.output).contains("Rien a traduire")
    }

    @Test
    fun `an absent source directory is reported without failing the build`() {
        createClientSite()

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(
                    "translateI18nClient",
                    "--i18nClientSource=maquette/nonexistent",
                    "--i18nClientTargetLangs=en",
                    "--i18nClientDryRun=true",
                ).build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(result.output).contains("n'existe pas")
    }

    private fun dictionaryFile(): File = projectDir.resolve("maquette/js/i18n.js")

    /** Returns the `en: { ... }` block of the fixture dictionary. */
    private fun englishBlock(): String =
        dictionaryFile()
            .readText()
            .substringAfter("en: {")
            .substringBefore("}")

    private fun createClientSite(complete: Boolean = false) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "talaria-i18n-client"
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

        projectDir.resolve("site.yml").writeText(
            """
            bake:
              srcPath: "jbake"
              destDirPath: "bake"
              cname: "talaria.school"
            """.trimIndent(),
        )

        projectDir.resolve("jbake").mkdirs()

        val enBlock =
            if (complete) {
                "    en: {\n      \"nav.home\": \"Home\",\n      \"nav.cart\": \"Cart\"\n    }"
            } else {
                "    en: {\n      \"nav.home\": \"Home\"\n    }"
            }

        val dictionary =
            buildString {
                appendLine("(function (ns) {")
                appendLine("  var DICT = {")
                appendLine("    fr: {")
                appendLine("      \"nav.home\": \"Accueil\",")
                appendLine("      \"nav.cart\": \"Panier\"")
                appendLine("    },")
                appendLine(enBlock)
                appendLine("  };")
                appendLine("})(window.TALARIA);")
            }

        dictionaryFile().also {
            it.parentFile.mkdirs()
            it.writeText(dictionary)
        }
    }
}
