package bakery.lens

import com.cheroliv.graphify.model.GraphCommunity
import com.cheroliv.graphify.model.GraphEdge
import com.cheroliv.graphify.model.GraphModel
import com.cheroliv.graphify.model.GraphNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

/**
 * Extracteur de sous-graphe — Pattern LENTILLE (BKY-LENS-1).
 *
 * Filtre le graphe global (203k nœuds, 188k edges) en un sous-graphe ciblé
 * (~200 nœuds) selon les critères de [LensConfig] :
 * - communautés incluses
 * - types de nœuds inclus
 * - types d'edges inclus
 * - profondeur maximale de voisinage
 * - extensions de fichiers
 *
 * Le résultat est un [SiteSubgraph] prêt pour l'enrichissement (BKY-LENS-2).
 *
 * Architecture :
 * ```
 * graph.json (203k nœuds)
 *     ↓ SubgraphExtractor.extract(graphModel, lensConfig)
 * site-subgraph (~200 nœuds, ~300 edges)
 *     ↓ + RAG pgvector + règles métier (LENS-2)
 * site-graph prêt pour injection JBake
 * ```
 *
 * Contrat DAG : bakery (N2) importe graphify-plugin (N0).
 */
class SubgraphExtractor {

    private val objectMapper = ObjectMapper().registerKotlinModule()

    /**
     * Charge un fichier graph.json et retourne le [GraphModel].
     */
    fun loadGraph(graphFilePath: String): GraphModel {
        val file = File(graphFilePath)
        if (!file.exists()) {
            return GraphModel(nodes = emptyList(), edges = emptyList(), communities = emptyList())
        }
        return objectMapper.readValue<GraphModel>(file)
    }

    /**
     * Extrait un sous-graphe du graphe global selon la configuration [LensConfig].
     *
     * Étapes :
     * 1. Filtrer les nœuds par communautés + types + extensions
     * 2. Filtrer les edges par types
     * 3. Conserver les edges dont les deux bouts sont dans le sous-graphe
     * 4. Appliquer la profondeur max si un nœud de départ est spécifié
     *
     * @param graphModel Le graphe complet chargé depuis graph.json
     * @param lensConfig La configuration de la lentille
     * @return [SiteSubgraph] contenant les nœuds et edges filtrés
     */
    fun extract(graphModel: GraphModel, lensConfig: LensConfig): SiteSubgraph {
        if (lensConfig.scope == LensScope.FULL) {
            return SiteSubgraph(
                nodes = graphModel.nodes,
                edges = graphModel.edges,
                communities = graphModel.communities
            )
        }

        if (lensConfig.scope == LensScope.SEMANTIC_ONLY) {
            return SiteSubgraph(
                nodes = emptyList(),
                edges = emptyList(),
                communities = emptyList()
            )
        }

        // SUBGRAPH — filtrer par communautés, types, extensions

        // 1. Filtrer les nœuds
        val filteredNodes = graphModel.nodes.filter { node ->
            val communityMatch = lensConfig.communities.isEmpty() ||
                node.community in lensConfig.communities ||
                node.community == null // nœuds sans communauté = orphelins

            val typeMatch = lensConfig.nodeTypes.isEmpty() ||
                node.type in lensConfig.nodeTypes

            val extensionMatch = lensConfig.fileExtensions.isEmpty() ||
                node.id.substringAfterLast('.', "").lowercase() in lensConfig.fileExtensions ||
                node.type != "file" // les modules passent toujours le filtre d'extension

            communityMatch && typeMatch && extensionMatch
        }

        // 2. Filtrer les edges par type
        val edgeTypeSet = lensConfig.edgeTypes.toSet()
        val filteredEdges = graphModel.edges.filter { edge ->
            edgeTypeSet.isEmpty() || edge.type in edgeTypeSet
        }

        // 3. Conserver les edges dont les deux bouts sont dans le sous-graphe
        val nodeIds = filteredNodes.map { it.id }.toSet()
        val connectedEdges = filteredEdges.filter { edge ->
            edge.source in nodeIds && edge.target in nodeIds
        }

        // 4. Appliquer la profondeur max
        val resultNodes = if (lensConfig.communities.isNotEmpty()) {
            // BFS depuis les nœuds des communautés ciblées
            // maxDepth=0 → semences seulement, maxDepth=1 → +voisins directs, etc.
            val seedIds = filteredNodes
                .filter { it.community in lensConfig.communities }
                .map { it.id }
                .toSet()
            expandBfs(seedIds, nodeIds, connectedEdges, lensConfig.maxDepth)
        } else {
            // Pas de communauté cible → tous les nœuds filtrés
            nodeIds
        }

        val finalNodes = filteredNodes.filter { it.id in resultNodes }
        val finalEdges = connectedEdges.filter { it.source in resultNodes && it.target in resultNodes }

        // Filtrer les communautés visibles
        val visibleCommunityIds = finalNodes.mapNotNull { it.community }.toSet()
        val visibleCommunities = graphModel.communities.filter { it.id in visibleCommunityIds }

        return SiteSubgraph(
            nodes = finalNodes,
            edges = finalEdges,
            communities = visibleCommunities
        )
    }

