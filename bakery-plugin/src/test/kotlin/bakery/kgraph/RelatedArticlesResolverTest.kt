package bakery.kgraph

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class RelatedArticlesResolverTest {

    private val mapper = jacksonObjectMapper()

    @Test
    fun `should resolve suggestions by exact slug match`(@TempDir tempDir: File) {
        val jsonFile = tempDir.resolve("related-articles.json")
        val output = RelatedArticlesOutput(
            suggestions = mapOf(
                "/blog/2024/kotlin.html" to listOf(
                    RelatedArticleSuggestion("/blog/2024/gradle.html", "Gradle Tips", 3.0, listOf("tag:kotlin")),
                    RelatedArticleSuggestion("/blog/2024/java.html", "Java 24", 1.5, listOf("tag:jvm"))
                )
            )
        )
        mapper.writeValue(jsonFile, output)

        val resolver = RelatedArticlesResolver.load(jsonFile, maxResults = 3)
        val result = resolver.resolve("/blog/2024/kotlin.html")

        assertThat(result).hasSize(2)
        assertThat(result[0].url).isEqualTo("/blog/2024/gradle.html")
        assertThat(result[0].score).isEqualTo(3.0)
        assertThat(result[1].url).isEqualTo("/blog/2024/java.html")
    }

    @Test
    fun `should resolve suggestions by suffix match`(@TempDir tempDir: File) {
        val jsonFile = tempDir.resolve("related-articles.json")
        val output = RelatedArticlesOutput(
            suggestions = mapOf(
                "/blog/2024/kotlin.html" to listOf(
                    RelatedArticleSuggestion("/blog/2024/gradle.html", "Gradle Tips", 2.0)
                )
            )
        )
        mapper.writeValue(jsonFile, output)

        val resolver = RelatedArticlesResolver.load(jsonFile)
        val result = resolver.resolve("kotlin.html")

        assertThat(result).hasSize(1)
        assertThat(result[0].url).isEqualTo("/blog/2024/gradle.html")
    }

    @Test
    fun `should return empty when slug has no suggestions`(@TempDir tempDir: File) {
        val jsonFile = tempDir.resolve("related-articles.json")
        val output = RelatedArticlesOutput(
            suggestions = mapOf(
                "/blog/2024/kotlin.html" to listOf(
                    RelatedArticleSuggestion("/blog/2024/gradle.html", "Gradle Tips", 2.0)
                )
            )
        )
        mapper.writeValue(jsonFile, output)

        val resolver = RelatedArticlesResolver.load(jsonFile)
        val result = resolver.resolve("/blog/2024/nonexistent.html")

        assertThat(result).isEmpty()
    }

    @Test
    fun `should return empty when related-articles json file is missing`(@TempDir tempDir: File) {
        val nonExistentFile = tempDir.resolve("does-not-exist.json")
        val resolver = RelatedArticlesResolver.load(nonExistentFile)

        val result = resolver.resolve("/blog/2024/kotlin.html")
        assertThat(result).isEmpty()
    }

    @Test
    fun `should respect maxResults limit`(@TempDir tempDir: File) {
        val jsonFile = tempDir.resolve("related-articles.json")
        val output = RelatedArticlesOutput(
            suggestions = mapOf(
                "/blog/2024/kotlin.html" to listOf(
                    RelatedArticleSuggestion("/blog/2024/a.html", "A", 1.0),
                    RelatedArticleSuggestion("/blog/2024/b.html", "B", 2.0),
                    RelatedArticleSuggestion("/blog/2024/c.html", "C", 3.0),
                    RelatedArticleSuggestion("/blog/2024/d.html", "D", 4.0)
                )
            )
        )
        mapper.writeValue(jsonFile, output)

        val resolver = RelatedArticlesResolver.load(jsonFile, maxResults = 2)
        val result = resolver.resolve("/blog/2024/kotlin.html")

        assertThat(result).hasSize(2)
    }

    @Test
    fun `should handle empty suggestions in file`(@TempDir tempDir: File) {
        val jsonFile = tempDir.resolve("related-articles.json")
        val output = RelatedArticlesOutput(suggestions = emptyMap())
        mapper.writeValue(jsonFile, output)

        val resolver = RelatedArticlesResolver.load(jsonFile)
        assertThat(resolver.hasSuggestions()).isFalse()
        assertThat(resolver.articleCount()).isEqualTo(0)
        assertThat(resolver.resolve("/anything.html")).isEmpty()
    }

    @Test
    fun `should contain match for partial slug`(@TempDir tempDir: File) {
        val jsonFile = tempDir.resolve("related-articles.json")
        val output = RelatedArticlesOutput(
            suggestions = mapOf(
                "/blog/2024/kotlin-coroutines.html" to listOf(
                    RelatedArticleSuggestion("/blog/2024/threads.html", "Threads", 2.0)
                )
            )
        )
        mapper.writeValue(jsonFile, output)

        val resolver = RelatedArticlesResolver.load(jsonFile)
        val result = resolver.resolve("coroutines")

        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("Threads")
    }

    @Test
    fun `should report article count correctly`(@TempDir tempDir: File) {
        val jsonFile = tempDir.resolve("related-articles.json")
        val output = RelatedArticlesOutput(
            suggestions = mapOf(
                "/a.html" to listOf(RelatedArticleSuggestion("/b.html", "B", 1.0)),
                "/c.html" to listOf(RelatedArticleSuggestion("/d.html", "D", 1.0)),
                "/e.html" to listOf(RelatedArticleSuggestion("/f.html", "F", 1.0))
            )
        )
        mapper.writeValue(jsonFile, output)

        val resolver = RelatedArticlesResolver.load(jsonFile)
        assertThat(resolver.articleCount()).isEqualTo(3)
        assertThat(resolver.hasSuggestions()).isTrue()
    }

    @Test
    fun `should read blogArticles from related-articles json`(@TempDir tempDir: File) {
        val jsonFile = tempDir.resolve("related-articles.json")
        val blogArticles = mapOf(
            "/blog/kotlin.html" to BlogArticleSummary(
                title = "Introduction à Kotlin",
                description = "Un guide complet pour débuter avec Kotlin",
                tags = listOf("kotlin", "programmation"),
                date = "2024-07-24",
                author = "John Doe"
            ),
            "/blog/gradle.html" to BlogArticleSummary(
                title = "Gradle pour les nuls",
                description = "Tutoriel Gradle",
                tags = listOf("gradle", "build")
            )
        )
        val output = RelatedArticlesOutput(
            blogArticles = blogArticles,
            suggestions = mapOf(
                "/blog/kotlin.html" to listOf(
                    RelatedArticleSuggestion("/blog/gradle.html", "Gradle pour les nuls", 2.0)
                )
            )
        )
        mapper.writeValue(jsonFile, output)

        val resolver = RelatedArticlesResolver.load(jsonFile)
        val result = resolver.resolve("/blog/kotlin.html")
        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("Gradle pour les nuls")
    }

    @Test
    fun `should get blog article metadata from output`(@TempDir tempDir: File) {
        val jsonFile = tempDir.resolve("related-articles.json")
        val blogArticles = mapOf(
            "/blog/pgvector.html" to BlogArticleSummary(
                title = "PGVector avec Kotlin",
                description = "Recherche vectorielle avec PGVector et LangChain4j",
                tags = listOf("pgvector", "kotlin", "rag"),
                date = "2024-08-15",
                author = "Jane Smith"
            )
        )
        val output = RelatedArticlesOutput(
            blogArticles = blogArticles,
            suggestions = emptyMap()
        )
        mapper.writeValue(jsonFile, output)

        // Verify deserialization round-trip
        val read: RelatedArticlesOutput = mapper.readValue(jsonFile.readText(), RelatedArticlesOutput::class.java)
        assertThat(read.blogArticles).hasSize(1)
        assertThat(read.blogArticles["/blog/pgvector.html"]!!.title).isEqualTo("PGVector avec Kotlin")
        assertThat(read.blogArticles["/blog/pgvector.html"]!!.description).contains("PGVector")
        assertThat(read.blogArticles["/blog/pgvector.html"]!!.tags).containsExactly("pgvector", "kotlin", "rag")
    }

    @Test
    fun `should handle output with blogArticles but no suggestions`(@TempDir tempDir: File) {
        val jsonFile = tempDir.resolve("related-articles.json")
        val blogArticles = mapOf(
            "/a.html" to BlogArticleSummary(title = "Article A", tags = listOf("kotlin")),
            "/b.html" to BlogArticleSummary(title = "Article B", tags = listOf("java"))
        )
        val output = RelatedArticlesOutput(
            blogArticles = blogArticles,
            suggestions = emptyMap()
        )
        mapper.writeValue(jsonFile, output)

        val resolver = RelatedArticlesResolver.load(jsonFile)
        assertThat(resolver.hasSuggestions()).isFalse()
        assertThat(resolver.resolve("/a.html")).isEmpty()
    }
}
