package bakery.i18n.js

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-5 (extraction, REFACTOR).
 *
 * Shared string-aware balanced-brace scanner for the JavaScript i18n sources.
 * Both the flat parser ([I18nJsDictionary]) and the structured catalogue
 * planner ([I18nCatalog]) need the same rule: `{` / `}` inside a double-quoted
 * literal (escaped or not) are ignored, so a value like `"{0}"` never truncates
 * a block.
 *
 * Internal and domain-pure: no I/O, no Gradle, no LLM.
 */
internal object I18nJsBlockScanner {
    /**
     * Index of the `}` matching the `{` at [openBrace], or `null` when the block
     * is unbalanced. Braces inside quoted strings do not count.
     */
    fun matchingBrace(
        source: String,
        openBrace: Int,
    ): Int? {
        var depth = 0
        var inString = false
        var escaped = false
        for (index in openBrace until source.length) {
            val char = source[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = false
                }
            } else {
                when (char) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return index
                    }
                }
            }
        }
        return null
    }

    /**
     * Body between the first `{` at or after [start] and its matching `}`, or
     * `null` when there is no brace or the block is unbalanced.
     */
    fun bodyFrom(
        source: String,
        start: Int,
    ): String? {
        val openBrace = source.indexOf('{', start)
        if (openBrace < 0) return null
        val closeBrace = matchingBrace(source, openBrace) ?: return null
        return source.substring(openBrace + 1, closeBrace)
    }
}
