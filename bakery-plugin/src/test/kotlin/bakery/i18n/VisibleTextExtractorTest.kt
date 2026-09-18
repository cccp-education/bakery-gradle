package bakery.i18n

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * CHE-I18N-22 US-7a — the visible text extractor must see bare text nodes mixed
 * with inline markup (the hero headline stayed French), while never leaking tag
 * attributes, comments, or `<script>`/`<style>` bodies.
 */
class VisibleTextExtractorTest {

    @Test
    fun `a bare text node mixed with an inline span is extracted`() {
        val template = """
            <h1 class="display-5 fw-bold mb-5">
                Développeur
                <span class="text-primary">spécialisé en Ingénierie Pédagogique</span>
            </h1>
        """.trimIndent()

        val segments = VisibleTextExtractor.extract(template)

        assertTrue(segments.contains("Développeur"), "Bare text must be extracted: $segments")
        assertTrue(segments.contains("spécialisé en Ingénierie Pédagogique"), "Span text must be extracted: $segments")
    }

    @Test
    fun `a whole element body is extracted`() {
        val template = """<p class="lead">Passionné par l'innovation.</p>"""

        val segments = VisibleTextExtractor.extract(template)

        assertEquals(listOf("Passionné par l'innovation."), segments)
    }

    @Test
    fun `tag attributes are never extracted`() {
        val template = """<a th:href="${'$'}{root}index.html" data-lang="fr" title="Français">Français</a>"""

        val segments = VisibleTextExtractor.extract(template)

        assertEquals(listOf("Français"), segments)
    }

    @Test
    fun `script and style bodies are ignored`() {
        val template =
            """
            <script>var label = "Développeur";</script>
            <style>.hero::after { content: "Innovation"; }</style>
            <h1>Développeur</h1>
            """.trimIndent()

        val segments = VisibleTextExtractor.extract(template)

        assertEquals(listOf("Développeur"), segments)
    }

    @Test
    fun `html comments are ignored`() {
        val template =
            """
            <!-- Mes Services -->
            <h1>Développeur</h1>
            """.trimIndent()

        val segments = VisibleTextExtractor.extract(template)

        assertEquals(listOf("Développeur"), segments)
    }

    @Test
    fun `whitespace only runs are ignored`() {
        val template = "<div>\n   \n\t\n</div>"

        val segments = VisibleTextExtractor.extract(template)

        assertTrue(segments.isEmpty(), "Whitespace must not be extracted: $segments")
    }

    @Test
    fun `runs shorter than two characters are ignored`() {
        val template = "<span>/</span><span>ok</span>"

        val segments = VisibleTextExtractor.extract(template)

        assertEquals(listOf("ok"), segments)
    }

    @Test
    fun `duplicate segments are collapsed preserving first occurrence order`() {
        val template = "<h1>Développeur</h1><p>Texte</p><footer>Développeur</footer>"

        val segments = VisibleTextExtractor.extract(template)

        assertEquals(listOf("Développeur", "Texte"), segments)
    }

    @Test
    fun `an empty template yields no segments`() {
        assertTrue(VisibleTextExtractor.extract("").isEmpty())
    }

    @Test
    fun `text entities are kept verbatim`() {
        val template = "<p>Recherche &amp; innovation</p>"

        val segments = VisibleTextExtractor.extract(template)

        assertEquals(listOf("Recherche &amp; innovation"), segments)
    }

    @Test
    fun `an unclosed tag does not drop the preceding text`() {
        val template = "<h1>Développeur"

        val segments = VisibleTextExtractor.extract(template)

        assertTrue(segments.contains("Développeur"), "Text before an unclosed tag must survive: $segments")
        assertFalse(segments.any { it.contains("<") }, "No markup may leak into a segment: $segments")
    }
}
