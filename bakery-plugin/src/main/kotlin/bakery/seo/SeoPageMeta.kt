package bakery.seo

/**
 * Type of page for SEO purposes (EPIC BKY-SEO).
 *
 * Determines OG image derivation strategy: POST pages derive their image
 * from the page URI (`.html` → `.png` under `og-images/{lang}/`), while
 * PAGE pages fall back to [SeoConfig.defaultOgImage].
 */
enum class SeoPageType {
    POST,
    PAGE,
}

/**
 * SEO metadata resolved for a single page (EPIC BKY-SEO).
 *
 * Pure DDD domain type — no I/O, no Gradle coupling. Computes canonical,
 * hreflang alternates and OG image URLs from a page URI + site host.
 *
 * @param uri Page URI relative to site root (e.g. `blog/2026/0081_post.html`).
 * @param title Page title (used in OG/Twitter tags downstream).
 * @param description Page meta description.
 * @param type POST or PAGE — drives OG image derivation.
 * @param canonOverride Optional canonical override (e.g. archive URL). When
 *   non-null, [canonicalUrl] returns it verbatim instead of deriving from uri.
 */
data class SeoPageMeta(
    val uri: String,
    val title: String,
    val description: String,
    val type: SeoPageType = SeoPageType.PAGE,
    val canonOverride: String? = null,
) {
    /**
     * Resolve the canonical URL for this page.
     *
     * If [canonOverride] is set, it takes priority. Otherwise the canonical
     * is `siteHost + "/" + uri` (with a trailing slash for empty uri = home).
     */
    fun canonicalUrl(siteHost: String): String {
        if (canonOverride != null) return canonOverride
        val base = siteHost.trimEnd('/')
        return if (uri.isBlank()) "$base/" else "$base/$uri"
    }

    /**
     * Resolve hreflang alternates for this page.
     *
     * For each language, the href is `siteHost + "/" + uri` for the default
     * language and `siteHost + "/" + lang + "/" + uri` for non-default
     * languages. `x-default` mirrors the default language URL.
     *
     * @param defaultLanguage The default language code (e.g. "fr").
     * @param languages Full list of supported languages including the default.
     */
    fun hreflangAlternates(
        siteHost: String,
        defaultLanguage: String,
        languages: List<String>,
    ): List<HreflangAlternate> {
        val base = siteHost.trimEnd('/')
        val uriPart = if (uri.isBlank()) "" else "/$uri"
        return languages.map { lang ->
            val href =
                if (lang == defaultLanguage) {
                    "$base$uriPart"
                } else {
                    "$base/$lang$uriPart"
                }
            HreflangAlternate(language = lang, href = href)
        } + HreflangAlternate(language = "x-default", href = "$base$uriPart")
    }

    /**
     * Resolve the Open Graph image URL for this page.
     *
     * POST type: derives from uri by replacing `.html` with `.png` and
     * prepending `og-images/{lang}/` (where lang defaults to `fr` — the
     * caller passes the appropriate language when available).
     *
     * Non-POST or uri without `.html`: falls back to [defaultOgImage].
     */
    fun ogImage(
        siteHost: String,
        defaultOgImage: String,
        lang: String = "fr",
    ): String {
        if (type != SeoPageType.POST || !uri.endsWith(".html")) {
            return siteHost.trimEnd('/') + "/" + defaultOgImage
        }
        val png = uri.removeSuffix(".html") + ".png"
        return siteHost.trimEnd('/') + "/og-images/" + lang + "/" + png
    }
}

/**
 * A single hreflang alternate link.
 *
 * @property language Language code (e.g. "fr", "en", "x-default").
 * @property href Absolute URL of the alternate page.
 */
data class HreflangAlternate(
    val language: String,
    val href: String,
)