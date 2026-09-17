package bakery.i18n.js

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-3.
 *
 * One file to rewrite for one language: [file] owns the language block, [keys]
 * are the reference keys that language is missing.
 */
data class I18nClientFilePlan(
    val file: String,
    val language: String,
    val keys: List<String>,
)

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-3.
 *
 * Pure delta planner for the client-side i18n dictionaries.
 *
 * Reference behaviour: the talaria node harness `tools/translate-i18n.mjs`
 * (S-044). The chrome dictionary (`var DICT`) and the additive patch
 * (`TALARIA.I18N.extend`) are merged before the delta is computed, so a
 * language split across both files keeps the union of its keys. Only the
 * missing keys are planned (Ink Economy Law) and `.html` variants are never
 * translated. No I/O, no Gradle, no LLM.
 */
object I18nClientDelta {
    private const val HTML_SUFFIX = ".html"

    /** Reference keys (minus `.html`) that every target language must own, sorted. */
    fun referenceFloor(
        dictionaries: Map<String, Map<String, String>>,
        referenceLanguage: String,
    ): List<String> =
        dictionaries[referenceLanguage]
            .orEmpty()
            .keys
            .filterNot { it.endsWith(HTML_SUFFIX) }
            .sorted()

    /** Per target language, the reference keys it does not own yet. */
    fun missingKeys(
        dictionaries: Map<String, Map<String, String>>,
        referenceLanguage: String,
        targetLanguages: List<String>,
    ): Map<String, List<String>> {
        val floor = referenceFloor(dictionaries, referenceLanguage)
        return targetLanguages.associateWith { language ->
            if (language == referenceLanguage) {
                emptyList()
            } else {
                val owned = dictionaries[language].orEmpty().keys
                floor.filterNot { it in owned }
            }
        }
    }

    /**
     * Maps each language to the first **flat** file that owns its block, in
     * iteration order (the chrome dictionary wins over the patch when both
     * declare a language).
     *
     * The structured catalogue (`TALARIA.I18N.CATALOG`) reuses the flat 4-space
     * `code: {` block indentation, so [I18nJsDictionary.languagesOf] sees its
     * languages too. It must never own a flat key: its nested, unquoted
     * structure would be corrupted by a flat insertion. Catalogue sources are
     * skipped ([I18nJsFormat.of]) — with the real alphabetical tree order
     * (`i18n-content.js` first) this is the difference between writing into the
     * chrome dictionary and writing into the catalogue.
     */
    fun languageOwners(
        files: Map<String, String>,
        languages: List<String>,
    ): Map<String, String> {
        val owners = LinkedHashMap<String, String>()
        for (language in languages) {
            for ((file, source) in files) {
                if (I18nJsFormat.of(source) == I18nJsFormat.CATALOG) continue
                if (language in I18nJsDictionary.languagesOf(source)) {
                    owners[language] = file
                    break
                }
            }
        }
        return owners
    }

    /**
     * Builds the rewrite plan for [targetLanguages], in the requested order.
     *
     * A language without any owning block is skipped (nothing to append to),
     * and a language whose dictionary is already complete produces no entry —
     * a full corpus is a strict no-op.
     */
    fun plan(
        files: Map<String, String>,
        referenceLanguage: String,
        targetLanguages: List<String>,
    ): List<I18nClientFilePlan> {
        val dictionaries = I18nJsDictionary.parse(*files.values.toTypedArray())
        val missing = missingKeys(dictionaries, referenceLanguage, targetLanguages)
        val owners = languageOwners(files, targetLanguages)

        return targetLanguages.mapNotNull { language ->
            val keys = missing[language].orEmpty()
            val owner = owners[language]
            if (keys.isEmpty() || owner == null) {
                null
            } else {
                I18nClientFilePlan(owner, language, keys)
            }
        }
    }
}
