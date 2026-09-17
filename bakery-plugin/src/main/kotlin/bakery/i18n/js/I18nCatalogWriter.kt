package bakery.i18n.js

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-6.
 *
 * Pure writer for the structured catalogue (`TALARIA.I18N.CATALOG`).
 *
 * Because the catalogue is nested, it can never go through the flat
 * [I18nJsDictionary] writer: a flat insertion would corrupt the nested
 * structure.
 *
 * Two operations, both idempotent and Ink-Economy-compliant (a translation that
 * changes nothing returns the source byte-identically):
 *
 * . [translate] substitutes the values of the literals a language already owns,
 *    in place — every other byte is preserved.
 * . [complete] fulfils the cadrage talaria S-054 contract on the nested
 *    catalogue: a language without a block is created by cloning the reference
 *    language (with the provided translations applied), and an existing block
 *    missing formations or fields is completed from the reference structure.
 *
 * No I/O, no Gradle, no LLM.
 */
object I18nCatalogWriter {
    /**
     * Substitutes the quoted literals [language] already owns with
     * [translations] (path → value). Paths absent from the block are ignored;
     * the reference language and all other entries are left byte-identical.
     */
    fun translate(
        source: String,
        language: String,
        translations: Map<String, String>,
    ): String {
        if (translations.isEmpty()) return source
        val block = locatedLiterals(source, language)
        if (block.isEmpty()) return source
        return splice(
            source,
            substitutions(
                located = block,
                offset = 0,
                translations = translations,
            ),
        )
    }

    /**
     * Completes [language] against the [referenceLanguage] block of
     * [referenceSource].
     *
     * An existing language keeps its own values (an existing value always wins
     * over a translation, which wins over the reference value) and gains the
     * reference formations and fields it was missing. An absent language is
     * created as a clone of the reference block. The reference language is
     * never rewritten.
     */
    fun complete(
        source: String,
        referenceSource: String,
        referenceLanguage: String,
        language: String,
        translations: Map<String, String>,
    ): String {
        if (language == referenceLanguage) return source
        val referenceBrace = I18nCatalog.languageBrace(referenceSource, referenceLanguage) ?: return source
        val referenceEnd = I18nCatalog.languageEnd(referenceSource, referenceLanguage) ?: return source
        val referenceBlock = referenceSource.substring(referenceBrace, referenceEnd)

        val existing = locatedLiterals(source, language)
        if (existing.isEmpty()) {
            val resolved = resolvedValues(referenceSource, referenceBrace, emptyMap(), translations)
            val created = render(referenceBlock, resolved)
            return insertLanguage(source, language, created)
        }

        val owned = existing.associate { it.literal.path to it.literal.value }
        val referencePaths = I18nCatalogDocument.locatedLiteralsInBlock(referenceSource, referenceBrace).map { it.literal.path }
        val hasGap = referencePaths.any { it !in owned }
        if (!hasGap) {
            return translate(source, language, translations)
        }

        val resolved = resolvedValues(referenceSource, referenceBrace, owned, translations)
        val rebuilt = render(referenceBlock, resolved)
        val start = I18nCatalog.languageBrace(source, language) ?: return source
        val end = I18nCatalog.languageEnd(source, language) ?: return source
        if (source.substring(start, end) == rebuilt) return source
        return source.substring(0, start) + rebuilt + source.substring(end)
    }

    /**
     * Resolved value of every reference path: an existing target value wins, then
     * a translation, then the reference value (structural completeness).
     */
    private fun resolvedValues(
        referenceSource: String,
        referenceBrace: Int,
        owned: Map<String, String>,
        translations: Map<String, String>,
    ): Map<String, String> =
        I18nCatalogDocument.locatedLiteralsInBlock(referenceSource, referenceBrace).associate { entry ->
            val path = entry.literal.path
            path to (owned[path] ?: translations[path] ?: entry.literal.value)
        }

    /** Renders [block] (a language object slice) with the resolved [values]. */
    private fun render(
        block: String,
        values: Map<String, String>,
    ): String =
        splice(
            block,
            substitutions(
                located = I18nCatalogDocument.locatedLiteralsInBlock(block, 0),
                offset = 0,
                translations = values,
            ),
        )

    private fun insertLanguage(
        source: String,
        language: String,
        block: String,
    ): String {
        val bodyClose = I18nCatalog.catalogueBodyRange(source)?.second ?: return source
        val head = source.substring(0, bodyClose).trimEnd()
        val separator = if (head.endsWith(",")) "" else ","
        val tail = source.substring(bodyClose)
        return "$head$separator\n$LANGUAGE_INDENT$language: $block\n$LANGUAGE_CLOSE$tail"
    }

    private fun locatedLiterals(
        source: String,
        language: String,
    ): List<I18nCatalogDocument.LocatedLiteral> {
        val brace = I18nCatalog.languageBrace(source, language) ?: return emptyList()
        return I18nCatalogDocument.locatedLiteralsInBlock(source, brace)
    }

    private fun substitutions(
        located: List<I18nCatalogDocument.LocatedLiteral>,
        offset: Int,
        translations: Map<String, String>,
    ): List<Triple<Int, Int, String>> =
        located.mapNotNull { entry ->
            val value = translations[entry.literal.path] ?: return@mapNotNull null
            if (entry.literal.value == value) return@mapNotNull null
            Triple(
                entry.contentStart - offset,
                entry.contentEnd - offset,
                I18nJsStringCodec.escape(value),
            )
        }

    private fun splice(
        source: String,
        edits: List<Triple<Int, Int, String>>,
    ): String {
        if (edits.isEmpty()) return source
        val builder = StringBuilder(source)
        edits.sortedByDescending { it.first }.forEach { (start, end, replacement) ->
            builder.replace(start, end, replacement)
        }
        return builder.toString()
    }

    private const val LANGUAGE_INDENT = "    "
    private const val LANGUAGE_CLOSE = "  "
}
