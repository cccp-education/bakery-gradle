package bakery.i18n

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * CHE-I18N-QUALITY — the contact form stayed French in every variant because
 * [VisibleTextExtractor] only sees text nodes: a `placeholder="Nom"` or an
 * `aria-label="Retour en haut de page"` was never sent to the model.
 *
 * This extractor yields the *human-readable* attribute values only
 * (`placeholder`, `alt`, `title`, `aria-label`); structural, technical and
 * already-interpolated values are never scheduled.
 *
 * Pure domain: no I/O, no LLM, no Gradle.
 */
class VisibleAttributeExtractorTest {

    @Test
    fun `a placeholder is extracted`() {
        val template = """<input type="text" name="name" placeholder="Nom" required />"""

        val segments = VisibleAttributeExtractor.extract(template)

        assertEquals(listOf("Nom"), segments)
    }

    @Test
    fun `an aria-label is extracted`() {
        val template = """<a href="#" aria-label="Retour en haut de page"><i class="bi"></i></a>"""

        val segments = VisibleAttributeExtractor.extract(template)

        assertEquals(listOf("Retour en haut de page"), segments)
    }

    @Test
    fun `alt and title are extracted`() {
        val template = """<img src="x.svg" alt="Innovation Pédagogique"/><a title="Cheroliv — Flux RSS">RSS</a>"""

        val segments = VisibleAttributeExtractor.extract(template)

        assertTrue(segments.contains("Innovation Pédagogique"), "alt must be extracted: $segments")
        assertTrue(segments.contains("Cheroliv — Flux RSS"), "title must be extracted: $segments")
    }

    @Test
    fun `structural attributes are never extracted`() {
        val template =
            """
            <a class="dropdown-item" href="en/index.html" data-lang="fr" id="x"
               th:href="${'$'}{link}" rel="alternate" aria-hidden="true" data-bs-theme="light">Français</a>
            """.trimIndent()

        val segments = VisibleAttributeExtractor.extract(template)

        assertEquals(emptyList(), segments)
    }

    @Test
    fun `technical meta content is never extracted`() {
        val template =
            """
            <meta property="og:locale" content="fr_FR"/>
            <meta name="robots" content="noindex, follow"/>
            <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
            <meta name="twitter:card" content="summary"/>
            """.trimIndent()

        val segments = VisibleAttributeExtractor.extract(template)

        assertEquals(emptyList(), segments)
    }

    @Test
    fun `an already interpolated attribute is never extracted`() {
        val template = """<input placeholder="#{contact.name}" th:placeholder="#{contact.name}"/>"""

        val segments = VisibleAttributeExtractor.extract(template)

        assertEquals(emptyList(), segments)
    }

    @Test
    fun `a url valued attribute is never extracted`() {
        val template = """<link rel="alternate" hreflang="en" href="https://cheroliv.com/en/"/>"""

        val segments = VisibleAttributeExtractor.extract(template)

        assertEquals(emptyList(), segments)
    }

    @Test
    fun `script and style bodies are ignored`() {
        val template = """<script>el.setAttribute("placeholder", "Nom");</script><style>x{content:"Nom"}</style>"""

        val segments = VisibleAttributeExtractor.extract(template)

        assertEquals(emptyList(), segments)
    }

    @Test
    fun `html comments are ignored`() {
        val template = """<!-- placeholder="Nom" --><input placeholder="Prénom"/>"""

        val segments = VisibleAttributeExtractor.extract(template)

        assertEquals(listOf("Prénom"), segments)
    }

    @Test
    fun `duplicate values are collapsed preserving first occurrence order`() {
        val template =
            """<input placeholder="Nom"/><input placeholder="Email"/><input aria-label="Nom"/>"""

        val segments = VisibleAttributeExtractor.extract(template)

        assertEquals(listOf("Nom", "Email"), segments)
    }

    @Test
    fun `an empty template yields no segments`() {
        assertTrue(VisibleAttributeExtractor.extract("").isEmpty())
    }

    @Test
    fun `replacement only touches translatable attribute values`() {
        val template =
            """<input class="Nom" placeholder="Nom"/><p>Nom</p><a data-lang="Nom">x</a>"""

        val output = VisibleAttributeExtractor.replace(template, mapOf("Nom" to "Name"))

        assertEquals(
            """<input class="Nom" placeholder="Name"/><p>Nom</p><a data-lang="Nom">x</a>""",
            output,
        )
    }

    @Test
    fun `replacement preserves every tag byte`() {
        val template = """<input type="text" name="name" placeholder="Votre message" required />"""

        val output = VisibleAttributeExtractor.replace(template, mapOf("Votre message" to "Your message"))

        assertEquals("""<input type="text" name="name" placeholder="Your message" required />""", output)
    }

    @Test
    fun `replacement is a strict no-op on an empty map`() {
        val template = """<input placeholder="Nom"/>"""

        assertEquals(template, VisibleAttributeExtractor.replace(template, emptyMap()))
    }
}
