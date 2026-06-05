package bakery.lens

/**
 * CS-FIN-9 — Cache lazy de proximités BFS pour AugmentedArticlesService.
 *
 * Problème : scoreAll() appelait buildAdjacency() + bfsDistances() pour
 * CHAQUE nœud → O(n²). Ce cache construit l'adjacency UNE FOIS et
 * calcule les proximités avec lazy evaluation.
 *
 * Architecture :
 * ```
 * SiteSubgraph
 *     │
 *     ▼
 * BfsProximityCache
 *     ├── adjacency: Map<String, List<String>>   (construit 1 fois)
 *     ├── proximityCache: Map<String, Double>       (lazy, calculé à la demande)
 *     └── proximityFor(nodeId): Double              (O(1) après calcul)
 * ```
 */
class BfsProximityCache(subgraph: SiteSubgraph) {

    private val adjacency: Map<String, List<String>> = buildAdjacency(subgraph)
    private val totalNodes: Double = subgraph.nodes.size.toDouble()

    private val proximityCache: MutableMap<String, Double> = mutableMapOf()

    /** IDs des nœuds dans le cache. */
    val nodeIds: Set<String> = subgraph.nodeIds

    /**
     * Retourne la proximité graphique normalisée [0.0–1.0] pour un nœud.
     *
     * Distance BFS → proximité inverse :
     * - distance 0 (le nœud lui-même) = 1.0
     * - distance 1 (voisins directs) = 0.7
     * - distance 2 = 0.4
     * - distance 3+ = 0.1
     * - impossible d'atteindre = 0.0
     */
    fun proximityFor(nodeId: String): Double {
        if (nodeId !in nodeIds) return 0.0
        return proximityCache.getOrPut(nodeId) {
            computeProximity(nodeId)
        }
    }

    private fun computeProximity(nodeId: String): Double {
        if (totalNodes <= 1.0) return 1.0

        val distances = bfsDistances(nodeId)
        if (distances.isEmpty()) return 1.0

        val proximityScore = distances.values.sumOf { distance ->
            when {
                distance == 0 -> 1.0
                distance == 1 -> 0.7
                distance == 2 -> 0.4
                distance >= 3 -> 0.1
                else -> 0.0
            }
        } / totalNodes

        return minOf(1.0, proximityScore)
    }

    private fun bfsDistances(sourceId: String): Map<String, Int> {
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

    private fun buildAdjacency(subgraph: SiteSubgraph): Map<String, List<String>> {
        val adjacency = mutableMapOf<String, MutableList<String>>()
        for (edge in subgraph.edges) {
            adjacency.getOrPut(edge.source) { mutableListOf() }.add(edge.target)
            adjacency.getOrPut(edge.target) { mutableListOf() }.add(edge.source)
        }
        for (node in subgraph.nodes) {
            adjacency.getOrPut(node.id) { mutableListOf() }
        }
        return adjacency
    }
}