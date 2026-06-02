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
}
