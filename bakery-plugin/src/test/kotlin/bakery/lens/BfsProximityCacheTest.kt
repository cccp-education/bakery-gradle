package bakery.lens

import com.cheroliv.graphify.model.GraphCommunity
import com.cheroliv.graphify.model.GraphEdge
import com.cheroliv.graphify.model.GraphNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * CS-FIN-9 — BfsProximityCache : lazy cache de proximités BFS.
 *
 * Problème : AugmentedArticlesService.scoreAll() reconstruit
 * l'adjacency map et relance un BFS complet pour CHAQUE nœud → O(n²).
 *
 * Solution : BfsProximityCache construit l'adjacency UNE FOIS,
 * puis calcule les proximités à la demande avec lazy evaluation.
 *
 * Méthodologie : DDD/TDD baby steps — RED d'abord, GREEN ensuite.
 */
class BfsProximityCacheTest {

    private val nodes = listOf(
        GraphNode("a.md", "A", "file", "community-a"),
        GraphNode("b.md", "B", "file", "community-a"),
        GraphNode("c.md", "C", "file", "community-b"),
        GraphNode("d.md", "D", "file", null)
    )

    private val edges = listOf(
        GraphEdge("a.md", "b.md", "reference"),
        GraphEdge("b.md", "c.md", "reference"),
        GraphEdge("a.md", "c.md", "agent_reference"),
        GraphEdge("b.md", "d.md", "agent_reference")
    )

    private val communities = listOf(
        GraphCommunity("community-a", "Community A", 2),
        GraphCommunity("community-b", "Community B", 1)
    )

    private val subgraph = SiteSubgraph(
        nodes = nodes,
        edges = edges,
        communities = communities
    )

    private lateinit var cache: BfsProximityCache

    @BeforeEach
    fun setUp() {
        cache = BfsProximityCache(subgraph)
    }

    @Nested
    @DisplayName("CS-FIN-9 : BfsProximityCache — Construction")
    inner class Construction {

        @Test
        @DisplayName("BfsProximityCache — contient tous les nodeIds du sous-graphe")
        fun `cache contains all node ids`() {
            assertThat(cache.nodeIds).containsExactlyInAnyOrder(
                "a.md", "b.md", "c.md", "d.md"
            )
        }

        @Test
        @DisplayName("BfsProximityCache — nœud isolé a une proximité de 1.0")
        fun `isolated node has proximity 1_0`() {
            val isolatedSubgraph = SiteSubgraph(
                nodes = listOf(GraphNode("solo.md", "Solo", "file", null)),
                edges = emptyList(),
                communities = emptyList()
            )
            val isolatedCache = BfsProximityCache(isolatedSubgraph)

            assertThat(isolatedCache.proximityFor("solo.md")).isEqualTo(1.0)
        }

        @Test
        @DisplayName("BfsProximityCache — nœud inexistant a une proximité de 0.0")
        fun `non-existent node has proximity 0_0`() {
            assertThat(cache.proximityFor("unknown.md")).isEqualTo(0.0)
        }

        @Test
        @DisplayName("BfsProximityCache — sous-graphe vide retourne nodeIds vide")
        fun `empty subgraph has empty nodeIds`() {
            val emptySubgraph = SiteSubgraph(
                nodes = emptyList(),
                edges = emptyList(),
                communities = emptyList()
            )
            val emptyCache = BfsProximityCache(emptySubgraph)

            assertThat(emptyCache.nodeIds).isEmpty()
        }
    }

    @Nested
    @DisplayName("CS-FIN-9 : BfsProximityCache — Proximité BFS")
    inner class ProximityBfs {

        @Test
        @DisplayName("proximityFor — nœud bien connecté a une proximité > 0")
        fun `well connected node has proximity above zero`() {
            val proximity = cache.proximityFor("a.md")
            assertThat(proximity).isGreaterThan(0.0)
        }

        @Test
        @DisplayName("proximityFor — proximité dans [0.0, 1.0]")
        fun `proximity is clamped between 0 and 1`() {
            for (nodeId in cache.nodeIds) {
                val proximity = cache.proximityFor(nodeId)
                assertThat(proximity).isBetween(0.0, 1.0)
            }
        }

        @Test
        @DisplayName("proximityFor — nœud central a proximité > nœud périphérique")
        fun `central node has higher proximity than peripheral`() {
            val centralProximity = cache.proximityFor("a.md")
            val peripheralProximity = cache.proximityFor("d.md")

            assertThat(centralProximity).isGreaterThan(peripheralProximity)
        }

        @Test
        @DisplayName("proximityFor — résultats cohérents avec scoreAll existant")
        fun `proximity matches existing computeGraphProximity`() {
            val service = AugmentedArticlesService()
            val results = service.scoreAll(subgraph = subgraph)

            for (scored in results) {
                val cachedProximity = cache.proximityFor(scored.nodeId)
                assertThat(cachedProximity).isEqualTo(scored.graphProximity)
            }
        }
    }

    @Nested
    @DisplayName("CS-FIN-9 : BfsProximityCache — Performance")
    inner class Performance {

        @Test
        @DisplayName("BfsProximityCache — adjacency construite une seule fois (idempotent)")
        fun `adjacency is built only once`() {
            val proximity1 = cache.proximityFor("a.md")
            val proximity2 = cache.proximityFor("a.md")

            assertThat(proximity1).isEqualTo(proximity2)
        }

        @Test
        @DisplayName("BfsProximityCache — tous les nœuds accessibles immédiatement")
        fun `all nodes accessible immediately`() {
            for (nodeId in cache.nodeIds) {
                assertThat(cache.proximityFor(nodeId)).isBetween(0.0, 1.0)
            }
        }
    }
}