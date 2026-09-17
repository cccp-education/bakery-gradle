package bakery.i18n

import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * CHE-I18N-22 US-4 — prompt-leak guard on the bakery translation adapter.
 *
 * Observed on the real cheroliv.com run (nemotron-3-super:cloud): the model
 * occasionally echoed the *instruction block* as its answer (the "You are a
 * professional translator … Output only the translated text" preamble, followed
 * by the injected JBake reference). The adapter accepted it as a translation and
 * wrote a corrupted article — silent data loss.
 *
 * The guard treats a response containing the prompt preamble as a [TranslationResult.Failure]
 * so the caller keeps the source (the deltas are not marked translated).
 */
class LlmServiceTranslationAdapterLeakGuardTest {

    @Test
    fun `a response echoing the prompt preamble is rejected`() {
        val leaked =
            "You are a professional translator. JBake reference (this content uses JBake templates and conventions):\n" +
                "```html\n<div th:replace=\"~{menu :: menu}\"></div>\n```\n" +
                "Translate from fr to en.\nOutput only the translated text."

        val adapter = LlmServiceTranslationAdapter(StubLlmService(leaked))

        val result = adapter.translate(TranslationRequest("Bonjour", "fr", "en"))

        assertTrue(result is TranslationResult.Failure, "Prompt leak must be rejected, got: $result")
    }

    @Test
    fun `a response echoing only the role line is rejected`() {
        val adapter = LlmServiceTranslationAdapter(StubLlmService("You are a professional translator."))

        val result = adapter.translate(TranslationRequest("Bonjour", "fr", "en"))

        assertTrue(result is TranslationResult.Failure, "Prompt leak must be rejected, got: $result")
    }

    @Test
    fun `a legitimate translation is accepted`() {
        val adapter = LlmServiceTranslationAdapter(StubLlmService("Hello, world"))

        val result = adapter.translate(TranslationRequest("Bonjour le monde", "fr", "en"))

        assertTrue(result is TranslationResult.Success)
    }

    @Test
    fun `a translation that merely contains the word translator is accepted`() {
        val adapter = LlmServiceTranslationAdapter(StubLlmService("The translator is a professional."))

        val result = adapter.translate(TranslationRequest("Le traducteur est un professionnel.", "fr", "en"))

        assertTrue(result is TranslationResult.Success, "A genuine sentence must not be flagged: $result")
    }

    @Test
    fun `a response echoing the injected JBake reference is rejected`() {
        val leaked =
            "= ### Thymeleaf Template Examples\n" +
                "=== index.thyme (Homepage)\n```html\n<div th:replace=\"~{menu :: menu}\"></div>\n```"

        val adapter = LlmServiceTranslationAdapter(StubLlmService(leaked))

        val result = adapter.translate(TranslationRequest("Bonjour", "fr", "en"))

        assertTrue(result is TranslationResult.Failure, "Injected context leak must be rejected, got: $result")
    }

    @Test
    fun `a translated echo of the reference (other language) is rejected by structure`() {
        // German run: the model translated the injected reference and echoed its
        // Thymeleaf markup. No English marker remains — the structure must catch it.
        val leaked =
            "= ### Thymeleaf-Vorlagenbeispiele\n" +
                "=== index.thyme (Startseite)\n```html\n<div th:replace=\"~{menu :: menu}\"></div>\n```"

        val adapter = LlmServiceTranslationAdapter(StubLlmService(leaked))

        val result = adapter.translate(TranslationRequest("Bonjour", "fr", "de"))

        assertTrue(result is TranslationResult.Failure, "Structural leak must be rejected, got: $result")
    }

    @Test
    fun `a response inflating the source by an order of magnitude is rejected`() {
        val leaked = "x".repeat(4000)

        val adapter = LlmServiceTranslationAdapter(StubLlmService(leaked))

        val result = adapter.translate(TranslationRequest("Bonjour", "fr", "de"))

        assertTrue(result is TranslationResult.Failure, "An inflated response must be rejected, got: $result")
    }

    @Test
    fun `a long but legitimate translation is accepted`() {
        val source = "Bonjour le monde"
        val translation = "Hallo Welt, dies ist eine deutlich laengere Uebersetzung mit vielen Woertern."

        val adapter = LlmServiceTranslationAdapter(StubLlmService(translation))

        val result = adapter.translate(TranslationRequest(source, "fr", "de"))

        assertTrue(result is TranslationResult.Success, "A genuine verbose translation must pass: $result")
    }

    @Test
    fun `a structural marker absent from the source is rejected even when short`() {
        // Measured on the real run: 115 bytes of source, 406 bytes echoed with
        // translated reference headings (`### Thymeleaf Vorlagenbeispiele`).
        val source = "= Article 3\n:jbake-type: post\n\nContenu de article 3.\n"
        val leaked =
            "= ### Thymeleaf Vorlagenbeispiele\n=== index.thyme (Startseite)\n\n" +
                "=== post.thyme (Einzelner Beitrag)\n\nArtikel 3\n:jbake-type: post\n"

        val adapter = LlmServiceTranslationAdapter(StubLlmService(leaked))

        val result = adapter.translate(TranslationRequest(source, "fr", "de"))

        assertTrue(result is TranslationResult.Failure, "Structural leak must be rejected, got: $result")
    }

    @Test
    fun `a source that legitimately mentions a thyme file is not flagged`() {
        val source = "Voir le fichier post.thyme pour le template."
        val translation = "See the post.thyme file for the template."

        val adapter = LlmServiceTranslationAdapter(StubLlmService(translation))

        val result = adapter.translate(TranslationRequest(source, "fr", "en"))

        assertTrue(result is TranslationResult.Success, "A genuine mention must pass: $result")
    }

    private class StubLlmService(
        private val response: String,
    ) : bakery.llm.LlmService {
        override suspend fun complete(prompt: String): String = response
    }
}
