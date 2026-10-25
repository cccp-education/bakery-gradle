package bakery.i18n

import bakery.tree.TemplateNode
import bakery.tree.TemplateTranslationStatus
import bakery.tree.TemplateTree

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
     * GBL-010 G-1b — status-driven plan from the [TemplateTree] audit.
     *
     * This is the production rule: only a node whose status is not
     * [TemplateTranslationStatus.TRANSLATED] is scheduled — the second run over a
     * converged variant is a strict no-op. [include] narrows the node set (the
     * `translateTemplates` task owns `.thyme` files only) without duplicating the
     * status rule.
     */
    fun plan(
        tree: TemplateTree,
        language: String,
        force: Boolean = false,
        include: (TemplateNode) -> Boolean = { true },
    ): List<String> {
        val nodes = tree.nodes.filter { it.language == language && include(it) }
        return if (force) {
            nodes.map { it.template }
        } else {
            nodes
                .filter { it.status != TemplateTranslationStatus.TRANSLATED }
                .map { it.template }
        }
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
