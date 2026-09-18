package bakery.i18n

import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * CHE-I18N-22 US-7 — the template translator must translate visible text only,
 * never tag markup, and must degrade instead of corrupting a template.
 */
class TemplateTextTranslatorTest {

    @Test
    fun `visible text is translated and tags are preserved`() {
        val template =
            """<h1 class="display-5">Développeur</h1>
<p class="lead">Passionné par l'innovation.</p>"""
        val translator = TemplateTextTranslator(PrefixTranslationService("EN"))

        val result = translator.translate(template, "fr", "en")

        assertTrue(result.content.contains("EN:Développeur"), "Text must be translated: ${result.content}")
        assertTrue(result.content.contains("""class="display-5""""), "Class must be preserved")
        assertTrue(result.content.contains("<h1"), "Tag must be preserved")
        assertEquals(0, result.failedSegments)
    }

    @Test
    fun `a bare text node mixed with an inline span is translated`() {
        val template =
            """
            <h1 class="display-5 fw-bold mb-5">
                Développeur
                <span class="text-primary">spécialisé en Ingénierie Pédagogique</span>
            </h1>
            """.trimIndent()
        val translator = TemplateTextTranslator(PrefixTranslationService("EN"))

        val result = translator.translate(template, "fr", "en")

        assertTrue(result.content.contains("EN:Développeur"), "Bare text must be translated: ${result.content}")
        assertTrue(
            result.content.contains("EN:spécialisé en Ingénierie Pédagogique"),
            "Span text must be translated: ${result.content}",
        )
        assertTrue(result.content.contains("""class="display-5 fw-bold mb-5""""), "Attributes preserved")
        assertTrue(result.content.contains("""class="text-primary""""), "Span attributes preserved")
    }

    @Test
    fun `script and style bodies are never translated`() {
        val template =
            """
            <script>var label = "Développeur";</script>
            <style>.hero::after { content: "Innovation"; }</style>
            <h1>Développeur</h1>
            """.trimIndent()
        val translator = TemplateTextTranslator(PrefixTranslationService("EN"))

        val result = translator.translate(template, "fr", "en")

        assertTrue(result.content.contains("""var label = "Développeur";"""), "Script body must be untouched")
        assertTrue(result.content.contains("""content: "Innovation";"""), "Style body must be untouched")
        assertTrue(result.content.contains("<h1>EN:Développeur</h1>"), "Visible text must be translated")
    }

    @Test
    fun `attribute values are never substituted`() {
        val template = """<a th:href="${'$'}{root}index.html" data-lang="fr">Français</a>"""
        val translator = TemplateTextTranslator(PrefixTranslationService("EN"))

        val result = translator.translate(template, "fr", "en")

        assertTrue(result.content.contains("""data-lang="fr""""), "Attribute value must not change: ${result.content}")
        assertTrue(result.content.contains("EN:Français"), "Visible text must change: ${result.content}")
    }

    @Test
    fun `a failed segment keeps the source text`() {
        val template = """<h1>Développeur</h1>"""
        val translator = TemplateTextTranslator(FailingTranslationService())

        val result = translator.translate(template, "fr", "en")

        assertTrue(result.content.contains("Développeur"), "Source must be preserved on failure: ${result.content}")
        assertTrue(result.failedSegments > 0, "A failure must be counted")
    }

    @Test
    fun `same source and target language is a strict no-op`() {
        val template = """<h1>Développeur</h1>"""
        val translator = TemplateTextTranslator(PrefixTranslationService("EN"))

        val result = translator.translate(template, "fr", "fr")

        assertEquals(template, result.content)
        assertEquals(0, result.translatedSegments)
    }

    @Test
    fun `an empty template is a no-op`() {
        val translator = TemplateTextTranslator(PrefixTranslationService("EN"))

        val result = translator.translate("", "fr", "en")

        assertEquals("", result.content)
    }

    private class PrefixTranslationService(
        private val prefix: String,
    ) : TranslationService {
        override fun translate(request: TranslationRequest): TranslationResult =
            TranslationResult.Success("$prefix:${request.sourceText}")
    }

    private class FailingTranslationService : TranslationService {
        override fun translate(request: TranslationRequest): TranslationResult =
            TranslationResult.Failure("unavailable")
    }
}
