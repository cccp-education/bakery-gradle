package bakery.lens

import com.cheroliv.graphify.model.GraphNode
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
 * Architecture :
 * ```
 * ragResults (pgvector) ───────┐
 * subgraph (graphify) ────────┼──▶ score() ──▶ ScoredNode
 * nodeTags (JBake metadata) ──┤       │
 * currentPageCommunity ─────────┘       ↓
 *                                    scoreAll()
 *                                  applyRules()
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

        // 1. RAG Similarity (α × ragSimilarity)
        val ragSimilarity = ragResults[nodeId] ?: 0.0

        // 2. Graph Proximity (β × graphProximity) — BFS distance normalisée
        val graphProximity = computeGraphProximity(nodeId, subgraph)

        // 3. Tag Overlap (γ × tagOverlap) — Jaccard index
        val nodeTagList = nodeTags[nodeId] ?: emptyList()
        val tagOverlap = jaccardSimilarity(currentPageTags, nodeTagList)

        // 4. Cross-ref Count (δ × crossRefCount) — agent_reference edges vers ce nœud
        val crossRefCount = subgraph.edges.count {
            it.type == "agent_reference" && (it.target == nodeId || it.source == nodeId)
        }

        // Score composite pondéré (avant bonuses)
        var score = ALPHA * ragSimilarity +
            BETA * graphProximity +
            GAMMA * tagOverlap +
            DELTA * min(1.0, crossRefCount / 3.0) // Normalisé sur 3 références

        // Bonus communauté
        if (lensRules.communityAffinity > 0 &&
            currentPageCommunity != null &&
            node.community == currentPageCommunity
        ) {
            score += lensRules.communityAffinity
        }

        // Bonus cross-reference
        if (lensRules.prioritizeCrossReferences && crossRefCount > 0) {
            score += lensRules.crossRefBonus
        }

        // Clamp score dans [0.0, 1.0] (les bonuses peuvent dépasser)
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
     * Score tous les nœuds du sous-graphe.
     *
     * @param subgraph Sous-graphe contenant les nœuds à scorer
     * @param ragResults Map nodeId → similarité RAG
     * @param nodeTags Map nodeId → liste de tags
     * @param currentPageTags Tags de la page courante
     * @param currentPageCommunity Communauté de la page courante
     * @param lensRules Règles métier
     * @return Liste de [ScoredNode] triée par score décroissant
     */
    fun scoreAll(
        subgraph: SiteSubgraph,
        ragResults: Map<String, Double> = emptyMap(),
        nodeTags: Map<String, List<String>> = emptyMap(),
        currentPageTags: List<String> = emptyList(),
        currentPageCommunity: String? = null,
        lensRules: LensRules = LensRules()
    ): List<ScoredNode> {
        return subgraph.nodes
            .map { node ->
                score(
                    nodeId = node.id,
                    subgraph = subgraph,
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
     * - [LensRules.excludeDrafts] : exclure les nœuds avec tag "draft"
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
        return scoredNodes.filter { node ->
            // excludeDrafts : exclure "draft" dans les tags du nœud
            val draftOk = !lensRules.excludeDrafts ||
                "draft" !in node.tags.map { it.lowercase() }

            // excludeTags : aucun tag interdit dans les tags du nœud
            val excludeTagsLower = lensRules.excludeTags.map { it.lowercase() }.toSet()
            val tagsOk = node.tags.map { it.lowercase() }.none { it in excludeTagsLower }

            draftOk && tagsOk
        }
    }

    // ──────────────────────────────────────────────────
    // Méthodes privées — Calculs de proximité
    // ──────────────────────────────────────────────────

    /**
     * Calcule la proximité graphique normalisée [0.0–1.0] depuis un nœud source.
     *
     * Distance BFS → proximité inverse.
     * - distance 0 (le nœud lui-même) = 1.0
     * - distance 1 (voisins directs) = 0.7
     * - distance 2 = 0.4
     * - distance 3+ = 0.1
     * - impossible d'atteindre = 0.0
     *
     * @param nodeId Nœud cible (distance depuis le nœud avec le plus court chemin depuis les autres)
     * Attendu : on passe le nodeId de la page courante comme source implicite,
     * mais ici on calcule la "centralité" relative dans le graphe.
     */
    private fun computeGraphProximity(
        nodeId: String,
        subgraph: SiteSubgraph
    ): Double {
        // Si le nœud n'est pas dans le sous-graphe → 0
        if (nodeId !in subgraph.nodeIds) return 0.0

        // BFS depuis le nœud pour calculer les distances vers tous les autres
        val distances = bfsDistances(nodeId, subgraph)

        // Proximité moyenne pondérée : plus c'est proche de beaucoup de nœuds, plus c'est haut
        if (distances.isEmpty()) return 1.0 // nœud isolé mais présent

        val totalNodes = subgraph.nodes.size.toDouble()
        if (totalNodes <= 1) return 1.0

        val proximityScore = distances.values.sumOf { distance ->
            when {
                distance == 0 -> 1.0
                distance == 1 -> 0.7
                distance == 2 -> 0.4
                distance >= 3 -> 0.1
                else -> 0.0
            }
        } / totalNodes

        return min(1.0, proximityScore)
    }

    /**
     * BFS distances depuis un nœud source dans le sous-graphe.
     * Retourne une map nodeId → distance en nombre d'edges.
     * Limite à [maxBfsDepth] sauts.
     */
    private fun bfsDistances(
        sourceId: String,
        subgraph: SiteSubgraph
    ): Map<String, Int> {
        val adjacency = buildAdjacency(subgraph)
        val distances = mutableMapOf<String, Int>()
        val queue = ArrayDeque<String>()
        val visited = mutableSetOf<String>()

        queue.add(sourceId)
        visited.add(sourceId)
        distances[sourceId] = 0

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val currentDepth = distances[current] ?: 0
            for (neighbor in adjacency[current] ?: emptyList()) {
                if (neighbor !in visited) {
                    visited.add(neighbor)
                    distances[neighbor] = currentDepth + 1
                    queue.add(neighbor)
                }
            }
        }

        return distances
    }

    /**
     * Construit une map d'adjacence bidirectionnelle depuis le sous-graphe.
     */
    private fun buildAdjacency(subgraph: SiteSubgraph): Map<String, List<String>> {
        val adjacency = mutableMapOf<String, MutableList<String>>()
        for (edge in subgraph.edges) {
            adjacency.getOrPut(edge.source) { mutableListOf() }.add(edge.target)
            adjacency.getOrPut(edge.target) { mutableListOf() }.add(edge.source)
        }
        // Assurer que tous les nœuds ont une entrée (même isolés)
        for (node in subgraph.nodes) {
            adjacency.getOrPut(node.id) { mutableListOf() }
        }
        return adjacency
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
