package bakery.kgraph

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RelatedArticlesServiceTest {

    private val service = RelatedArticlesService()

    @Nested
    inner class `Build graph from articles` {

        @Test
        fun `should create empty graph when no articles`() {
            val graph = service.buildGraph(emptyList())
            assertThat(graph.articles).isEmpty()
            assertThat(graph.edges).isEmpty()
        }

        @Test
        fun `should create graph with single article and no edges`() {
            val articles = listOf(
                ArticleNode("/a.html", "Article A", tags = listOf("kotlin"))
            )
            val graph = service.buildGraph(articles)
            assertThat(graph.articles).hasSize(1)
            assertThat(graph.edges).isEmpty()
        }

        @Test
        fun `should create edges between articles sharing tags`() {
            val articles = listOf(
                ArticleNode("/a.html", "Article A", tags = listOf("kotlin", "gradle")),
                ArticleNode("/b.html", "Article B", tags = listOf("kotlin", "java")),
                ArticleNode("/c.html", "Article C", tags = listOf("java", "python")),
            )
            val graph = service.buildGraph(articles)

            // A et B partagent "kotlin" → edge
            assertNotNull(edgesBetween(graph, "/a.html", "/b.html"))
            // B et C partagent "java" → edge
            assertNotNull(edgesBetween(graph, "/b.html", "/c.html"))
            // A et C ne partagent rien → pas d'edge direct
            assertNull(edgesBetween(graph, "/a.html", "/c.html"))
        }

        @Test
        fun `should score edges by number of shared tags`() {
            val articles = listOf(
                ArticleNode("/a.html", "Article A", tags = listOf("kotlin", "gradle", "tutorial")),
                ArticleNode("/b.html", "Article B", tags = listOf("kotlin", "gradle")),
                ArticleNode("/c.html", "Article C", tags = listOf("kotlin")),
            )
            val graph = service.buildGraph(articles)

            // A-B partagent 2 tags → score plus élevé
            val edgeAB = edgesBetween(graph, "/a.html", "/b.html")
            assertNotNull(edgeAB)
            assertThat(edgeAB!!.score).isEqualTo(2.0)

            // A-C partagent 1 tag → score moins élevé
            val edgeAC = edgesBetween(graph, "/a.html", "/c.html")
            assertNotNull(edgeAC)
            assertThat(edgeAC!!.score).isEqualTo(1.0)
        }

        @Test
        fun `should add keyword reasons for shared tags`() {
            val articles = listOf(
                ArticleNode("/a.html", "Article A", tags = listOf("kotlin", "gradle")),
                ArticleNode("/b.html", "Article B", tags = listOf("kotlin", "java")),
            )
            val graph = service.buildGraph(articles)

            val edge = edgesBetween(graph, "/a.html", "/b.html")
            assertNotNull(edge)
            assertThat(edge!!.reasons).contains("tag:kotlin")
            assertThat(edge.reasons).doesNotContain("tag:gradle") // B n'a pas gradle
        }

        @Test
        fun `should score title keyword overlap`() {
            val articles = listOf(
                ArticleNode("/a.html", "Introduction à Kotlin et Gradle"),
                ArticleNode("/b.html", "Kotlin pour les débutants"),
                ArticleNode("/c.html", "Java 24 nouvelles fonctionnalités"),
            )
            val graph = service.buildGraph(articles)

            assertNotNull(edgesBetween(graph, "/a.html", "/b.html"))
            assertNull(edgesBetween(graph, "/a.html", "/c.html"))
        }

        @Test
        fun `should combine tag score and title score`() {
            val articles = listOf(
                ArticleNode("/a.html", "Kotlin Gradle Tutorial", tags = listOf("kotlin", "gradle")),
                ArticleNode("/b.html", "Gradle Kotlin DSL Guide", tags = listOf("kotlin", "gradle")),
                ArticleNode("/c.html", "Java Spring Boot", tags = listOf("java")),
            )
            val graph = service.buildGraph(articles)

            // A-B : 2 tags partagés (2.0) + 2 mots titre partagés (Kotlin, Gradle → 0.4) = 2.4
            val edgeAB = edgesBetween(graph, "/a.html", "/b.html")
            assertNotNull(edgeAB)
            assertThat(edgeAB!!.score).isGreaterThan(2.0)

            // A-C : 0 tag, 0 titre → pas d'edge
            assertNull(edgesBetween(graph, "/a.html", "/c.html"))
        }
    }

    @Nested
    inner class `Build suggestions from graph` {

        @Test
        fun `should return ordered suggestions for each article`() {
            val articles = listOf(
                ArticleNode("/a.html", "Article A", tags = listOf("kotlin", "gradle")),
                ArticleNode("/b.html", "Article B", tags = listOf("kotlin", "java")),
                ArticleNode("/c.html", "Article C", tags = listOf("gradle")),
            )
            val graph = service.buildGraph(articles)
            val output = service.toSuggestions(graph)

            // A a 2 edges : A-B (score 1.0) et A-C (score 1.0)
            assertThat(output.suggestions).containsKey("/a.html")
            assertThat(output.suggestions["/a.html"]).hasSize(2)
        }

        @Test
        fun `should skip articles with no relations`() {
            val articles = listOf(
                ArticleNode("/a.html", "Article A", tags = listOf("kotlin")),
                ArticleNode("/b.html", "Article B", tags = listOf("java")),
            )
            val graph = service.buildGraph(articles)
            val output = service.toSuggestions(graph)

            // Aucun tag partagé → pas de suggestions
            assertThat(output.suggestions).isEmpty()
        }

        @Test
        fun `should map edge to suggestion with url title and score`() {
            val articles = listOf(
                ArticleNode("/a.html", "Article A", tags = listOf("kotlin")),
                ArticleNode("/b.html", "Article B", tags = listOf("kotlin")),
            )
            val graph = service.buildGraph(articles)
            val output = service.toSuggestions(graph)

            val suggestions = output.suggestions["/a.html"]!!
            assertThat(suggestions).hasSize(1)
            assertThat(suggestions[0].url).isEqualTo("/b.html")
            assertThat(suggestions[0].title).isEqualTo("Article B")
            assertThat(suggestions[0].score).isEqualTo(1.0)
        }
    }

    // ——— helpers ———

    private fun edgesBetween(graph: RelatedArticlesGraph, a: String, b: String): ArticleEdge? =
        graph.edges.firstOrNull { edgeOrdered(it, a, b) }

    private fun edgeOrdered(e: ArticleEdge, a: String, b: String): Boolean =
        (e.sourceUrl == a && e.targetUrl == b) || (e.sourceUrl == b && e.targetUrl == a)

    /**
     * AssertJ Kotlin extension : vérifie qu'une valeur nullable est null.
     * Contourne le problème de type inference des génériques avec AssertJ 3.x.
     */
    private fun <T> assertNull(actual: T?) {
        assertThat(actual).isNull()
    }

    private fun assertNotNull(actual: Any?) {
        assertThat(actual).isNotNull
    }
}
