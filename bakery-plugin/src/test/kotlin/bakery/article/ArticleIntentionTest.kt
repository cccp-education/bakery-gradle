package bakery.article

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests unitaires pour [ArticleIntention] — modèle domaine DDD.
 *
 * Baby-step TDD : chaque test définit une règle métier avant l'implémentation.
 */
class ArticleIntentionTest {

    // ── Construction valide ────────────────────────────────────────────

    @Test
    fun `minimal intention with topic only has default values`() {
        val intention = ArticleIntention(topic = "Introduction à Kotlin")

        assertEquals("Introduction à Kotlin", intention.topic)
        assertEquals(ArticleTon.INFORMATIF, intention.ton)
        assertEquals(ArticleAudience.GENERAL, intention.audience)
        assertTrue(intention.keywords.isEmpty())
        assertTrue(intention.lang == "fr")
    }

    @Test
    fun `full intention with all parameters`() {
        val intention = ArticleIntention(
            topic = "Kotlin Coroutines",
            ton = ArticleTon.TECHNIQUE,
            audience = ArticleAudience.DEVELOPPEUR,
            rawKeywords = listOf("coroutines", "async", "flow"),
            lang = "en"
        )

        assertEquals("Kotlin Coroutines", intention.topic)
        assertEquals(ArticleTon.TECHNIQUE, intention.ton)
        assertEquals(ArticleAudience.DEVELOPPEUR, intention.audience)
        assertEquals(listOf("coroutines", "async", "flow"), intention.keywords)
        assertEquals("en", intention.lang)
    }

    // ── Validation topic obligatoire ───────────────────────────────────

    @Test
    fun `intention requires non-blank topic`() {
        assertThrows<IllegalArgumentException> {
            ArticleIntention(topic = "")
        }
    }

    @Test
    fun `intention requires non-whitespace-only topic`() {
        assertThrows<IllegalArgumentException> {
            ArticleIntention(topic = "   ")
        }
    }

    // ── Enum ArticleTon ────────────────────────────────────────────────

    @Test
    fun `ArticleTon has expected values`() {
        val tons = ArticleTon.entries
        assertEquals(4, tons.size)
        assertTrue(tons.contains(ArticleTon.INFORMATIF))
        assertTrue(tons.contains(ArticleTon.TECHNIQUE))
        assertTrue(tons.contains(ArticleTon.PEDAGOGIQUE))
        assertTrue(tons.contains(ArticleTon.CONVAINCRE))
    }

    @Test
    fun `ArticleTon has readable labels`() {
        assertEquals("informatif", ArticleTon.INFORMATIF.label)
        assertEquals("technique", ArticleTon.TECHNIQUE.label)
        assertEquals("pédagogique", ArticleTon.PEDAGOGIQUE.label)
        assertEquals("convaincre", ArticleTon.CONVAINCRE.label)
    }

    // ── Enum ArticleAudience ──────────────────────────────────────────

    @Test
    fun `ArticleAudience has expected values`() {
        val audiences = ArticleAudience.entries
        assertEquals(3, audiences.size)
        assertTrue(audiences.contains(ArticleAudience.GENERAL))
        assertTrue(audiences.contains(ArticleAudience.DEVELOPPEUR))
        assertTrue(audiences.contains(ArticleAudience.FORMATEUR))
    }

    @Test
    fun `ArticleAudience has readable labels`() {
        assertEquals("grand public", ArticleAudience.GENERAL.label)
        assertEquals("développeur", ArticleAudience.DEVELOPPEUR.label)
        assertEquals("formateur", ArticleAudience.FORMATEUR.label)
    }

    // ── Keywords validation ────────────────────────────────────────────

    @Test
    fun `keywords are trimmed and empty ones removed`() {
        val intention = ArticleIntention(
            topic = "Kotlin",
            rawKeywords = listOf("  coroutines  ", "", "  flow  ", "  ")
        )
        assertEquals(listOf("coroutines", "flow"), intention.keywords)
    }

    @Test
    fun `intention with duplicate keywords deduplicates them`() {
        val intention = ArticleIntention(
            topic = "Kotlin",
            rawKeywords = listOf("coroutines", "coroutines", "flow")
        )
        assertEquals(listOf("coroutines", "flow"), intention.keywords)
    }

    // ── Lang validation ────────────────────────────────────────────────

    @Test
    fun `lang defaults to fr`() {
        val intention = ArticleIntention(topic = "Test")
        assertEquals("fr", intention.lang)
    }

    @Test
    fun `lang accepts en and fr only`() {
        val frIntention = ArticleIntention(topic = "Test", lang = "fr")
        val enIntention = ArticleIntention(topic = "Test", lang = "en")
        assertEquals("fr", frIntention.lang)
        assertEquals("en", enIntention.lang)
    }

    @Test
    fun `lang rejects unsupported language`() {
        assertThrows<IllegalArgumentException> {
            ArticleIntention(topic = "Test", lang = "de")
        }
    }

    // ── toPromptContext ────────────────────────────────────────────────

    @Test
    fun `toPromptContext includes topic and audience guidance`() {
        val intention = ArticleIntention(
            topic = "Kotlin pour Gradle",
            ton = ArticleTon.TECHNIQUE,
            audience = ArticleAudience.DEVELOPPEUR,
            rawKeywords = listOf("gradle", "dsl")
        )
        val context = intention.toPromptContext()

        assertTrue(context.contains("Kotlin pour Gradle"), "Must contain topic")
        assertTrue(context.contains("développeur"), "Must contain audience label")
        assertTrue(context.contains("technique"), "Must contain ton label")
        assertTrue(context.contains("gradle"), "Must contain keyword")
        assertTrue(context.contains("dsl"), "Must contain keyword")
    }

    @Test
    fun `toPromptContext with minimal intention has defaults`() {
        val intention = ArticleIntention(topic = "Test simple")
        val context = intention.toPromptContext()

        assertTrue(context.contains("Test simple"))
        assertTrue(context.contains("grand public"))
        assertTrue(context.contains("informatif"))
        assertFalse(context.contains("Mots-clés"), "No keywords section when empty")
    }
}