package bakery.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * E2E tests for augmented-articles component — BKY-LENS-6.3.
 *
 * Verifies that the augmented-articles Thymeleaf template renders correctly
 * in a real browser, with JavaScript execution and conditional visibility.
 *
 * Run with: ./gradlew e2eTest
 * Prerequisite: npx playwright install chromium
 */
@Tag("e2e")
@DisplayName("E2E - Augmented Articles")
class E2EAugmentedArticlesTest : E2ETestBase() {

    @Test
    @DisplayName("Augmented Articles: Section visible with data-attributes when context enabled")
    fun `augmented articles section visible when context enabled`() {
        val path = serveHtml(
            "augmented-articles",
            mapOf(
                "augmentedContextEnabled" to "true",
                "augmentedContextData" to """{"scoredNodes":[{"uri":"/post1","title":"Post 1","channels":["RAG","KG"]},{"uri":"/post2","title":"Post 2","channels":["Docs"]}]}""",
                "lensBudgetMaxArticlesPerPage" to "4"
            )
        )

        val page = navigateTo(path)

        // Verify augmented-articles section is rendered with data-attributes
        assertThat(page.locator("aside.augmented-articles").count()).isGreaterThan(0)
        assertThat(page.locator("[data-augmented-context]").count()).isGreaterThan(0)
        assertThat(page.locator("[data-max-results]").count()).isGreaterThan(0)

        // Verify heading is rendered (default "Articles connexes du workspace")
        assertThat(page.locator(".related-articles-heading").count()).isGreaterThan(0)

        // Verify JavaScript populates articles from scoredNodes
        val listItems = page.locator(".related-article-item")
        assertThat(listItems.count()).isGreaterThan(0)

        // Verify source badges are created by JavaScript
        assertThat(page.locator(".badge-source-RAG").count()).isGreaterThan(0)
        assertThat(page.locator(".badge-source-KG").count()).isGreaterThan(0)

        // Verify no Thymeleaf attributes remain in rendered output
        assertThat(page.content()).doesNotContain("th:if")
        assertThat(page.content()).doesNotContain("th:data-augmented-context")
    }

    @Test
    @DisplayName("Augmented Articles: No section when context not enabled")
    fun `no augmented articles section when context not enabled`() {
        val path = serveHtml("augmented-articles")

        val page = navigateTo(path)

        // Verify augmented-articles section is NOT rendered (th:if guard)
        assertThat(page.locator("aside.augmented-articles").count()).isEqualTo(0)
        assertThat(page.locator("[data-augmented-context]").count()).isEqualTo(0)
    }

    @Test
    @DisplayName("Augmented Articles: Empty scoredNodes hides the section")
    fun `empty scored nodes hides the section`() {
        val path = serveHtml(
            "augmented-articles",
            mapOf(
                "augmentedContextEnabled" to "true",
                "augmentedContextData" to """{"scoredNodes":[]}""",
                "lensBudgetMaxArticlesPerPage" to "4"
            )
        )

        val page = navigateTo(path)

        // JavaScript sets aside.style.display = 'none' when scoredNodes is empty
        val aside = page.locator("aside.augmented-articles")
        if (aside.count() > 0) {
            // If section is initially rendered, JavaScript should hide it
            assertThat(aside.first().isVisible()).isFalse()
        }
    }

    @Test
    @DisplayName("Augmented Articles: Custom heading text is rendered")
    fun `custom heading text is rendered`() {
        val path = serveHtml(
            "augmented-articles",
            mapOf(
                "augmentedContextEnabled" to "true",
                "augmentedContextData" to """{"scoredNodes":[{"uri":"/post1","title":"Post 1","channels":["RAG"]}]}""",
                "lensBudgetMaxArticlesPerPage" to "4",
                "relatedArticlesHeading" to "Voir aussi"
            )
        )

        val page = navigateTo(path)

        // Verify custom heading text is rendered
        assertThat(page.locator(".related-articles-heading").textContent()).contains("Voir aussi")
    }
}