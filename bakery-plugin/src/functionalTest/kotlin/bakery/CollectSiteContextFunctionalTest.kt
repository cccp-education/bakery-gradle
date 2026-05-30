@file:Suppress("FunctionName")

package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CollectSiteContextFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `collectSiteContext task is registered under collect group`() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "collect-test"
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

        val siteDir = projectDir.resolve("site")
        siteDir.mkdirs()
        siteDir.resolve("jbake.properties").writeText(
            """
            template.folder=templates
            content.folder=content
            """.trimIndent()
        )

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
}
