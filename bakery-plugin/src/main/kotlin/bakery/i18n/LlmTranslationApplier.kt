package bakery.i18n

import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService

/**
 * Service bakery qui applique une traduction FR → EN via le [TranslationService]
 * transverse de codebase-gradle.
 *
 * Mode auto-LLM : quand aucun `translations_en.properties` n'est fourni,
 * chaque valeur FR est traduite une par une. En cas d'échec du service,
 * la valeur FR originale est conservée (fallback, pas de crash).
 */
class LlmTranslationApplier(private val translator: TranslationService) {

    fun translateFrenchToEnglish(frenchValues: Map<String, String>): Map<String, String> {
        if (frenchValues.isEmpty()) return emptyMap()

        val result = linkedMapOf<String, String>()
        for ((key, value) in frenchValues) {
            val translated = translateSingle(value)
            result[key] = translated
        }
        return result
    }

    private fun translateSingle(value: String): String {
        val request = TranslationRequest(
            sourceText = value,
            sourceLanguage = "fr",
            targetLanguage = "en"
        )
        return when (val outcome = translator.translate(request)) {
            is TranslationResult.Success -> outcome.translatedText
            is TranslationResult.Failure -> value
        }
    }
}
