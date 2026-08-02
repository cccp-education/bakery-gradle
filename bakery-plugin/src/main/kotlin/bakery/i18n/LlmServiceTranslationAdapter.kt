package bakery.i18n

import bakery.llm.LlmService
import bakery.rag.DocKnowledgeBase
import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
import kotlinx.coroutines.runBlocking

class LlmServiceTranslationAdapter(
    private val llm: LlmService,
    private val docKnowledgeBase: DocKnowledgeBase = DocKnowledgeBase.forJBake(),
) : TranslationService {
    override fun translate(request: TranslationRequest): TranslationResult {
        val prompt = buildPrompt(request)
        return try {
            val raw = runBlocking { llm.complete(prompt) }
            val cleaned = raw.trim().trim('"', '«', '»', '`', '\n')
            if (cleaned.isBlank()) {
                TranslationResult.Failure("LLM returned blank response")
            } else {
                TranslationResult.Success(cleaned)
            }
        } catch (e: Exception) {
            TranslationResult.Failure(e.message ?: "LLM call failed")
        }
    }

    private fun buildPrompt(request: TranslationRequest): String {
        val docContext = docKnowledgeBase.queryContext(request.sourceText)
        val docSection =
            if (docContext.isNotBlank()) {
                """JBake reference (this content uses JBake templates and conventions):
$docContext

"""
            } else {
                ""
            }

        return """You are a professional translator. ${docSection}Translate from ${request.sourceLanguage} to ${request.targetLanguage}.
Preserve ALL backtick code spans (`...`) exactly as-is — never modify backtick content, spacing, or position.
This text may be a fragment of a larger sentence — translate the fragment without requesting more context.
Output only the translated text — no explanation, no commentary, no introduction, no alternatives, no options.

${request.sourceText}"""
    }
}
