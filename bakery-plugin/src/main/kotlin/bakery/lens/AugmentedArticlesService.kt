package bakery.lens

import kotlin.math.max
import kotlin.math.min

/**
 * Scoring hybride pour le Pattern LENTILLE — BKY-LENS-2.2.
 *
 * Combine 4 signaux pour calculer un score de pertinence composite :
 * - RAG (α = 0.40) : similarité sémantique via pgvector
 * - Graph (β = 0.30) : proximité structurelle dans le sous-graphe (BFS)
 * - Tags (γ = 0.15) : recouvrement Jaccard des tags éditoriaux
 * - Cross-ref (δ = 0.15) : nombre de xref AsciiDoc pointant vers ce nœud
 *
 * Bonuses optionnels :
 * - Community (+0.2) : même communauté que la page courante
 * - CrossRef (+0.2) : xref AsciiDoc vérifiée (si prioritizeCrossReferences)
 *
 * Architecture (CS-FIN-9 : cache BFS O(n) au lieu de O(n²)) :
 * ```
 * ragResults (pgvector) ───────┐
 * subgraph (graphify) ────────┼──▶ BfsProximityCache ──▶ score() ──▶ ScoredNode
 * nodeTags (JBake metadata) ──┤                              │
 * currentPageCommunity ─────────┘                              ↓
 *                                                        scoreAll()
 *                                                      applyRules()
 * ```
 */
class AugmentedArticlesService {

    companion object {
        const val ALPHA = 0.40           // poids RAG
        const val BETA = 0.30            // poids graphe
        const val GAMMA = 0.15           // poids tags
        const val DELTA = 0.15           // poids xref
        const val COMMUNITY_BONUS = 0.2  // bonus même communauté
        const val CROSS_REF_BONUS = 0.2  // bonus xref vérifiée
    }

    /**
     * Calcule le score hybride pour un nœud donné.
     *
     * @param nodeId ID du nœud à scorer
     * @param subgraph Sous-graphe du site (contient nœuds et edges)
     * @param ragResults Map nodeId → similarité RAG (0.0–1.0). Vide si RAG désactivé.
     * @param nodeTags Map nodeId → liste de tags éditoriaux (JBake metadata)
     * @param currentPageCommunity Communauté de la page courante (pour communityAffinity bonus)
     * @param lensRules Règles métier (communityAffinity, prioritizeCrossReferences, etc.)
     * @param currentPageTags Tags de la page courante (pour le recouvrement Jaccard)
     * @return [ScoredNode] avec tous les composants et le score composite
     */
    fun score(
        nodeId: String,
        subgraph: SiteSubgraph,
        ragResults: Map<String, Double> = emptyMap(),
        nodeTags: Map<String, List<String>> = emptyMap(),
        currentPageTags: List<String> = emptyList(),
        currentPageCommunity: String? = null,
        lensRules: LensRules = LensRules()
    ): ScoredNode {
        val proximityCache = BfsProximityCache(subgraph)
        return scoreWithCache(
            nodeId = nodeId,
            subgraph = subgraph,
            proximityCache = proximityCache,
            ragResults = ragResults,
            nodeTags = nodeTags,
            currentPageTags = currentPageTags,
            currentPageCommunity = currentPageCommunity,
            lensRules = lensRules
        )
    }

    /**
     * Score un nœud en réutilisant un cache de proximités BFS.
     * Utilisé par [scoreAll] pour éviter la reconstruction O(n²) de l'adjacency.
     */
    fun scoreWithCache(
        nodeId: String,
        subgraph: SiteSubgraph,
        proximityCache: BfsProximityCache,
        ragResults: Map<String, Double> = emptyMap(),
        nodeTags: Map<String, List<String>> = emptyMap(),
        currentPageTags: List<String> = emptyList(),
        currentPageCommunity: String? = null,
        lensRules: LensRules = LensRules()
    ): ScoredNode {
        val node = subgraph.nodes.find { it.id == nodeId }
            ?: return ScoredNode(
                nodeId = nodeId,
                nodeName = nodeId,
                community = null,
                tags = emptyList(),
                ragSimilarity = 0.0,
                graphProximity = 0.0,
                tagOverlap = 0.0,
                crossRefCount = 0,
                score = 0.0
            )

        val ragSimilarity = ragResults[nodeId] ?: 0.0
        val graphProximity = proximityCache.proximityFor(nodeId)
        val nodeTagList = nodeTags[nodeId] ?: emptyList()
        val tagOverlap = jaccardSimilarity(currentPageTags, nodeTagList)
        val crossRefCount = subgraph.edges.count {
            it.type == "agent_reference" && (it.target == nodeId || it.source == nodeId)
        }

        var score = ALPHA * ragSimilarity +
            BETA * graphProximity +
            GAMMA * tagOverlap +
            DELTA * min(1.0, crossRefCount / 3.0)

        if (lensRules.communityAffinity > 0 &&
            currentPageCommunity != null &&
            node.community == currentPageCommunity
        ) {
            score += lensRules.communityAffinity
        }

        if (lensRules.prioritizeCrossReferences && crossRefCount > 0) {
            score += lensRules.crossRefBonus
        }

        score = min(1.0, max(0.0, score))

        return ScoredNode(
            nodeId = nodeId,
            nodeName = node.label,
            community = node.community,
            tags = nodeTagList,
            ragSimilarity = ragSimilarity,
            graphProximity = graphProximity,
            tagOverlap = tagOverlap,
            crossRefCount = crossRefCount,
            score = score
        )
    }

