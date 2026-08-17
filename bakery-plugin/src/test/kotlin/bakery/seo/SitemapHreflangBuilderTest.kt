package bakery.seo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SitemapHreflangBuilderTest {

    private val builder = SitemapHreflangBuilder()

    @Nested
    @DisplayName("root url")
    inner class RootUrl {
        @Test
        fun `builds sitemap with root url for default language`() {
            val xml = builder.build(
                siteHost = "https://example.com",
                defaultLanguage = "fr",
                languages = listOf("fr"),
                pageUris = listOf(""),
            )

            assertThat(xml).contains("<loc>https://example.com/</loc>")
        }

        @Test
        fun `sitemap starts with xml declaration and urlset namespace`() {
            val xml = builder.build(
                siteHost = "https://example.com",
                defaultLanguage = "fr",
                languages = listOf("fr"),
                pageUris = listOf(""),
            )

            assertThat(xml).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            assertThat(xml).contains("<urlset")
            assertThat(xml).contains("xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"")
            assertThat(xml).contains("xmlns:xhtml=\"http://www.w3.org/1999/xhtml\"")
        }
    }

    @Nested
    @DisplayName("hreflang alternates")
    inner class HreflangAlternates {
        @Test
        fun `includes xhtml link alternates for fr and en`() {
            val xml = builder.build(
                siteHost = "https://example.com",
                defaultLanguage = "fr",
                languages = listOf("fr", "en"),
                pageUris = listOf(""),
            )

            assertThat(xml).contains("<xhtml:link rel=\"alternate\" hreflang=\"fr\" href=\"https://example.com/\"")
            assertThat(xml).contains("<xhtml:link rel=\"alternate\" hreflang=\"en\" href=\"https://example.com/en/\"")
        }

        @Test
        fun `includes x-default alternate pointing to default language url`() {
            val xml = builder.build(
                siteHost = "https://example.com",
                defaultLanguage = "fr",
                languages = listOf("fr", "en"),
                pageUris = listOf(""),
            )

            assertThat(xml).contains("hreflang=\"x-default\"")
            assertThat(xml).contains("href=\"https://example.com/\"")
        }

        @Test
        fun `page uri produces alternates with uri in path`() {
            val xml = builder.build(
                siteHost = "https://example.com",
                defaultLanguage = "fr",
                languages = listOf("fr", "en"),
                pageUris = listOf("blog/2026/0081_demo.html"),
            )

            assertThat(xml).contains("<loc>https://example.com/blog/2026/0081_demo.html</loc>")
            assertThat(xml).contains("href=\"https://example.com/blog/2026/0081_demo.html\"")
            assertThat(xml).contains("href=\"https://example.com/en/blog/2026/0081_demo.html\"")
        }
    }

    @Nested
    @DisplayName("multiple pages")
    inner class MultiplePages {
        @Test
        fun `generates url entry per page uri`() {
            val xml = builder.build(
                siteHost = "https://example.com",
                defaultLanguage = "fr",
                languages = listOf("fr", "en"),
                pageUris = listOf("", "blog/2026/0081_demo.html"),
            )

            val urlCount = xml.split("<url>").size - 1
            assertThat(urlCount).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("edge cases")
    inner class EdgeCases {
        @Test
        fun `empty page uris produces empty urlset`() {
            val xml = builder.build(
                siteHost = "https://example.com",
                defaultLanguage = "fr",
                languages = listOf("fr"),
                pageUris = emptyList(),
            )

            assertThat(xml).contains("<urlset")
            assertThat(xml).doesNotContain("<url>")
        }

        @Test
        fun `site host trailing slash is trimmed`() {
            val xml = builder.build(
                siteHost = "https://example.com/",
                defaultLanguage = "fr",
                languages = listOf("fr"),
                pageUris = listOf(""),
            )

            assertThat(xml).contains("<loc>https://example.com/</loc>")
            assertThat(xml).doesNotContain("https://example.com//")
        }

        @Test
        fun `closes urlset tag`() {
            val xml = builder.build(
                siteHost = "https://example.com",
                defaultLanguage = "fr",
                languages = listOf("fr"),
                pageUris = listOf(""),
            )

            assertThat(xml).contains("</urlset>")
        }
    }
}