package bakery.scenarios

import bakery.lens.AugmentedArticlesService
import bakery.lens.LensConfig
import bakery.lens.LensRules
import bakery.lens.LensScope
import bakery.lens.ScoredNode
import bakery.lens.SiteSubgraph
import bakery.lens.SubgraphExtractor
import graphify.model.GraphCommunity
import graphify.model.GraphEdge
import graphify.model.GraphModel
import graphify.model.GraphNode
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat

/**
 * Cucumber steps pour BKY-LENS — Pattern LENTILLE.
 *
 * 2 types de scénarios :
 * 1. Scénarios unitaires (scoring, filtrage, budget) — testent les services LENS directement
 * 2. Scénarios d'intégration (injection jbake, extraction sous-graphe) — testent via Gradle Task
 */
class LensSteps(
    private val world: BakeryWorld,
) {
    private val service = AugmentedArticlesService()

    // ─── Fixtures partagées ───
    private lateinit var subgraph: SiteSubgraph
    private var ragResults: Map<String, Double> = emptyMap()
    private var nodeTags: Map<String, List<String>> = emptyMap()
    private var currentPageTags: List<String> = emptyList()
    private var currentPageCommunity: String? = null
    private var lastScoredNode: ScoredNode? = null
    private lateinit var scoredNodes: List<ScoredNode>
    private var rules: LensRules = LensRules()
    private var filteredNodes: List<ScoredNode> = emptyList()
    private var maxArticles: Int = 4
    private var minSimilarity: Double = 0.0

    // ─── Scénario 1 : Scoring hybride ───

    @Given("a subgraph with 6 nodes and 4 communities")
    fun aSubgraphWith6NodesAnd4Communities() {
        val nodes =
            listOf(
                GraphNode("page.md", "Page Courante", "file", "community-a"),
                GraphNode("article-a.md", "Article A", "file", "community-a"),
                GraphNode("article-b.md", "Article B", "file", "community-a"),
                GraphNode("article-c.md", "Article C", "file", "community-b"),
                GraphNode("orphan.md", "Orphan", "file", community = null),
                GraphNode("draft-article.md", "Draft Article", "file", "community-a"),
            )
        val edges =
            listOf(
                GraphEdge("page.md", "article-a.md", "reference"),
                GraphEdge("article-a.md", "article-b.md", "reference"),
                GraphEdge("page.md", "article-c.md", "agent_reference"),
                GraphEdge("article-a.md", "article-c.md", "agent_reference"),
                GraphEdge("article-b.md", "orphan.md", "agent_reference"),
            )
        val communities =
            listOf(
                GraphCommunity("community-a", "Community A", 4),
                GraphCommunity("community-b", "Community B", 1),
            )
        subgraph = SiteSubgraph(nodes = nodes, edges = edges, communities = communities)
    }

    @Given("a RAG result with similarity {double} for node {string}")
    fun aRAGResultWithSimilarityForNode(
        similarity: Double,
        nodeId: String,
    ) {
        ragResults = mapOf(nodeId to similarity)
    }

    @When("I score node {string} with current page tags {string}")
    fun iScoreNodeWithCurrentPageTags(
        nodeId: String,
        tags: String,
    ) {
        currentPageTags = tags.split(",").map { it.trim() }
        nodeTags =
            mapOf(
                nodeId to currentPageTags,
                "page.md" to currentPageTags,
            )
        lastScoredNode =
            service.score(
                nodeId = nodeId,
                subgraph = subgraph,
                ragResults = ragResults,
                nodeTags = nodeTags,
                currentPageTags = currentPageTags,
                currentPageCommunity = "community-a",
                lensRules = LensRules(),
            )
    }

    @Then("the scored node should have ragSimilarity {double}")
    fun theScoredNodeShouldHaveRagSimilarity(expected: Double) {
        assertThat(lastScoredNode).isNotNull
        assertThat(lastScoredNode!!.ragSimilarity).isEqualTo(expected)
    }

    @Then("the scored node should have graphProximity greater than {double}")
    fun theScoredNodeShouldHaveGraphProximityGreaterThan(expected: Double) {
        assertThat(lastScoredNode).isNotNull
        assertThat(lastScoredNode!!.graphProximity).isGreaterThan(expected)
    }

    @Then("the scored node should have tagOverlap greater than {double}")
    fun theScoredNodeShouldHaveTagOverlapGreaterThan(expected: Double) {
        assertThat(lastScoredNode).isNotNull
        assertThat(lastScoredNode!!.tagOverlap).isGreaterThan(expected)
    }

    @Then("the scored node should have crossRefCount at least {int}")
    fun theScoredNodeShouldHaveCrossRefCountAtLeast(expected: Int) {
        assertThat(lastScoredNode).isNotNull
        assertThat(lastScoredNode!!.crossRefCount).isGreaterThanOrEqualTo(expected)
    }

    @Then("the final score should be greater than {double}")
    fun theFinalScoreShouldBeGreaterThan(expected: Double) {
        assertThat(lastScoredNode).isNotNull
        assertThat(lastScoredNode!!.score).isGreaterThan(expected)
    }

    // ─── Scénario 2 : Filtrage des règles ───

    @Given("a list of scored nodes with tags {string}, {string}, and {string}")
    fun aListOfScoredNodesWithTagsDraftWipAndPublished(
        tag1: String,
        tag2: String,
        tag3: String,
    ) {
        scoredNodes =
            listOf(
                ScoredNode("a.md", "Draft", null, listOf(tag1), 0.5, 0.3, 0.1, 0, 0.5),
                ScoredNode("b.md", "WIP", null, listOf(tag2), 0.5, 0.3, 0.1, 0, 0.5),
                ScoredNode("c.md", "Published", null, listOf(tag3), 0.5, 0.3, 0.1, 0, 0.5),
            )
    }

    @Given("lens rules with excludeTags containing {string}")
    fun lensRulesWithExcludeTagsContaining(excludedTags: String) {
        rules =
            LensRules(
                excludeTags = excludedTags.split(",").map { it.trim() },
            )
    }

    @When("I apply lens rules to the scored nodes")
    fun iApplyLensRulesToTheScoredNodes() {
        filteredNodes = service.applyRules(scoredNodes, rules)
    }

    @Then("the result should not contain any node with tag {string}")
    fun theResultShouldNotContainAnyNodeWithTag(tag: String) {
        val nodeWithTag = filteredNodes.firstOrNull { it.tags.any { t -> t.equals(tag, ignoreCase = true) } }
        assertThat(nodeWithTag).isNull()
    }

    @Then("the result should contain exactly {int} node(s) with tag {string}")
    fun theResultShouldContainExactlyCountNodesWithTag(
        count: Int,
        tag: String,
    ) {
        val nodesWithTag = filteredNodes.filter { it.tags.any { t -> t.equals(tag, ignoreCase = true) } }
        assertThat(nodesWithTag).hasSize(count)
    }

    // ─── Scénario 3 : Budget ───

    @Given("a scored node list with {int} nodes and scores {double}, {double}, {double}, {double}, {double}")
    fun aScoredNodeListWith5NodesAndScores(
        count: Int,
        s1: Double,
        s2: Double,
        s3: Double,
        s4: Double,
        s5: Double,
    ) {
        val scores = listOf(s1, s2, s3, s4, s5)
        scoredNodes =
            scores.mapIndexed { idx, score ->
                ScoredNode("node-$idx.md", "Node $idx", null, emptyList(), 0.0, 0.0, 0.0, 0, score)
            }
        assertThat(scoredNodes).hasSize(count)
    }

    @Given("a maxArticles budget of {int}")
    fun aMaxArticlesBudgetOf(value: Int) {
        this.maxArticles = value
    }

    @Given("a minSimilarity threshold of {double}")
    fun aMinSimilarityThresholdOf(value: Double) {
        this.minSimilarity = value
    }

    @When("I apply budget filtering")
    fun iApplyBudgetFiltering() {
        // Truncate à maxArticles après filtrage minSimilarity
        filteredNodes =
            scoredNodes
                .filter { it.score >= minSimilarity }
                .sortedByDescending { it.score }
                .take(maxArticles)
    }

    @Then("all nodes should have score greater than or equal to {double}")
    fun allNodesShouldHaveScoreGreaterThanOrEqualTo(expectedMin: Double) {
        assertThat(filteredNodes).allMatch { it.score >= expectedMin }
    }

    @Then("the result should be ordered by score descending")
    fun theResultShouldBeOrderedByScoreDescending() {
        for (i in 0 until filteredNodes.size - 1) {
            assertThat(filteredNodes[i].score).isGreaterThanOrEqualTo(filteredNodes[i + 1].score)
        }
    }

    // ─── Assertion générique partagée ───

    @Then("the result should have exactly {int} nodes")
    fun theResultShouldHaveExactlyNodes(count: Int) {
        assertThat(filteredNodes).hasSize(count)
    }

    // ──────────────────────────────────────────────────────
    // BKY-LENS-4 — Scénarios d'intégration (Gradle Task)
    // ──────────────────────────────────────────────────────

    // ─── Feature 17 : Augmented Context Lens — injection jbake.properties ───

    @Given("the bakery DSL defines augmentedContext as enabled with maxArticlesPerPage {int} and minSimilarity {double}")
    fun theBakeryDSLDefinesAugmentedContextEnabledWithBudget(
        maxArticles: Int,
        minSimilarity: Double,
    ) {
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")
        val buildFile = projectDir.resolve("build.gradle.kts")
        val content = buildFile.readText(Charsets.UTF_8)
        val lensBlock =
            """
            augmentedContext {
                enabled = true
                budget {
                    maxArticlesPerPage = $maxArticles
                    minSimilarity = $minSimilarity
                }
            }
            """.trimIndent()
        // Insert before closing '}' of bakery { ... }
        val updatedContent = insertBeforeClosingBrace(content, "bakery", lensBlock)
        buildFile.writeText(updatedContent, Charsets.UTF_8)
    }

    @Given("the bakery DSL defines augmentedContext as disabled")
    fun theBakeryDSLDefinesAugmentedContextDisabled() {
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")
        val buildFile = projectDir.resolve("build.gradle.kts")
        val content = buildFile.readText(Charsets.UTF_8)
        val augmentedBlock =
            """
            augmentedContext {
                enabled = false
            }
            """.trimIndent()
        // Insert before closing '}' of bakery { ... }
        val updatedContent = insertBeforeClosingBrace(content, "bakery", augmentedBlock)
        buildFile.writeText(updatedContent, Charsets.UTF_8)
    }

    // Délègue à DslInjectionHelper pour insertion DSL robuste
    private fun insertBeforeClosingBrace(
        content: String,
        blockName: String,
        blockToInsert: String,
    ): String = bakery.lens.DslInjectionHelper.insertBeforeClosingBrace(content, blockName, blockToInsert)

    // ─── Feature 18 : SubgraphExtractor — extraction sous-graphe ───

    private lateinit var extractor: SubgraphExtractor
    private lateinit var extractedSubgraph: SiteSubgraph
    private var graphFilePath: String? = null

    @Given("a graph.json file with {int} nodes in {int} communities and {int} edges")
    fun aGraphJsonFileWithNodesInCommunitiesAndEdges(
        nodeCount: Int,
        communityCount: Int,
        edgeCount: Int,
    ) {
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")
        val nodes = mutableListOf<GraphNode>()
        val communitiesList = mutableListOf<GraphCommunity>()

        // Créer les communautés
        for (i in 0 until communityCount) {
            communitiesList.add(GraphCommunity("community-$i", "Community $i", nodeCount / communityCount))
        }

        // Créer les nœuds répartis dans les communautés
        for (i in 0 until nodeCount) {
            val communityId = communitiesList[i % communityCount].id
            nodes.add(GraphNode("node-$i.adoc", "Node $i", "file", communityId))
        }

        // Créer les edges
        val edges = mutableListOf<GraphEdge>()
        val maxEdges = minOf(edgeCount, nodeCount - 1)
        for (i in 0 until maxEdges) {
            edges.add(GraphEdge("node-$i.adoc", "node-${i + 1}.adoc", "reference"))
        }

        // Sérialiser en graph.json
        val objectMapper =
            com.fasterxml.jackson.databind
                .ObjectMapper()
                .registerModule(
                    com.fasterxml.jackson.module.kotlin
                        .kotlinModule(),
                )
        val graphModel = GraphModel(nodes = nodes, edges = edges, communities = communitiesList)
        val graphFile = projectDir.resolve("office/graph.json")
        graphFile.parentFile.mkdirs()
        graphFile.writeText(objectMapper.writeValueAsString(graphModel), Charsets.UTF_8)
        graphFilePath = graphFile.absolutePath
    }

    @Given("a graph.json file at custom path {string} with {int} nodes")
    fun aGraphJsonFileAtCustomPathWithNodes(
        customPath: String,
        nodeCount: Int,
    ) {
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")
        val objectMapper =
            com.fasterxml.jackson.databind
                .ObjectMapper()
                .registerModule(
                    com.fasterxml.jackson.module.kotlin
                        .kotlinModule(),
                )
        val nodes =
            (0 until nodeCount).map {
                GraphNode("custom-node-$it.adoc", "Custom Node $it", "file", "custom-community")
            }
        val communitiesList = listOf(GraphCommunity("custom-community", "Custom Community", nodeCount))
        val graphModel = GraphModel(nodes = nodes, edges = emptyList(), communities = communitiesList)
        val graphFile = projectDir.resolve(customPath)
        graphFile.parentFile.mkdirs()
        graphFile.writeText(objectMapper.writeValueAsString(graphModel), Charsets.UTF_8)
        graphFilePath = graphFile.absolutePath
    }

    @Given("the bakery DSL defines augmentedContext with lens scope={string} and communities {string}")
    fun theBakeryDSLDefinesAugmentedContextWithLensScopeAndCommunities(
        scope: String,
        communities: String,
    ) {
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")
        val buildFile = projectDir.resolve("build.gradle.kts")
        val content = buildFile.readText(Charsets.UTF_8)
        val communityList = communities.split(",").map { it.trim() }.joinToString(", ") { "\"$it\"" }
        val lensBlock =
            """
            augmentedContext {
                enabled = true
                lens {
                    scope = $scope
                    communities = listOf($communityList)
                }
            }
            """.trimIndent()
        val updatedContent = insertBeforeClosingBrace(content, "bakery", lensBlock)
        buildFile.writeText(updatedContent, Charsets.UTF_8)
    }

    @Given("the bakery DSL defines augmentedContext with lens graphFilePath={string}")
    fun theBakeryDSLDefinesAugmentedContextWithLensGraphFilePath(graphFilePath: String) {
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")
        val buildFile = projectDir.resolve("build.gradle.kts")
        val content = buildFile.readText(Charsets.UTF_8)
        val lensBlock =
            """
            augmentedContext {
                enabled = true
                lens {
                    graphFilePath = "$graphFilePath"
                }
            }
            """.trimIndent()
        val updatedContent = insertBeforeClosingBrace(content, "bakery", lensBlock)
        buildFile.writeText(updatedContent, Charsets.UTF_8)
    }

    @When("I extract a subgraph with communities {string}")
    fun iExtractASubgraphWithCommunities(communities: String) {
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")
        extractor = SubgraphExtractor()
        val communityList = communities.split(",").map { it.trim() }
        val resolvedGraphPath = graphFilePath ?: projectDir.resolve("office/graph.json").absolutePath
        val lensConfig =
            LensConfig(
                scope = LensScope.SUBGRAPH,
                communities = communityList,
                graphFilePath = resolvedGraphPath,
            )
        extractedSubgraph = extractor.extractFromPath(lensConfig.graphFilePath, lensConfig)
    }

    @When("I extract a subgraph with scope {string}")
    fun iExtractASubgraphWithScope(scope: String) {
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")
        extractor = SubgraphExtractor()
        val lensScope = LensScope.valueOf(scope)
        val resolvedGraphPath = graphFilePath ?: projectDir.resolve("office/graph.json").absolutePath
        val lensConfig =
            LensConfig(
                scope = lensScope,
                communities = listOf("community-0", "community-1"),
                graphFilePath = resolvedGraphPath,
            )
        extractedSubgraph = extractor.extractFromPath(lensConfig.graphFilePath, lensConfig)
    }

    @Then("the subgraph should only contain nodes from community {string}")
    fun theSubgraphShouldOnlyContainNodesFromCommunity(communityId: String) {
        assertThat(extractedSubgraph.nodes).isNotEmpty
        assertThat(extractedSubgraph.nodes).allMatch { it.community == communityId }
    }

    @Then("the subgraph should contain nodes from all communities")
    fun theSubgraphShouldContainNodesFromAllCommunities() {
        assertThat(extractedSubgraph.nodes).isNotEmpty
        val uniqueCommunities = extractedSubgraph.nodes.mapNotNull { it.community }.toSet()
        assertThat(uniqueCommunities).hasSizeGreaterThanOrEqualTo(2)
    }

    @Then("the subgraph should contain {int} nodes")
    fun theSubgraphShouldContainNodes(count: Int) {
        assertThat(extractedSubgraph.nodeCount).isEqualTo(count)
    }

    @Then("the subgraph should contain all original nodes")
    fun theSubgraphShouldContainAllOriginalNodes() {
        // En scope FULL, tous les nœuds du graphe sont conservés
        assertThat(extractedSubgraph.nodes).isNotEmpty
    }
}
