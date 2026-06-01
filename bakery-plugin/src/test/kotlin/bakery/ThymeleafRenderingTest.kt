package bakery

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Thymeleaf Rendering Tests — BKY-JB-9 Phase A.
 *
 * These tests prove that Thymeleaf templates render correctly with actual
 * variable interpolation and th:if guards — replacing the older
 * `template.readText().contains()` approach which only checked source text.
 */
class ThymeleafRenderingTest {

    private val factory = ThymeleafRenderingTestFactory()

    @Nested
    @DisplayName("Sanity — Factory resolves and renders templates")
    inner class SanityTest {

        @Test
        fun `factory resolves templates directory`() {
            assertThat(factory.templatesDirExists()).isTrue()
        }

        @Test
        fun `factory finds analytics-script template`() {
            assertThat(factory.templateExists("analytics-script")).isTrue()
        }
    }

    @Nested
    @DisplayName("analytics-script.thyme — rendering with context")
    inner class AnalyticsScriptRenderingTest {

        @Test
        fun `renders plausible script when analyticsProvider is plausible`() {
            val html = factory.render("analytics-script", mapOf(
                "analyticsProvider" to "plausible",
                "analyticsDomain" to "my-site.com",
                "analyticsScriptSrc" to "https://plausible.io/js/script.js"
            ))

            assertThat(html).contains("data-domain=\"my-site.com\"")
            assertThat(html).contains("src=\"https://plausible.io/js/script.js\"")
            assertThat(html).doesNotContain("th:if")
            assertThat(html).doesNotContain("analyticsProvider")
        }

        @Test
        fun `renders nothing when analyticsProvider is absent`() {
            val html = factory.render("analytics-script")

            assertThat(html).doesNotContain("<script")
            assertThat(html).doesNotContain("data-domain")
            assertThat(html).doesNotContain("_paq")
        }

        @Test
        fun `renders matomo script when analyticsProvider is matomo`() {
            val html = factory.render("analytics-script", mapOf(
                "analyticsProvider" to "matomo",
                "analyticsDomain" to "my-site.com",
                "analyticsScriptSrc" to "https://matomo.example.com/"
            ))

            assertThat(html).contains("_paq")
            assertThat(html).doesNotContain("data-domain")
            assertThat(html).doesNotContain("th:if")
        }
    }

    @Nested
    @DisplayName("google-forms.thyme — rendering with context")
    inner class GoogleFormsRenderingTest {

        @Test
        fun `renders iframe when googleFormsFormId is present`() {
            val html = factory.render("google-forms", mapOf(
                "googleFormsFormId" to "1ABC-x12345",
                "googleFormsWidth" to "640",
                "googleFormsHeight" to "800"
            ))

            assertThat(html).contains("1ABC-x12345")
            assertThat(html).contains("docs.google.com/forms")
            assertThat(html).contains("<iframe")
            assertThat(html).doesNotContain("th:if")
        }

        @Test
        fun `renders nothing when googleFormsFormId is absent`() {
            val html = factory.render("google-forms")

            assertThat(html).doesNotContain("<iframe")
            assertThat(html).doesNotContain("docs.google.com")
        }
    }

    @Nested
    @DisplayName("theme-script.thyme — rendering with context")
    inner class ThemeScriptRenderingTest {

        @Test
        fun `renders CSS variables when themePrimaryColor is present`() {
            val html = factory.render("theme-script", mapOf(
                "themePrimaryColor" to "#e74c3c",
                "themeSecondaryColor" to "#2c3e50",
                "themeFontFamily" to "Inter, sans-serif"
            ))

            assertThat(html).contains("setProperty('--bakery-primary', \"#e74c3c\")")
            assertThat(html).contains("setProperty('--bakery-secondary', \"#2c3e50\")")
            assertThat(html).contains("setProperty('--bakery-font-family', \"Inter, sans-serif\")")
            assertThat(html).doesNotContain("th:if")
        }

        @Test
        fun `renders nothing when themePrimaryColor is absent`() {
            val html = factory.render("theme-script")

            assertThat(html).doesNotContain("setProperty")
            assertThat(html).doesNotContain("<script")
        }
    }

    @Nested
    @DisplayName("auth-header.thyme — rendering with context")
    inner class AuthHeaderRenderingTest {

        @Test
        fun `renders login button when firebaseAuthApiKey is present`() {
            val html = factory.render("auth-header", mapOf(
                "firebaseAuthApiKey" to "AIzaSyTest123"
            ))

            assertThat(html).contains("auth-btn")
            assertThat(html).contains("Connexion")
            assertThat(html).doesNotContain("th:if")
        }

        @Test
        fun `renders nothing when firebaseAuthApiKey is absent`() {
            val html = factory.render("auth-header")

            assertThat(html).doesNotContain("auth-btn")
            assertThat(html).doesNotContain("Connexion")
        }
    }

    @Nested
    @DisplayName("comments.thyme — rendering with context")
    inner class CommentsRenderingTest {

        @Test
        fun `renders comments section when commentsEnabled is true`() {
            val html = factory.render("comments", mapOf(
                "commentsEnabled" to "true",
                "commentsCollection" to "blog-comments",
                "content" to mapOf("uri" to "/blog/my-post")
            ))

            assertThat(html).contains("comments-section")
            assertThat(html).contains("comment-text")
            assertThat(html).contains("comment-submit")
            assertThat(html).doesNotContain("th:if")
        }

        @Test
        fun `renders nothing when commentsEnabled is false`() {
            val html = factory.render("comments", mapOf(
                "commentsEnabled" to "false"
            ))

            assertThat(html).doesNotContain("comments-section")
            assertThat(html).doesNotContain("comment-text")
        }

        @Test
        fun `renders nothing when commentsEnabled is absent`() {
            val html = factory.render("comments")

            assertThat(html).doesNotContain("comments-section")
        }
    }

    @Nested
    @DisplayName("newsletter-form.thyme — rendering with context")
    inner class NewsletterFormRenderingTest {

        @Test
        fun `renders newsletter form when newsletterEnabled is true`() {
            val html = factory.render("newsletter-form", mapOf(
                "newsletterEnabled" to "true",
                "newsletterEndpoint" to "https://mailchimp.example.com/subscribe"
            ))

            assertThat(html).contains("newsletter-section")
            assertThat(html).contains("newsletter-input")
            assertThat(html).contains("newsletter-submit")
            assertThat(html).contains("mailchimp.example.com/subscribe")
            assertThat(html).doesNotContain("th:if")
        }

        @Test
        fun `renders nothing when newsletterEnabled is false`() {
            val html = factory.render("newsletter-form", mapOf(
                "newsletterEnabled" to "false"
            ))

            assertThat(html).doesNotContain("newsletter-section")
        }

        @Test
        fun `renders nothing when newsletterEnabled is absent`() {
            val html = factory.render("newsletter-form")

            assertThat(html).doesNotContain("newsletter-section")
        }
    }
}