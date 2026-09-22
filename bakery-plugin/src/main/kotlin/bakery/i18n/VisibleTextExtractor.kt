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
        TemplateScanner.forEachRun(content, onMarkup = {}, onText = { text ->
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
        TemplateScanner.forEachRun(
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
}
