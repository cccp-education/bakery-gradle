package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * BKY-OUT-1 — the single functional smoke of the `localizeSeoHead` task.
 *
 * The rewrite rules themselves are pinned by pure tests
 * ([bakery.seo.LocalizedSeoHeadTest], [bakery.seo.OgImageLanguageTest],
 * [bakery.i18n.DocumentDirectionTest], [bakery.i18n.I18nTemplateLayoutTest]) —
 * they run in milliseconds. What only Gradle can prove is the **wiring**: the
 * variant layout is probed on disk (`i18n/{lang}/templates/`), the reference head
 * stays prefixless, a language without its own cards falls back to the reference
 * on the real directory listing, and a second run rewrites nothing.
 *
 * Measured before (S-319, T3/XB-FP-6): 9 tests × one TestKit build ≈ 44 s. This
 * smoke keeps the same end-to-end evidence in 3 builds.
 */
class LocalizeSeoHeadFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `localizeSeoHead localises every variant head from disk and is idempotent`() {
        createProjectWithVariants()

        run("localizeSeoHead")

        val en = variantHeader("en")
        assertThat(en).contains("""th:href="${'$'}{config.site_host} + 'en/' + ${'$'}{canonUri}"""")
        assertThat(en).contains("""property="og:url" th:content="${'$'}{config.site_host} + 'en/' + ${'$'}{canonUri}"""")
        assertThat(en).contains("""content="en_US"""")
        assertThat(en).contains("og-images/en/")

        val de = variantHeader("de")
        assertThat(de).contains("""content="de_DE"""")
        assertThat(de).contains("og-images/fr/")
        assertThat(de).doesNotContain("og-images/de/")
        assertThat(de).doesNotContain("dir=")

        val ar = variantHeader("ar")
        assertThat(ar).contains("<html xmlns:th=\"http://www.thymeleaf.org\" lang=\"fr\" dir=\"rtl\">")

        val fr = referenceHeader()
        assertThat(fr).contains("""th:href="${'$'}{config.site_host} + ${'$'}{canonUri}"""")
        assertThat(fr).contains("""content="fr_FR"""")

        val second = run("localizeSeoHead")
        assertThat(second.output).contains("BUILD SUCCESSFUL")
        assertThat(variantHeader("en")).isEqualTo(en)
    }

    @Test
    fun `localizeSeoHead is registered in the transform group`() {
        createProjectWithVariants()

        run("tasks", "--group", "transform").let { result ->
            assertThat(result.output).contains("localizeSeoHead")
        }
    }

    private fun variantHeader(language: String): String = projectDir.resolve("site/i18n/$language/templates/header.thyme").readText()

    private fun referenceHeader(): String = projectDir.resolve("site/templates/header.thyme").readText()

    private fun run(vararg arguments: String) =
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(*arguments)
            .build()

    private fun createProjectWithVariants() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "localize-seo-head-test"
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("education.cccp.bakery") }
            bakery { configPath = "site.yml" }
            """.trimIndent(),
        )
        projectDir.resolve("site.yml").writeText(
            """
            bake:
              srcPath: site
              destDirPath: build/output
            language: fr
            supportedLanguages: [fr, en, de, ar]
            """.trimIndent(),
        )

        val siteDir = projectDir.resolve("site")
        siteDir.resolve("templates").mkdirs()
        siteDir.resolve("templates/header.thyme").writeText(head())
        listOf("en", "de", "ar").forEach { lang ->
            val dir = siteDir.resolve("i18n/$lang/templates")
            dir.mkdirs()
            dir.resolve("header.thyme").writeText(head())
        }
        siteDir.resolve("assets/og-images/fr/blog").mkdirs()
        siteDir.resolve("assets/og-images/en/blog").mkdirs()
    }

    private fun head(): String =
        """
        <html xmlns:th="http://www.thymeleaf.org" lang="fr">
        <head>
            <th:block th:with="canonUri=(${'$'}{canonOverride != null} ? ${'$'}{canonOverride} : ''),
                               ogImage=${'$'}{'og-images/fr/' + #strings.replace(content.uri, '.html', '.png')}">
                <link rel="canonical" th:href="${'$'}{config.site_host} + ${'$'}{canonUri}"/>
                <meta property="og:url" th:content="${'$'}{config.site_host} + ${'$'}{canonUri}"/>
                <meta property="og:locale" content="fr_FR"/>
                <meta property="og:image" th:content="${'$'}{config.site_host} + ${'$'}{ogImage}"/>
            </th:block>
        </head>
        </html>
        """.trimIndent()
}
