package bakery.article

import bakery.llm.FakeLlmService
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.YearMonth
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests pour [GenerateArticleTask] — validation de l'intégration Gradle.
 *
 * Baby-step DDD : utilise ProjectBuilder (pas de GradleRunner) pour éviter
 * la dépendance à une vraie instance Ollama.
 */
class GenerateArticleTaskTest {

    @TempDir
    lateinit var tempDir: File

    private val sampleArticle = """
        = Découvrir Kotlin pour Gradle
        :description: Un guide pratique pour débuter avec Kotlin et Gradle
        :tags: kotlin, gradle, tutoriel
        :date: 2026-05-30

        == Pourquoi Kotlin ?

        Kotlin est moderne, concis et sûr.

        == Premiers pas

        `./gradlew build` compile votre projet.
    """.trimIndent()

    @Test
    fun `task is registered with correct group and description`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .withName("test-article")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("generateArticle", GenerateArticleTask::class.java).get()

        assertNotNull(task)
        assertTrue(task.group == "generate")
        val desc = task.description
        assertTrue(desc != null && desc.contains("Génère un article de blog"))
    }

    @Test
    fun `executeGenerate creates article file in content dir`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .withName("test-article-gen")
            .build()
        project.pluginManager.apply("java-base")

        val contentRoot = tempDir.resolve("site")
        val task = project.tasks.register("generateArticle", GenerateArticleTask::class.java).get()
        task.contentRootDir = contentRoot
        task.topic.set("Découvrir Kotlin pour Gradle")
        task.llmService = FakeLlmService(sampleArticle)

        task.executeGenerate()

        val expectedBlogDir = "content/blog/${YearMonth.now().year}/${YearMonth.now().monthValue}"
        val blogDir = contentRoot.resolve(expectedBlogDir)
        assertTrue(blogDir.exists(), "Blog directory $expectedBlogDir should exist")
        val files = blogDir.listFiles()
        assertNotNull(files)
        assertTrue(files.any { it.name.endsWith(".adoc") }, "Should have at least one .adoc article")
    }

    @Test
    fun `executeGenerate writes AsciiDoc content with metadata`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .withName("test-article-meta")
            .build()
        project.pluginManager.apply("java-base")

        val contentRoot = tempDir.resolve("mysite")
        val task = project.tasks.register("generateArticle", GenerateArticleTask::class.java).get()
        task.contentRootDir = contentRoot
        task.topic.set("Kotlin pour Gradle")
        task.llmService = FakeLlmService(sampleArticle)

        task.executeGenerate()

        val expectedBlogDir = "content/blog/${YearMonth.now().year}/${YearMonth.now().monthValue}"
        val blogDir = contentRoot.resolve(expectedBlogDir)
        val articleFile = blogDir.listFiles()?.firstOrNull { it.name.endsWith(".adoc") }
        assertNotNull(articleFile, "Article file should exist")

        val content = articleFile!!.readText(Charsets.UTF_8)
        assertTrue(content.contains("= Découvrir Kotlin pour Gradle"), "Should contain title")
        assertTrue(content.contains(":description:"), "Should contain description metadata")
        assertTrue(content.contains(":tags:"), "Should contain tags metadata")
        assertTrue(content.contains(":date:"), "Should contain date metadata")
        assertTrue(content.contains(":slug:"), "Should contain slug metadata")
        assertTrue(content.contains(":page-liquid: blog"), "Should contain page-liquid metadata")
    }

    @Test
    fun `executeGenerate fails with meaningful error when no topic set`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .withName("test-article-no-topic")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("generateArticle", GenerateArticleTask::class.java).get()

        val exception = org.junit.jupiter.api.assertThrows<java.lang.IllegalArgumentException> {
            task.executeGenerate()
        }
        assertTrue(exception.message?.contains("-Ptopic") == true)
    }

    @Test
    fun `executeGenerate fails with meaningful error when no llmService injected`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .withName("test-article-no-llm")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("generateArticle", GenerateArticleTask::class.java).get()
        task.topic.set("Sujet test")

        val exception = org.junit.jupiter.api.assertThrows<java.lang.IllegalStateException> {
            task.executeGenerate()
        }
        assertTrue(exception.message?.contains("LlmService") == true)
    }

    @Test
    fun `executeGenerate fails with meaningful error when no contentRootDir configured`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .withName("test-article-no-root")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("generateArticle", GenerateArticleTask::class.java).get()
        task.topic.set("Sujet test")
        task.llmService = FakeLlmService(sampleArticle)

        val exception = org.junit.jupiter.api.assertThrows<java.lang.IllegalStateException> {
            task.executeGenerate()
        }
        assertTrue(exception.message?.contains("contentRootDir") == true)
    }

    @Test
    fun `executeGenerate respects custom topic from CLI`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .withName("test-article-topic")
            .build()
        project.pluginManager.apply("java-base")

        val contentRoot = tempDir.resolve("site")
        val fakeLlm = FakeLlmService(sampleArticle)
        val task = project.tasks.register("generateArticle", GenerateArticleTask::class.java).get()
        task.contentRootDir = contentRoot
        task.topic.set("Kotlin pour Gradle")
        task.llmService = fakeLlm

        task.executeGenerate()

        val prompts = fakeLlm.promptsReceived
        assertTrue(prompts.isNotEmpty(), "LLM should have been called")
        assertTrue(prompts.first().contains("Kotlin pour Gradle"), "Prompt should contain the topic")
    }

    @Test
    fun `executeGenerate creates slug-based filename`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .withName("test-article-slug")
            .build()
        project.pluginManager.apply("java-base")

        val contentRoot = tempDir.resolve("site")
        val task = project.tasks.register("generateArticle", GenerateArticleTask::class.java).get()
        task.contentRootDir = contentRoot
        task.topic.set("Découvrir Kotlin pour Gradle")
        task.llmService = FakeLlmService(sampleArticle)

        task.executeGenerate()

        val expectedBlogDir = "content/blog/${YearMonth.now().year}/${YearMonth.now().monthValue}"
        val blogDir = contentRoot.resolve(expectedBlogDir)
        val articleFile = blogDir.listFiles()?.firstOrNull { it.name.endsWith(".adoc") }
        assertNotNull(articleFile)

        assertTrue(articleFile!!.name.startsWith("decouvrir-kotlin-pour-gradle"),
            "Filename should be based on slug: ${articleFile.name}")
    }
}
