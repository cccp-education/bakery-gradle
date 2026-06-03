package bakery.lens

/**
 * Résumé du budget appliqué dans le pipeline LENS.
 *
 * @property maxArticlesPerPage Nombre maximum d'articles suggérés par page
 * @property minSimilarity Seuil de similarité minimal pour qu'un article soit retenu
 */
data class BudgetSummary(
    val maxArticlesPerPage: Int,
    val minSimilarity: Double
)

/**
 * Sortie standardisée du pipeline LENTILLE (BKY-LENS-1→3).
 *
 * Contient les métriques complètes du pipeline pour le diagnostic
 * et l'injection dans les templates JBake.
 *
 * @property version Version du format de sortie (défaut "1.0")
 * @property pipeline Identifiant du pipeline (défaut "LENS")
 * @property budget Résumé du budget appliqué
 * @property scoredNodes Articles scorés et retenus après budget
 * @property totalCandidates Nombre total de nœuds scorés (avant règles)
 * @property totalAfterRules Nombre de nœuds après application des règles métier
 * @property totalAfterBudget Nombre de nœuds après application du budget
 */
data class LensPipelineOutput(
    val version: String = "1.0",
    val pipeline: String = "LENS",
    val budget: BudgetSummary,
    val scoredNodes: List<ScoredNode>,
    val totalCandidates: Int,
    val totalAfterRules: Int,
    val totalAfterBudget: Int
)
