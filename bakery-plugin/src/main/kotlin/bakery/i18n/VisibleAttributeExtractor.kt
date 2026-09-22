package bakery.i18n

/**
 * CHE-I18N-QUALITY — extracts the human-readable *attribute* values of a
 * Thymeleaf template.
 *
 * S-049 shipped cheroliv.com's 22 variants with a French contact form: the
 * `placeholder` values (`Nom`, `Votre message`…), `aria-label` values
 * (`Retour en haut de page`…) and `alt` values are invisible to
 * [VisibleTextExtractor] — they are attributes, not text nodes — so the model
 * was never asked to translate them.
 *
 * Only four attributes carry user-facing prose:
 *
 *   - `placeholder`, `alt`, `title`, `aria-label`.
 *
 * Everything else is deliberately excluded:
 *
 *   - structural/behavioural (`class`, `id`, `href`, `src`, `rel`, `name`,
 *     `data-*`, `th:*`, `aria-hidden`…) is markup, not prose;
 *   - machine metadata (`<meta content="fr_FR">`, viewport, robots, OG locale)
 *     must never be translated;
 *   - a value already interpolated (`#{key}`, `${expr}`) is left to the
 *     resolver.
 *
 * A value is scheduled only when it looks like prose: it carries at least two
 * consecutive letters and is not a URL.
 *
 * Pure domain: no I/O, no LLM, no Gradle.
 */
object VisibleAttributeExtractor {

    /**
     * The four prose-bearing attributes. The lookbehind keeps `th:placeholder`
     * (an interpolation) and `data-alt` (markup) out — only a *plain* attribute
     * declaration is scheduled.
     */
    private val TRANSLATABLE_ATTRIBUTE =
        Regex("""(?<![\w:-])(?:placeholder|alt|title|aria-label)\s*=\s*"([^"]*)"""")

    private val PROSE = Regex("[\\p{L}]{2,}")
    private val INTERPOLATION = Regex("""[#${'$'}]\{""")
    private val URL = Regex("""^(https?:|/|\.\.?/|mailto:|tel:)""")

    fun extract(content: String): List<String> {
        if (content.isEmpty()) return emptyList()

        val segments = LinkedHashSet<String>()
        TemplateScanner.forEachRun(
            content,
            onMarkup = { markup ->
                if (markup.startsWith("<!--")) return@forEachRun
                TRANSLATABLE_ATTRIBUTE.findAll(markup).forEach { match ->
                    val value = match.groupValues[1].trim()
                    if (isTranslatable(value)) segments.add(value)
                }
            },
            onText = {},
        )
        return segments.toList()
    }

    /**
     * Substitutes every [replacements] key wherever it is the *value* of a
     * translatable attribute, never in another attribute, never in text, never
     * in a `<script>`/`<style>` body or an HTML comment.
     */
    fun replace(
        content: String,
        replacements: Map<String, String>,
    ): String {
        if (content.isEmpty() || replacements.isEmpty()) return content
        val builder = StringBuilder(content.length)
        TemplateScanner.forEachRun(
            content,
            onMarkup = { markup ->
                builder.append(if (markup.startsWith("<!--")) markup else replaceInMarkup(markup, replacements))
            },
            onText = builder::append,
        )
        return builder.toString()
    }

    private fun replaceInMarkup(
        markup: String,
        replacements: Map<String, String>,
    ): String =
        TRANSLATABLE_ATTRIBUTE.replace(markup) { match ->
            val quoted = match.groupValues[1]
            val value = quoted.trim()
            val target = replacements[value] ?: return@replace match.value
            match.value.replace(quoted, quoted.replace(value, target))
        }

    private fun isTranslatable(value: String): Boolean {
        if (value.length < 2) return false
        if (!PROSE.containsMatchIn(value)) return false
        if (INTERPOLATION.containsMatchIn(value)) return false
        return !URL.containsMatchIn(value)
    }
}
