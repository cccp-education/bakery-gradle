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

    /**
     * CHE-I18N-QUALITY US-19 — French function words, used by the repair to tell
     * a French run from a language-neutral one. A run that carries none of them
     * (`Contact`, `Blog`, `React`, `Android, SpringBoot`) must never be scheduled
     * for repair: a false positive on every language would drown the real signal.
     */
    private val FRENCH_MARKERS =
        setOf(
            "le", "la", "les", "des", "une", "dans", "pour", "avec", "sur", "est", "sont",
            "cette", "ces", "qui", "que", "aux", "par", "plus", "mais", "comme", "tout",
            "tous", "sans", "entre", "leur", "leurs", "nous", "vous", "votre", "vos",
            "notre", "nos", "au", "du", "ce", "et", "ou", "de", "un", "en", "il", "elle",
            "je", "tu", "mon", "ma", "mes", "son", "sa", "ses",
        )

    private val WORD = Regex("[\\p{L}]{2,}")

    /**
     * A run is French prose when it is a real phrase (three whitespace-separated
     * tokens at least) carrying a French function word. The token floor keeps
     * language codes (`en-gb`), place names and hyphenated technology names out.
     */
    fun isFrenchProse(text: String): Boolean {
        val tokens = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.size < MIN_TOKENS) return false
        return tokens.any { token ->
            WORD.findAll(token).any { it.value.lowercase() in FRENCH_MARKERS }
        }
    }

    private const val MIN_TOKENS = 3

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
