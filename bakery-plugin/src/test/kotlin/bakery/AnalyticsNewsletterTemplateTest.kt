package bakery

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Source-text assertion tests for analytics and newsletter templates.
 *
 * **Superseded by** [ThymeleafRenderingTest.AnalyticsScriptRenderingTest],
 * [ThymeleafRenderingTest.NewsletterFormRenderingTest], and
 * [ThymeleafRenderingTest.FooterIntegrationRenderingTest] which render actual
 * HTML via Thymeleaf engine instead of checking raw template source.
 *
 * Kept as reference literature — these tests verify the *presence* of
 * Thymeleaf expressions in the source, while rendering tests verify the
 * *evaluated output*. Both perspectives are valuable.
 */
class AnalyticsNewsletterTemplateTest {

    private val templatesDir = File("src/main/resources/site/templates")

    @Test
    fun `analytics-script thyme template contains plausible script with guard`() {
        val template = templatesDir.resolve("analytics-script.thyme")
        assertTrue(template.exists(), "analytics-script.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("analytics-script"), "must have analytics-script fragment")
        assertTrue(content.contains("th:if"), "must have th:if condition to hide when analytics absent")
        assertTrue(content.contains("analyticsProvider"), "th:if must guard on analyticsProvider")
        assertTrue(content.contains("plausible"), "must handle plausible provider")
        assertTrue(content.contains("analyticsDomain"), "must use analyticsDomain for data-domain")
        assertTrue(content.contains("analyticsScriptSrc"), "must use analyticsScriptSrc for script src")
    }

    @Test
    fun `analytics-script thyme template renders nothing when analyticsProvider is absent`() {
        val template = templatesDir.resolve("analytics-script.thyme")
        assertTrue(template.exists(), "analytics-script.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("th:if"), "must have th:if condition")
        assertTrue(
            content.contains("analyticsProvider") && content.contains("th:if"),
            "th:if must guard on analyticsProvider so fragment renders nothing when absent"
        )
    }

    @Test
    fun `analytics-script thyme template handles matomo provider`() {
        val template = templatesDir.resolve("analytics-script.thyme")
        assertTrue(template.exists(), "analytics-script.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("matomo"), "must handle matomo provider")
        assertTrue(content.contains("_paq"), "matomo tracker uses _paq")
    }

    @Test
    fun `newsletter-form thyme template contains form with enabled guard`() {
        val template = templatesDir.resolve("newsletter-form.thyme")
        assertTrue(template.exists(), "newsletter-form.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("newsletter-section"), "must have newsletter-section")
        assertTrue(content.contains("th:if"), "must have th:if condition to hide when newsletter disabled")
        assertTrue(content.contains("newsletterEnabled"), "th:if must guard on newsletterEnabled")
        assertTrue(content.contains("newsletter-input"), "must have email input")
        assertTrue(content.contains("newsletter-submit"), "must have submit button")
    }

    @Test
    fun `newsletter-form thyme template renders nothing when newsletterEnabled is false`() {
        val template = templatesDir.resolve("newsletter-form.thyme")
        assertTrue(template.exists(), "newsletter-form.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("th:if"), "must have th:if condition")
        assertTrue(
            content.contains("newsletterEnabled") && content.contains("th:if"),
            "th:if must guard on newsletterEnabled so fragment renders nothing when disabled"
        )
    }

    @Test
    fun `newsletter-form thyme template uses newsletterEndpoint for form action`() {
        val template = templatesDir.resolve("newsletter-form.thyme")
        assertTrue(template.exists(), "newsletter-form.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("newsletterEndpoint"), "form action must use newsletterEndpoint")
    }

    @Test
    fun `footer thyme includes analytics-script fragment`() {
        val template = templatesDir.resolve("footer.thyme")
        assertTrue(template.exists(), "footer.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("analytics-script"), "footer must include analytics-script fragment")
    }

    @Test
    fun `footer thyme includes newsletter-form fragment`() {
        val template = templatesDir.resolve("footer.thyme")
        assertTrue(template.exists(), "footer.thyme must exist")
        val content = template.readText()
        assertTrue(content.contains("newsletter-form"), "footer must include newsletter-form fragment")
    }
}