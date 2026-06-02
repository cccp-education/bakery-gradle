package bakery.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * E2E tests for analytics-script and newsletter-form components — BKY-JB-9 Phase B.
 *
 * Verifies that Thymeleaf templates render correctly in a real browser,
 * with JavaScript execution and conditional visibility.
 *
 * Run with: ./gradlew e2eTest
 * Prerequisite: npx playwright install chromium
 */
@Tag("e2e")
@DisplayName("E2E - Analytics & Newsletter")
class E2EAnalyticsNewsletterTest : E2ETestBase() {

    @Test
    @DisplayName("Analytics: Plausible script is rendered when analytics is configured")
    fun `plausible script is rendered when analytics is configured`() {
        val path = serveHtml(
            "analytics-script",
            mapOf(
                "analyticsProvider" to "plausible",
                "analyticsDomain" to "my-site.com",
                "analyticsScriptSrc" to "https://plausible.io/js/script.js"
            )
        )

        val page = navigateTo(path)

        // Verify Plausible script is present in the rendered HTML
        val scriptLocator = page.locator("script[data-domain='my-site.com']")
        assertThat(scriptLocator.count()).isGreaterThan(0)

        // Verify no Thymeleaf attributes remain in rendered output
        assertThat(page.content()).doesNotContain("th:if")
        assertThat(page.content()).doesNotContain("th:src")
    }

    @Test
    @DisplayName("Analytics: No script when analytics is not configured")
    fun `no script when analytics is not configured`() {
        val path = serveHtml("analytics-script")

        val page = navigateTo(path)

        // Verify no analytics script is rendered
        assertThat(page.locator("script[data-domain]").count()).isEqualTo(0)
        assertThat(page.locator("script[src*='plausible']").count()).isEqualTo(0)
        assertThat(page.locator("script[src*='matomo']").count()).isEqualTo(0)
    }

    @Test
    @DisplayName("Analytics: Matomo script is rendered when configured")
    fun `matomo script is rendered when configured`() {
        val path = serveHtml(
            "analytics-script",
            mapOf(
                "analyticsProvider" to "matomo",
                "analyticsDomain" to "my-site.com",
                "analyticsScriptSrc" to "https://matomo.example.com/"
            )
        )

        val page = navigateTo(path)

        // Verify Matomo tracking code is present
        assertThat(page.content()).contains("_paq")
        // No data-domain attribute (that's Plausible-specific)
        assertThat(page.locator("script[data-domain]").count()).isEqualTo(0)
    }

    @Test
    @DisplayName("Newsletter: Form is visible when newsletter is enabled")
    fun `newsletter form is visible when enabled`() {
        val path = serveHtml(
            "newsletter-form",
            mapOf(
                "newsletterEnabled" to "true",
                "newsletterEndpoint" to "https://mailchimp.example.com/subscribe"
            )
        )

        val page = navigateTo(path)

        // Verify newsletter form is rendered
        assertThat(page.locator(".newsletter-section").count()).isGreaterThan(0)
        assertThat(page.locator(".newsletter-input").count()).isGreaterThan(0)
        assertThat(page.locator(".newsletter-submit").count()).isGreaterThan(0)
    }

    @Test
    @DisplayName("Newsletter: No form when newsletter is not enabled")
    fun `no form_when_newsletter_is_not_enabled`() {
        val path = serveHtml("newsletter-form")

        val page = navigateTo(path)

        // Verify newsletter form is NOT rendered
        assertThat(page.locator(".newsletter-section").count()).isEqualTo(0)
        assertThat(page.locator(".newsletter-input").count()).isEqualTo(0)
    }
}