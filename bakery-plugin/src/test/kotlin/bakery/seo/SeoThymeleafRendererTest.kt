package bakery.seo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-SEO-3 — Unit tests for [SeoThymeleafRenderer].
 *
 * Transforms a [SeoPageMeta] + [SeoConfig] into a Thymeleaf SEO fragment
 * (`<link rel="canonical">`, hreflang, OG, Twitter, JSON-LD script).
 *
 * Methodology: DDD/TDD baby steps.
 */
class SeoThymeleafRendererTest {

    private val config =
        SeoConfig(
            siteName = "Example",
            brand = "Example",
            defaultOgImage = "example-default.png",
            twitterHandle = "@example",
            websiteUrl = "https://example.com",
            inLanguage = "fr",
            person = Person(name = "Jane Doe", url = "https://example.com/about", jobTitle = "Engineer"),
        )
    private val renderer = SeoThymeleafRenderer()

    @Nested
    @DisplayName("canonical")
    inner class Canonical {
        @Test
        @DisplayName("renders canonical link for a post")
        fun `renders canonical link for post`() {
            val meta =
                SeoPageMeta(
                    uri = "blog/2026/0081_demo_post.html",
                    title = "Demo",
                    description = "A demo post",
                    type = SeoPageType.POST,
                )

            val html = renderer.render(meta, config, defaultLanguage = "fr", languages = listOf("fr"))

            assertThat(html).contains("<link rel=\"canonical\"")
            assertThat(html).contains("blog/2026/0081_demo_post.html")
        }
    }

    @Nested
    @DisplayName("hreflang")
    inner class Hreflang {
        @Test
        @DisplayName("renders hreflang alternates for fr + en + x-default")
        fun `renders hreflang for fr en x-default`() {
            val meta = SeoPageMeta(uri = "index.html", title = "Home", description = "Home")

            val html = renderer.render(meta, config, defaultLanguage = "fr", languages = listOf("fr", "en"))

            assertThat(html).contains("hreflang=\"fr\"")
            assertThat(html).contains("hreflang=\"en\"")
            assertThat(html).contains("hreflang=\"x-default\"")
        }
    }

    @Nested
    @DisplayName("og tags")
    inner class OgTags {
        @Test
        @DisplayName("renders og:image for a post (derived from uri)")
        fun `renders og image for post`() {
            val meta =
                SeoPageMeta(
                    uri = "blog/2026/0081_demo_post.html",
                    title = "Demo",
                    description = "Demo post",
                    type = SeoPageType.POST,
                )

            val html = renderer.render(meta, config, defaultLanguage = "fr", languages = listOf("fr"))

            assertThat(html).contains("og:image")
            assertThat(html).contains("0081_demo_post.png")
        }

        @Test
        @DisplayName("renders og:title and og:description")
        fun `renders og title and description`() {
            val meta =
                SeoPageMeta(
                    uri = "index.html",
                    title = "Home Page",
                    description = "Welcome to Example",
                    type = SeoPageType.PAGE,
                )

            val html = renderer.render(meta, config, defaultLanguage = "fr", languages = listOf("fr"))

            assertThat(html).contains("og:title")
            assertThat(html).contains("Home Page")
            assertThat(html).contains("og:description")
            assertThat(html).contains("Welcome to Example")
        }
    }

    @Nested
    @DisplayName("twitter card")
    inner class TwitterCard {
        @Test
        @DisplayName("renders twitter:card and twitter:handle when handle is set")
        fun `renders twitter card with handle`() {
            val meta = SeoPageMeta(uri = "index.html", title = "Home", description = "Home")

            val html = renderer.render(meta, config, defaultLanguage = "fr", languages = listOf("fr"))

            assertThat(html).contains("twitter:card")
            assertThat(html).contains("@example")
        }

        @Test
        @DisplayName("omits twitter:handle when handle is null")
        fun `omits twitter handle when null`() {
            val noTwitterConfig = config.copy(twitterHandle = null)
            val meta = SeoPageMeta(uri = "index.html", title = "Home", description = "Home")

            val html = renderer.render(meta, noTwitterConfig, defaultLanguage = "fr", languages = listOf("fr"))

            assertThat(html).contains("twitter:card")
            assertThat(html).doesNotContain("twitter:site")
        }
    }

    @Nested
    @DisplayName("JSON-LD")
    inner class JsonLd {
        @Test
        @DisplayName("renders JSON-LD @graph with WebSite and Person")
        fun `renders jsonld graph with website and person`() {
            val meta = SeoPageMeta(uri = "index.html", title = "Home", description = "Home")

            val html = renderer.render(meta, config, defaultLanguage = "fr", languages = listOf("fr"))

            assertThat(html).contains("application/ld+json")
            assertThat(html).contains("\"@graph\"")
            assertThat(html).contains("WebSite")
            assertThat(html).contains("Jane Doe")
        }

        @Test
        @DisplayName("renders only WebSite when person is null")
        fun `renders only website when person null`() {
            val noPersonConfig = config.copy(person = null)
            val meta = SeoPageMeta(uri = "index.html", title = "Home", description = "Home")

            val html = renderer.render(meta, noPersonConfig, defaultLanguage = "fr", languages = listOf("fr"))

            assertThat(html).contains("WebSite")
            assertThat(html).doesNotContain("Person")
        }
    }

    @Nested
    @DisplayName("meta description")
    inner class MetaDescription {
        @Test
        @DisplayName("renders meta description tag")
        fun `renders meta description`() {
            val meta = SeoPageMeta(uri = "index.html", title = "Home", description = "Welcome")

            val html = renderer.render(meta, config, defaultLanguage = "fr", languages = listOf("fr"))

            assertThat(html).contains("<meta name=\"description\"")
            assertThat(html).contains("Welcome")
        }
    }
}