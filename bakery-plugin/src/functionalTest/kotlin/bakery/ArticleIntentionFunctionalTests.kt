@file:Suppress("FunctionName")

package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests fonctionnels pour l'EPIC BKY-JB-8 — Article IA.
 *
 * Valide que le DSL `bakery { articleIntention { ... } }` compile
 * et que la tâche `generateArticle` est correctement configurée.
 */
class ArticleIntentionFunctionalTests {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `DSL articleIntention block compiles with topic`() {
        createProjectWithArticleIntention(
            topic = "Kotlin pour Gradle"
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks")
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `DSL articleIntention block compiles with full config`() {
        createProjectWithArticleIntention(
            topic = "Kotlin Coroutines",
            ton = "technique",
            audience = "developpeur",
            keywords = "suspend, flow",
            lang = "en"
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks")
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `DSL articleIntention with pedagogique ton compiles`() {
        createProjectWithArticleIntention(
            topic = "Introduction à Gradle",
            ton = "pedagogique",
            audience = "formateur"
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks")
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `generateArticle task is registered with articleIntention DSL`() {
        createProjectWithArticleIntention(
            topic = "Test article"
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--group", "generate")
            .build()

        assertThat(result.output).contains("generateArticle")
    }

    private fun createProjectWithArticleIntention(
        topic: String,
        ton: String? = null,
        audience: String? = null,
        keywords: String? = null,
        lang: String? = null
    ) {
        projectDir.resolve("settings.gradle.kts").writeText("""
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "article-intention-test"
        """.trimIndent())

        val articleIntentionBlock = buildString {
            appendLine("    articleIntention {")
            appendLine("        topic = \"$topic\"")
            ton?.let { appendLine("        ton = \"$it\"") }
            audience?.let { appendLine("        audience = \"$it\"") }
            keywords?.let { appendLine("        keywords = listOf(${it.split(",").joinToString(", ") { "\"${it.trim()}\"" }})") }
            lang?.let { appendLine("        lang = \"$it\"") }
            appendLine("    }")
        }

        projectDir.resolve("build.gradle.kts").writeText("""
            plugins { id("education.cccp.bakery") }
            bakery {
                configPath = file("site.yml").absolutePath
$articleIntentionBlock
            }
        """.trimIndent())

        projectDir.resolve("site.yml").writeText("bake: { srcPath: src, destDirPath: build/bake }")
    }
}