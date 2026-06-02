package bakery.lens

/**
 * Règles métier éditoriales du Pattern LENTILLE — BKY-LENS-2.
 *
 * Contrôle le filtrage et les bonus appliqués lors de l'enrichissement
 * du sous-graphe par le scoring hybride (RAG + KG + tags + xref).
 *
 * Override order: CLI > gradle.properties > DSL > site.yml > defaults
 *
 * Usage:
 * ```
 * bakery {
 *     augmentedContext {
 *         lens {
 *             rules {
 *                 excludeDrafts = true
 *                 excludeTags = listOf("wip", "draft")
 *                 prioritizeCrossReferences = true
 *                 crossRefBonus = 0.2
 *                 communityAffinity = 0.3
 *             }
 *         }
 *     }
 * }
 * ```
 */
data class LensRules(
    /** Exclure les articles en brouillon (jbake-status = draft). */
    var excludeDrafts: Boolean = true,

    /** Tags éditoriaux à exclure du scoring (ex: ["wip", "draft"]). */
    var excludeTags: List<String> = listOf("wip", "draft"),

    /** Prioriser les articles qui ont des cross-references AsciiDoc (xref) vers la page courante. */
    var prioritizeCrossReferences: Boolean = true,

    /** Bonus de score pour les cross-references AsciiDoc vérifiées (défaut: 0.2). */
    var crossRefBonus: Double = 0.2,

    /** Bonus de score pour les articles dans la même communauté que la page courante (défaut: 0.3). */
    var communityAffinity: Double = 0.3
)