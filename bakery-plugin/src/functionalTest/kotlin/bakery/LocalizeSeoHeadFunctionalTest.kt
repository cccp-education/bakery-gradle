package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * BKY-OUT-1 — Functional tests (Gradle TestKit) for the `localizeSeoHead` task.
 *
 * The fixture reproduces the published defect (review S-263): every variant head
 * carries the French `canonical`, `og:url`, `og:locale` and `og-images/fr/`.
 * The task must repair each variant in its own language, fall back to `en` for a
 * language without cards, and be idempotent.
 */
class LocalizeSeoHeadFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `localizeSeoHead rewrites the canonical and og url of a variant`() {
        createProjectWithVariants()

        run("localizeSeoHead")

        val en = projectDir.resolve("site/i18n/en/templates/header.thyme").readText()
        assertThat(en).contains("""th:href="${'$'}{config.site_host} + 'en/' + ${'$'}{canonUri}"""")
        assertThat(en).contains("""property="og:url" th:content="${'$'}{config.site_host} + 'en/' + ${'$'}{canonUri}"""")
    }

    @Test
    fun `localizeSeoHead declares the variant og locale`() {
        createProjectWithVariants()

        run("localizeSeoHead")

        assertThat(projectDir.resolve("site/i18n/en/templates/header.thyme").readText())
            .contains("""content="en_US"""")
        assertThat(projectDir.resolve("site/i18n/de/templates/header.thyme").readText())
            .contains("""content="de_DE"""")
    }

    @Test
    fun `localizeSeoHead uses the variant og images when they exist`() {
        createProjectWithVariants()

        run("localizeSeoHead")

        assertThat(projectDir.resolve("site/i18n/en/templates/header.thyme").readText())
            .contains("og-images/en/")
    }

    @Test
    fun `localizeSeoHead falls back to the reference cards for a language without cards`() {
        createProjectWithVariants()

        run("localizeSeoHead")

        val de = projectDir.resolve("site/i18n/de/templates/header.thyme").readText()
        assertThat(de).contains("og-images/fr/")
        assertThat(de).doesNotContain("og-images/de/")
    }

    @Test
    fun `localizeSeoHead leaves the reference head prefixless`() {
        createProjectWithVariants()

        run("localizeSeoHead")

        val fr = projectDir.resolve("site/templates/header.thyme").readText()
        assertThat(fr).contains("""th:href="${'$'}{config.site_host} + ${'$'}{canonUri}"""")
        assertThat(fr).contains("""content="fr_FR"""")
    }

    @Test
    fun `localizeSeoHead is idempotent`() {
        createProjectWithVariants()

        run("localizeSeoHead")
        val first = projectDir.resolve("site/i18n/en/templates/header.thyme").readText()

        run("localizeSeoHead").let { result ->
            assertThat(result.output).contains("BUILD SUCCESSFUL")
            assertThat(projectDir.resolve("site/i18n/en/templates/header.thyme").readText()).isEqualTo(first)
        }
    }

    @Test
    fun `localizeSeoHead is registered in the transform group`() {
        createProjectWithVariants()

        run("tasks", "--group", "transform").let { result ->
            assertThat(result.output).contains("localizeSeoHead")
        }
    }

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
            supportedLanguages: [fr, en, de]
            """.trimIndent(),
        )

        val siteDir = projectDir.resolve("site")
        siteDir.resolve("templates").mkdirs()
        siteDir.resolve("templates/header.thyme").writeText(head())
        listOf("en", "de").forEach { lang ->
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
