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

        // The language-switcher block is generated markup, re-injected after
        // every translation: never sent to the model (Ink Economy Law — 81 lines
        // × 22 languages, and the model corrupts the big menu.thyme skeleton).
        val masked = SwitcherBlockMask.mask(templateContent)

        val segments = VisibleTextExtractor.extract(masked.text)
        val attributes = VisibleAttributeExtractor.extract(masked.text)
        if (segments.isEmpty() && attributes.isEmpty()) {
            return Result(templateContent, translatedSegments = 0, failedSegments = 0)
        }

        val values = translateValues((segments + attributes).distinct(), sourceLanguage, targetLanguage)
        val visibleText = VisibleTextExtractor.replaceVisibleText(masked.text, values.replacements)
        val output = VisibleAttributeExtractor.replace(visibleText, values.replacements)
        return Result(SwitcherBlockMask.unmask(output, masked.blocks), values.translated, values.failed)
    }

    /**
     * CHE-I18N-QUALITY — translates a *closed set of reference values* and returns
     * the successful substitutions only.
     *
     * The whole-file [translate] sends every visible segment of the reference;
     * the attribute repair of a preserved variant ([TemplateAttributeRepair])
     * must instead send only the values still verbatim French. Both share this
     * single translation loop — one concurrency-free unit of work, one failure
     * policy (a failed value keeps its source text and is counted).
     */
    fun translateValues(
        values: List<String>,
        sourceLanguage: String,
        targetLanguage: String,
    ): ValueTranslations {
        var translated = 0
        var failed = 0
        val replacements = linkedMapOf<String, String>()
        values.forEach { value ->
            when (val outcome = translateSegment(value, sourceLanguage, targetLanguage)) {
                is TranslationResult.Success -> {
                    if (outcome.translatedText != value) {
                        replacements[value] = outcome.translatedText
                        translated++
                    }
                }
                is TranslationResult.Failure -> failed++
            }
        }
        return ValueTranslations(replacements, translated, failed)
    }

    data class ValueTranslations(
        val replacements: Map<String, String>,
        val translated: Int,
        val failed: Int,
    )

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
