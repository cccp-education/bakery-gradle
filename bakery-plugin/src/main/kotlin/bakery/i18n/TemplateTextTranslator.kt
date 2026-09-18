package bakery.i18n

import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService

/**
 * CHE-I18N-22 US-7 — translates a Thymeleaf template in place.
 *
 * cheroliv.com uses the **full-template swap** strategy (`i18n/{lang}/templates/`
 * is an integrally translated copy) because `jbake-core:2.7.0` installs no
 * MessageResolver: the `#{key}` bundles `migrateToI18n` produces would never be
 * resolved at bake time.
 *
 * This object reuses [I18nMigrationService.extractHardcodedText] to find the
 * translatable segments, translates each one through the N0
 * [TranslationService] (the codebase-backed pool), then substitutes the values
 * in place — preserving every tag, attribute and Thymeleaf expression.
 *
 * Ink economy: a segment already translated (identical to its target) is not
 * re-sent; a failed segment keeps its source text (never a corrupted template).
 */
class TemplateTextTranslator(
    private val translationService: TranslationService,
) {

    data class Result(
        val content: String,
        val translatedSegments: Int,
        val failedSegments: Int,
    )

    fun translate(
        templateContent: String,
        sourceLanguage: String,
        targetLanguage: String,
    ): Result {
        if (sourceLanguage == targetLanguage) {
            return Result(templateContent, translatedSegments = 0, failedSegments = 0)
        }

        val segments = VisibleTextExtractor.extract(templateContent)
        if (segments.isEmpty()) {
            return Result(templateContent, translatedSegments = 0, failedSegments = 0)
        }

        var translated = 0
        var failed = 0
        val replacements = linkedMapOf<String, String>()
        segments.forEach { segment ->
            when (val outcome = translateSegment(segment, sourceLanguage, targetLanguage)) {
                is TranslationResult.Success -> {
                    if (outcome.translatedText != segment) {
                        replacements[segment] = outcome.translatedText
                        translated++
                    }
                }
                is TranslationResult.Failure -> failed++
            }
        }
        val output = VisibleTextExtractor.replaceVisibleText(templateContent, replacements)
        return Result(output, translated, failed)
    }

    private fun translateSegment(
        segment: String,
        sourceLanguage: String,
        targetLanguage: String,
    ): TranslationResult {
        val request =
            TranslationRequest(
                sourceText = segment,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
            )
        return try {
            translationService.translate(request)
        } catch (e: Exception) {
            TranslationResult.Failure(e.message ?: "translation failed")
        }
    }
}
