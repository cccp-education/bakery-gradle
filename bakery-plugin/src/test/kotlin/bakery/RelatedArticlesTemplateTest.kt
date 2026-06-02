package bakery

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Related Articles template rendering tests — BKY-BKG Step 3 + BKG-1.4.
 *
 * Verifies that related-articles.thyme:
 * - Renders nothing when relatedArticlesEnabled is absent/false
 * - Renders the section with heading when enabled=true
 * - Uses the custom heading when provided
 * - BKG-1.4: Injects relatedArticlesData as data attribute and JavaScript consumption
 */
class RelatedArticlesTemplateTest {

    private val factory = ThymeleafRenderingTestFactory()

    @Nested
    @DisplayName("related-articles.thyme — rendering with context")
    inner class RenderingTest {

        @Test
        fun `renders related-articles section when enabled=true`() {
            val html = factory.render("related-articles", mapOf(
                "relatedArticlesEnabled" to "true",
                "relatedArticlesHeading" to "Articles connexes"
            ))

            assertThat(html).contains("related-articles")
            assertThat(html).contains("Articles connexes")
            assertThat(html).contains("related-articles-list")
            assertThat(html).doesNotContain("th:if")
        }

        @Test
        fun `renders nothing when relatedArticlesEnabled is absent`() {
            val html = factory.render("related-articles")

            // The whole aside should be absent (th:if=false)
            assertThat(html).doesNotContain("related-articles-heading")
            assertThat(html).doesNotContain("related-articles-list")
        }

        @Test
        fun `renders nothing when relatedArticlesEnabled is false`() {
            val html = factory.render("related-articles", mapOf(
                "relatedArticlesEnabled" to "false"
            ))

            // The whole aside should be absent
            assertThat(html).doesNotContain("related-articles-heading")
            assertThat(html).doesNotContain("related-articles-list")
        }

        @Test
        fun `renders custom heading when provided`() {
            val html = factory.render("related-articles", mapOf(
                "relatedArticlesEnabled" to "true",
                "relatedArticlesHeading" to "Voir aussi"
            ))

            assertThat(html).contains("Voir aussi")
            assertThat(html).doesNotContain("th:text")
        }

        @Test
        fun `renders default heading when relatedArticlesHeading is absent`() {
            val html = factory.render("related-articles", mapOf(
                "relatedArticlesEnabled" to "true"
            ))

            assertThat(html).contains("Articles connexes")
        }
    }

    @Nested
    @DisplayName("related-articles.thyme — BKG-1.4 graph consumption")
    inner class GraphConsumptionTest {

        @Test
        fun `renders data-related-articles attribute with graph JSON when enabled and data provided`() {
            val graphJson = """{"suggestions":{"/a.html":[{"url":"/b.html","title":"B","score":2.0}]},"blogArticles":{"/a.html":{"title":"A"},"/b.html":{"title":"B"}}}"""
            val html = factory.render("related-articles", mapOf(
                "relatedArticlesEnabled" to "true",
                "relatedArticlesData" to graphJson,
                "relatedArticlesMaxResults" to "4"
            ))

            assertThat(html).contains("data-related-articles=")
            assertThat(html).contains("data-max-results=")
            assertThat(html).contains("related-articles-list")
            assertThat(html).contains("related-articles-heading")
            assertThat(html).doesNotContain("th:if")
            assertThat(html).doesNotContain("th:data-")
        }

        @Test
        fun `renders aside without data-related-articles when enabled but no data`() {
            val html = factory.render("related-articles", mapOf(
                "relatedArticlesEnabled" to "true",
                "relatedArticlesData" to "",
                "relatedArticlesMaxResults" to "4"
            ))

            // The aside is still rendered (enabled=true), but data-related-articles is empty
            assertThat(html).contains("related-articles-heading")
            assertThat(html).contains("related-articles-list")
            assertThat(html).contains("data-max-results=")
        }
    }
}