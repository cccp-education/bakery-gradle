package bakery.i18n

/**
 * CHE-I18N-22 US-7b — decides which templates of a target language still need
 * translation (full-template swap strategy, JBake has no MessageResolver).
 *
 * A reference template is scheduled when the variant has no copy of it, or when
 * the copy is byte-identical to the reference (the "copy FR" trap: eight
 * cheroliv.com variants shipped seven templates as verbatim French). A copy that
 * differs is preserved — never re-translated (Ink Economy Law).
 *
 * [force] regenerates every reference template for a legacy variant whose
 * structural drift (pre-SEO markup) keeps it French while differing from the
 * reference — the only case where "differs" does not mean "translated".
 *
 * Pure domain: no I/O, no LLM, no Gradle.
 */
object TemplateTranslationPlanner {

    fun plan(
        reference: Map<String, String>,
        target: Map<String, String>,
        force: Boolean = false,
    ): List<String> {
        if (force) return reference.keys.toList()
        return reference
            .filterKeys { relativePath ->
                val existing = target[relativePath] ?: return@filterKeys true
                existing == reference.getValue(relativePath)
            }.keys
            .toList()
    }

    /**
     * CHE-I18N-22 US-7c — bounds the template translation concurrency to the
     * healthy ports of the pool (pilot decision S-044), exactly like the article
     * task: at most one provider per healthy port.
     */
    fun requireParallelism(value: Int): Int {
        require(value in 1..MAX_PARALLELISM) {
            "Parallélisme templates '$value' invalide. Utilisez une valeur entre 1 et $MAX_PARALLELISM (port sain du pool)."
        }
        return value
    }

    const val MAX_PARALLELISM = 25
}
