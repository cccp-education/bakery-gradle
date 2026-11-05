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
 * BKY-OUT-3 — the prose rule needs three tokens, so a short label
 * (`Me Contacter`, `Innovation Pédagogique`) stayed invisible and frozen too.
 * The template guard is template-scoped: it also accepts
 * [VisibleTextExtractor.isShortFrenchLabel], while the global prose floor stays
 * unchanged so technology names and language codes are never scheduled.
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
        // The generated language-switcher block lists every language in its own
        // name (`Français`, `Deutsch`). A one-token short label would look French,
        // so the generated markup is masked out exactly as the whole-template
        // translator does (SwitcherBlockMask).
        val referenceText = SwitcherBlockMask.mask(reference).text
        val targetText = SwitcherBlockMask.mask(target).text
        val targetRuns =
            (
                VisibleTextExtractor.extract(targetText) +
                    VisibleTextExtractor.extractShortLabels(targetText)
            ).toSet()
        return (
            VisibleTextExtractor.extract(referenceText) +
                VisibleTextExtractor.extractShortLabels(referenceText)
        ).distinct().filter {
            it in targetRuns &&
                (VisibleTextExtractor.isFrenchProse(it) || VisibleTextExtractor.isShortFrenchLabel(it))
        }
    }

    fun repair(
        target: String,
        replacements: Map<String, String>,
    ): String {
        if (target.isEmpty() || replacements.isEmpty()) return target
        val masked = SwitcherBlockMask.mask(target)
        val repaired = VisibleTextExtractor.replaceVisibleText(masked.text, replacements)
        return SwitcherBlockMask.unmask(repaired, masked.blocks)
    }
}
