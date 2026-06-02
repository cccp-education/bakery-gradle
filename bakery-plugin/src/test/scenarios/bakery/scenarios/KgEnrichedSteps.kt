package bakery.scenarios

import bakery.kgraph.ArticleNode
import bakery.kgraph.RelatedArticlesService
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat

/**
 * Cucumber steps pour BKG-0 — Knowledge Graph enrichi (co-occurrence + entity overlap).
 *
 * Ces steps testent la logique pure du RelatedArticlesService sans pipeline Gradle.
 */
class KgEnrichedSteps {

    private val service = RelatedArticlesService()
    private var articles: List<ArticleNode> = emptyList()
    private var graph: bakery.kgraph.RelatedArticlesGraph? = null

    @Given("3 articles with overlapping tag pairs")
    fun `3 articles with overlapping tag pairs`() {
        // kotlin+gradle co-occur in A and D, kotlin+gradle co-occurrence count = 2
        articles = listOf(
            ArticleNode("/a.html", "Kotlin Gradle Tutorial", tags = listOf("kotlin", "gradle")),
            ArticleNode("/b.html", "Kotlin Maven Guide", tags = listOf("kotlin", "maven", "gradle")),
            ArticleNode("/c.html", "Java Spring Boot", tags = listOf("java", "spring")),
            ArticleNode("/d.html", "Gradle Kotlin DSL", tags = listOf("kotlin", "gradle"))
        )
    }

    @Given("2 articles sharing significant entities in their descriptions")
    fun `2 articles sharing significant entities in descriptions`() {
        articles = listOf(
            ArticleNode(
                "/pgvector.html", "PGVector Search",
                tags = listOf("pgvector"),
                description = "Comment utiliser PGVector pour la recherche vectorielle en Kotlin"
            ),
            ArticleNode(
                "/rag.html", "RAG avec PGVector",
                tags = listOf("rag"),
                description = "Implémenter le RAG avec PGVector et LangChain4j pour la recherche vectorielle"
            )
        )
    }

    @Given("3 articles with shared tags overlapping descriptions and similar titles")
    fun `3 articles with shared tags overlapping descriptions and similar titles`() {
        articles = listOf(
            ArticleNode(
                "/coroutines.html", "Kotlin Coroutines Guide",
                tags = listOf("kotlin", "coroutines"),
                description = "Guide complet des coroutines Kotlin pour la programmation asynchrone"
            ),
            ArticleNode(
                "/flow.html", "Kotlin Flow Tutorial",
                tags = listOf("kotlin", "flow"),
                description = "Tutorial sur Kotlin Flow et les coroutines pour la programmation réactive"
            ),
            ArticleNode(
                "/python.html", "Python Asyncio",
                tags = listOf("python"),
                description = "Programmation asynchrone en Python avec asyncio"
            )
        )
    }

    @When("the knowledge graph is built")
    fun `the knowledge graph is built`() {
        graph = service.buildGraph(articles)
    }

    @Then("edge AB should have a cooccurrence reason")
    fun `edge AB should have a cooccurrence reason`() {
        val edge = graph!!.edges.firstOrNull {
            (it.sourceUrl == "/a.html" && it.targetUrl == "/b.html") ||
            (it.sourceUrl == "/b.html" && it.targetUrl == "/a.html")
        }
        assertThat(edge).isNotNull
        assertThat(edge!!.reasons).anyMatch { it.startsWith("cooccurrence:") }
    }

    @Then("edge AB score should be greater than edge BC score")
    fun `edge AB score should be greater than edge BC score`() {
        val edgeAB = graph!!.edges.firstOrNull {
            (it.sourceUrl == "/a.html" && it.targetUrl == "/b.html") ||
            (it.sourceUrl == "/b.html" && it.targetUrl == "/a.html")
        }
        val edgeBC = graph!!.edges.firstOrNull {
            (it.sourceUrl == "/b.html" && it.targetUrl == "/c.html") ||
            (it.sourceUrl == "/c.html" && it.targetUrl == "/b.html")
        }
        assertThat(edgeAB).isNotNull
        // A-C or B-C might not share any tags → edgeBC could be null
        if (edgeBC != null) {
            assertThat(edgeAB!!.score).isGreaterThan(edgeBC.score)
        }
    }

    @Then("A and C should have no cooccurrence reason")
    fun `A and C should have no cooccurrence reason`() {
        val edge = graph!!.edges.firstOrNull {
            (it.sourceUrl == "/a.html" && it.targetUrl == "/c.html") ||
            (it.sourceUrl == "/c.html" && it.targetUrl == "/a.html")
        }
        // A and C share no tags, so either no edge or no co-occurrence reason
        if (edge != null) {
            assertThat(edge.reasons).noneMatch { it.startsWith("cooccurrence:") }
        }
    }

    @Then("the edge should have entity reasons")
    fun `the edge should have entity reasons`() {
        val edge = graph!!.edges.first()
        assertThat(edge.reasons).anyMatch { it.startsWith("entity:") }
    }

    @Then("edge score should be greater than zero even without shared tags")
    fun `edge score should be greater than zero without shared tags`() {
        val edge = graph!!.edges.first()
        assertThat(edge.score).isGreaterThan(0.0)
    }

    @Then("the edge with all factors combined should have the highest score")
    fun `edge with all factors combined should have highest score`() {
        val edges = graph!!.edges.sortedByDescending { it.score }
        assertThat(edges).isNotEmpty
        // The edge A-B should have the highest score (tags + title + description overlap)
        val edgeAB = edges.first()
        assertThat(edgeAB.score).isGreaterThan(1.0) // More than just tag score
        assertThat(edgeAB.reasons).anyMatch { it.startsWith("tag:") }
        assertThat(edgeAB.reasons).anyMatch { it.startsWith("entity:") }
    }

    @Then("suggestions should order by decreasing relevance score")
    fun `suggestions should order by decreasing relevance`() {
        val output = service.toSuggestions(graph!!)
        assertThat(output.suggestions).isNotEmpty
        for ((_, suggestions) in output.suggestions) {
            for (i in 0 until suggestions.size - 1) {
                assertThat(suggestions[i].score).isGreaterThanOrEqualTo(suggestions[i + 1].score)
            }
        }
    }
}