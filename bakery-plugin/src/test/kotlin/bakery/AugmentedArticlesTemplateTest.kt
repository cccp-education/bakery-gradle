package bakery

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * BKY-LENS-6.1 — Template tests for augmented-articles fragment.
 *
 * Two levels of validation:
 * 1. Source text assertions — verify Thymeleaf expressions exist in the template source
 * 2. Thymeleaf rendering tests — verify the HTML output when Thymeleaf evaluates
 *    th:if guards and variable interpolation
 *
 * Note: Badge rendering (data-channel, CSS classes) happens via **JavaScript** at
 * browser time. Thymeleaf renders the data-attribute container and script, but the
 * actual badge elements (`<span class="badge badge-source-RAG">`) are generated
 * client-side. Tests here validate the Thymeleaf layer only.
 */
class AugmentedArticlesTemplateTest {

    private val factory = ThymeleafRenderingTestFactory()

    @Nested
    @DisplayName("augmented-articles.thyme — source text assertions")
    inner class SourceAssertionsTest {

        private val template = java.io.File("src/main/resources/site/templates/augmented-articles.thyme")

        @Test
        fun `template file exists`() {
            assertThat(template.exists()).isTrue()
        }

        @Test
        fun `template has th-if guard on augmentedContextEnabled`() {
            val content = template.readText()
            assertThat(content).contains("th:if")
            assertThat(content).contains("augmentedContextEnabled")
        }

        @Test
        fun `template references augmentedContextData for badge sources`() {
            val content = template.readText()
            assertThat(content).contains("augmentedContextData")
        }

        @Test
        fun `template has fragment definition`() {
            val content = template.readText()
            assertThat(content).contains("th:fragment=\"augmented-articles\"")
        }

        @Test
        fun `template has section heading`() {
            val content = template.readText()
            assertThat(content).contains("related-articles-heading")
        }

        @Test
        fun `template has JavaScript for runtime badge rendering`() {
            val content = template.readText()
            // The JavaScript parses augmentedContextData and creates badges dynamically
            assertThat(content).contains("scoredNodes")
            assertThat(content).contains("badge")
            assertThat(content).contains("data-channel")
        }

        @Test
        fun `template has augmented CSS class on aside`() {
            val content = template.readText()
            assertThat(content).contains("augmented-articles")
        }

        @Test
        fun `template respects lensBudgetMaxArticlesPerPage for truncation`() {
            val content = template.readText()
            assertThat(content).contains("maxResults")
        }
    }

