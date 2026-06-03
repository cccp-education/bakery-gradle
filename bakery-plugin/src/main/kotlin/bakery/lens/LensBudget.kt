package bakery.lens

import org.gradle.api.Action

/**
 * Budget du Pattern LENTILLE — BKY-LENS-3.
 *
 * Contrôle le nombre d'articles connexes affichés par page et
 * le seuil de similarité minimal en dessous duquel un article est exclu.
 *
 * 3 couches du Pattern LENTILLE :
 * 1. SÉGRÉGATION (BKY-LENS-1) : filtrer le graphe global
 * 2. ENRICHISSEMENT (BKY-LENS-2) : scoring hybride RAG+KG+tags+xref
 * 3. BUDGET (cette config) : tronquer à N articles/page, similarité min
 *
 * Usage:
 * ```
 * bakery {
 *     augmentedContext {
 *         budget {
 *             maxArticlesPerPage = 6
 *             minSimilarity = 0.8
 *         }
 *     }
 * }
 * ```
 */
data class LensBudget(
    /** Nombre max d'articles connexes par page (défaut: 4) */
    var maxArticlesPerPage: Int = 4,

    /** Seuil de similarité minimum pour les suggestions (défaut: 0.7) */
    var minSimilarity: Double = 0.7
) {
    /**
     * Filtre une liste de nœuds scorés selon le budget.
     *
     * 1. Exclut les nœuds dont le score < minSimilarity
     * 2. Garde les maxArticlesPerPage meilleurs
     *
     * @param scoredNodes Liste de nœuds scorés (tri décroissant recommandé)
     * @return Nœuds filtrés et tronqués
     */
    fun apply(scoredNodes: List<ScoredNode>): List<ScoredNode> {
        return scoredNodes
            .filter { it.score >= minSimilarity }
            .take(maxArticlesPerPage)
    }
}