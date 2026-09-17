package bakery.i18n.js

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-6.
 *
 * Minimal, domain-pure recursive-descent parser for the JavaScript object
 * literals of the structured catalogue (`TALARIA.I18N.CATALOG`). The flat
 * [I18nJsDictionary] only sees quoted `"key": "value"` entries; the nested
 * catalogue uses bare keys and arbitrary depth (`language → formation → fields`,
 * lists of strings and inline objects), so translation needs a real structural
 * read.
 *
 * Every node keeps its byte range in the source, and every string literal keeps
 * the range of its content so a writer can substitute it in place. Comments and
 * whitespace are skipped outside strings; escaped quotes are respected.
 * Internal: no I/O, no Gradle, no LLM.
 */
internal sealed interface I18nJsNode

internal data class I18nJsObject(
    val entries: List<I18nJsEntry>,
    val start: Int,
    val end: Int,
) : I18nJsNode

internal data class I18nJsArray(
    val items: List<I18nJsNode>,
    val start: Int,
    val end: Int,
) : I18nJsNode

internal data class I18nJsStringNode(
    val value: String,
    val contentStart: Int,
    val contentEnd: Int,
    val start: Int,
    val end: Int,
) : I18nJsNode

internal data class I18nJsEntry(
    val key: String,
    val keyStart: Int,
    val keyEnd: Int,
    val value: I18nJsNode,
)

internal object I18nJsObjectParser {
    /** Parses the object literal whose opening brace is at [brace], or null. */
    fun parseObjectAt(
        source: String,
        brace: Int,
    ): I18nJsObject? {
        if (brace < 0 || brace >= source.length || source[brace] != '{') return null
        return Cursor(source, brace).readObject()
    }

    private class Cursor(
        private val source: String,
        private var pos: Int,
    ) {
        fun readObject(): I18nJsObject? {
            skipWhitespace()
            if (pos >= source.length || source[pos] != '{') return null
            val start = pos
            pos++
            val entries = mutableListOf<I18nJsEntry>()
            while (true) {
                skipWhitespace()
                if (pos >= source.length) return null
                if (source[pos] == '}') {
                    pos++
                    break
                }
                val keyStart = pos
                val key = readKey() ?: return null
                val keyEnd = pos
                skipWhitespace()
                if (pos >= source.length || source[pos] != ':') return null
                pos++
                val value = readValue() ?: return null
                entries += I18nJsEntry(key, keyStart, keyEnd, value)
                skipWhitespace()
                if (pos < source.length && source[pos] == ',') {
                    pos++
                    continue
                }
                skipWhitespace()
                if (pos < source.length && source[pos] == '}') {
                    pos++
                    break
                }
                return null
            }
            return I18nJsObject(entries, start, pos)
        }

        fun readValue(): I18nJsNode? {
            skipWhitespace()
            if (pos >= source.length) return null
            return when (source[pos]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> readString()
                else -> null
            }
        }

        private fun readArray(): I18nJsArray? {
            skipWhitespace()
            if (pos >= source.length || source[pos] != '[') return null
            val start = pos
            pos++
            val items = mutableListOf<I18nJsNode>()
            while (true) {
                skipWhitespace()
                if (pos >= source.length) return null
                if (source[pos] == ']') {
                    pos++
                    break
                }
                items += readValue() ?: return null
                skipWhitespace()
                if (pos < source.length && source[pos] == ',') {
                    pos++
                    continue
                }
                skipWhitespace()
                if (pos < source.length && source[pos] == ']') {
                    pos++
                    break
                }
                return null
            }
            return I18nJsArray(items, start, pos)
        }

        private fun readString(): I18nJsStringNode? {
            if (pos >= source.length || source[pos] != '"') return null
            val start = pos
            pos++
            val contentStart = pos
            while (pos < source.length) {
                val char = source[pos]
                when {
                    char == '\\' -> pos += 2
                    char == '"' -> {
                        val contentEnd = pos
                        pos++
                        return I18nJsStringNode(
                            value = I18nJsStringCodec.unescape(source.substring(contentStart, contentEnd)),
                            contentStart = contentStart,
                            contentEnd = contentEnd,
                            start = start,
                            end = pos,
                        )
                    }
                    else -> pos++
                }
            }
            return null
        }

        private fun readKey(): String? {
            skipWhitespace()
            if (pos >= source.length) return null
            if (source[pos] == '"') return readString()?.value
            val start = pos
            while (pos < source.length && isKeyChar(source[pos])) pos++
            return if (pos > start) source.substring(start, pos) else null
        }

        private fun isKeyChar(char: Char): Boolean =
            char.isLetterOrDigit() || char == '_' || char == '-' || char == '.' || char == '$'

        private fun skipWhitespace() {
            while (pos < source.length) {
                val char = source[pos]
                when {
                    char == ' ' || char == '\t' || char == '\n' || char == '\r' -> pos++
                    char == '/' && pos + 1 < source.length && source[pos + 1] == '/' -> {
                        while (pos < source.length && source[pos] != '\n') pos++
                    }
                    char == '/' && pos + 1 < source.length && source[pos + 1] == '*' -> {
                        pos += 2
                        while (pos + 1 < source.length && !(source[pos] == '*' && source[pos + 1] == '/')) pos++
                        pos = (pos + 2).coerceAtMost(source.length)
                    }
                    else -> return
                }
            }
        }
    }
}
