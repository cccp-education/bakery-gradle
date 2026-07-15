package bakery.i18n

import bakery.llm.LlmService
import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
import kotlinx.coroutines.runBlocking

/**
 * Adapte le [LlmService] bakery au contrat [TranslationService] transverse.
 *
 * Injecté dans [MigrateToI18nTask] quand l'IA est activée, pour activer
 * le mode auto-LLM de migration i18n (traduction FR → EN).
 */
class LlmServiceTranslationAdapter(private val llm: LlmService) : TranslationService {

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

    private fun buildPrompt(request: TranslationRequest): String =
        """You are a professional translator. Translate the following text from ${request.sourceLanguage} to ${request.targetLanguage}.
Output only the translated text — no explanation, no commentary, no alternatives, no options.

Text to translate:
${request.sourceText}"""
}
