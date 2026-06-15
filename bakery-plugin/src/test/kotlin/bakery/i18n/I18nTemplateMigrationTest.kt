package bakery.i18n

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.InputStreamReader

class I18nTemplateMigrationTest {

    private val siteTemplates = listOf(
        "site/templates/index.thyme",
        "site/templates/page.thyme",
        "site/templates/blog.thyme",
        "site/templates/post.thyme",
        "site/templates/archive.thyme",
        "site/templates/tags.thyme",
        "site/templates/contact.thyme",
        "site/templates/header.thyme",
        "site/templates/footer.thyme",
        "site/templates/menu.thyme",
        "site/templates/comments.thyme",
        "site/templates/newsletter-form.thyme",
        "site/templates/google-forms.thyme",
        "site/templates/pdf-viewer.thyme",
        "site/templates/hero_section.thyme",
        "site/templates/breadcrumb.thyme",
        "site/templates/toc-sidebar.thyme",
        "site/templates/progress-bar.thyme",
        "site/templates/auth-header.thyme",
        "site/templates/augmented-articles.thyme",
    )

    private val siteBasicTemplates = listOf(
        "site-basic/templates/index.thyme",
        "site-basic/templates/page.thyme",
        "site-basic/templates/header.thyme",
        "site-basic/templates/footer.thyme",
        "site-basic/templates/menu.thyme",
    )

    private fun readTemplate(path: String): String {
        val stream = javaClass.classLoader.getResourceAsStream(path)
            ?: throw AssertionError("Template not found: $path")
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    }

    @Nested
    inner class MessageKeyUsageTest {

        @Test
        fun `site templates use message keys for i18n`() {
            val expectedKeys = mapOf(
                "site/templates/index.thyme" to listOf("action.scroll.top"),
                "site/templates/blog.thyme" to listOf("nav.blog", "nav.archives", "action.search", "nav.memo", "action.read.more", "action.search.article", "action.scroll.top"),
                "site/templates/contact.thyme" to listOf("nav.contact", "contact.subtitle", "contact.form.name.placeholder", "contact.form.email.placeholder", "contact.form.phone.placeholder", "contact.form.subject.placeholder", "contact.form.message.placeholder", "contact.form.submit", "contact.success.message", "contact.error.message"),
                "site/templates/header.thyme" to listOf("site.title", "meta.description", "meta.keywords", "meta.author"),
                "site/templates/footer.thyme" to listOf("site.title"),
                "site/templates/menu.thyme" to listOf("image.alt.logo", "site.title", "a11y.toggle.navigation", "nav.home", "nav.blog", "nav.contact", "a11y.theme.switcher", "theme.light", "theme.dark", "theme.high.contrast"),
                "site/templates/comments.thyme" to listOf("comments.heading", "comments.loading", "comments.form.label", "comments.form.placeholder", "comments.form.submit", "comments.login.hint", "comments.empty", "comments.anonymous", "comments.error.load", "comments.error.auth"),
                "site/templates/newsletter-form.thyme" to listOf("newsletter.heading", "newsletter.description", "newsletter.email.placeholder", "newsletter.subscribe.button"),
                "site/templates/google-forms.thyme" to listOf("google.forms.heading", "google.forms.title", "google.forms.loading"),
                "site/templates/pdf-viewer.thyme" to listOf("pdf.viewer.heading", "pdf.viewer.download", "pdf.viewer.title"),
                "site/templates/hero_section.thyme" to listOf("hero.brand", "hero.brand.sub", "hero.description", "hero.button.project", "hero.button.template", "hero.card.call.to.action", "hero.card.description"),
                "site/templates/breadcrumb.thyme" to listOf("nav.home", "nav.blog"),
                "site/templates/toc-sidebar.thyme" to listOf("toc.heading"),
                "site/templates/progress-bar.thyme" to listOf("progress.bar.label"),
                "site/templates/auth-header.thyme" to listOf("auth.login.aria", "auth.login.label", "auth.logout.label"),
                "site/templates/augmented-articles.thyme" to listOf("augmented.articles.heading"),
                "site/templates/archive.thyme" to listOf("archive.heading", "archive.no.posts", "label.published.on"),
                "site/templates/post.thyme" to listOf("label.published.on"),
                "site/templates/tags.thyme" to listOf("tags.prefix"),
            )

            val failures = mutableListOf<String>()
            for ((path, keys) in expectedKeys) {
                val content = readTemplate(path)
                for (key in keys) {
                    val pattern = "#{$key}"
                    if (!content.contains(pattern)) {
                        failures.add("$path missing message key: $pattern")
                    }
                }
            }
            assertTrue(failures.isEmpty(), "Missing message keys:\n${failures.joinToString("\n")}")
        }

        @Test
        fun `site-basic templates use message keys for i18n`() {
            val expectedKeys = mapOf(
                "site-basic/templates/index.thyme" to listOf("nav.home", "index.welcome", "index.description.fallback", "index.content.fallback"),
                "site-basic/templates/page.thyme" to listOf("page.title.fallback"),
                "site-basic/templates/header.thyme" to listOf("brand.fallback"),
                "site-basic/templates/footer.thyme" to listOf("site.title.fallback"),
                "site-basic/templates/menu.thyme" to listOf("image.alt.logo", "brand.fallback", "nav.home", "nav.about", "nav.contact"),
            )

            val failures = mutableListOf<String>()
            for ((path, keys) in expectedKeys) {
                val content = readTemplate(path)
                for (key in keys) {
                    val pattern = "#{$key}"
                    if (!content.contains(pattern)) {
                        failures.add("$path missing message key: $pattern")
                    }
                }
            }
            assertTrue(failures.isEmpty(), "Missing message keys:\n${failures.joinToString("\n")}")
        }
    }

