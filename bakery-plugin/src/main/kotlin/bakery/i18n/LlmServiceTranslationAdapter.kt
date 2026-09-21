package bakery.i18n

import bakery.llm.LlmService
import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
import kotlinx.coroutines.runBlocking

class LlmServiceTranslationAdapter(
    private val llm: LlmService,
) : TranslationService {
    override fun translate(request: TranslationRequest): TranslationResult {
        val prompt = buildPrompt(request)
        return try {
            val raw = runBlocking { llm.complete(prompt) }
            val cleaned = raw.trim().trim('"', '«', '»', '`', '\n')
            when {
                cleaned.isBlank() -> TranslationResult.Failure("LLM returned blank response")
                isPromptLeak(cleaned, request.sourceText) -> TranslationResult.Failure("LLM echoed the prompt instead of translating")
                else -> TranslationResult.Success(cleaned)
            }
        } catch (e: Exception) {
            TranslationResult.Failure(e.message ?: "LLM call failed")
        }
    }

    /**
     * CHE-I18N-22 US-4 — a response echoing the instruction block is not a
     * translation. Observed on the real run with `nemotron-3-super:cloud`: the
     * model occasionally returned the whole prompt preamble (role line + JBake
     * reference + rules). Accepting it wrote a corrupted article — silent data
     * loss.
     *
     * CHE-I18N-QUALITY — the original guard only tested a *response-level* leak.
     * Measured on the real corpus: the model translated a fragment then **pasted
     * a reference section inside it** (`._Zielgruppe :_ ### Gradle Integration
     * JBake can be integrated …`), or answered with an LLM sentinel
     * (`(No output)`) in place of the fragment. Both hybrids passed the guard and
     * were frozen by the block checksums — silent data loss again.
     *
     * Three complementary signals, because the model *translates* the injected
     * context (a German run echoed "JBake-Referenz …" /
     * "Thymeleaf-Vorlagenbeispiele"):
     *
     *  - literal English markers of the preamble (the common case);
     *  - **structural / embedded markers** that belong to the injected reference
     *    or to the model's own machinery and never to a translated fragment
     *    (`th:replace`, `<!DOCTYPE html`, a markdown fence, a reference section
     *    heading, the `(No output)` sentinel), source-aware so a legitimate
     *    mention in the source stays acceptable;
     *  - a response an order of magnitude longer than its source (a fragment
     *    translation cannot inflate 10×).
     */
    private fun isPromptLeak(
        text: String,
        sourceText: String,
    ): Boolean {
        val head = text.lowercase()
        if (PROMPT_LEAK_MARKERS.any { head.contains(it) }) return true
        // A structural marker introduced by the response but absent from the
        // source can only come from the injected reference (source-aware: an
        // article may legitimately mention a `.thyme` file and keep it).
        val sourceLower = sourceText.lowercase()
        if (STRUCTURAL_LEAK_MARKERS.any { head.contains(it) && !sourceLower.contains(it) }) return true
        if (EMBEDDED_LEAK_MARKERS.any { head.contains(it) && !sourceLower.contains(it) }) return true
        return text.length > (sourceText.length * MAX_INFLATION).coerceAtLeast(MAX_INFLATION_FLOOR)
    }

    private companion object {
        /** Literal fragments of [buildPrompt]'s preamble. */
        val PROMPT_LEAK_MARKERS = listOf(
            "you are a professional translator",
            "preserve all backtick code spans",
            "this text may be a fragment of a larger sentence",
            "output only the translated text — no explanation",
            "output only the translated text - no explanation",
        )

        /** Markers of the injected reference — never gained by a translated fragment. */
        val STRUCTURAL_LEAK_MARKERS = listOf(
            "th:replace",
            "<!DOCTYPE html",
            "```html",
            ".thyme",
            "thymeleaf",
            "jbake reference",
            "jbake-Referenz",
            "directory structure",
        )

        /**
         * CHE-I18N-QUALITY — fragments observed *inside* otherwise translated
         * text: reference section headings and the model's own sentinels. All are
         * tested source-aware, so an article that legitimately carries the same
         * token is never flagged.
         */
        val EMBEDDED_LEAK_MARKERS = listOf(
            "(no output)",
            "(no text to translate)",
            "### ",
            "jbake gradle plugin",
            "calling the jbake cli",
            "thymeleaf template examples",
            "jbake configuration properties",
        )

        /**
         * A translated fragment may grow (some languages are more verbose) but
         * never tenfold. Measured leak: 115 bytes of source → 3518 bytes echoed.
         */
        const val MAX_INFLATION = 10
        const val MAX_INFLATION_FLOOR = 400
    }

    /**
     * CHE-I18N-QUALITY D1 — the prompt carries **only** the instruction and the
     * source text. The S-186 `DocKnowledgeBase` injection (JBake reference) was
     * removed here: the model pasted its sections into translated paragraphs
     * across the real cheroliv.com corpus (de 48, hi 31, it 31, es 8, zh 5
     * corrupted articles).
     */
    private fun buildPrompt(request: TranslationRequest): String =
        """You are a professional translator. Translate from ${request.sourceLanguage} to ${request.targetLanguage}.
Preserve ALL backtick code spans (`...`) exactly as-is — never modify backtick content, spacing, or position.
This text may be a fragment of a larger sentence — translate the fragment without requesting more context.
Output only the translated text — no explanation, no commentary, no introduction, no alternatives, no options.

${request.sourceText}"""
}
