package bakery.lens

import graphify.model.GraphCommunity
import graphify.model.GraphEdge
import graphify.model.GraphModel
import graphify.model.GraphNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * BKY-LENS-1.3/1.4 — Tests unitaires pour SubgraphExtractor.
 *
 * Fixture : graph-lens-test.json (3 communautés principales + 2 secondaires, ~37 nœuds, ~24 edges)
 *
 * Méthodologie : DDD/TDD baby steps — chaque test compile ET passe AVANT de passer au suivant.
 */
class SubgraphExtractorTest {

    private lateinit var extractor: SubgraphExtractor

    @TempDir
    lateinit var tempDir: File

    // ─── Graphe de test construit en mémoire ───

    /** Nœuds du fixture (communautés: bakery-gradle, codebase-gradle, slider-gradle, workspace-bom, graphify-gradle) */
    private val bakeryNodes = listOf(
        GraphNode("bakery/BakeryPlugin.kt", "BakeryPlugin.kt", "file", "bakery-gradle"),
        GraphNode("bakery/SiteManager.kt", "SiteManager.kt", "file", "bakery-gradle"),
        GraphNode("bakery/BakeryExtension.kt", "BakeryExtension.kt", "file", "bakery-gradle"),
        GraphNode("bakery/LensConfig.kt", "LensConfig.kt", "file", "bakery-gradle"),
        GraphNode("bakery/SubgraphExtractor.kt", "SubgraphExtractor.kt", "file", "bakery-gradle"),
        GraphNode("bakery/post.thyme", "post.thyme", "file", "bakery-gradle"),
        GraphNode("bakery/docs/index.adoc", "index.adoc", "file", "bakery-gradle"),
        GraphNode("bakery/docs/getting-started.adoc", "getting-started.adoc", "file", "bakery-gradle"),
        GraphNode("bakery/README.adoc", "README.adoc", "file", "bakery-gradle"),
        GraphNode("bakery-gradle", "bakery-gradle", "module", "bakery-gradle")
    )

    private val codebaseNodes = listOf(
        GraphNode("codebase/RagService.kt", "RagService.kt", "file", "codebase-gradle"),
        GraphNode("codebase/CodebasePlugin.kt", "CodebasePlugin.kt", "file", "codebase-gradle"),
        GraphNode("codebase/VectorStore.kt", "VectorStore.kt", "file", "codebase-gradle"),
        GraphNode("codebase/docs/architecture.adoc", "architecture.adoc", "file", "codebase-gradle"),
        GraphNode("codebase-gradle", "codebase-gradle", "module", "codebase-gradle")
    )

    private val sliderNodes = listOf(
        GraphNode("slider/SliderPlugin.kt", "SliderPlugin.kt", "file", "slider-gradle"),
        GraphNode("slider/RevealJsTask.kt", "RevealJsTask.kt", "file", "slider-gradle"),
        GraphNode("slider-gradle", "slider-gradle", "module", "slider-gradle")
    )

    private val bomNodes = listOf(
        GraphNode("bom/CompositeContext.kt", "CompositeContext.kt", "file", "workspace-bom"),
        GraphNode("bom/ContextChannel.kt", "ContextChannel.kt", "file", "workspace-bom"),
        GraphNode("bom/ChannelBudget.kt", "ChannelBudget.kt", "module", "workspace-bom"),
        GraphNode("workspace-bom", "workspace-bom", "module", "workspace-bom")
    )

    private val graphifyNodes = listOf(
        GraphNode("graphify/GraphifyPlugin.kt", "GraphifyPlugin.kt", "file", "graphify-gradle"),
        GraphNode("graphify/GraphModel.kt", "GraphModel.kt", "file", "graphify-gradle"),
        GraphNode("graphify-gradle", "graphify-gradle", "module", "graphify-gradle")
    )

    private val orphanNode = GraphNode("orphan.adoc", "orphan.adoc", "file", community = null)

    private val allNodes: List<GraphNode>
        get() = bakeryNodes + codebaseNodes + sliderNodes + bomNodes + graphifyNodes + orphanNode

