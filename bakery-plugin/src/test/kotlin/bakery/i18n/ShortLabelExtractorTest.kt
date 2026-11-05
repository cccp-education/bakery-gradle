package bakery.i18n

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * BKY-OUT-3 (i18n_review S-263) — the short French label under the prose floor.
 *
 * The template guard needed three tokens, so a two-token label (`Me Contacter`,
 * `Innovation Pédagogique`) stayed French in every variant, invisible and frozen.
 * The prose floor stays global on purpose (technology names and language codes
 * share short tokens); this second predicate is only used by the template guard.
 */
class ShortLabelExtractorTest {
    @Test
    fun `a two-token French label with a diacritic is a short label`() {
        assertTrue(VisibleTextExtractor.isShortFrenchLabel("Innovation Pédagogique"))
        assertTrue(VisibleTextExtractor.isShortFrenchLabel("Développeur Formateur"))
    }

    @Test
    fun `a two-token French-only wording is a short label`() {
        assertTrue(VisibleTextExtractor.isShortFrenchLabel("Me Contacter"))
        assertTrue(VisibleTextExtractor.isShortFrenchLabel("Mes Projets"))
        assertFalse(VisibleTextExtractor.isShortFrenchLabel("En savoir plus"))
    }

    @Test
    fun `a technology name is never a short label`() {
        assertFalse(VisibleTextExtractor.isShortFrenchLabel("React"))
        assertFalse(VisibleTextExtractor.isShortFrenchLabel("Spring Boot"))
        assertFalse(VisibleTextExtractor.isShortFrenchLabel("GitHub Actions"))
        assertFalse(VisibleTextExtractor.isShortFrenchLabel("Contact"))
        assertFalse(VisibleTextExtractor.isShortFrenchLabel("Blog"))
    }

    @Test
    fun `a three-token run belongs to the prose rule, not this one`() {
        assertFalse(VisibleTextExtractor.isShortFrenchLabel("Une expérience concrète"))
        assertTrue(VisibleTextExtractor.isFrenchProse("Une expérience concrète"))
    }

    @Test
    fun `the extractor yields the short labels of a template only`() {
        val template =
            """
            <header>
              <h2>Innovation Pédagogique</h2>
              <a href="/x">Me Contacter</a>
              <span>Contact</span>
              <span>Spring Boot</span>
            </header>
            """.trimIndent()

        assertEquals(
            listOf("Innovation Pédagogique", "Me Contacter"),
            VisibleTextExtractor.extractShortLabels(template),
        )
    }

    @Test
    fun `script and style bodies are never short labels`() {
        val template = "<script>var titre = \"Équipe Pédagogique\";</script><style>.a{content:\"Équipe\"}</style>"

        assertTrue(VisibleTextExtractor.extractShortLabels(template).isEmpty())
    }
}
