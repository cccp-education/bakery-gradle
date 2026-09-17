package bakery.i18n.js

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-4.
 *
 * Pure format guard for the client-side i18n dictionaries (decision S-054 §6:
 * "bakery adds its format guard — parse↔write round-trip, idempotence").
 *
 * Two invariants are enforced:
 *
 * . **Balanced blocks** — every `code: { ... }` block is balanced. The parser
 *    skips an unbalanced block silently, which would hide a broken dictionary
 *    from the delta planner; the guard reports it instead.
 * . **Round-trip** — re-inserting the keys a language already owns must return
 *    the source byte-identically (writer idempotence, Ink Economy Law). A
 *    non-preserving writer is reported before it ever reaches a real file.
 *
 * This object is domain-pure: no I/O, no Gradle, no LLM.
 */
object I18nClientFormatGuard {
    const val UNBALANCED_LANGUAGE_BLOCK = "unbalanced-language-block"
    const val ROUND_TRIP_DRIFT = "round-trip-drift"

    private const val HTML_SUFFIX = ".html"

    data class Violation(
        val kind: String,
        val detail: String,
    )

    data class Report(
        val violations: List<Violation>,
    ) {
        val isValid: Boolean get() = violations.isEmpty()
    }

    /**
     * Verifies [sources] as a merged dictionary. [rewriter] defaults to the real
     * [I18nJsDictionary.insertTranslations] writer and can be overridden to
     * prove the guard detects a non-preserving writer.
     */
    fun verify(
        vararg sources: String,
        rewriter: (String, String, Map<String, String>) -> String = I18nJsDictionary::insertTranslations,
    ): Report {
        val violations = mutableListOf<Violation>()

        for (source in sources) {
            I18nJsDictionary.unbalancedLanguageBlocks(source).forEach { language ->
                violations += Violation(UNBALANCED_LANGUAGE_BLOCK, language)
            }
        }

        val mergeable = sources.filter { I18nJsDictionary.unbalancedLanguageBlocks(it).isEmpty() }
        if (mergeable.isNotEmpty()) {
            val merged = mergeable.joinToString("\n")
            val dictionaries = I18nJsDictionary.parse(merged)
            for ((language, entries) in dictionaries) {
                val owned = entries.filterKeys { !it.endsWith(HTML_SUFFIX) }
                if (rewriter(merged, language, owned) != merged) {
                    violations += Violation(ROUND_TRIP_DRIFT, language)
                }
            }
        }

        return Report(violations)
    }
}
