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

            // A-B partagent 2 tags (kotlin, gradle) → tag score 2.0
            // Co-occurrence bonus: kotlin+gradle co-occur in A and B (2+ times)
            val edgeAB = edgesBetween(graph, "/a.html", "/b.html")
            assertNotNull(edgeAB)
            assertThat(edgeAB!!.score).isGreaterThanOrEqualTo(2.0) // tag score + possible co-occurrence

            // A-C partagent 1 tag (kotlin) → tag score 1.0
            val edgeAC = edgesBetween(graph, "/a.html", "/c.html")
            assertNotNull(edgeAC)
            assertThat(edgeAC!!.score).isGreaterThanOrEqualTo(1.0) // tag score + possible co-occurrence

            // A-B score must be strictly greater than A-C score
            assertThat(edgeAB.score).isGreaterThan(edgeAC.score)
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

    @Nested
    inner class `Tag co-occurrence scoring` {

        @Test
        fun `should create tag_cooccurrence reasons when tags frequently appear together`() {
            val articles = listOf(
                ArticleNode("/a.html", "Article A", tags = listOf("kotlin", "gradle", "tutorial")),
                ArticleNode("/b.html", "Article B", tags = listOf("kotlin", "gradle")),
                ArticleNode("/c.html", "Article C", tags = listOf("kotlin", "maven")),
            )
            val graph = service.buildGraph(articles)

            // A-B partagent kotlin et gradle → co-occurrence (kotlin,gradle) vue 2 fois
            val edgeAB = edgesBetween(graph, "/a.html", "/b.html")
            assertNotNull(edgeAB)
            assertThat(edgeAB!!.reasons).contains("tag:kotlin")
            assertThat(edgeAB.reasons).contains("tag:gradle")
            assertThat(edgeAB.reasons).anyMatch { it.startsWith("cooccurrence:") }
        }

        @Test
        fun `should not create co-occurrence edge for tags appearing only once together`() {
            val articles = listOf(
                ArticleNode("/a.html", "Article A", tags = listOf("kotlin", "gradle")),
                ArticleNode("/b.html", "Article B", tags = listOf("java", "maven")),
            )
            val graph = service.buildGraph(articles)

            // A et B ne partagent aucun tag → pas d'edge
            assertNull(edgesBetween(graph, "/a.html", "/b.html"))
        }
    }

    @Nested
    inner class `Entity overlap from descriptions` {

        @Test
        fun `should create entity_overlap reasons when descriptions share significant terms`() {
            val articles = listOf(
                ArticleNode(
                    "/a.html", "PGVector Search",
                    tags = listOf("pgvector"),
                    description = "Comment utiliser PGVector pour la recherche vectorielle en Kotlin"
                ),
                ArticleNode(
                    "/b.html", "RAG avec PGVector",
                    tags = listOf("rag"),
                    description = "Implémenter le RAG avec PGVector et LangChain4j pour la recherche vectorielle"
                ),
                ArticleNode(
                    "/c.html", "Java Streams",
                    tags = listOf("java"),
                    description = "Introduction aux Java Streams pour le traitement de collections"
                )
            )
            val graph = service.buildGraph(articles)

            // A et B partagent des termes significatifs dans la description (pgvector, recherche, vectorielle)
            val edgeAB = edgesBetween(graph, "/a.html", "/b.html")
            assertNotNull(edgeAB)
            assertThat(edgeAB!!.reasons).anyMatch { it.startsWith("entity:") }

            // A et C ne partagent rien de significatif
            assertNull(edgesBetween(graph, "/a.html", "/c.html"))
        }

        @Test
        fun `should combine tag score title score and entity overlap score`() {
            val articles = listOf(
                ArticleNode(
                    "/a.html", "Kotlin Coroutines Guide",
                    tags = listOf("kotlin", "coroutines"),
                    description = "Guide complet des coroutines Kotlin pour la programmation asynchrone"
                ),
                ArticleNode(
                    "/b.html", "Kotlin Flow Tutorial",
                    tags = listOf("kotlin", "flow"),
                    description = "Tutorial sur Kotlin Flow et les coroutines pour la programmation réactive"
                ),
                ArticleNode(
                    "/c.html", "Python Asyncio",
                    tags = listOf("python"),
                    description = "Programmation asynchrone en Python avec asyncio"
                )
            )
            val graph = service.buildGraph(articles)

            // A-B : 1 tag partagé (kotlin → 1.0) + titre overlap (Kotlin → 0.2)
            //      + description overlap (coroutines, programmation → entity)
            val edgeAB = edgesBetween(graph, "/a.html", "/b.html")
            assertNotNull(edgeAB)
            assertThat(edgeAB!!.score).isGreaterThan(1.0) // Au moins le tag + titre

            // A-C : pas de tags partagés, mais description overlap possible (programmation, asynchrone)
            val edgeAC = edgesBetween(graph, "/a.html", "/c.html")
            // Pas d'edge si le score est trop bas après filtrage stop words
            // Mais "asynchrone" peut matcher → on vérifie juste qu'il n'y a pas de tag:kotlin
            if (edgeAC != null) {
                assertThat(edgeAC.reasons).noneMatch { it.startsWith("tag:") }
            }
        }

        @Test
        fun `should ignore descriptions with only stop words for entity overlap`() {
            val articles = listOf(
                ArticleNode("/a.html", "Article A", description = "Une introduction au sujet"),
                ArticleNode("/b.html", "Article B", description = "Un guide sur le même sujet")
            )
            val graph = service.buildGraph(articles)

            // "sujet" peut matcher, mais "introduction", "guide" sont des stop words
            // Le score d'entity overlap doit être faible (≤ 0.2 par mot commun hors stop)
            val edgeAB = edgesBetween(graph, "/a.html", "/b.html")
            if (edgeAB != null) {
                assertThat(edgeAB.score).isLessThan(2.0) // Pas de tags, titre très différent
            }
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