    @Nested
    @DisplayName("augmented-articles.thyme — Thymeleaf rendering tests")
    inner class RenderingTest {

        @Test
        fun `renders nothing when augmentedContextEnabled is absent`() {
            val html = factory.render("augmented-articles")
            assertThat(html).doesNotContain("<aside")
            assertThat(html).doesNotContain("related-articles")
        }

        @Test
        fun `renders aside when enabled with data`() {
            val augmentedData = """{"version":"1.0","pipeline":"LENS","scoredNodes":[],"totalCandidates":0}"""
            val html = factory.render("augmented-articles", mapOf(
                "augmentedContextEnabled" to "true",
                "augmentedContextData" to augmentedData
            ))
            assertThat(html).contains("<aside")
            assertThat(html).contains("augmented-articles")
            assertThat(html).doesNotContain("th:if")
        }

        @Test
        fun `renders data-augmented-context attribute with JSON`() {
            val augmentedData = """{"version":"1.0","pipeline":"LENS","scoredNodes":[{"id":"node-1","title":"Article","uri":"/a.html","score":0.92,"channels":["RAG"]}],"totalCandidates":1}"""
            val html = factory.render("augmented-articles", mapOf(
                "augmentedContextEnabled" to "true",
                "augmentedContextData" to augmentedData
            ))
            // Thymeleaf injects augmentedContextData as a data- attribute
            assertThat(html).contains("data-augmented-context")
            assertThat(html).contains("scoredNodes")
        }

        @Test
        fun `renders default heading when no custom heading`() {
            val augmentedData = """{"version":"1.0","pipeline":"LENS","scoredNodes":[]}"""
            val html = factory.render("augmented-articles", mapOf(
                "augmentedContextEnabled" to "true",
                "augmentedContextData" to augmentedData
            ))
            assertThat(html).contains("Articles connexes")
        }

        @Test
        fun `renders custom heading when provided`() {
            val augmentedData = """{"version":"1.0","pipeline":"LENS","scoredNodes":[]}"""
            val html = factory.render("augmented-articles", mapOf(
                "augmentedContextEnabled" to "true",
                "augmentedContextData" to augmentedData,
                "relatedArticlesHeading" to "Voir aussi"
            ))
            assertThat(html).contains("Voir aussi")
            assertThat(html).doesNotContain("th:text")
        }

        @Test
        fun `hides section when enabled flag is false`() {
            val html = factory.render("augmented-articles", mapOf(
                "augmentedContextEnabled" to "false",
                "augmentedContextData" to """{"version":"1.0","pipeline":"LENS","scoredNodes":[]}"""
            ))
            assertThat(html).doesNotContain("<aside")
        }

        @Test
        fun `hides section when augmentedContextData is empty string`() {
            val html = factory.render("augmented-articles", mapOf(
                "augmentedContextEnabled" to "true",
                "augmentedContextData" to ""
            ))
            // Empty data → th:if guards prevent rendering (augmentedContextData != '')
            assertThat(html).doesNotContain("<aside")
        }

        @Test
        fun `injects lensBudgetMaxArticlesPerPage as data attribute`() {
            val augmentedData = """{"version":"1.0","pipeline":"LENS","scoredNodes":[]}"""
            val html = factory.render("augmented-articles", mapOf(
                "augmentedContextEnabled" to "true",
                "augmentedContextData" to augmentedData,
                "lensBudgetMaxArticlesPerPage" to "6"
            ))
            assertThat(html).contains("data-max-results=\"6\"")
        }

        @Test
        fun `JavaScript parses scoredNodes and creates badges`() {
            val template = java.io.File("src/main/resources/site/templates/augmented-articles.thyme")
            val content = template.readText()
            // JavaScript creates badge elements at runtime
            assertThat(content).contains("node.channels")
            assertThat(content).contains("badge-source-")
            assertThat(content).contains("data-channel")
            assertThat(content).contains("appendChild(badge)")
        }

        @Test
        fun `JavaScript applies maxResults truncation`() {
            val template = java.io.File("src/main/resources/site/templates/augmented-articles.thyme")
            val content = template.readText()
            assertThat(content).contains(".slice(0, maxResults)")
        }
    }

    @Nested
    @DisplayName("post.thyme — references augmented-articles fragment")
    inner class PostTemplateIntegrationTest {

        private val template = java.io.File("src/main/resources/site/templates/post.thyme")

        @Test
        fun `post thyme includes augmented-articles fragment`() {
            val content = template.readText()
            assertThat(content).contains("augmented-articles.thyme")
            assertThat(content).contains("augmented-articles")
        }

        @Test
        fun `post thyme no longer uses legacy related-articles fragment`() {
            val content = template.readText()
            // Should NOT contain the old related-articles.thyme reference
            assertThat(content).doesNotContain("related-articles.thyme::related-articles")
        }
    }

    @Nested
    @DisplayName("page.thyme — references augmented-articles fragment")
    inner class PageTemplateIntegrationTest {

        private val template = java.io.File("src/main/resources/site/templates/page.thyme")

        @Test
        fun `page thyme includes augmented-articles fragment`() {
            val content = template.readText()
            assertThat(content).contains("augmented-articles.thyme")
            assertThat(content).contains("augmented-articles")
        }
    }
}