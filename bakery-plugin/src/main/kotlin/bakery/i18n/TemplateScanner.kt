package bakery.i18n

/**
 * CHE-I18N-QUALITY — the single structural scanner of a Thymeleaf document.
 *
 * Walks [content] once and reports every run in document order: markup runs
 * (tags, whole `<script>`/`<style>` bodies, HTML comments) to [onMarkup], text
 * runs to [onText]. Concatenating both callbacks in call order reproduces
 * [content] byte-for-byte.
 *
 * Extracted from [VisibleTextExtractor] so the visible-text domain and the
 * visible-attribute domain share one scanner instead of two divergent ones —
 * the attribute blind spot of S-049 came precisely from having a single view of
 * a template.
 *
 * Pure domain: no I/O, no regex on the whole document, no Gradle.
 */
internal object TemplateScanner {

    fun forEachRun(
        content: String,
        onMarkup: (String) -> Unit,
        onText: (String) -> Unit,
    ) {
        val lower = content.lowercase()
        var index = 0
        var textStart = -1

        while (index < content.length) {
            if (content[index] == '<') {
                if (textStart in 0 until index) onText(content.substring(textStart, index))
                textStart = -1
                val markupEnd = skipMarkup(content, lower, index)
                onMarkup(content.substring(index, markupEnd))
                index = markupEnd
                continue
            }
            if (textStart < 0) textStart = index
            index++
        }
        if (textStart in 0 until content.length) onText(content.substring(textStart))
    }

    /** Returns the index just after the markup opening at [index]. */
    private fun skipMarkup(
        content: String,
        lower: String,
        index: Int,
    ): Int {
        if (content.startsWith("<!--", index)) {
            val commentEnd = content.indexOf("-->", index + 4)
            return if (commentEnd < 0) content.length else commentEnd + 3
        }

        val tagEnd = content.indexOf('>', index)
        if (tagEnd < 0) return content.length

        val name = tagName(lower, index, tagEnd)
        val afterTag = tagEnd + 1
        if (name != "script" && name != "style") return afterTag

        val closeStart = lower.indexOf("</$name", afterTag)
        if (closeStart < 0) return content.length
        val closeEnd = content.indexOf('>', closeStart)
        return if (closeEnd < 0) content.length else closeEnd + 1
    }

    private fun tagName(
        lower: String,
        tagStart: Int,
        tagEnd: Int,
    ): String {
        var cursor = tagStart + 1
        if (cursor < tagEnd && (lower[cursor] == '/' || lower[cursor] == '!' || lower[cursor] == '?')) {
            return ""
        }
        val nameStart = cursor
        while (cursor < tagEnd && (lower[cursor].isLetterOrDigit() || lower[cursor] == '-')) cursor++
        return lower.substring(nameStart, cursor)
    }
}
