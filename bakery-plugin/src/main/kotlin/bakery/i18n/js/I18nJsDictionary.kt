package bakery.i18n.js

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-2.
 *
 * Pure parser/writer for the client-side i18n dictionaries of a JBake site
 * (`var DICT = { <lang>: { "key": "value" } }` / `TALARIA.I18N.extend({...})`).
 *
 * Reference implementation: the talaria node harness `tools/translate-i18n.mjs`
 * (S-044) — same language-block regex, same key regex, same indentation and the
 * same ink economy (only missing keys are appended; a complete dictionary is a
 * strict no-op). This object is domain-pure: no I/O, no Gradle, no LLM.
 *
 * Round-trip guarantee: parsing never mutates, and inserting only already-present
 * keys returns the input source byte-identically.
 */
object I18nJsDictionary {
    private val LANGUAGE_BLOCK = Regex("^\\s{4}([a-z]{2}):\\s*\\{", RegexOption.MULTILINE)

    private val KEY = Regex(
        "^\\s*\"([a-zA-Z][a-zA-Z0-9._-]*)\":\\s*\"((?:[^\"\\\\]|\\\\.)*)\"",
        RegexOption.MULTILINE,
    )

    private const val KEY_INDENT = "      "

    /**
     * Extracts every `code: { "key": "value" }` block from one or several sources.
     *
     * Sources are merged in order, so a language split across the chrome
     * dictionary and the additive patch keeps the union of its keys.
     * Insertion order is preserved (first appearance wins) — deterministic and
     * suitable for iteration when writing back.
     */
    fun parse(vararg sources: String): Map<String, Map<String, String>> {
        val dictionaries = LinkedHashMap<String, MutableMap<String, String>>()
        for (source in sources) {
            for (match in LANGUAGE_BLOCK.findAll(source)) {
                val language = match.groupValues[1]
                val body = matchBody(source, match.range.first) ?: continue
                val target = dictionaries.getOrPut(language) { LinkedHashMap() }
                for (entry in KEY.findAll(body)) {
                    target[entry.groupValues[1]] = unescapeJsString(entry.groupValues[2])
                }
            }
        }
        return dictionaries
    }

    /** Returns the language codes owned by [source], in document order. */
    fun languagesOf(source: String): List<String> = parse(source).keys.toList()

    /**
     * Appends the missing [translations] to the [language] block of [source].
     *
     * Keys already present are left untouched (ink economy) and the surrounding
     * document is preserved byte-identically. Returns [source] unchanged when no
     * key is missing, which makes the operation idempotent.
     *
     * @throws IllegalArgumentException when [language] owns no block in [source].
     */
    fun insertTranslations(
        source: String,
        language: String,
        translations: Map<String, String>,
    ): String {
        val existing = parse(source)[language].orEmpty()
        val additions = translations.filterKeys { it !in existing }
        if (additions.isEmpty()) return source

        val marker = Regex("^\\s{4}${Regex.escape(language)}:\\s*\\{", RegexOption.MULTILINE)
        val match =
            marker.find(source)
                ?: throw IllegalArgumentException("language block '$language' not found")

        val openBrace = source.indexOf('{', match.range.first)
        val closeBrace =
            matchingBrace(source, openBrace)
                ?: throw IllegalArgumentException("unbalanced block for '$language'")

        val lines =
            additions.entries.joinToString(",\n") { (key, value) ->
                "$KEY_INDENT\"$key\": \"${escapeJsString(value)}\""
            }

        val before =
            source
                .substring(0, closeBrace)
                .trimEnd()
                .let { if (it.endsWith(",")) it else "$it," }
        val after = source.substring(closeBrace)

        return "$before\n$lines\n${KEY_INDENT.dropLast(2)}$after"
    }

    /** Returns the body between the language block brace and its matching one. */
    private fun matchBody(
        source: String,
        matchStart: Int,
    ): String? {
        val openBrace = source.indexOf('{', matchStart)
        if (openBrace < 0) return null
        val closeBrace = matchingBrace(source, openBrace) ?: return null
        return source.substring(openBrace + 1, closeBrace)
    }

    /**
     * String-aware balanced-brace scanner: `{` / `}` inside a double-quoted
     * literal (escaped or not) are ignored, so a value like `"{0}"` never
     * truncates a block.
     */
    private fun matchingBrace(
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

    private fun unescapeJsString(value: String): String =
        value
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")

    private fun escapeJsString(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            // Lambda replacement : Java Matcher interprete `\n` comme escape et
            // perdrait le backslash (bug attrape par I18nJsDictionaryTest S-054).
            .replace(Regex("\r?\n")) { "\\n" }
}