    private val allEdges = listOf(
        // Références internes bakery
        GraphEdge("bakery/BakeryPlugin.kt", "bakery/SiteManager.kt", "reference"),
        GraphEdge("bakery/BakeryPlugin.kt", "bakery/BakeryExtension.kt", "reference"),
        GraphEdge("bakery/SiteManager.kt", "bakery/LensConfig.kt", "reference"),
        GraphEdge("bakery/SubgraphExtractor.kt", "bakery/LensConfig.kt", "reference"),
        // Cross-borough bakery → bom
        GraphEdge("bakery/SubgraphExtractor.kt", "bom/CompositeContext.kt", "reference"),
        GraphEdge("bakery/LensConfig.kt", "bom/ContextChannel.kt", "reference"),
        // Cross-borough codebase → bom
        GraphEdge("codebase/RagService.kt", "bom/CompositeContext.kt", "reference"),
        GraphEdge("codebase/VectorStore.kt", "bom/ChannelBudget.kt", "reference"),
        // Cross-borough slider → bom + bakery
        GraphEdge("slider/SliderPlugin.kt", "bom/ContextChannel.kt", "reference"),
        // Contains edges
        GraphEdge("bakery-gradle", "bakery/BakeryPlugin.kt", "contains"),
        GraphEdge("codebase-gradle", "codebase/CodebasePlugin.kt", "contains"),
        GraphEdge("slider-gradle", "slider/SliderPlugin.kt", "contains"),
        // Agent reference (cross-community)
        GraphEdge("bakery/docs/index.adoc", "bakery/docs/getting-started.adoc", "agent_reference"),
        GraphEdge("bakery/docs/index.adoc", "codebase/docs/architecture.adoc", "agent_reference"),
        // Graphify internal
        GraphEdge("graphify/GraphModel.kt", "graphify-gradle", "contains"),
        // Orphan
        GraphEdge("orphan.adoc", "bakery/docs/index.adoc", "reference")
    )

    private val allCommunities = listOf(
        GraphCommunity("bakery-gradle", "Bakery Gradle Plugin", 10),
        GraphCommunity("codebase-gradle", "Codebase Gradle Plugin", 5),
        GraphCommunity("slider-gradle", "Slider Gradle Plugin", 3),
        GraphCommunity("workspace-bom", "Workspace BOM", 4),
        GraphCommunity("graphify-gradle", "Graphify Gradle Plugin", 3)
    )

    private val testGraphModel: GraphModel
        get() = GraphModel(
            nodes = allNodes,
            edges = allEdges,
            communities = allCommunities
        )

    @BeforeEach
    fun setUp() {
        extractor = SubgraphExtractor()
    }

    // ──────────────────────────────────────
    // LENS-1.3 : Filtrage par communauté
    // ──────────────────────────────────────

    @Nested
    @DisplayName("LENS-1.3 : SubgraphExtractor — Filtrage par communauté")
    inner class CommunityFiltering {

        @Test
        @DisplayName("Filtrer par une seule communauté — bakery-gradle")
        fun `filter single community bakery-gradle`() {
            val config = LensConfig(communities = listOf("bakery-gradle"))
            val result = extractor.extract(testGraphModel, config)

            // Les nœuds bakery seulement (communauté bakery + orphelins inclus)
            assertThat(result.nodes).isNotEmpty
            assertThat(result.nodes.map { it.community }.distinct()).containsExactly("bakery-gradle", null)
            assertThat(result.communityIds).containsExactly("bakery-gradle")
        }

        @Test
        @DisplayName("Filtrer par plusieurs communautés — bakery + codebase")
        fun `filter multiple communities`() {
            val config = LensConfig(communities = listOf("bakery-gradle", "codebase-gradle"))
            val result = extractor.extract(testGraphModel, config)

            val communityIds = result.nodes.mapNotNull { it.community }.distinct()
            assertThat(communityIds).containsExactlyInAnyOrder("bakery-gradle", "codebase-gradle")
        }

        @Test
        @DisplayName("Communautés vides = pas de filtrage par communauté (tous les nœuds)")
        fun `empty communities includes all nodes matching type filter`() {
            val config = LensConfig(communities = emptyList(), nodeTypes = listOf("file"), fileExtensions = emptyList())
            val result = extractor.extract(testGraphModel, config)

            // Pas de filtre communauté = tous les fichiers passent
            assertThat(result.nodes.size).isGreaterThan(10)
        }
    }

