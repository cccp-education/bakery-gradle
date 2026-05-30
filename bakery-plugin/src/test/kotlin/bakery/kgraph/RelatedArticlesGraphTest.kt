package bakery.kgraph

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RelatedArticlesGraphTest {

    @Test
    fun `should create ArticleNode with all fields`() {
        val node = ArticleNode(
            url = "/blog/2024/kotlin.html",
            title = "Introduction à Kotlin",
            date = "2024-07-24",
            tags = listOf("kotlin", "programmation", "tutoriel"),
            author = "John Doe"
        )
        assertThat(node.url).isEqualTo("/blog/2024/kotlin.html")
        assertThat(node.title).isEqualTo("Introduction à Kotlin")
        assertThat(node.date).isEqualTo("2024-07-24")
        assertThat(node.tags).containsExactly("kotlin", "programmation", "tutoriel")
        assertThat(node.author).isEqualTo("John Doe")
    }

    @Test
    fun `should create ArticleEdge with score and reasons`() {
        val edge = ArticleEdge(
            sourceUrl = "/blog/2024/kotlin.html",
            targetUrl = "/blog/2024/gradle.html",
            score = 2.5,
            reasons = listOf("tag:kotlin", "tag:build")
        )
        assertThat(edge.sourceUrl).isEqualTo("/blog/2024/kotlin.html")
        assertThat(edge.targetUrl).isEqualTo("/blog/2024/gradle.html")
        assertThat(edge.score).isEqualTo(2.5)
        assertThat(edge.reasons).containsExactly("tag:kotlin", "tag:build")
    }

    @Test
    fun `should find related articles for a given url`() {
        val articles = listOf(
            ArticleNode("/a.html", "Article A", tags = listOf("kotlin")),
            ArticleNode("/b.html", "Article B", tags = listOf("kotlin", "gradle")),
            ArticleNode("/c.html", "Article C", tags = listOf("java")),
            ArticleNode("/d.html", "Article D", tags = listOf("kotlin", "java")),
        )
        val edges = listOf(
            ArticleEdge("/a.html", "/b.html", 3.0, listOf("tag:kotlin")),
            ArticleEdge("/a.html", "/d.html", 2.0, listOf("tag:kotlin")),
            ArticleEdge("/b.html", "/d.html", 1.5, listOf("tag:kotlin")),
        )
        val graph = RelatedArticlesGraph(articles, edges)

        val related = graph.relatedTo("/a.html", maxResults = 2)

        assertThat(related).hasSize(2)
        assertThat(related[0].targetUrl).isEqualTo("/b.html") // score 3.0
        assertThat(related[1].targetUrl).isEqualTo("/d.html") // score 2.0
    }

    @Test
    fun `should return empty list when article has no relations`() {
        val graph = RelatedArticlesGraph(
            articles = listOf(ArticleNode("/orphan.html", "Orphan")),
            edges = emptyList()
        )

        val related = graph.relatedTo("/orphan.html")

        assertThat(related).isEmpty()
    }

    @Test
    fun `should respect maxResults limit`() {
        val articles = listOf(
            ArticleNode("/a.html", "Article A", tags = listOf("kotlin")),
            ArticleNode("/b.html", "Article B", tags = listOf("kotlin")),
            ArticleNode("/c.html", "Article C", tags = listOf("kotlin")),
            ArticleNode("/d.html", "Article D", tags = listOf("kotlin")),
        )
        val edges = listOf(
            ArticleEdge("/a.html", "/b.html", 1.0),
            ArticleEdge("/a.html", "/c.html", 2.0),
            ArticleEdge("/a.html", "/d.html", 3.0),
        )
        val graph = RelatedArticlesGraph(articles, edges)

        val related = graph.relatedTo("/a.html", maxResults = 1)

        assertThat(related).hasSize(1)
        assertThat(related[0].targetUrl).isEqualTo("/d.html") // highest score
    }
}
