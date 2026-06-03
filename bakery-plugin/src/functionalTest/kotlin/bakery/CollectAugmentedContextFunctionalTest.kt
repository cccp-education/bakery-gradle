@file:Suppress("FunctionName")

package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * BKY-LENS-6 — Functional tests for the collectAugmentedContext task.
 *
 * Replaces CollectRelatedArticlesFunctionalTest (BKG legacy, deleted in LENS-3.3).
 * Tests the new LENS pipeline task registration.
 *
 * Note: The legacy CollectRelatedArticlesFunctionalTest tests FAIL because the
 * collectRelatedArticles task no longer exists (superseded by collectAugmentedContext).
 * That file should be deleted or marked @Disabled in a future cleanup session.
 */
class CollectAugmentedContextFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `collectAugmentedContext task is registered under collect group`() {
        setupGradleProject()
        setupSiteContent()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--group", "collect")
            .build()

        assertThat(result.output).contains("collectAugmentedContext")
        assertThat(result.output).contains("collectSiteContext")
        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    // ——— helpers ———

    private fun setupGradleProject() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "lens-test"
            """.trimIndent()
        )

        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("education.cccp.bakery") }
            bakery { configPath = file("site.yml").absolutePath }
            """.trimIndent()
        )

        projectDir.resolve("site.yml").writeText(
            """
            bake: { srcPath: site, destDirPath: build/bake }
            pushPage: { from: site, to: pages }
            pushMaquette: { from: maquette, to: maquette-pages }
            """.trimIndent()
        )
    }

    private fun setupSiteContent() {
        val siteDir = projectDir.resolve("site")
        siteDir.mkdirs()
        siteDir.resolve("jbake.properties").writeText(
            """
            template.folder=templates
            content.folder=content
            render.tags=true
            """.trimIndent()
        )

        val templatesDir = siteDir.resolve("templates")
        templatesDir.mkdirs()
        templatesDir.resolve("menu.thyme").writeText("<html></html>")
        templatesDir.resolve("post.thyme").writeText("<html th:text=\"\${content.body}\"></html>")
        templatesDir.resolve("page.thyme").writeText("<html th:text=\"\${content.body}\"></html>")
        templatesDir.resolve("index.thyme").writeText("<html></html>")
        templatesDir.resolve("tags.thyme").writeText("<html></html>")
        templatesDir.resolve("tag.thyme").writeText("<html></html>")
        templatesDir.resolve("feed.thyme").writeText("<html></html>")
        templatesDir.resolve("archive.thyme").writeText("<html></html>")
        templatesDir.resolve("sitemap.thyme").writeText("<html></html>")
    }
}