    // ──────────────────────────────────────
    // LENS-1.3 : Filtrage par type de nœud
    // ──────────────────────────────────────

    @Nested
    @DisplayName("LENS-1.3 : SubgraphExtractor — Filtrage par type de nœud")
    inner class NodeTypeFiltering {

        @Test
        @DisplayName("Filtrer type = file seulement (exclure les modules)")
        fun `filter file type only excludes modules`() {
            val config = LensConfig(
                communities = listOf("bakery-gradle"),
                nodeTypes = listOf("file")
            )
            val result = extractor.extract(testGraphModel, config)

            // Pas de nœud "module" dans le résultat
            assertThat(result.nodes.none { it.type == "module" }).isTrue
            // Tous les nœuds sont "file"
            assertThat(result.nodes.all { it.type == "file" }).isTrue
        }

        @Test
        @DisplayName("Filtrer type = module seulement")
        fun `filter module type only`() {
            val config = LensConfig(
                communities = listOf("bakery-gradle"),
                nodeTypes = listOf("module")
            )
            val result = extractor.extract(testGraphModel, config)

            assertThat(result.nodes.map { it.type }).containsExactly("module")
        }
    }

    // ──────────────────────────────────────
    // LENS-1.3 : Filtrage par type d'edge
    // ──────────────────────────────────────

    @Nested
    @DisplayName("LENS-1.3 : SubgraphExtractor — Filtrage par type d'edge")
    inner class EdgeTypeFiltering {

        @Test
        @DisplayName("Filtrer edges reference seulement (exclure contains)")
        fun `filter reference edges only`() {
            val config = LensConfig(
                communities = listOf("bakery-gradle", "workspace-bom"),
                nodeTypes = listOf("file"),
                edgeTypes = listOf("reference")
            )
            val result = extractor.extract(testGraphModel, config)

            assertThat(result.edges.all { it.type == "reference" }).isTrue
            assertThat(result.edges.none { it.type == "contains" }).isTrue
        }

        @Test
        @DisplayName("Filtrer edges agent_reference seulement")
        fun `filter agent_reference edges only`() {
            val config = LensConfig(
                communities = listOf("bakery-gradle", "codebase-gradle"),
                nodeTypes = listOf("file"),
                edgeTypes = listOf("agent_reference")
            )
            val result = extractor.extract(testGraphModel, config)

            assertThat(result.edges.all { it.type == "agent_reference" }).isTrue
        }

        @Test
        @DisplayName("EdgeTypes vide = tous les types d'edges inclus")
        fun `empty edge types includes all edge types`() {
            val config = LensConfig(
                communities = listOf("bakery-gradle", "workspace-bom"),
                nodeTypes = listOf("file", "module"),
                edgeTypes = emptyList()
            )
            val result = extractor.extract(testGraphModel, config)

            val edgeTypes = result.edges.map { it.type }.distinct()
            assertThat(edgeTypes).containsAnyOf("reference", "contains", "agent_reference")
        }
    }

    // ──────────────────────────────────────
    // LENS-1.3 : Filtrage par extension de fichier
    // ──────────────────────────────────────

