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
    private val sampleAsciiDoc =
        """
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
    fun `generate produces ArticleOutput with parsed title`() =
        runBlocking {
            val fakeLlm = FakeLlmService(sampleAsciiDoc)
            val generator = ArticleGenerator()

            val article = generator.generate("Kotlin pour Gradle", fakeLlm)

            assertNotNull(article)
            assertEquals("Introduction à Kotlin pour les plugins Gradle", article.titre)
        }

    @Test
    fun `generate produces ArticleOutput with parsed description`() =
        runBlocking {
            val fakeLlm = FakeLlmService(sampleAsciiDoc)
            val generator = ArticleGenerator()

            val article = generator.generate("Kotlin pour Gradle", fakeLlm)

            assertEquals(
                "Découvrez les bases de Kotlin pour créer des plugins Gradle robustes",
                article.description,
            )
        }

    @Test
    fun `generate produces ArticleOutput with parsed tags`() =
        runBlocking {
            val fakeLlm = FakeLlmService(sampleAsciiDoc)
            val generator = ArticleGenerator()

            val article = generator.generate("Kotlin pour Gradle", fakeLlm)

            assertEquals(listOf("kotlin", "gradle", "plugin", "tutoriel"), article.tags)
        }

    @Test
    fun `generate produces ArticleOutput with today date by default`() =
        runBlocking {
            val fakeLlm = FakeLlmService(sampleAsciiDoc)
            val generator = ArticleGenerator()

            val article = generator.generate("Kotlin pour Gradle", fakeLlm)

            assertEquals(LocalDate.of(2026, 5, 30), article.date)
        }

    @Test
    fun `generate produces ArticleOutput with slug from title`() =
        runBlocking {
            val fakeLlm = FakeLlmService(sampleAsciiDoc)
            val generator = ArticleGenerator()

            val article = generator.generate("Kotlin pour Gradle", fakeLlm)

            assertEquals("introduction-a-kotlin-pour-les-plugins-gradle", article.slug)
        }

    @Test
    fun `generate produces ArticleOutput with body containing content`() =
        runBlocking {
            val fakeLlm = FakeLlmService(sampleAsciiDoc)
            val generator = ArticleGenerator()

            val article = generator.generate("Kotlin pour Gradle", fakeLlm)

            assertTrue(article.body.contains("Pourquoi Kotlin pour Gradle ?"))
            assertTrue(article.body.contains("Créer son premier plugin"))
            assertTrue(article.body.contains("Conclusion"))
        }

    @Test
    fun `generate sends prompt containing the topic to LLM`() =
        runBlocking {
            val fakeLlm = FakeLlmService(sampleAsciiDoc)
            val generator = ArticleGenerator()

            generator.generate("Kotlin pour Gradle", fakeLlm)

            assertTrue(fakeLlm.promptsReceived.isNotEmpty())
            assertTrue(fakeLlm.promptsReceived.first().contains("Kotlin pour Gradle"))
        }

    @Test
    fun `generate handles empty response gracefully`() =
        runBlocking {
            val fakeLlm = FakeLlmService("") // réponse vide
            val generator = ArticleGenerator()

            val article = generator.generate("Sujet quelconque", fakeLlm)

            assertNotNull(article)
            assertFalse(article.titre.isBlank(), "Doit générer une article même sur réponse vide")
        }

    @Test
    fun `generate builds prompt with AsciiDoc format instruction`() =
        runBlocking {
            val fakeLlm = FakeLlmService(sampleAsciiDoc)
            val generator = ArticleGenerator()

            generator.generate("Kotlin pour Gradle", fakeLlm)

            val prompt = fakeLlm.promptsReceived.first()
            assertTrue(prompt.contains("AsciiDoc"), "Le prompt doit demander du format AsciiDoc")
            assertTrue(prompt.contains(":tags:"), "Le prompt doit demander les metadata :tags:")
            assertTrue(prompt.contains(":description:"), "Le prompt doit demander :description:")
        }

    // ── generate(ArticleIntention) — BKY-JB-8 ────────────────────────────

    @Test
    fun `generate with intention includes audience in prompt`() =
        runBlocking {
            val fakeLlm = FakeLlmService(sampleAsciiDoc)
            val generator = ArticleGenerator()

            val intention =
                ArticleIntention(
                    topic = "Kotlin pour Gradle",
                    audience = ArticleAudience.DEVELOPPEUR,
                )
            generator.generate(intention, fakeLlm)

            val prompt = fakeLlm.promptsReceived.first()
            assertTrue(prompt.contains("développeur"), "Le prompt doit contenir l'audience")
        }

    @Test
    fun `generate with intention includes ton in prompt`() =
        runBlocking {
            val fakeLlm = FakeLlmService(sampleAsciiDoc)
            val generator = ArticleGenerator()

            val intention =
                ArticleIntention(
                    topic = "Kotlin pour Gradle",
                    ton = ArticleTon.PEDAGOGIQUE,
                )
            generator.generate(intention, fakeLlm)

            val prompt = fakeLlm.promptsReceived.first()
            assertTrue(prompt.contains("pédagogique"), "Le prompt doit contenir le ton")
        }

    @Test
    fun `generate with intention includes keywords in prompt`() =
        runBlocking {
            val fakeLlm = FakeLlmService(sampleAsciiDoc)
            val generator = ArticleGenerator()

            val intention =
                ArticleIntention(
                    topic = "Kotlin pour Gradle",
                    rawKeywords = listOf("dsl", "plugin"),
                )
            generator.generate(intention, fakeLlm)

            val prompt = fakeLlm.promptsReceived.first()
            assertTrue(prompt.contains("dsl"), "Le prompt doit contenir le mot-clé 'dsl'")
            assertTrue(prompt.contains("plugin"), "Le prompt doit contenir le mot-clé 'plugin'")
        }

    @Test
    fun `generate with intention includes language guidance in prompt`() =
        runBlocking {
            val fakeLlm = FakeLlmService(sampleAsciiDoc)
            val generator = ArticleGenerator()

            val intention =
                ArticleIntention(
                    topic = "Kotlin for Gradle",
                    lang = "en",
                )
            generator.generate(intention, fakeLlm)

            val prompt = fakeLlm.promptsReceived.first()
            assertTrue(prompt.contains("en"), "Le prompt doit contenir la langue 'en'")
        }

    @Test
    fun `generate with intention delegates to same parseResponse`() =
        runBlocking {
            val fakeLlm = FakeLlmService(sampleAsciiDoc)
            val generator = ArticleGenerator()

            val intention =
                ArticleIntention(
                    topic = "Kotlin pour Gradle",
                    ton = ArticleTon.TECHNIQUE,
                    audience = ArticleAudience.DEVELOPPEUR,
                )
            val article = generator.generate(intention, fakeLlm)

            assertEquals("Introduction à Kotlin pour les plugins Gradle", article.titre)
            assertEquals("introduction-a-kotlin-pour-les-plugins-gradle", article.slug)
        }

    @Test
    fun `generate with minimal intention uses defaults in prompt`() =
        runBlocking {
            val fakeLlm = FakeLlmService(sampleAsciiDoc)
            val generator = ArticleGenerator()

            val intention = ArticleIntention(topic = "Test simple")
            generator.generate(intention, fakeLlm)

            val prompt = fakeLlm.promptsReceived.first()
            assertTrue(prompt.contains("informatif"), "Default ton must appear in prompt")
            assertTrue(prompt.contains("grand public"), "Default audience must appear in prompt")
            assertTrue(prompt.contains("fr"), "Default lang must appear in prompt")
        }

    @Test
    fun `buildPrompt from intention includes all contextual guidance`() =
        runBlocking {
            val generator = ArticleGenerator()

            val intention =
                ArticleIntention(
                    topic = "Kotlin Coroutines",
                    ton = ArticleTon.TECHNIQUE,
                    audience = ArticleAudience.DEVELOPPEUR,
                    rawKeywords = listOf("suspend", "flow"),
                    lang = "en",
                )
            val prompt = generator.buildPrompt(intention)

            assertTrue(prompt.contains("Kotlin Coroutines"), "Must contain topic")
            assertTrue(prompt.contains("technique"), "Must contain ton")
            assertTrue(prompt.contains("développeur"), "Must contain audience")
            assertTrue(prompt.contains("suspend"), "Must contain keyword")
            assertTrue(prompt.contains("en"), "Must contain language")
            assertTrue(prompt.contains("AsciiDoc"), "Must contain format instruction")
        }

    // ── GBL-006 § 7 — langue de l'article : 22 langues, pas 2 ──────────────

    @Test
    fun `buildPrompt from intention names the Hindi language for lang hi`() {
        val generator = ArticleGenerator()

        val intention = ArticleIntention(topic = "Devenir formateur", lang = "hi")
        val prompt = generator.buildPrompt(intention)

        assertTrue(
            prompt.contains("Hindi"),
            "Le prompt doit nommer la langue 'Hindi' pour lang='hi' — obtenu : ${prompt.lineSequence().first { it.contains("language", ignoreCase = true) || it.contains("langue", ignoreCase = true) }}",
        )
        assertTrue(prompt.contains("hi"), "Le prompt doit contenir le code 'hi'")
    }

    @Test
    fun `buildPrompt from intention does not claim French for a non French language`() {
        val generator = ArticleGenerator()

        val intention = ArticleIntention(topic = "Devenir formateur", lang = "bn")
        val prompt = generator.buildPrompt(intention)

        assertTrue(prompt.contains("Bengali"), "Le prompt doit nommer 'Bengali' pour lang='bn'")
        assertFalse(
            prompt.contains("en français"),
            "Le prompt ne doit pas demander le français pour lang='bn'",
        )
    }

    @Test
    fun `buildPrompt from intention names each of the 22 catalog languages`() {
        val generator = ArticleGenerator()

        contracts.i18n.LanguageCatalog.ALL.forEach { language ->
            val intention = ArticleIntention(topic = "Sujet", lang = language.code)
            val prompt = generator.buildPrompt(intention)
            assertTrue(
                prompt.contains(language.name),
                "Le prompt doit nommer '${language.name}' pour lang='${language.code}'",
            )
            assertTrue(
                prompt.contains(language.code),
                "Le prompt doit contenir le code '${language.code}'",
            )
        }
    }
}
