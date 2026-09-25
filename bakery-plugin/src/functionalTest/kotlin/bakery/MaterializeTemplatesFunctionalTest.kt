package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * BKY-LANG-NAV-8 — `materializeTemplates` produces a deployable EN template tree
 * from a **frozen** bundle, with **zero LLM call** (Ink Economy Law).
 *
 * `jbake-core:2.7.0` has no MessageResolver, so a site cannot bake `#{key}`
 * templates. When a site already owns frozen `messages_{lang}.properties` (the
 * i18n golden masters), the variant is materialised deterministically: the
 * reference templates are keyed once (the same extraction `migrateToI18n` runs)
 * then resolved with the frozen bundle. A complete bundle is a no-op on a second
 * run; a missing template is left absent rather than corrupted.
 */
class MaterializeTemplatesFunctionalTest {
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

        assertThat(result.output).contains("materializeTemplates")
    }

    @Test
    fun `a frozen bundle materializes the EN template tree without any LLM`() {
        createSite()

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(
                    "materializeTemplates",
                    "--materializeTargetLangs=en",
                    "--materializeSourceLang=fr",
                    "--materializeDryRun=false",
                ).build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")

        val enMenu = projectDir.resolve("site/i18n/en/templates/menu.thyme")
        assertThat(enMenu).exists()
        val content = enMenu.readText()
        assertThat(content)
            .describedAs("the EN variant must be literal, never a raw #{key}")
            .doesNotContain("#{")
            .contains("Home")
            .doesNotContain("Accueil")
    }

    @Test
    fun `a frozen bundle with non-ascii values is decoded as UTF-8`() {
        createSite(nonAsciiBundle = true)

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(
                    "materializeTemplates",
                    "--materializeTargetLangs=en",
                    "--materializeSourceLang=fr",
                    "--materializeDryRun=false",
                ).build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        val content = projectDir.resolve("site/i18n/en/templates/menu.thyme").readText()
        assertThat(content)
            .describedAs("UTF-8 values of the frozen bundle must survive the resolution")
            .contains("Reveal.js AsciiDoc→HTML")
            .doesNotContain("â")
    }

    @Test
    fun `a second run is a strict no-op`() {
        createSite()
        runMaterialize()
        val enMenu = projectDir.resolve("site/i18n/en/templates/menu.thyme")
        val first = enMenu.readText()

        runMaterialize()

        assertThat(enMenu.readText()).isEqualTo(first)
    }

    @Test
    fun `a language without a frozen bundle is skipped, never invented`() {
        createSite()

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(
                    "materializeTemplates",
                    "--materializeTargetLangs=de",
                    "--materializeSourceLang=fr",
                    "--materializeDryRun=false",
                ).build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(projectDir.resolve("site/i18n/de/templates")).doesNotExist()
    }

    private fun runMaterialize() {
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                "materializeTemplates",
                "--materializeTargetLangs=en",
                "--materializeSourceLang=fr",
                "--materializeDryRun=false",
            ).build()
    }

    private fun createSite(nonAsciiBundle: Boolean = false) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "materialize-templates-test"
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("education.cccp.bakery") }
            bakery { configPath = "site.yml" }
            """.trimIndent(),
        )

        val siteDir = projectDir.resolve("site")
        siteDir.resolve("templates").mkdirs()
        siteDir.resolve("content").mkdirs()

        siteDir.resolve("templates/menu.thyme").writeText(
            """
            <html xmlns:th="http://www.thymeleaf.org">
            <body>
            <nav>
                <a th:href="@{/}" >Accueil</a>
                <a th:href="@{/about.html}">A propos</a>
            </nav>
            </body>
            </html>
            """.trimIndent(),
        )
        siteDir.resolve("content/index.html").writeText("<h1>Bonjour</h1>")

        // The frozen EN bundle, as produced once by the golden-master migration.
        // A real bundle carries UTF-8 (`→`, accents): it is written raw so the
        // loader decoding is exercised, not the test encoding.
        val bundleContent =
            if (nonAsciiBundle) {
                """
                menu.1=Home
                menu.2=Reveal.js AsciiDoc→HTML presentations.
                """.trimIndent()
            } else {
                """
                menu.1=Home
                menu.2=About
                """.trimIndent()
            }
        siteDir
            .resolve("templates/messages_en.properties")
            .writeBytes(bundleContent.toByteArray(Charsets.UTF_8))

        projectDir.resolve("site.yml").writeText(
            """
            bake:
              srcPath: site
              destDirPath: build/output
            language: fr
            supportedLanguages: [fr, en]
            """.trimIndent(),
        )
    }
}