    @Nested
    @DisplayName("LENS-1.3 : SubgraphExtractor — Filtrage par extension de fichier")
    inner class FileExtensionFiltering {

        @Test
        @DisplayName("Filtrer fichiers .kt seulement")
        fun `filter kt files only`() {
            val config = LensConfig(
                communities = listOf("bakery-gradle"),
                nodeTypes = listOf("file"),
                fileExtensions = listOf("kt")
            )
            val result = extractor.extract(testGraphModel, config)

            // Tous les nœuds fichier doivent avoir l'extension .kt
            assertThat(result.nodes.all { it.id.endsWith(".kt") || it.type != "file" }).isTrue
        }

        @Test
        @DisplayName("Filtrer fichiers .adoc seulement")
        fun `filter adoc files only`() {
            val config = LensConfig(
                communities = listOf("bakery-gradle"),
                nodeTypes = listOf("file"),
                fileExtensions = listOf("adoc")
            )
            val result = extractor.extract(testGraphModel, config)

            // Tous les nœuds fichier doivent avoir l'extension .adoc
            assertThat(result.nodes.all { it.id.endsWith(".adoc") || it.type != "file" }).isTrue
        }

        @Test
        @DisplayName("Filtrer fichiers .kt + .adoc")
        fun `filter kt and adoc files`() {
            val config = LensConfig(
                communities = listOf("bakery-gradle"),
                nodeTypes = listOf("file"),
                fileExtensions = listOf("kt", "adoc")
            )
            val result = extractor.extract(testGraphModel, config)

            val extensions = result.nodes
                .filter { it.type == "file" }
                .map { it.id.substringAfterLast('.', "") }
                .distinct()
            assertThat(extensions).isSubsetOf("kt", "adoc")
        }
    }

    // ──────────────────────────────────────
    // LENS-1.3 : Profondeur BFS
    // ──────────────────────────────────────

    @Nested
    @DisplayName("LENS-1.3 : SubgraphExtractor — Profondeur BFS (maxDepth)")
    inner class BfsDepth {

        @Test
        @DisplayName("maxDepth=0 : seulement les nœuds des communautés ciblées")
        fun `maxDepth 0 returns only seed nodes`() {
            val config = LensConfig(
                communities = listOf("bakery-gradle"),
                nodeTypes = listOf("file"),
                edgeTypes = listOf("reference"),
                maxDepth = 0
            )
            val result = extractor.extract(testGraphModel, config)

            // Avec maxDepth=0, on ne garde que les nœuds des communautés ciblées
            // (pas de expansion vers les communautés voisines)
            val bakeryFileIds = bakeryNodes.filter { it.type == "file" }.map { it.id }.toSet()
            result.nodes.forEach { node ->
                assertThat(node.community).isIn("bakery-gradle", null)
            }
        }

        @Test
        @DisplayName("maxDepth=1 : semences + voisins directs (1 saut)")
        fun `maxDepth 1 expands to direct neighbors`() {
            val configDepth0 = LensConfig(
                communities = listOf("bakery-gradle"),
                nodeTypes = listOf("file"),
                edgeTypes = listOf("reference"),
                maxDepth = 0
            )
            val result0 = extractor.extract(testGraphModel, configDepth0)

            val configDepth1 = configDepth0.copy(maxDepth = 1)
            val result1 = extractor.extract(testGraphModel, configDepth1)

            // Depth 1 doit avoir au moins autant de nœuds que depth 0
            // et potentiellement plus (voisins cross-community)
            assertThat(result1.nodes.size).isGreaterThanOrEqualTo(result0.nodes.size)
        }

        @Test
        @DisplayName("maxDepth=2 : semences + voisins + voisins des voisins")
        fun `maxDepth 2 expands further`() {
            val configDepth1 = LensConfig(
                communities = listOf("bakery-gradle"),
                nodeTypes = listOf("file"),
                edgeTypes = listOf("reference"),
                maxDepth = 1
            )
            val result1 = extractor.extract(testGraphModel, configDepth1)

            val configDepth2 = configDepth1.copy(maxDepth = 2)
            val result2 = extractor.extract(testGraphModel, configDepth2)

            // Depth 2 >= depth 1
            assertThat(result2.nodes.size).isGreaterThanOrEqualTo(result1.nodes.size)
        }
    }

    // ──────────────────────────────────────
    // LENS-1.3 : Scopes (SUBGRAPH, FULL, SEMANTIC_ONLY)
    // ──────────────────────────────────────