    /**
     * Extrait un sous-graphe depuis un fichier graph.json.
     * Convenience method combining [loadGraph] and [extract].
     */
    fun extractFromPath(graphFilePath: String, lensConfig: LensConfig): SiteSubgraph {
        val graphModel = loadGraph(graphFilePath)
        return extract(graphModel, lensConfig)
    }

    /**
     * BFS expansion depuis des nœuds semences.
     * Retourne l'ensemble des IDs de nœuds atteignables en au plus [maxDepth] sauts.
     * maxDepth=0 retourne les semences uniquement.
     */
    private fun expandBfs(
        seedIds: Set<String>,
        candidateIds: Set<String>,
        edges: List<GraphEdge>,
        maxDepth: Int
    ): Set<String> {
        val seeds = seedIds.intersect(candidateIds)
        if (maxDepth <= 0) return seeds

        // Construire l'adjacence (bidirectionnelle)
        val adjacency = mutableMapOf<String, MutableList<String>>()
        for (edge in edges) {
            if (edge.source in candidateIds && edge.target in candidateIds) {
                adjacency.getOrPut(edge.source) { mutableListOf() }.add(edge.target)
                adjacency.getOrPut(edge.target) { mutableListOf() }.add(edge.source)
            }
        }

        val visited = mutableSetOf<String>()
        visited.addAll(seeds)

        var currentLevel = seeds.toMutableSet()

        for (depth in 1..maxDepth) {
            if (currentLevel.isEmpty()) break

            val nextLevel = mutableSetOf<String>()
            for (nodeId in currentLevel) {
                for (neighbor in adjacency[nodeId] ?: emptyList()) {
                    if (neighbor !in visited && neighbor in candidateIds) {
                        nextLevel.add(neighbor)
                    }
                }
            }
            visited.addAll(nextLevel)
            currentLevel = nextLevel.toMutableSet()
        }

        return visited
    }
}

/**
 * Résultat du filtrage du graphe — sous-graphe ciblé.
 *
 * Contient les nœuds, edges et communautés filtrés prêts
 * pour l'enrichissement RAG (BKY-LENS-2).
 */
data class SiteSubgraph(
    /** Nœuds filtrés du sous-graphe */
    val nodes: List<GraphNode>,
    /** Edges filtrés (les deux bouts dans [nodes]) */
    val edges: List<GraphEdge>,
    /** Communautés visibles dans le sous-graphe */
    val communities: List<GraphCommunity>
) {
    /** Nombre de nœuds dans le sous-graphe */
    val nodeCount: Int get() = nodes.size

    /** Nombre d'edges dans le sous-graphe */
    val edgeCount: Int get() = edges.size

    /** Nombre de communautés visibles */
    val communityCount: Int get() = communities.size

    /** IDs des nœuds pour lookup rapide */
    val nodeIds: Set<String> get() = nodes.map { it.id }.toSet()

    /** IDs des communautés visibles */
    val communityIds: Set<String> get() = communities.map { it.id }.toSet()

    /** Nœuds groupés par communauté */
    val nodesByCommunity: Map<String?, List<GraphNode>> get() = nodes.groupBy { it.community }

    /** Edges groupés par type */
    val edgesByType: Map<String, List<GraphEdge>> get() = edges.groupBy { it.type }

    /** Retourne les nœuds correspondant à une communauté donnée */
    fun nodesInCommunity(communityId: String): List<GraphNode> =
        nodes.filter { it.community == communityId }

    /** Retourne les edges d'un type donné */
    fun edgesOfType(type: String): List<GraphEdge> =
        edges.filter { it.type == type }

    /** Retourne les voisins d'un nœud */
    fun neighbors(nodeId: String): List<GraphNode> {
        val neighborIds = edges
            .filter { it.source == nodeId || it.target == nodeId }
            .map { if (it.source == nodeId) it.target else it.source }
            .toSet()
        return nodes.filter { it.id in neighborIds }
    }
}