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
    private val extractor: I18nMigrationService = I18nMigrationService(),
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

        // The extractor works on a file; wrap the content in a temp-free adapter.
        val segments = extractSegments(templateContent)
        if (segments.isEmpty()) {
            return Result(templateContent, translatedSegments = 0, failedSegments = 0)
        }

        var translated = 0
        var failed = 0
        var output = templateContent
        // Longest first: a segment that contains another must be replaced first,
        // otherwise the shorter substitution would corrupt the longer one.
        segments
            .sortedByDescending { it.length }
            .forEach { segment ->
                when (val outcome = translateSegment(segment, sourceLanguage, targetLanguage)) {
                    is TranslationResult.Success -> {
                        if (outcome.translatedText != segment) {
                            output = replaceOutsideTags(output, segment, outcome.translatedText)
                            translated++
                        }
                    }
                    is TranslationResult.Failure -> failed++
                }
            }
        return Result(output, translated, failed)
    }

    internal fun extractSegments(content: String): List<String> {
        val temp = java.io.File.createTempFile("cheroliv-template", ".thyme")
        return try {
            temp.writeText(content)
            extractor.extractHardcodedText(temp).values.toList()
        } finally {
            temp.delete()
        }
    }

    /**
     * Substitutes [source] wherever it appears as visible text, never inside a
     * tag (`<…>`). A naive `String.replace` would rewrite attribute values too.
     */
    internal fun replaceOutsideTags(
        content: String,
        source: String,
        target: String,
    ): String {
        val result = StringBuilder()
        var i = 0
        while (i < content.length) {
            val tagStart = content.indexOf('<', i)
            if (tagStart < 0) {
                result.append(content.substring(i).replace(source, target))
                break
            }
            result.append(content.substring(i, tagStart).replace(source, target))
            val tagEnd = content.indexOf('>', tagStart)
            if (tagEnd < 0) {
                result.append(content.substring(tagStart))
                break
            }
            result.append(content.substring(tagStart, tagEnd + 1))
            i = tagEnd + 1
        }
        return result.toString()
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