    @Nested
    @DisplayName("LENS-1.3 : SubgraphExtractor — Scope (LensScope)")
    inner class ScopeSelection {

        @Test
        @DisplayName("FULL scope retourne tout le graphe")
        fun `FULL scope returns entire graph`() {
            val config = LensConfig(scope = LensScope.FULL)
            val result = extractor.extract(testGraphModel, config)

            assertThat(result.nodes.size).isEqualTo(allNodes.size)
            assertThat(result.edges.size).isEqualTo(allEdges.size)
        }

        @Test
        @DisplayName("SEMANTIC_ONLY scope retourne sous-graphe vide (pas de graphe)")
        fun `SEMANTIC_ONLY scope returns empty subgraph`() {
            val config = LensConfig(scope = LensScope.SEMANTIC_ONLY)
            val result = extractor.extract(testGraphModel, config)

            assertThat(result.nodes).isEmpty()
            assertThat(result.edges).isEmpty()
            assertThat(result.communities).isEmpty()
        }

        @Test
        @DisplayName("SUBGRAPH scope applique le filtrage normal")
        fun `SUBGRAPH scope applies normal filtering`() {
            val config = LensConfig(
                scope = LensScope.SUBGRAPH,
                communities = listOf("bakery-gradle"),
                nodeTypes = listOf("file")
            )
            val result = extractor.extract(testGraphModel, config)

            assertThat(result.nodes).isNotEmpty
            assertThat(result.nodes.map { it.community }.distinct()).containsExactly("bakery-gradle", null)
        }
    }

    // ──────────────────────────────────────
    // LENS-1.3 : Nœuds orphelins (community=null)
    // ──────────────────────────────────────

    @Nested
    @DisplayName("LENS-1.3 : SubgraphExtractor — Nœuds orphelins (community=null)")
    inner class OrphanNodes {

        @Test
        @DisplayName("Les nœuds sans communauté sont inclus par défaut")
        fun `orphan nodes are included by default`() {
            val config = LensConfig(communities = listOf("bakery-gradle"), nodeTypes = listOf("file"))
            val result = extractor.extract(testGraphModel, config)

            // L'orfelin "orphan.adoc" (community=null) doit être inclus
            assertThat(result.nodes.any { it.community == null }).isTrue
        }
    }

    // ──────────────────────────────────────
    // LENS-1.3 : SiteSubgraph — Opérations utilitaires
    // ──────────────────────────────────────

    @Nested
    @DisplayName("LENS-1.3 : SiteSubgraph — Utilitaires")
    inner class SiteSubgraphUtils {

        @Test
        @DisplayName("nodeCount, edgeCount, communityCount")
        fun `subgraph counts`() {
            val config = LensConfig(communities = listOf("bakery-gradle"))
            val result = extractor.extract(testGraphModel, config)

            assertThat(result.nodeCount).isEqualTo(result.nodes.size)
            assertThat(result.edgeCount).isEqualTo(result.edges.size)
            assertThat(result.communityCount).isEqualTo(result.communities.size)
        }

        @Test
        @DisplayName("nodesInCommunity retourne les nœuds d'une communauté")
        fun `nodesInCommunity returns nodes for a given community`() {
            val config = LensConfig(communities = listOf("bakery-gradle"), nodeTypes = listOf("file"))
            val result = extractor.extract(testGraphModel, config)

            val bakeryNodes = result.nodesInCommunity("bakery-gradle")
            assertThat(bakeryNodes).isNotEmpty
            assertThat(bakeryNodes.all { it.community == "bakery-gradle" }).isTrue
        }

        @Test
        @DisplayName("edgesOfType retourne les edges d'un type donné")
        fun `edgesOfType returns edges for a given type`() {
            val config = LensConfig(communities = listOf("bakery-gradle", "workspace-bom"), edgeTypes = listOf("reference"))
            val result = extractor.extract(testGraphModel, config)

            val references = result.edgesOfType("reference")
            assertThat(references).isNotEmpty
            assertThat(references.all { it.type == "reference" }).isTrue
        }

        @Test
        @DisplayName("neighbors retourne les voisins d'un nœud")
        fun `neighbors returns nodes connected to a given node`() {
            val config = LensConfig(
                communities = listOf("bakery-gradle"),
                nodeTypes = listOf("file", "module"),
                edgeTypes = listOf("reference")
            )
            val result = extractor.extract(testGraphModel, config)

            // BakeryPlugin.kt a des voisins reference dans bakery-gradle
            val bakeryPlugin = result.nodes.find { it.id == "bakery/BakeryPlugin.kt" }
            if (bakeryPlugin != null) {
                val neighbors = result.neighbors("bakery/BakeryPlugin.kt")
                assertThat(neighbors).isNotEmpty
            }
        }
    }

