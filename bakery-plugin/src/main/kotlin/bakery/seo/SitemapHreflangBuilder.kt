package bakery.seo

class SitemapHreflangBuilder {

    fun build(
        siteHost: String,
        defaultLanguage: String,
        languages: List<String>,
        pageUris: List<String>,
    ): String {
        val base = siteHost.trimEnd('/')
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"")
        sb.append(" xmlns:xhtml=\"http://www.w3.org/1999/xhtml\">\n")

        for (uri in pageUris) {
            sb.append("  <url>\n")
            val uriPart = if (uri.isBlank()) "" else "/$uri"
            val loc = "$base$uriPart"
            if (uri.isBlank()) {
                sb.append("    <loc>$base/</loc>\n")
            } else {
                sb.append("    <loc>$loc</loc>\n")
            }
            for (lang in languages) {
                val href =
                    if (lang == defaultLanguage) {
                        if (uri.isBlank()) "$base/" else loc
                    } else if (uri.isBlank()) {
                        "$base/$lang/"
                    } else {
                        "$base/$lang$uriPart"
                    }
                sb.append("    <xhtml:link rel=\"alternate\" hreflang=\"$lang\" href=\"$href\" />\n")
            }
            val defaultHref = if (uri.isBlank()) "$base/" else loc
            sb.append("    <xhtml:link rel=\"alternate\" hreflang=\"x-default\" href=\"$defaultHref\" />\n")
            sb.append("  </url>\n")
        }

        sb.append("</urlset>\n")
        return sb.toString()
    }
}