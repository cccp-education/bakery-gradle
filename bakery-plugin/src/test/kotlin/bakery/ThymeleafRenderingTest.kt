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
    @DisplayName("header.thyme — theme integration rendering")
    inner class HeaderThemeRenderingTest {

        @Test
        fun `header includes theme-script fragment which renders CSS variables`() {
            val html = factory.render("header", mapOf(
                "themePrimaryColor" to "#e74c3c",
                "themeSecondaryColor" to "#2c3e50",
                "themeFontFamily" to "Inter",
                "content" to mapOf("rootpath" to "")
            ))

            assertThat(html).contains("setProperty('--bakery-primary'")
            assertThat(html).doesNotContain("th:replace")
        }

        @Test
        fun `header renders custom favicon when themeFaviconUrl is present`() {
            val html = factory.render("header", mapOf(
                "themeFaviconUrl" to "/img/custom-favicon.png",
                "content" to mapOf("rootpath" to "")
            ))

            assertThat(html).contains("/img/custom-favicon.png")
            assertThat(html).doesNotContain("cheroliv_logo_icon.png")
        }

        @Test
        fun `header renders default favicon when themeFaviconUrl is absent`() {
            val html = factory.render("header", mapOf(
                "content" to mapOf("rootpath" to "")
            ))

            assertThat(html).contains("cheroliv_logo_icon.png")
            assertThat(html).doesNotContain("th:if")
            assertThat(html).doesNotContain("th:unless")
        }
    }

    @Nested
    @DisplayName("menu.thyme — theme integration rendering")
    inner class MenuThemeRenderingTest {

        @Test
        fun `menu renders custom logo when themeLogoUrl is present`() {
            val html = factory.render("menu", mapOf(
                "themeLogoUrl" to "/img/custom-logo.png",
                "content" to mapOf("uri" to "index.html", "rootpath" to "", "type" to "page")
            ))

            assertThat(html).contains("/img/custom-logo.png")
            assertThat(html).contains("navbar-logo")
            assertThat(html).doesNotContain("th:if")
        }

        @Test
        fun `menu renders brand text when themeLogoUrl is absent`() {
            val html = factory.render("menu", mapOf(
                "content" to mapOf("uri" to "index.html", "rootpath" to "", "type" to "page")
            ))

            assertThat(html).doesNotContain("navbar-logo")
            assertThat(html).contains("Blog Template")
        }
    }

    @Nested
    @DisplayName("footer.thyme — service integration rendering")
    inner class FooterIntegrationRenderingTest {

        @Test
        fun `footer includes firebase-auth-compat script`() {
            val html = factory.render("footer", mapOf(
                "firebaseApiKey" to "AIzaSyTest",
                "firebaseProjectId" to "my-project",
                "firebaseAuthDomain" to "my-project.firebaseapp.com",
                "content" to mapOf("rootpath" to "")
            ))

            assertThat(html).contains("firebase-auth-compat.js")
            assertThat(html).contains("firebase-firestore-compat.js")
            assertThat(html).contains("AIzaSyTest")
            assertThat(html).contains("my-project.firebaseapp.com")
        }

        @Test
        fun `footer includes analytics-script fragment when configured`() {
            val html = factory.render("footer", mapOf(
                "analyticsProvider" to "plausible",
                "analyticsDomain" to "my-site.com",
                "analyticsScriptSrc" to "https://plausible.io/js/script.js",
                "content" to mapOf("rootpath" to "")
            ))

            assertThat(html).contains("data-domain=\"my-site.com\"")
            assertThat(html).doesNotContain("th:replace")
        }

        @Test
        fun `footer includes newsletter-form fragment when enabled`() {
            val html = factory.render("footer", mapOf(
                "newsletterEnabled" to "true",
                "newsletterEndpoint" to "https://mailchimp.example.com/subscribe",
                "content" to mapOf("rootpath" to "")
            ))

            assertThat(html).contains("newsletter-section")
            assertThat(html).contains("mailchimp.example.com/subscribe")
            assertThat(html).doesNotContain("th:replace")
        }
    }

    @Nested
    @DisplayName("breadcrumb.thyme — rendering with context")
    inner class BreadcrumbRenderingTest {

        @Test
        fun `renders breadcrumb with title when content variables are present`() {
            val html = factory.render("breadcrumb", mapOf(
                "content" to mapOf("title" to "Mon Article", "rootpath" to "/blog/")
            ))

            assertThat(html).contains("Mon Article")
            assertThat(html).contains("breadcrumb-item active")
            assertThat(html).contains("/blog/")
            assertThat(html).doesNotContain("th:text")
            assertThat(html).doesNotContain("th:href")
        }

        @Test
        fun `renders blog link in breadcrumb`() {
            val html = factory.render("breadcrumb", mapOf(
                "content" to mapOf("title" to "Test", "rootpath" to "")
            ))

            assertThat(html).contains("blog.html")
            assertThat(html).contains("Accueil")
        }
    }

    @Nested
    @DisplayName("toc-sidebar.thyme — rendering with context")
    inner class TocSidebarRenderingTest {

        @Test
        fun `renders sidebar with Sommaire heading`() {
            val html = factory.render("toc-sidebar")

            assertThat(html).contains("toc-sidebar")
            assertThat(html).contains("Sommaire")
            assertThat(html).contains("toc-list")
            assertThat(html).doesNotContain("th:fragment")
        }
    }

    @Nested
    @DisplayName("progress-bar.thyme — rendering with context")
    inner class ProgressBarRenderingTest {

        @Test
        fun `renders progress bar with ARIA attributes`() {
            val html = factory.render("progress-bar")

            assertThat(html).contains("reading-progress-bar")
            assertThat(html).contains("progressbar")
            assertThat(html).contains("aria-valuenow")
            assertThat(html).doesNotContain("th:fragment")
        }
    }

    @Nested
    @DisplayName("pdf-viewer.thyme — rendering with context")
    inner class PdfViewerRenderingTest {

        @Test
        fun `renders PDF viewer when content pdf is present`() {
            val html = factory.render("pdf-viewer", mapOf(
                "content" to mapOf("pdf" to "pdfs/document.pdf", "rootpath" to "")
            ))

            assertThat(html).contains("pdf-viewer-container")
            assertThat(html).contains("document.pdf")
            assertThat(html).contains("<iframe")
            assertThat(html).contains("Télécharger")
            assertThat(html).doesNotContain("th:if")
        }

        @Test
        fun `renders nothing when content pdf is absent`() {
            val html = factory.render("pdf-viewer", mapOf(
                "content" to mapOf("rootpath" to "")
            ))

            assertThat(html).doesNotContain("pdf-viewer-container")
            assertThat(html).doesNotContain("<iframe")
        }

        @Test
        fun `renders nothing when content pdf is empty string`() {
            val html = factory.render("pdf-viewer", mapOf(
                "content" to mapOf("pdf" to "", "rootpath" to "")
            ))

            assertThat(html).doesNotContain("pdf-viewer-container")
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