    // ──────────────────────────────────────
    // LENS-1.4 : Chargement graph.json depuis fichier
    // ──────────────────────────────────────

    @Nested
    @DisplayName("LENS-1.4 : SubgraphExtractor — Chargement depuis fichier")
    inner class FileLoading {

        @Test
        @DisplayName("loadGraph retourne graphe vide si fichier inexistant")
        fun `loadGraph returns empty for non-existent file`() {
            val result = extractor.loadGraph("/nonexistent/graph.json")
            assertThat(result.nodes).isEmpty()
            assertThat(result.edges).isEmpty()
            assertThat(result.communities).isEmpty()
        }

        @Test
        @DisplayName("extractFromPath charge et filtre depuis un fichier JSON")
        fun `extractFromPath loads and filters from JSON file`() {
            // Écrire le fixture
            val graphFile = File(tempDir, "test-graph.json")
            val objectMapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(graphFile, testGraphModel)

            val config = LensConfig(
                communities = listOf("bakery-gradle"),
                nodeTypes = listOf("file")
            )
            val result = extractor.extractFromPath(graphFile.absolutePath, config)

            assertThat(result.nodes).isNotEmpty
            assertThat(result.nodes.mapNotNull { it.community }.distinct()).containsExactly("bakery-gradle")
        }
    }

    // ──────────────────────────────────────
    // LENS-1.3 : Cross-community edges
    // ──────────────────────────────────────

    @Nested
    @DisplayName("LENS-1.3 : SubgraphExtractor — Cross-community edges")
    inner class CrossCommunityEdges {

        @Test
        @DisplayName("Les edges cross-community sont inclus si les deux bouts sont dans le sous-graphe")
        fun `cross-community edges preserved when both endpoints in subgraph`() {
            val config = LensConfig(
                communities = listOf("bakery-gradle", "workspace-bom"),
                nodeTypes = listOf("file", "module"),
                edgeTypes = listOf("reference")
            )
            val result = extractor.extract(testGraphModel, config)

            // Les edges bakery→bom doivent être préservés
            val crossEdges = result.edges.filter { edge ->
                val sourceCommunity = result.nodes.find { it.id == edge.source }?.community
                val targetCommunity = result.nodes.find { it.id == edge.target }?.community
                sourceCommunity != targetCommunity
            }
            // Il existe au moins un edge cross-community
            assertThat(crossEdges).isNotEmpty
        }
    }

    // ──────────────────────────────────────
    // LENS-1.3 : Edge dont un bout est hors du sous-graphe
    // ──────────────────────────────────────

    @Nested
    @DisplayName("LENS-1.3 : SubgraphExtractor — Edges orphelins")
    inner class OrphanEdges {

        @Test
        @DisplayName("Les edges dont un bout est hors du sous-graphe sont exclus")
        fun `edges with one endpoint outside subgraph are excluded`() {
            val config = LensConfig(
                communities = listOf("bakery-gradle"),
                nodeTypes = listOf("file"),
                edgeTypes = listOf("reference", "agent_reference")
            )
            val result = extractor.extract(testGraphModel, config)

            // Tous les edges doivent avoir leurs deux bouts dans le sous-graphe
            val nodeIds = result.nodeIds
            result.edges.forEach { edge ->
                assertThat(edge.source).isIn(nodeIds)
                assertThat(edge.target).isIn(nodeIds)
            }
        }
    }
}
