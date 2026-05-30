package bakery.article

import bakery.llm.FakeLlmService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests unitaires pour [ArticleGenerator] — domaine métier pur, zéro Gradle.
 *
 * Baby-step 🔴 RED : ces tests échouent (ArticleGenerator n'existe pas encore).
 */
class ArticleGeneratorTest {

    // ── Sample AsciiDoc response from LLM ──────────────────────────────────
    private val sampleAsciiDoc = """
        = Introduction à Kotlin pour les plugins Gradle
        :description: Découvrez les bases de Kotlin pour créer des plugins Gradle robustes
        :tags: kotlin, gradle, plugin, tutoriel
        :date: 2026-05-30

        == Pourquoi Kotlin pour Gradle ?

        Kotlin est un langage moderne qui s'intègre nativement avec Gradle.
        Il offre des fonctionnalités de programmation fonctionnelle et orientée objet.

        == Créer son premier plugin

        `src/main/kotlin/` contient le code source de votre plugin.

        [source,kotlin]
        ----
        class MonPlugin : Plugin<Project> {
            override fun apply(project: Project) {
                project.tasks.register("hello") {
                    it.doLast { println("Hello!") }
                }
            }
        }
        ----

        == Conclusion

        Kotlin + Gradle = une combinaison puissante pour l'automatisation.
    """.trimIndent()

    @Test
    fun `generate produces ArticleOutput with parsed title`() = runBlocking {
        val fakeLlm = FakeLlmService(sampleAsciiDoc)
        val generator = ArticleGenerator()

        val article = generator.generate("Kotlin pour Gradle", fakeLlm)

        assertNotNull(article)
        assertEquals("Introduction à Kotlin pour les plugins Gradle", article.titre)
    }

    @Test
    fun `generate produces ArticleOutput with parsed description`() = runBlocking {
        val fakeLlm = FakeLlmService(sampleAsciiDoc)
        val generator = ArticleGenerator()

        val article = generator.generate("Kotlin pour Gradle", fakeLlm)

        assertEquals(
            "Découvrez les bases de Kotlin pour créer des plugins Gradle robustes",
            article.description
        )
    }

    @Test
    fun `generate produces ArticleOutput with parsed tags`() = runBlocking {
        val fakeLlm = FakeLlmService(sampleAsciiDoc)
        val generator = ArticleGenerator()

        val article = generator.generate("Kotlin pour Gradle", fakeLlm)

        assertEquals(listOf("kotlin", "gradle", "plugin", "tutoriel"), article.tags)
    }

    @Test
    fun `generate produces ArticleOutput with today date by default`() = runBlocking {
        val fakeLlm = FakeLlmService(sampleAsciiDoc)
        val generator = ArticleGenerator()

        val article = generator.generate("Kotlin pour Gradle", fakeLlm)

        assertEquals(LocalDate.of(2026, 5, 30), article.date)
    }

    @Test
    fun `generate produces ArticleOutput with slug from title`() = runBlocking {
        val fakeLlm = FakeLlmService(sampleAsciiDoc)
        val generator = ArticleGenerator()

        val article = generator.generate("Kotlin pour Gradle", fakeLlm)

        assertEquals("introduction-a-kotlin-pour-les-plugins-gradle", article.slug)
    }

    @Test
    fun `generate produces ArticleOutput with body containing content`() = runBlocking {
        val fakeLlm = FakeLlmService(sampleAsciiDoc)
        val generator = ArticleGenerator()

        val article = generator.generate("Kotlin pour Gradle", fakeLlm)

        assertTrue(article.body.contains("Pourquoi Kotlin pour Gradle ?"))
        assertTrue(article.body.contains("Créer son premier plugin"))
        assertTrue(article.body.contains("Conclusion"))
    }

    @Test
    fun `generate sends prompt containing the topic to LLM`() = runBlocking {
        val fakeLlm = FakeLlmService(sampleAsciiDoc)
        val generator = ArticleGenerator()

        generator.generate("Kotlin pour Gradle", fakeLlm)

        assertTrue(fakeLlm.promptsReceived.isNotEmpty())
        assertTrue(fakeLlm.promptsReceived.first().contains("Kotlin pour Gradle"))
    }

    @Test
    fun `generate handles empty response gracefully`() = runBlocking {
        val fakeLlm = FakeLlmService("") // réponse vide
        val generator = ArticleGenerator()

        val article = generator.generate("Sujet quelconque", fakeLlm)

        assertNotNull(article)
        assertFalse(article.titre.isBlank(), "Doit générer une article même sur réponse vide")
    }

    @Test
    fun `generate builds prompt with AsciiDoc format instruction`() = runBlocking {
        val fakeLlm = FakeLlmService(sampleAsciiDoc)
        val generator = ArticleGenerator()

        generator.generate("Kotlin pour Gradle", fakeLlm)

        val prompt = fakeLlm.promptsReceived.first()
        assertTrue(prompt.contains("AsciiDoc"), "Le prompt doit demander du format AsciiDoc")
        assertTrue(prompt.contains(":tags:"), "Le prompt doit demander les metadata :tags:")
        assertTrue(prompt.contains(":description:"), "Le prompt doit demander :description:")
    }
}
