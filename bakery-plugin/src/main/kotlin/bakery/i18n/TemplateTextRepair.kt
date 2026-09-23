package bakery.i18n

/**
 * CHE-I18N-QUALITY US-19 — the scope-exact repair of a preserved variant's
 * **visible text**.
 *
 * [TemplateTranslationPlanner] decides *whole files*: a variant translated
 * before the body was handled differs from the reference, so it is preserved
 * forever and its French prose never converges. [TemplateAttributeRepair]
 * narrowed the granularity for attributes; this object does the same for the
 * visible text — the second half of the same blind spot.
 *
 *   - [pending] returns the reference visible runs still present verbatim in the
 *     target and looking like French prose — the exact scope a repair must cover;
 *   - [repair] substitutes only those runs.
 *
 * Ink economy: a translated run is never re-sent, a language-neutral run
 * (`Contact`, `Blog`, `React`) is never scheduled, a converged variant is a
 * strict no-op, and a second pass changes nothing. Sending the whole file would
 * be both wasteful and corrupting (already-translated prose becomes the model's
 * source text).
 *
 * Pure domain: no I/O, no LLM, no Gradle.
 */
object TemplateTextRepair {

    fun pending(
        reference: String,
        target: String,
    ): List<String> {
        if (reference.isEmpty() || target.isEmpty()) return emptyList()
        val targetRuns = VisibleTextExtractor.extract(target).toSet()
        return VisibleTextExtractor
            .extract(reference)
            .filter { it in targetRuns && VisibleTextExtractor.isFrenchProse(it) }
    }

    fun repair(
        target: String,
        replacements: Map<String, String>,
    ): String = VisibleTextExtractor.replaceVisibleText(target, replacements)
}
