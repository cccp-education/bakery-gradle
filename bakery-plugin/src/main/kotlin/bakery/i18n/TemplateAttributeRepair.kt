package bakery.i18n

/**
 * CHE-I18N-QUALITY — the scope-exact repair of a preserved variant's attributes.
 *
 * [TemplateTranslationPlanner] decides *whole files*: a variant translated before
 * attributes were translated at all differs from the reference, so it is
 * preserved forever and its `placeholder="Nom"` never converges. Re-translating
 * the whole file would be both wasteful and corrupting (already-translated prose
 * would be sent back to the model as source text).
 *
 * This object narrows the granularity to the single attribute value:
 *
 *   - [pending] returns the reference attribute values still present verbatim in
 *     the target — the exact scope a repair must cover;
 *   - [repair] substitutes only those values.
 *
 * Ink economy: a translated value is never re-sent, a converged variant is a
 * strict no-op, and a second pass changes nothing.
 *
 * Pure domain: no I/O, no LLM, no Gradle.
 */
object TemplateAttributeRepair {

    fun pending(
        reference: String,
        target: String,
    ): List<String> {
        if (reference.isEmpty() || target.isEmpty()) return emptyList()
        val targetValues = VisibleAttributeExtractor.extract(target).toSet()
        return VisibleAttributeExtractor.extract(reference).filter { it in targetValues }
    }

    fun repair(
        target: String,
        replacements: Map<String, String>,
    ): String = VisibleAttributeExtractor.replace(target, replacements)
}
