@file:Suppress("FunctionName")

package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CollectSiteContextFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    private fun setupMinimalProject(additionalDsl: String = "") {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "collect-test"
            """.trimIndent()
        )

        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("education.cccp.bakery") }
            bakery {
                configPath = file("site.yml").absolutePath
                $additionalDsl
            }
            """.trimIndent()
        )

        projectDir.resolve("site.yml").writeText(
            """
            bake: { srcPath: site, destDirPath: build/bake }
            pushPage: { from: site, to: pages }
            pushMaquette: { from: maquette, to: maquette-pages }
            """.trimIndent()
        )

        val siteDir = projectDir.resolve("site")
        siteDir.mkdirs()
        siteDir.resolve("jbake.properties").writeText(
            """
            template.folder=templates
            content.folder=content
            """.trimIndent()
        )
    }

    @Test
    fun `collectSiteContext task is registered under collect group`() {
        setupMinimalProject()

        val siteDir = projectDir.resolve("site")
        val contentDir = siteDir.resolve("content")
        contentDir.mkdirs()
        contentDir.resolve("index.adoc").writeText("= Hello\n\nWorld")

        val templatesDir = siteDir.resolve("templates")
        templatesDir.mkdirs()
        templatesDir.resolve("menu.thyme").writeText("<html></html>")

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--group", "collect")
            .build()

        assertThat(result.output).contains("collectSiteContext")
        assertThat(result.output).contains("Collecte le contexte du site baké")
        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    // ──────────────────────────────────────
    // BKY-LENS-5 : Functional Tests — Augmented Context dans collectSiteContext
    // ──────────────────────────────────────

    @Nested
    @DisplayName("BKY-LENS-5 : CollectSiteContext with Augmented Context")
    inner class AugmentedContextFunctionalTests {

        @Test
        @DisplayName("collectSiteContext avec augmentedContext enabled → tâche enregistrée")
        fun `collectSiteContext with augmented context enabled registers task`() {
            setupMinimalProject(
                """
                augmentedContext {
                    enabled = true
                }
                """.trimIndent()
            )

            val result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("tasks", "--group", "collect")
                .build()

            assertThat(result.output).contains("collectSiteContext")
            assertThat(result.output).contains("BUILD SUCCESSFUL")
        }

        @Test
        @DisplayName("collectSiteContext avec augmentedContext disabled → tâche enregistrée (sans enriched metadata)")
        fun `collectSiteContext with augmented context disabled still registers task`() {
            setupMinimalProject(
                """
                augmentedContext {
                    enabled = false
                }
                """.trimIndent()
            )

            val result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("tasks", "--group", "collect")
                .build()

            assertThat(result.output).contains("collectSiteContext")
            assertThat(result.output).contains("BUILD SUCCESSFUL")
        }

        @Test
        @DisplayName("collectAugmentedContext avec augmentedContext enabled → tâche enregistrée")
        fun `collectAugmentedContext task registered when enabled`() {
            setupMinimalProject(
                """
                augmentedContext {
                    enabled = true
                }
                """.trimIndent()
            )

            val result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("tasks", "--group", "collect")
                .build()

            assertThat(result.output).contains("collectAugmentedContext")
            assertThat(result.output).contains("BUILD SUCCESSFUL")
        }
    }
}
