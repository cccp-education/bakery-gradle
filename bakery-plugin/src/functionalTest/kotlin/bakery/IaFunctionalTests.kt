@file:Suppress("FunctionName")

package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class IaFunctionalTests {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `DSL ia block compiles with default values`() {
        createProjectWithIaBlock()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks")
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `DSL ia block compiles with custom values`() {
        createProjectWithIaBlock(
            baseUrl = "https://ollama.custom.example.com:11434",
            modelName = "custom-model:7b"
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks")
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `DSL ia block with disabled flag compiles without error`() {
        createProjectWithIaBlock(enabled = false)

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks")
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `missing ia block does not break existing tasks`() {
        // Projet sans bloc ia (backward compat)
        projectDir.resolve("settings.gradle.kts").writeText("""
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "ia-test"
        """.trimIndent())

        projectDir.resolve("build.gradle.kts").writeText("""
            plugins { id("education.cccp.bakery") }
            bakery { configPath = file("site.yml").absolutePath }
        """.trimIndent())

        projectDir.resolve("site.yml").writeText("bake: { srcPath: src, destDirPath: build/bake }")

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks")
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    private fun createProjectWithIaBlock(
        baseUrl: String? = null,
        modelName: String? = null,
        enabled: Boolean? = null
    ) {
        projectDir.resolve("settings.gradle.kts").writeText("""
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "ia-test"
        """.trimIndent())

        val iaBlock = buildString {
            appendLine("    ia {")
            baseUrl?.let { appendLine("        baseUrl = \"$it\"") }
            modelName?.let { appendLine("        modelName = \"$it\"") }
            enabled?.let { appendLine("        enabled = $it") }
            appendLine("    }")
        }

        projectDir.resolve("build.gradle.kts").writeText("""
            plugins { id("education.cccp.bakery") }
            bakery {
                configPath = file("site.yml").absolutePath
$iaBlock
            }
        """.trimIndent())

        projectDir.resolve("site.yml").writeText("bake: { srcPath: src, destDirPath: build/bake }")
    }
}
