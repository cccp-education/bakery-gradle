package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * EPIC BKY-SEO-5 — Functional tests for the injectSeo task (Gradle TestKit).
 */
class InjectSeoFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `injectSeo injects canonical and hreflang into header thyme for FR`() {
        createProjectWithFixture()
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("injectSeo")
            .build()
            .let { result ->
                assertThat(result.output).contains("BUILD SUCCESSFUL")
                val frHeader = projectDir.resolve("site/templates/header.thyme")
                assertThat(frHeader.exists()).isTrue()
                val content = frHeader.readText()
                assertThat(content).contains("canonical")
                assertThat(content).contains("hreflang=\"fr\"")
            }
    }

    @Test
    fun `injectSeo injects into EN header thyme`() {
        createProjectWithFixture()
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("injectSeo")
            .build()

        val enHeader = projectDir.resolve("site/en/templates/header.thyme")
        assertThat(enHeader.exists()).isTrue()
        val content = enHeader.readText()
        assertThat(content).contains("canonical")
        assertThat(content).contains("hreflang=\"en\"")
    }

    @Test
    fun `injectSeo is idempotent`() {
        createProjectWithFixture()
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("injectSeo")
            .build()

        val frHeader = projectDir.resolve("site/templates/header.thyme")
        val firstContent = frHeader.readText()

        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("injectSeo")
            .build()
            .let { result2 ->
                assertThat(result2.output).contains("BUILD SUCCESSFUL")
                val secondContent = frHeader.readText()
                assertThat(secondContent).isEqualTo(firstContent)
            }
    }

    @Test
    fun `injectSeo is no-op without seo config`() {
        createProjectWithoutSeoConfig()
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("injectSeo")
            .build()
            .let { result ->
                assertThat(result.output).contains("BUILD SUCCESSFUL")
                assertThat(result.output).contains("No seo config")
            }
    }

    @Test
    fun `injectSeo is registered in transform group`() {
        createProjectWithFixture()
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--group", "transform")
            .build()
            .let { result ->
                assertThat(result.output).contains("injectSeo")
            }
    }

    private fun createProjectWithFixture() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "inject-seo-test"
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

        val headerThyme =
            """
            <html xmlns:th="http://www.thymeleaf.org">
            <head>
            <!-- SEO: bakery -->
            <old-seo />
            <!-- /SEO: bakery -->
            <title>Test</title>
            </head>
            <body></body>
            </html>
            """.trimIndent()

        siteDir.resolve("templates/header.thyme").writeText(headerThyme)
        siteDir.resolve("content/index.html").writeText("<h1>Hello FR</h1>")

        val enDir = siteDir.resolve("en")
        enDir.resolve("templates").mkdirs()
        enDir.resolve("content").mkdirs()
        enDir.resolve("templates/header.thyme").writeText(headerThyme)
        enDir.resolve("content/index.html").writeText("<h1>Hello EN</h1>")

        projectDir.resolve("site.yml").writeText(
            """
            bake:
              srcPath: site
              destDirPath: build/output
            language: fr
            supportedLanguages: [fr, en]
            seo:
              siteName: Example
              brand: Example
              defaultOgImage: example-default.png
              websiteUrl: "https://example.com"
              inLanguage: fr
            """.trimIndent(),
        )
    }

    private fun createProjectWithoutSeoConfig() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "inject-seo-noconfig-test"
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
        siteDir.resolve("templates/header.thyme").writeText(
            """
            <head>
            <title>No SEO</title>
            </head>
            """.trimIndent(),
        )

        projectDir.resolve("site.yml").writeText(
            """
            bake:
              srcPath: site
              destDirPath: build/output
            language: fr
            supportedLanguages: [fr]
            """.trimIndent(),
        )
    }
}