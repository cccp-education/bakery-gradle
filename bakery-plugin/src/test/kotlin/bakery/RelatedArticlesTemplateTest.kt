package bakery

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Related Articles template rendering tests — BKY-BKG Step 3.
 *
 * Verifies that related-articles.thyme:
 * - Renders nothing when relatedArticlesEnabled is absent/false
 * - Renders the section with heading when enabled=true
 * - Uses the custom heading when provided
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
}