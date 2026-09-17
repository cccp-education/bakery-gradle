package bakery.i18n.js

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-5.
 *
 * The client-side i18n tree ships two incompatible shapes behind the same
 * 4-space `code: {` block indentation:
 *
 * . [FLAT] — `var DICT = { <lang>: { "key": "value" } }` (chrome, `i18n.js`)
 *    and its additive patch `TALARIA.I18N.extend({...})` (`i18n-extra-langs.js`).
 *    Keys are quoted flat strings, parsed by [I18nJsDictionary].
 * . [CATALOG] — `TALARIA.I18N.CATALOG = { <lang>: { <formation>: { ... } } }`
 *    (`i18n-content.js`). Keys are nested and unquoted, invisible to the flat
 *    parser.
 *
 * Because the catalogue also declares `code: {` language blocks, the flat
 * parser recognises its languages. Without an explicit classifier the catalogue
 * would be mistaken for the owner of the flat keys and receive them (corruption
 * of the nested structure). This object is the single source of truth for that
 * distinction — no I/O, no Gradle, no LLM.
 */
enum class I18nJsFormat {
    FLAT,
    CATALOG,
    ;

    val isFlat: Boolean get() = this == FLAT

    val isCatalogue: Boolean get() = this == CATALOG

    companion object {
        private val CATALOG_MARKER = Regex("TALARIA\\s*\\.\\s*I18N\\s*\\.\\s*CATALOG\\s*=")

        /**
         * Classifies [source]: [CATALOG] when it assigns `TALARIA.I18N.CATALOG`,
         * [FLAT] otherwise. The catalogue marker is authoritative — the nested
         * catalogue reuses the flat block indentation and must never be treated
         * as a flat dictionary.
         */
        fun of(source: String): I18nJsFormat = if (CATALOG_MARKER.containsMatchIn(source)) CATALOG else FLAT
    }
}
