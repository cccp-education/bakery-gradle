package bakery.lens

import com.cheroliv.graphify.model.GraphCommunity
import com.cheroliv.graphify.model.GraphEdge
import com.cheroliv.graphify.model.GraphNode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * BKY-LENS-2.2 — Tests unitaires pour AugmentedArticlesService (scoring hybride).
 *
 * Méthodologie : DDD/TDD baby steps — chaque test compile ET passe AVANT de passer au suivant.
 *
 * Fixture : sous-graphe de test (6 nœuds, 3 communautés, cross-references agent_reference).
 */
class AugmentedArticlesServiceTest {

    private lateinit var service: AugmentedArticlesService

    // ─── Graphe de test en mémoire ───

    private val nodes = listOf(
        // Communauté A
        GraphNode("page.md", "Page Courante", "file", "community-a"),
        GraphNode("article-a.md", "Article A", "file", "community-a"),
        GraphNode("article-b.md", "Article B", "file", "community-a"),
        // Communauté B (cross)
        GraphNode("article-c.md", "Article C", "file", "community-b"),
        // Orphelin
        GraphNode("orphan.md", "Orphan", "file", community = null),
        // Draft
        GraphNode("draft-article.md", "Draft Article", "file", "community-a")
    )

    private val edges = listOf(
        // Références directes
        GraphEdge("page.md", "article-a.md", "reference"),
        GraphEdge("article-a.md", "article-b.md", "reference"),
        // Cross-reference agent_reference
        GraphEdge("page.md", "article-c.md", "agent_reference"),
        GraphEdge("article-a.md", "article-c.md", "agent_reference"),
        GraphEdge("article-b.md", "orphan.md", "agent_reference"),
        // Containment
        GraphEdge("community-a", "page.md", "contains"),
        GraphEdge("community-a", "draft-article.md", "contains")
    )

    private val communities = listOf(
        GraphCommunity("community-a", "Community A", 4),
        GraphCommunity("community-b", "Community B", 1)
    )

    private val subgraph: SiteSubgraph
        get() = SiteSubgraph(
            nodes = nodes,
            edges = edges,
            communities = communities
        )

    @BeforeEach
    fun setUp() {
        service = AugmentedArticlesService()
    }

    // ──────────────────────────────────────
    // LENS-2.2 : score() — scoring hybride
    // ──────────────────────────────────────

    @Nested
    @DisplayName("LENS-2.2 : AugmentedArticlesService — Scoring hybride")
    inner class ScoringHybrid {

        @Test
        @DisplayName("score() retourne un ScoredNode pour un nœud existant")
        fun `score returns scored node for existing node`() {
            val result = service.score(
                nodeId = "article-a.md",
                subgraph = subgraph
            )

            assertThat(result.nodeId).isEqualTo("article-a.md")
            assertThat(result.nodeName).isEqualTo("Article A")
            assertThat(result.community).isEqualTo("community-a")
        }

        @Test
        @DisplayName("score() retourne 0 pour un nœud inexistant")
        fun `score returns zero for non-existent node`() {
            val result = service.score(
                nodeId = "unknown.md",
                subgraph = subgraph
            )

            assertThat(result.score).isEqualTo(0.0)
            assertThat(result.ragSimilarity).isEqualTo(0.0)
            assertThat(result.graphProximity).isEqualTo(0.0)
        }

        @Test
        @DisplayName("score() — RAG donne α × similarité")
        fun `score uses RAG similarity with ALPHA weight`() {
            val ragResults = mapOf("article-a.md" to 1.0)
            val result = service.score(
                nodeId = "article-a.md",
                subgraph = subgraph,
                ragResults = ragResults
            )

            assertThat(result.ragSimilarity).isEqualTo(1.0)
            assertThat(result.score).isGreaterThan(0.0)
        }

        @Test
        @DisplayName("score() — Graph Proximity > 0 pour voisins directs")
        fun `score has graphProximity for connected nodes`() {
            val result = service.score(
                nodeId = "article-a.md",
                subgraph = subgraph
            )

            assertThat(result.graphProximity).isGreaterThan(0.0)
        }

        @Test
        @DisplayName("score() — Tag overlap Jaccard = 1.0 si tags identiques")
        fun `score tagOverlap is 1_0 for identical tags`() {
            val nodeTags = mapOf(
                "page.md" to listOf("kotlin", "gradle"),
                "article-a.md" to listOf("kotlin", "gradle")
            )
            val result = service.score(
                nodeId = "article-a.md",
                subgraph = subgraph,
                nodeTags = nodeTags,
                currentPageTags = listOf("kotlin", "gradle")
            )

            assertThat(result.tagOverlap).isEqualTo(1.0)
        }

        @Test
        @DisplayName("score() — Tag overlap Jaccard = 0 si aucun tag commun")
        fun `score tagOverlap is 0 for no common tags`() {
            val nodeTags = mapOf(
                "article-c.md" to listOf("java", "spring")
            )
            val result = service.score(
                nodeId = "article-c.md",
                subgraph = subgraph,
                nodeTags = nodeTags,
                currentPageTags = listOf("kotlin", "gradle")
            )

            assertThat(result.tagOverlap).isEqualTo(0.0)
        }

        @Test
        @DisplayName("score() — crossRefCount compte les agent_reference")
        fun `score crossRefCount counts agent_reference edges`() {
            val result = service.score(
                nodeId = "article-c.md",
                subgraph = subgraph
            )

            // article-c.md a 2 agent_reference edges
            assertThat(result.crossRefCount).isEqualTo(2)
        }

        @Test
        @DisplayName("score() — RAG désactivé (ragResults vide) = similarity 0")
        fun `score with empty ragResults has zero ragSimilarity`() {
            val result = service.score(
                nodeId = "article-a.md",
                subgraph = subgraph,
                ragResults = emptyMap()
            )

            assertThat(result.ragSimilarity).isEqualTo(0.0)
        }
    }