    @Nested
    inner class LangAttributeTest {

        @Test
        fun `site templates use th_lang instead of hardcoded lang`() {
            val templatesNeedingThLang = listOf(
                "site/templates/index.thyme",
                "site/templates/blog.thyme",
                "site/templates/contact.thyme",
                "site/templates/header.thyme",
                "site/templates/hero_section.thyme",
                "site/templates/page.thyme",
                "site/templates/post.thyme",
                "site/templates/archive.thyme",
                "site/templates/tags.thyme",
            )

            val failures = mutableListOf<String>()
            for (path in templatesNeedingThLang) {
                val content = readTemplate(path)
                if (!content.contains("th:lang=")) {
                    failures.add("$path missing th:lang attribute")
                }
            }
            assertTrue(failures.isEmpty(), "Missing th:lang:\n${failures.joinToString("\n")}")
        }
    }

    @Nested
    inner class I18nAttributePresenceTest {

        @Test
        fun `site templates have th_text or th_placeholder or th_attr with message keys`() {
            val templatesWithI18n = listOf(
                "site/templates/index.thyme",
                "site/templates/blog.thyme",
                "site/templates/contact.thyme",
                "site/templates/header.thyme",
                "site/templates/footer.thyme",
                "site/templates/menu.thyme",
                "site/templates/comments.thyme",
                "site/templates/newsletter-form.thyme",
                "site/templates/google-forms.thyme",
                "site/templates/pdf-viewer.thyme",
                "site/templates/hero_section.thyme",
                "site/templates/breadcrumb.thyme",
                "site/templates/toc-sidebar.thyme",
                "site/templates/progress-bar.thyme",
                "site/templates/auth-header.thyme",
                "site/templates/augmented-articles.thyme",
                "site/templates/archive.thyme",
                "site/templates/post.thyme",
                "site/templates/tags.thyme",
            )

            val failures = mutableListOf<String>()
            for (path in templatesWithI18n) {
                val content = readTemplate(path)
                if (!content.contains("#{")) {
                    failures.add("$path has no i18n message key pattern (#{...})")
                }
            }
            assertTrue(failures.isEmpty(), "Templates without i18n attributes:\n${failures.joinToString("\n")}")
        }

        @Test
        fun `site-basic templates have th_text with message keys`() {
            val templatesWithI18n = listOf(
                "site-basic/templates/index.thyme",
                "site-basic/templates/page.thyme",
                "site-basic/templates/header.thyme",
                "site-basic/templates/footer.thyme",
                "site-basic/templates/menu.thyme",
            )

            val failures = mutableListOf<String>()
            for (path in templatesWithI18n) {
                val content = readTemplate(path)
                if (!content.contains("#{")) {
                    failures.add("$path has no i18n message key")
                }
            }
            assertTrue(failures.isEmpty(), "Templates without i18n attributes:\n${failures.joinToString("\n")}")
        }
    }
}
