package bakery.i18n.plantuml

import bakery.pivot.PivotBlock
import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService

/**
 * EPIC BKY-I18N-DEPLOY — US DEPLOY-2.
 *
 * Applies a [PlantUmlStrategy] to a `[plantuml]` block during translation.
 * Dispatches on the [PlantUmlClassifier] decision:
 *
 * - [PlantUmlStrategy.TranslateLabels] : translates every translatable label
 *   (between quotes) through the [TranslationService] port. Identifiers that
 *   look like qualified code (dotted) are skipped upstream by [PlantUmlBlock.labels].
 * - [PlantUmlStrategy.PreserveTechnical] : returns the block unchanged.
 * - [PlantUmlStrategy.BorrowVocabulary] : translates labels while preserving
 *   the borrowed business vocabulary (REAC, AFNOR, DC, TS, RNCP, CP, ECF).
 *   Each occurrence is masked behind a placeholder before the label is sent
 *   to the translator, then restored afterwards so the term survives the LLM
 *   pass verbatim.
 *
 * I/O is isolated here through the injected [TranslationService] port — the
 * domain types ([PlantUmlBlock], [PlantUmlStrategy], [PlantUmlClassifier]) stay
 * pure. Inspired by HTOWN EPIC PLT-BOUNDARY (S163-166).
 */
class PlantUmlTranslationAdapter(
    private val translationService: TranslationService,
    private val classifier: PlantUmlClassifier = PlantUmlClassifier()
) {

    private val borrowedVocabulary = setOf("REAC", "AFNOR", "DC", "TS", "RNCP", "CP", "ECF")

    private val placeholderOpen = "\uE000"
    private val placeholderClose = "\uE001"

    fun translate(
        block: PivotBlock.Source,
        sourceLanguage: String,
        targetLanguage: String
    ): PivotBlock.Source {
        if (block.language != "plantuml") return block
        val strategy = classifier.classify(PlantUmlBlock(block.content))
        return when (strategy) {
            PlantUmlStrategy.PreserveTechnical -> block
            PlantUmlStrategy.TranslateLabels -> translateLabels(block, sourceLanguage, targetLanguage, preserveVocabulary = false)
            PlantUmlStrategy.BorrowVocabulary -> translateLabels(block, sourceLanguage, targetLanguage, preserveVocabulary = true)
        }
    }

    private fun translateLabels(
        block: PivotBlock.Source,
        sourceLanguage: String,
        targetLanguage: String,
        preserveVocabulary: Boolean
    ): PivotBlock.Source {
        var content = block.content
        val placeholders = mutableMapOf<String, String>()
        if (preserveVocabulary) {
            borrowedVocabulary.forEachIndexed { idx, term ->
                val token = "$placeholderOpen$idx$placeholderClose"
                placeholders[token] = term
                content = content.replace("\"$term\"", "\"$token\"")
            }
        }
        val labels = PlantUmlBlock(content).labels()
        var translated = content
        for (label in labels.distinct()) {
            val replacement = doTranslate(label, sourceLanguage, targetLanguage) ?: continue
            translated = translated.replace("\"$label\"", "\"$replacement\"")
        }
        if (preserveVocabulary) {
            for ((token, term) in placeholders) {
                translated = translated.replace("\"$token\"", "\"$term\"")
            }
        }
        return block.copy(content = translated)
    }

    private fun doTranslate(text: String, sourceLanguage: String, targetLanguage: String): String? {
        if (text.isBlank()) return null
        val request = TranslationRequest(text, sourceLanguage, targetLanguage)
        return when (val result = translationService.translate(request)) {
            is TranslationResult.Success -> result.translatedText
            is TranslationResult.Failure -> null
        }
    }
}