    // ──────────────────────────────────────
    // LENS-2.2 : Bonus — communityAffinity, prioritizeCrossReferences
    // ──────────────────────────────────────

    @Nested
    @DisplayName("LENS-2.2 : AugmentedArticlesService — Bonus")
    inner class Bonus {

        @Test
        @DisplayName("score() — même communauté = bonus communityAffinity")
        fun `score includes communityAffinity bonus for same community`() {
            val rules = LensRules(communityAffinity = 0.2)
            val ragResults = mapOf("article-a.md" to 0.5)

            val resultSame = service.score(
                nodeId = "article-a.md",
                subgraph = subgraph,
                ragResults = ragResults,
                currentPageCommunity = "community-a",
                lensRules = rules
            )

            val resultDiff = service.score(
                nodeId = "article-c.md",
                subgraph = subgraph,
                ragResults = ragResults,
                currentPageCommunity = "community-a",
                lensRules = rules
            )

            // Même communauté → score supérieur
            assertThat(resultSame.score).isGreaterThan(resultDiff.score)
        }

        @Test
        @DisplayName("score() — cross-reference = bonus crossRefBonus")
        fun `score includes crossRefBonus for prioritized cross-references`() {
            val rules = LensRules(
                prioritizeCrossReferences = true,
                crossRefBonus = 0.2
            )
            val result = service.score(
                nodeId = "article-c.md",
                subgraph = subgraph,
                lensRules = rules
            )

            // article-c.md a des agent_reference → bonus si prioritizeCrossReferences
            assertThat(result.score).isGreaterThan(0.0)
        }

        @Test
        @DisplayName("score() — pas de bonus community si communityAffinity = 0")
        fun `score no community bonus when affinity is zero`() {
            val rules = LensRules(communityAffinity = 0.0)
            val result = service.score(
                nodeId = "article-a.md",
                subgraph = subgraph,
                currentPageCommunity = "community-a",
                lensRules = rules
            )

            assertThat(result.score).isLessThan(1.0)
        }
    }

    // ──────────────────────────────────────
    // LENS-2.2 : scoreAll() — scorer tous les nœuds
    // ──────────────────────────────────────

    @Nested
    @DisplayName("LENS-2.2 : AugmentedArticlesService — scoreAll()")
    inner class ScoreAll {

        @Test
        @DisplayName("scoreAll() retourne tous les nœuds du sous-graphe")
        fun `scoreAll returns all nodes in subgraph`() {
            val results = service.scoreAll(
                subgraph = subgraph,
                currentPageCommunity = "community-a"
            )

            assertThat(results).hasSize(nodes.size)
        }

        @Test
        @DisplayName("scoreAll() trie par score décroissant")
        fun `scoreAll sorts by descending score`() {
            val ragResults = mapOf("article-a.md" to 1.0)
            val results = service.scoreAll(
                subgraph = subgraph,
                ragResults = ragResults,
                currentPageCommunity = "community-a"
            )

            // Vérifier que c'est trié par score décroissant
            for (i in 0 until results.size - 1) {
                assertThat(results[i].score).isGreaterThanOrEqualTo(results[i + 1].score)
            }
        }

        @Test
        @DisplayName("scoreAll() inclut les scores individuels")
        fun `scoreAll includes individual score components`() {
            val nodeTags = mapOf(
                "article-a.md" to listOf("kotlin"),
                "article-b.md" to listOf("kotlin")
            )
            val results = service.scoreAll(
                subgraph = subgraph,
                nodeTags = nodeTags,
                currentPageTags = listOf("kotlin")
            )

            val articleAResult = results.find { it.nodeId == "article-a.md" }
            assertThat(articleAResult).isNotNull
            assertThat(articleAResult!!.tagOverlap).isGreaterThan(0.0)
        }
    }

