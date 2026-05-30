package bakery.kgraph

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

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
}
