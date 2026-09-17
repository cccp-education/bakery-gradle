package bakery.i18n.js

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-6 (extraction, REFACTOR).
 *
 * Shared JavaScript double-quoted string escape/unescape helpers.
 *
 * Both the flat writer ([I18nJsDictionary]) and the nested catalogue writer
 * ([I18nCatalogWriter]) must agree on the exact byte-level encoding, otherwise a
 * translated value would round-trip differently depending on the dictionary
 * shape. Domain-pure: no I/O, no Gradle, no LLM.
 */
internal object I18nJsStringCodec {
    /** Decodes the raw content of a double-quoted JS literal. */
    fun unescape(value: String): String =
        value
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")

    /** Encodes [value] as the raw content of a double-quoted JS literal. */
    fun escape(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            // Lambda replacement : Java Matcher interprete `\n` comme escape et
            // perdrait le backslash (bug attrape par I18nJsDictionaryTest S-054).
            .replace(Regex("\r?\n")) { "\\n" }
}