    /**
     * Score tous les nœuds du sous-graphe — O(n) grâce au cache BFS.
     *
     * CS-FIN-9 : Construit [BfsProximityCache] une seule fois,
     * puis l'utilise pour chaque nœud. L'adjacency map et les
     * distances BFS sont calculées à la demande et mises en cache.
     */
    fun scoreAll(
        subgraph: SiteSubgraph,
        ragResults: Map<String, Double> = emptyMap(),
        nodeTags: Map<String, List<String>> = emptyMap(),
        currentPageTags: List<String> = emptyList(),
        currentPageCommunity: String? = null,
        lensRules: LensRules = LensRules()
    ): List<ScoredNode> {
        val proximityCache = BfsProximityCache(subgraph)
        return subgraph.nodes
            .map { node ->
                scoreWithCache(
                    nodeId = node.id,
                    subgraph = subgraph,
                    proximityCache = proximityCache,
                    ragResults = ragResults,
                    nodeTags = nodeTags,
                    currentPageTags = currentPageTags,
                    currentPageCommunity = currentPageCommunity,
                    lensRules = lensRules
                )
            }
            .sortedByDescending { it.score }
    }

    /**
     * Applique les règles métier pour filtrer les nœuds scorés.
     *
     * Filtres :
     * - [LensRules.excludeTags] : exclure les nœuds contenant un tag interdit
     *
     * @param scoredNodes Liste de nœuds scorés
     * @param lensRules Règles à appliquer
     * @return Nœuds filtrés
     */
    fun applyRules(
        scoredNodes: List<ScoredNode>,
        lensRules: LensRules
    ): List<ScoredNode> {
        val excludeTagsLower = lensRules.excludeTags.map { it.lowercase() }.toSet()
        return scoredNodes.filter { node ->
            node.tags.map { it.lowercase() }.none { it in excludeTagsLower }
        }
    }

    /**
     * Indice de Jaccard entre deux ensembles de tags.
     * |A ∩ B| / |A ∪ B|. Retourne 0.0 si l'union est vide.
     */
    private fun jaccardSimilarity(a: List<String>, b: List<String>): Double {
        val setA = a.map { it.lowercase() }.toSet()
        val setB = b.map { it.lowercase() }.toSet()
        val intersection = setA.intersect(setB)
        val union = setA.union(setB)
        return if (union.isEmpty()) 0.0 else intersection.size.toDouble() / union.size.toDouble()
    }
}

/**
 * Nœud scoré — résultat du scoring hybride LENS-2.
 *
 * Tous les composants du score sont exposés pour l'affichage dans les templates
 * (ex: "Similaire à 87% (RAG) + dans la même communauté").
 */
data class ScoredNode(
    /** ID du nœud dans le graphe */
    val nodeId: String,
    /** Label human-readable */
    val nodeName: String,
    /** Communauté du nœud (null si orphelin) */
    val community: String?,
    /** Tags éditoriaux JBake */
    val tags: List<String>,
    /** Similarité sémantique RAG (0.0–1.0) */
    val ragSimilarity: Double,
    /** Proximité structurelle dans le graphe (0.0–1.0) */
    val graphProximity: Double,
    /** Recouvrement Jaccard des tags (0.0–1.0) */
    val tagOverlap: Double,
    /** Nombre de xref AsciiDoc pointant vers ce nœud */
    val crossRefCount: Int,
    /** Score composite final (0.0–1.0+, clampé) */
    val score: Double
)
