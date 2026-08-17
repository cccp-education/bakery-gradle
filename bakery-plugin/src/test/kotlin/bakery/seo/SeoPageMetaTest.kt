package bakery.seo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-SEO-2 — Unit tests for [SeoPageMeta].
 *
 * Pure DDD domain type computing canonical, hreflang and OG image URLs
 * for a single page. No I/O, no Gradle coupling.
 *
 * Methodology: DDD/TDD baby steps.
 */
class SeoPageMetaTest {

    private val siteHost = "https://example.com"

    @Nested
    @DisplayName("canonicalUrl")
    inner class CanonicalUrl {
        @Test
        @DisplayName("post uri resolves to site host + uri")
        fun `post uri resolves to canonical`() {
            val meta = SeoPageMeta(uri = "blog/2026/0081_seo_demo_post.html", title = "Demo", description = "Demo post")

            assertThat(meta.canonicalUrl(siteHost))
                .isEqualTo("https://example.com/blog/2026/0081_seo_demo_post.html")
        }

        @Test
        @DisplayName("home page (empty uri) resolves to site host root")
        fun `home page resolves to root`() {
            val meta = SeoPageMeta(uri = "", title = "Home", description = "Home page")

            assertThat(meta.canonicalUrl(siteHost))
                .isEqualTo("https://example.com/")
        }

        @Test
        @DisplayName("canonOverride takes priority over derived uri")
        fun `canonOverride takes priority`() {
            val meta =
                SeoPageMeta(
                    uri = "blog/2026/0081_seo_demo_post.html",
                    title = "Demo",
                    description = "Demo post",
                    canonOverride = "https://archive.example.com/blog/2026/0081",
                )

            assertThat(meta.canonicalUrl(siteHost))
                .isEqualTo("https://archive.example.com/blog/2026/0081")
        }
    }

    @Nested
    @DisplayName("hreflangAlternates")
    inner class HreflangAlternates {
        @Test
        @DisplayName("FR default + EN produces fr, en, x-default")
        fun `fr default plus en produces three alternates`() {
            val meta = SeoPageMeta(uri = "index.html", title = "Home", description = "Home")

            val alternates = meta.hreflangAlternates(siteHost, defaultLanguage = "fr", languages = listOf("fr", "en"))

            assertThat(alternates).hasSize(3)
            assertThat(alternates.map { it.language }).containsExactly("fr", "en", "x-default")
            assertThat(alternates[0].href).isEqualTo("https://example.com/index.html")
            assertThat(alternates[1].href).isEqualTo("https://example.com/en/index.html")
            assertThat(alternates[2].href).isEqualTo("https://example.com/index.html")
        }

        @Test
        @DisplayName("10 languages produce 10 alternates + x-default")
        fun `ten languages produce eleven alternates`() {
            val meta = SeoPageMeta(uri = "index.html", title = "Home", description = "Home")
            val langs = listOf("fr", "en", "zh", "hi", "es", "ar", "bn", "pt", "ru", "ur")

            val alternates = meta.hreflangAlternates(siteHost, defaultLanguage = "fr", languages = langs)

            assertThat(alternates).hasSize(11)
            assertThat(alternates.last().language).isEqualTo("x-default")
        }
    }

    @Nested
    @DisplayName("ogImage")
    inner class OgImage {
        @Test
        @DisplayName("post type derives image from uri (html -> png)")
        fun `post derives og image from uri`() {
            val meta =
                SeoPageMeta(
                    uri = "blog/2026/0081_seo_demo_post.html",
                    title = "Demo",
                    description = "Demo post",
                    type = SeoPageType.POST,
                )

            assertThat(meta.ogImage(siteHost, defaultOgImage = "cheroliv-default.png"))
                .isEqualTo("https://example.com/og-images/fr/blog/2026/0081_seo_demo_post.png")
        }

        @Test
        @DisplayName("non-post type falls back to defaultOgImage")
        fun `non post falls back to default og image`() {
            val meta = SeoPageMeta(uri = "index.html", title = "Home", description = "Home", type = SeoPageType.PAGE)

            assertThat(meta.ogImage(siteHost, defaultOgImage = "cheroliv-default.png"))
                .isEqualTo("https://example.com/cheroliv-default.png")
        }

        @Test
        @DisplayName("post type without .html extension falls back to default")
        fun `post without html falls back to default`() {
            val meta =
                SeoPageMeta(
                    uri = "blog/2026/some-post",
                    title = "Demo",
                    description = "Demo post",
                    type = SeoPageType.POST,
                )

            assertThat(meta.ogImage(siteHost, defaultOgImage = "default.png"))
                .isEqualTo("https://example.com/default.png")
        }
    }

    @Nested
    @DisplayName("defaults")
    inner class Defaults {
        @Test
        @DisplayName("default type is PAGE")
        fun `default type is page`() {
            val meta = SeoPageMeta(uri = "", title = "", description = "")

            assertThat(meta.type).isEqualTo(SeoPageType.PAGE)
            assertThat(meta.canonOverride).isNull()
        }
    }
}