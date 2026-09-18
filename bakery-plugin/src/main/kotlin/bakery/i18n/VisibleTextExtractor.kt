package bakery.i18n

/**
 * CHE-I18N-22 US-7a — extracts the visible text of a Thymeleaf template.
 *
 * The historical `I18nMigrationService.extractHardcodedText` only matched a
 * whole element body (`<tag …>text</tag>`): a bare text node mixed with inline
 * markup (`<h1>Développeur <span>…</span></h1>`) was invisible, so the hero
 * headline of cheroliv.com stayed French in every variant.
 *
 * This object is the structural replacement: a single-pass scanner that yields
 * every text run outside tags, excluding `<script>`/`<style>` bodies and HTML
 * comments. It never decodes entities, never touches attributes — the caller
 * substitutes the returned runs verbatim.
 *
 * Pure domain: no I/O, no regex on the whole document, no Gradle.
 */
object VisibleTextExtractor {

    private const val MIN_SEGMENT_LENGTH = 2

    fun extract(content: String): List<String> {
        if (content.isEmpty()) return emptyList()

        val segments = LinkedHashSet<String>()
        forEachRun(content, onMarkup = {}, onText = { text ->
            val candidate = text.trim()
            if (candidate.length >= MIN_SEGMENT_LENGTH) segments.add(candidate)
        })
        return segments.toList()
    }

    /**
     * Substitutes every [replacements] key wherever it appears as visible text,
     * never inside a tag, a `<script>`/`<style>` body or an HTML comment. Keys
     * are applied longest first so a segment containing another is never
     * corrupted by the shorter one.
     */
    fun replaceVisibleText(
        content: String,
        replacements: Map<String, String>,
    ): String {
        if (content.isEmpty() || replacements.isEmpty()) return content
        val ordered = replacements.entries.sortedByDescending { it.key.length }
        val builder = StringBuilder(content.length)
        forEachRun(
            content,
            onMarkup = builder::append,
            onText = { text ->
                var substituted = text
                for ((source, target) in ordered) {
                    substituted = substituted.replace(source, target)
                }
                builder.append(substituted)
            },
        )
        return builder.toString()
    }

    /**
     * Walks [content] and reports every run in document order: markup runs
     * (tags, whole `<script>`/`<style>` bodies, comments) to [onMarkup], text
     * runs to [onText]. Concatenating both callbacks in call order reproduces
     * [content] byte-for-byte.
     */
    private fun forEachRun(
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