    // ──────────────────────────────────────
    // LENS-2.2 : applyRules() — filtrage
    // ──────────────────────────────────────

    @Nested
    @DisplayName("LENS-2.2 : AugmentedArticlesService — applyRules()")
    inner class ApplyRules {

        @Test
        @DisplayName("applyRules() — excludeDrafts filtre les nœuds avec tag draft")
        fun `applyRules filters draft nodes`() {
            val scoredNodes = listOf(
                ScoredNode("a.md", "A", null, listOf("draft"), 0.5, 0.3, 0.1, 0, 0.5),
                ScoredNode("b.md", "B", null, listOf("published"), 0.5, 0.3, 0.1, 0, 0.5),
                ScoredNode("c.md", "C", null, emptyList(), 0.5, 0.3, 0.1, 0, 0.5)
            )

            val rules = LensRules(excludeDrafts = true)
            val filtered = service.applyRules(scoredNodes, rules)

            assertThat(filtered).hasSize(2)
            assertThat(filtered.map { it.nodeId }).containsExactly("b.md", "c.md")
        }

        @Test
        @DisplayName("applyRules() — excludeDrafts false ne filtre pas")
        fun `applyRules does not filter when excludeDrafts is false`() {
            val scoredNodes = listOf(
                ScoredNode("a.md", "A", null, listOf("draft"), 0.5, 0.3, 0.1, 0, 0.5),
                ScoredNode("b.md", "B", null, listOf("published"), 0.5, 0.3, 0.1, 0, 0.5)
            )

            val rules = LensRules(excludeDrafts = false, excludeTags = emptyList())
            val filtered = service.applyRules(scoredNodes, rules)

            assertThat(filtered).hasSize(2)
        }

        @Test
        @DisplayName("applyRules() — excludeTags filtre les tags interdits (insensible à la casse)")
        fun `applyRules filters excluded tags case insensitive`() {
            val scoredNodes = listOf(
                ScoredNode("a.md", "A", null, listOf("WIP"), 0.5, 0.3, 0.1, 0, 0.5),
                ScoredNode("b.md", "B", null, listOf("wip"), 0.5, 0.3, 0.1, 0, 0.5),
                ScoredNode("c.md", "C", null, listOf("published"), 0.5, 0.3, 0.1, 0, 0.5)
            )

            val rules = LensRules(excludeTags = listOf("wip"))
            val filtered = service.applyRules(scoredNodes, rules)

            assertThat(filtered).hasSize(1)
            assertThat(filtered.first().nodeId).isEqualTo("c.md")
        }

        @Test
        @DisplayName("applyRules() — nœuds sans tags ne sont pas filtrés")
        fun `applyRules does not filter nodes without tags`() {
            val scoredNodes = listOf(
                ScoredNode("a.md", "A", null, emptyList(), 0.5, 0.3, 0.1, 0, 0.5),
                ScoredNode("b.md", "B", null, listOf("wip"), 0.5, 0.3, 0.1, 0, 0.5)
            )

            val rules = LensRules(excludeTags = listOf("wip"))
            val filtered = service.applyRules(scoredNodes, rules)

            assertThat(filtered).hasSize(1)
            assertThat(filtered.first().nodeId).isEqualTo("a.md")
        }
    }

    // ──────────────────────────────────────
    // LENS-2.3 : Boundary cases — cas aux limites
    // ──────────────────────────────────────

