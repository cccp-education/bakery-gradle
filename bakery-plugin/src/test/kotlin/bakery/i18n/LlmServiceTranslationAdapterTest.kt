package bakery.i18n

import contracts.i18n.TranslationRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LlmServiceTranslationAdapterTest {
    @Test
    fun `prompt instructs LLM to translate without commentary or options`() {
        val captured = CapturingLlmService()
        val adapter = LlmServiceTranslationAdapter(captured)

        adapter.translate(TranslationRequest("Bonjour le monde", "fr", "en"))

        val prompt = captured.lastPrompt
        assertTrue(
            prompt.contains("You are a professional translator"),
            "Prompt must set a role: got '$prompt'",
        )
        assertTrue(
            prompt.contains("fr"),
            "Prompt must mention source language: got '$prompt'",
        )
        assertTrue(
            prompt.contains("en"),
            "Prompt must mention target language: got '$prompt'",
        )
        assertTrue(
            prompt.contains("Bonjour le monde"),
            "Prompt must include the source text: got '$prompt'",
        )
        assertTrue(
            prompt.contains("only the translated text"),
            "Prompt must instruct to output only translation: got '$prompt'",
        )
        assertTrue(
            prompt.contains("no explanation"),
            "Prompt must forbid explanations: got '$prompt'",
        )
    }

    @Test
    fun `prompt forbids commentary and multiple options`() {
        val captured = CapturingLlmService()
        val adapter = LlmServiceTranslationAdapter(captured)

        adapter.translate(TranslationRequest("Test", "fr", "en"))

        val prompt = captured.lastPrompt
        assertTrue(
            prompt.contains("no alternatives"),
            "Prompt must forbid alternative translations: got '$prompt'",
        )
        assertTrue(
            prompt.contains("no options"),
            "Prompt must forbid multiple options: got '$prompt'",
        )
    }

    @Test
    fun `the translation prompt never injects the JBake reference document`() {
        // CHE-I18N-QUALITY D1 — the S-186 DocKnowledgeBase injection corrupted
        // real cheroliv.com translations: the model pasted reference sections
        // (`### Gradle Integration …`) inside translated paragraphs. A pure
        // translation prompt must carry only the instruction and the source text.
        val captured = CapturingLlmService()
        val adapter = LlmServiceTranslationAdapter(captured)

        adapter.translate(
            TranslationRequest(
                "Installer Gradle et configurer les templates JBake du site.",
                "fr",
                "de",
            ),
        )

        val prompt = captured.lastPrompt
        assertTrue(
            !prompt.contains("JBake reference"),
            "Prompt must not inject the JBake reference, got '$prompt'",
        )
        assertTrue(
            !prompt.contains("Gradle Integration"),
            "Prompt must not inject reference sections, got '$prompt'",
        )
        assertTrue(
            !prompt.contains("th:replace"),
            "Prompt must not inject Thymeleaf examples, got '$prompt'",
        )
    }

    @Test
    fun `translate returns cleaned LLM output on success`() {
        val llm = StubLlmService("  \"Hello world\"  ")
        val adapter = LlmServiceTranslationAdapter(llm)

        val result = adapter.translate(TranslationRequest("Bonjour", "fr", "en"))

        assertTrue(result is contracts.i18n.TranslationResult.Success)
        assertEquals("Hello world", (result as contracts.i18n.TranslationResult.Success).translatedText)
    }

    @Test
    fun `translate returns failure on blank LLM response`() {
        val llm = StubLlmService("   ")
        val adapter = LlmServiceTranslationAdapter(llm)

        val result = adapter.translate(TranslationRequest("Bonjour", "fr", "en"))

        assertTrue(result is contracts.i18n.TranslationResult.Failure)
    }

    @Test
    fun `translate returns failure on LLM exception`() {
        val llm = FailingLlmService()
        val adapter = LlmServiceTranslationAdapter(llm)

        val result = adapter.translate(TranslationRequest("Bonjour", "fr", "en"))

        assertTrue(result is contracts.i18n.TranslationResult.Failure)
    }

    private class CapturingLlmService : bakery.llm.LlmService {
        var lastPrompt: String = ""

        override suspend fun complete(prompt: String): String {
            lastPrompt = prompt
            return "translated"
        }
    }

    private class StubLlmService(
        private val response: String,
    ) : bakery.llm.LlmService {
        override suspend fun complete(prompt: String): String = response
    }

    private class FailingLlmService : bakery.llm.LlmService {
        override suspend fun complete(prompt: String): String = throw RuntimeException("LLM unavailable")
    }
}
