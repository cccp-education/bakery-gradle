package bakery.i18n.js

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-6.
 *
 * One catalogue file to rewrite for one language: [file] owns the language
 * block, [paths] are the reference literal paths that language is missing.
 */
data class I18nCatalogFilePlan(
    val file: String,
    val language: String,
    val paths: List<String>,
)

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-6.
 *
 * Pure delta planner for the structured catalogue (`TALARIA.I18N.CATALOG`,
 * `i18n-content.js`). Extends the flat [I18nClientDelta] to the nested shape:
 * the unit of work is a literal path (`fpa.title`, `fpa.modules[0].desc`), so
 * only the literals a target language does not already own are planned (Ink
 * Economy Law). Non-catalogue sources are ignored — they belong to
 * [I18nClientDelta].
 *
 * No I/O, no Gradle, no LLM.
 */
object I18nCatalogDelta {
    /**
     * Rewrite plan for [targetLanguages], in the requested order. A file that is
     * not a catalogue, a reference without a catalogue, or a complete target
     * language produces no entry.
     */
    fun plan(
        files: Map<String, String>,
        referenceLanguage: String,
        targetLanguages: List<String>,
    ): List<I18nCatalogFilePlan> {
        val catalogues = files.filterValues { I18nJsFormat.of(it).isCatalogue }
        if (catalogues.isEmpty()) return emptyList()

        return catalogues.flatMap { (file, source) ->
            targetLanguages.mapNotNull { language ->
                val missing =
                    I18nCatalogDocument.missingLiterals(source, referenceLanguage, language).map { it.path }
                if (missing.isEmpty()) null else I18nCatalogFilePlan(file, language, missing)
            }
        }
    }
}
