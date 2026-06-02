package bakery.kgraph

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import bakery.kgraph.BlogArticleSummary

class RelatedArticlesOutputTest {

    private val mapper = jacksonObjectMapper()

    @Test
    fun `should serialize and deserialize RelatedArticlesOutput`() {
        val output = RelatedArticlesOutput(
            version = "1.0",
            suggestions = mapOf(
                "/blog/2024/kotlin.html" to listOf(
                    RelatedArticleSuggestion(
                        url = "/blog/2024/gradle.html",
                        title = "Gradle for Beginners",
                        score = 3.0,
                        reasons = listOf("tag:kotlin", "tag:build")
                    ),
                    RelatedArticleSuggestion(
                        url = "/blog/2024/java.html",
                        title = "Java 24 Features",
                        score = 1.5,
                        reasons = listOf("tag:programmation")
                    )
                )
            )
        )

        val json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(output)
        val deserialized: RelatedArticlesOutput = mapper.readValue(json)

        assertThat(deserialized.version).isEqualTo("1.0")
        assertThat(deserialized.generatedAt).isNotNull()
        assertThat(deserialized.suggestions).hasSize(1)
        assertThat(deserialized.suggestions["/blog/2024/kotlin.html"]).hasSize(2)
        assertThat(deserialized.suggestions["/blog/2024/kotlin.html"]!![0].url)
            .isEqualTo("/blog/2024/gradle.html")
        assertThat(deserialized.suggestions["/blog/2024/kotlin.html"]!![0].score)
            .isEqualTo(3.0)
    }

    @Test
    fun `should handle empty suggestions map`() {
        val output = RelatedArticlesOutput(
            suggestions = emptyMap()
        )

        val json = mapper.writeValueAsString(output)
        val deserialized: RelatedArticlesOutput = mapper.readValue(json)

        assertThat(deserialized.suggestions).isEmpty()
    }

    @Test
    fun `should write and read output file`(@org.junit.jupiter.api.io.TempDir tempDir: File) {
        val output = RelatedArticlesOutput(
            suggestions = mapOf(
                "/a.html" to listOf(
                    RelatedArticleSuggestion("/b.html", "B", 2.0)
                )
            )
        )

        val outputFile = tempDir.resolve("related-articles.json")
        mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, output)

        val read: RelatedArticlesOutput = mapper.readValue(outputFile)
        assertThat(read.suggestions).hasSize(1)
        assertThat(read.suggestions["/a.html"]!![0].url).isEqualTo("/b.html")
    }

    @Test
    fun `should serialize and deserialize blogArticles in output`() {
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
                tags = listOf("gradle", "build"),
                date = "2024-08-15"
            )
        )

        val output = RelatedArticlesOutput(
            version = "1.0",
            blogArticles = blogArticles,
            suggestions = mapOf(
                "/blog/kotlin.html" to listOf(
                    RelatedArticleSuggestion(
                        url = "/blog/gradle.html",
                        title = "Gradle pour les nuls",
                        score = 2.0,
                        reasons = listOf("tag:gradle")
                    )
                )
            )
        )

        val json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(output)
        val deserialized: RelatedArticlesOutput = mapper.readValue(json)

        assertThat(deserialized.blogArticles).hasSize(2)
        assertThat(deserialized.blogArticles["/blog/kotlin.html"]!!.title).isEqualTo("Introduction à Kotlin")
        assertThat(deserialized.blogArticles["/blog/kotlin.html"]!!.description).isEqualTo("Un guide complet pour débuter avec Kotlin")
        assertThat(deserialized.blogArticles["/blog/kotlin.html"]!!.tags).containsExactly("kotlin", "programmation")
        assertThat(deserialized.blogArticles["/blog/kotlin.html"]!!.date).isEqualTo("2024-07-24")
        assertThat(deserialized.blogArticles["/blog/kotlin.html"]!!.author).isEqualTo("John Doe")
        assertThat(deserialized.blogArticles["/blog/gradle.html"]!!.author).isEmpty()
        assertThat(deserialized.suggestions).hasSize(1)
    }

    @Test
    fun `should handle empty blogArticles map`() {
        val output = RelatedArticlesOutput(
            blogArticles = emptyMap(),
            suggestions = emptyMap()
        )

        val json = mapper.writeValueAsString(output)
        val deserialized: RelatedArticlesOutput = mapper.readValue(json)

        assertThat(deserialized.blogArticles).isEmpty()
        assertThat(deserialized.suggestions).isEmpty()
    }
}
