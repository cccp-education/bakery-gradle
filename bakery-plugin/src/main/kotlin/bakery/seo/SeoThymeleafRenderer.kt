package bakery.seo

/**
 * Renders a [SeoPageMeta] + [SeoConfig] into a Thymeleaf SEO fragment (EPIC BKY-SEO).
 *
 * Produces the HTML tags for canonical, hreflang, Open Graph, Twitter Card,
 * meta description and JSON-LD (`@graph` WebSite + Person when present).
 *
 * The output is a plain HTML fragment intended to be injected into `<head>`
 * (replacing a `<!-- SEO: bakery -->` marker block or added at the top).
 *
 * No I/O, no Gradle coupling — pure String generation.
 */
class SeoThymeleafRenderer {

    fun render(
        meta: SeoPageMeta,
        config: SeoConfig,
        defaultLanguage: String,
        languages: List<String>,
    ): String {
        val siteHost = (config.websiteUrl ?: "").trimEnd('/')
        val canonical = meta.canonicalUrl(siteHost)
        val alternates = meta.hreflangAlternates(siteHost, defaultLanguage, languages)
        val ogImage = meta.ogImage(siteHost, config.defaultOgImage, lang = defaultLanguage)

        val lines = mutableListOf<String>()

        // Canonical
        lines += """    <link rel="canonical" href="$canonical" />"""

        // Hreflang
        for (alt in alternates) {
            lines += """    <link rel="alternate" hreflang="${alt.language}" href="${alt.href}" />"""
        }

        // Open Graph
        lines += """    <meta property="og:title" content="${escape(meta.title)}" />"""
        lines += """    <meta property="og:description" content="${escape(meta.description)}" />"""
        lines += """    <meta property="og:image" content="$ogImage" />"""
        lines += """    <meta property="og:url" content="$canonical" />"""
        lines += """    <meta property="og:type" content="${if (meta.type == SeoPageType.POST) "article" else "website"}" />"""
        lines += """    <meta property="og:site_name" content="${escape(config.siteName)}" />"""
        lines += """    <meta property="og:locale" content="${config.inLanguage}" />"""

        // Twitter Card
        lines += """    <meta name="twitter:card" content="summary_large_image" />"""
        if (config.twitterHandle != null) {
            lines += """    <meta name="twitter:site" content="${config.twitterHandle}" />"""
        }
        lines += """    <meta name="twitter:title" content="${escape(meta.title)}" />"""
        lines += """    <meta name="twitter:description" content="${escape(meta.description)}" />"""
        lines += """    <meta name="twitter:image" content="$ogImage" />"""

        // Meta description
        lines += """    <meta name="description" content="${escape(meta.description)}" />"""

        // JSON-LD
        lines += renderJsonLd(config, siteHost)

        return lines.joinToString("\n")
    }

    private fun renderJsonLd(config: SeoConfig, siteHost: String): String {
        val nodes = mutableListOf<String>()

        nodes +=
            """      {"@type":"WebSite","name":"${escape(config.siteName)}","url":"$siteHost","inLanguage":"${config.inLanguage}"}"""

        if (config.person != null) {
            val p = config.person!!
            val sameAs =
                if (p.sameAs.isEmpty()) {
                    "[]"
                } else {
                    p.sameAs.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
                }
            nodes +=
                """      {"@type":"Person","name":"${escape(p.name)}","url":"${p.url ?: ""}","jobTitle":"${p.jobTitle ?: ""}","sameAs":$sameAs}"""
        }

        val graphBody = nodes.joinToString(",\n")
        return """    <script type="application/ld+json">
    {
      "@context": "https://schema.org",
      "@graph": [
$graphBody
      ]
    }
    </script>"""
    }

    private fun escape(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\n", " ")
}