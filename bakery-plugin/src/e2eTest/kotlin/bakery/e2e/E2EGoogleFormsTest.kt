package bakery.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * E2E tests for Google Forms embed component — BKY-JB-9 Phase B.
 *
 * Verifies that the Google Forms iframe is rendered correctly
 * in a real browser with th:if guards.
 *
 * Run with: ./gradlew e2eTest
 * Prerequisite: npx playwright install chromium
 */
@Tag("e2e")
@DisplayName("E2E - Google Forms Embed")
class E2EGoogleFormsTest : E2ETestBase() {

    @Test
    @DisplayName("Google Forms: Iframe is visible when formId is configured")
    fun `iframe is visible when form_id_is_configured`() {
        val path = serveHtml(
            "google-forms",
            mapOf(
                "googleFormsFormId" to "1ABC-x12345",
                "googleFormsWidth" to "640",
                "googleFormsHeight" to "800"
            )
        )

        val page = navigateTo(path)

        // Verify Google Forms iframe is present
        val iframeLocator = page.locator("iframe[src*='docs.google.com/forms']")
        assertThat(iframeLocator.count()).isGreaterThan(0)

        // Verify iframe contains the formId
        val iframeSrc = iframeLocator.first().getAttribute("src")
        assertThat(iframeSrc).contains("1ABC-x12345")

        // Verify iframe dimensions
        assertThat(iframeLocator.first().getAttribute("width")).isEqualTo("640")
        assertThat(iframeLocator.first().getAttribute("height")).isEqualTo("800")

        // No Thymeleaf guards in rendered output
        assertThat(page.content()).doesNotContain("th:if")
    }

    @Test
    @DisplayName("Google Forms: No iframe when formId is not configured")
    fun `no iframe when form_id_is_not_configured`() {
        val path = serveHtml("google-forms")

        val page = navigateTo(path)

        // Verify no iframe is rendered
        assertThat(page.locator("iframe").count()).isEqualTo(0)
        assertThat(page.locator("#google-form-container, .google-form-container").count()).isEqualTo(0)
    }
}