    @Nested
    @DisplayName("LENS-2.3 : AugmentedArticlesService — Boundary cases")
    inner class BoundaryCases {

        @Test
        @DisplayName("scoreAll() avec sous-graphe vide retourne liste vide")
        fun `scoreAll returns empty list for empty subgraph`() {
            val empty = SiteSubgraph(
                nodes = emptyList(),
                edges = emptyList(),
                communities = emptyList()
            )
            val results = service.scoreAll(subgraph = empty)
            assertThat(results).isEmpty()
        }

        @Test
        @DisplayName("score() avec sous-graphe 1 nœud isolé → graphProximity = 1.0")
        fun `score single isolated node has graphProximity 1_0`() {
            val singleSubgraph = SiteSubgraph(
                nodes = listOf(GraphNode("solo.md", "Solo", "file", null)),
                edges = emptyList(),
                communities = emptyList()
            )
            val result = service.score(nodeId = "solo.md", subgraph = singleSubgraph)

            assertThat(result.graphProximity).isEqualTo(1.0)
        }

        @Test
        @DisplayName("score() — clamp à 1.0 quand bonuses font dépasser")
        fun `score is clamped to 1_0 when bonuses exceed`() {
            val ragResults = mapOf("article-a.md" to 1.0)
            val nodeTags = mapOf(
                "article-a.md" to listOf("kotlin", "gradle"),
                "page.md" to listOf("kotlin", "gradle")
            )
            val rules = LensRules(
                communityAffinity = 0.3,
                prioritizeCrossReferences = true,
                crossRefBonus = 0.2
            )
            val result = service.score(
                nodeId = "article-a.md",
                subgraph = subgraph,
                ragResults = ragResults,
                nodeTags = nodeTags,
                currentPageTags = listOf("kotlin", "gradle"),
                currentPageCommunity = "community-a",
                lensRules = rules
            )

            assertThat(result.score).isLessThanOrEqualTo(1.0)
        }

        @Test
        @DisplayName("score() — Jaccard partial overlap = 0.5")
        fun `score jaccard partial overlap`() {
            val nodeTags = mapOf(
                "article-a.md" to listOf("kotlin")
            )
            val result = service.score(
                nodeId = "article-a.md",
                subgraph = subgraph,
                nodeTags = nodeTags,
                currentPageTags = listOf("kotlin", "gradle")
            )

            assertThat(result.tagOverlap).isEqualTo(0.5)
        }

        @Test
        @DisplayName("score() — crossRefCount normalisé : 3+ refs = contribution max")
        fun `score crossRefCount normalized for 3+ refs`() {
            // Créer un nœud cible avec 3 agent_reference edges
            val target = GraphNode("target.md", "Target", "file", "community-a")
            val heavyXrefNodes = listOf(
                GraphNode("page.md", "Page", "file", "community-a"),
                target
            )
            val heavyXrefEdges = listOf(
                GraphEdge("page.md", "target.md", "agent_reference"),
                GraphEdge("other1.md", "target.md", "agent_reference"),
                GraphEdge("other2.md", "target.md", "agent_reference")
            )
            val heavySubgraph = SiteSubgraph(
                nodes = heavyXrefNodes,
                edges = heavyXrefEdges,
                communities = emptyList()
            )
            val result = service.score(nodeId = "target.md", subgraph = heavySubgraph)

            // 3 agent_reference edges → crossRefCount = 3 → normalisé min(1.0, 3/3.0) = 1.0
            assertThat(result.crossRefCount).isEqualTo(3)
        }

        @Test
        @DisplayName("score() — communityAffinity = 0 → pas de bonus même si même communauté")
        fun `score no community bonus when affinity zero even with same community`() {
            val rules = LensRules(communityAffinity = 0.0)
            val ragResults = mapOf("article-a.md" to 0.8)
            val resultWithCommunity = service.score(
                nodeId = "article-a.md",
                subgraph = subgraph,
                ragResults = ragResults,
                currentPageCommunity = "community-a",
                lensRules = rules
            )
            val resultWithoutCommunity = service.score(
                nodeId = "article-a.md",
                subgraph = subgraph,
                ragResults = ragResults,
                currentPageCommunity = null,
                lensRules = rules
            )

            // communityAffinity = 0 → mêmes scores
            assertThat(resultWithCommunity.score).isEqualTo(resultWithoutCommunity.score)
        }

        @Test
        @DisplayName("scoreAll() avec sous-graphe 2 nœuds retourne 2 résultats")
        fun `scoreAll with 2 nodes returns 2 results`() {
            val twoNodes = listOf(
                GraphNode("a.md", "A", "file", null),
                GraphNode("b.md", "B", "file", null)
            )
            val twoSubgraph = SiteSubgraph(
                nodes = twoNodes,
                edges = listOf(GraphEdge("a.md", "b.md", "reference")),
                communities = emptyList()
            )
            val results = service.scoreAll(subgraph = twoSubgraph)

            assertThat(results).hasSize(2)
            // Le nœud avec le plus de voisins devrait avoir un score plus élevé
            assertThat(results.first().score).isGreaterThanOrEqualTo(results.last().score)
        }
    }
}
