package bakery.seo

/**
 * BKY-OUT-1 (i18n_review S-263) — the head of a **variant** page must declare
 * its own locale.
 *
 * Measured on the published site (review S-263, cheroliv.com `/en/`, `/ar/`,
 * `/ja/`): the `canonical` and `og:url` point to the French page (no `{lang}/`
 * prefix), `og:locale` is `fr_FR` everywhere and the `og:image` expression reads
 * `og-images/fr/…`. Every translated page therefore declares itself the French
 * original, which is exactly what search engines must not be told.
 *
 * The cause is structural: a variant template is a *copy* of the reference head,
 * translated by `translateTemplates`, and nothing ever localises its SEO
 * literals. Fixing it by re-translation would cost an LLM round-trip and a
 * frozen-block eviction; the fields are deterministic, so the repair is too.
 *
 * Scope: four fields only — `canonical`, `og:url`, `og:locale`, and the
 * `og-images/{lang}/` directory. `<title>` and the meta description already
 * follow the translated `content.title`/`summary` (they need no rewrite), and
 * `dir="rtl"` belongs to BKY-OUT-2. `<html lang>` is left untouched: the
 * translation already sets it, and rewriting it here would hide a translation
 * defect instead of reporting it.
 *
 * Pure value object: no I/O, no Gradle, no Thymeleaf engine. Idempotent by
 * construction (a prefixed path is recognised and left alone).
 */
object LocalizedSeoHead {

    /**
     * The `og:locale` catalog: the Facebook `ll_CC` form expected by crawlers.
     * An unknown language falls back to its own code — never to the reference
     * locale, which would reproduce the very defect this object removes.
     */
    private val LOCALES: Map<String, String> =
        mapOf(
            "fr" to "fr_FR",
            "en" to "en_US",
            "de" to "de_DE",
            "es" to "es_ES",
            "it" to "it_IT",
            "nl" to "nl_NL",
            "pt" to "pt_PT",
            "ru" to "ru_RU",
            "ar" to "ar_AR",
            "hi" to "hi_IN",
            "bn" to "bn_IN",
            "ur" to "ur_PK",
            "zh" to "zh_CN",
            "ja" to "ja_JP",
            "ko" to "ko_KR",
            "tr" to "tr_TR",
            "vi" to "vi_VN",
            "th" to "th_TH",
            "id" to "id_ID",
            "el" to "el_GR",
            "sr" to "sr_RS",
            "fa" to "fa_IR",
        )

    /** The `og:locale` value of [language] (`ll_CC`, or the code itself when unknown). */
    fun localeOf(language: String): String {
        require(language.isNotBlank()) { "language must not be blank" }
        return LOCALES[language] ?: language
    }

    /**
     * Rewrite the locale-blind SEO fields of [header] for [language].
     *
     * @param header the variant Thymeleaf head (any layout, text only)
     * @param language the variant language (`en`, `ar`, …)
     * @param defaultLanguage the reference language (its canonical stays prefixless)
     * @param ogImageLanguage the directory under `og-images/` that actually holds
     *   the cards for this variant — the caller probes the disk and passes a
     *   fallback, so a language without its own cards never emits a 404
     */
    fun rewrite(
        header: String,
        language: String,
        defaultLanguage: String,
        ogImageLanguage: String,
    ): String {
        require(defaultLanguage.isNotBlank()) { "defaultLanguage must not be blank" }
        require(ogImageLanguage.isNotBlank()) { "ogImageLanguage must not be blank" }
        var out = OG_LOCALE.replace(header) { match -> match.groupValues[1] + localeOf(language) + match.groupValues[2] }
        out = prefixPagePath(out, CANONICAL_MARKER, language, defaultLanguage)
        out = prefixPagePath(out, OG_URL_MARKER, language, defaultLanguage)
        out = OG_IMAGE_DIR.replace(out) { "'og-images/$ogImageLanguage/" }
        return out
    }

    /**
     * Prefix the page path of the [marker] tag with `'{lang}/'` for a variant
     * language. The reference language keeps a prefixless (auto-referent)
     * canonical. A line already carrying this language's prefix is left alone.
     */
    private fun prefixPagePath(
        text: String,
        marker: String,
        language: String,
        defaultLanguage: String,
    ): String {
        if (language == defaultLanguage) return text
        val line = Regex("""(?m)^(.*${Regex.escape(marker)}.*)$""")
        return line.replace(text) { match ->
            val original = match.groupValues[1]
            PREFIX.replace(original) { expression ->
                val host = expression.groupValues[1]
                val tail = expression.groupValues[2]
                if (tail.startsWith("'$language/")) {
                    expression.value
                } else {
                    "$host'$language/' + $tail"
                }
            }
        }
    }

    private const val CANONICAL_MARKER = "rel=\"canonical\""
    private const val OG_URL_MARKER = "property=\"og:url\""

    private val OG_LOCALE = Regex("""(<meta\s+property="og:locale"[^>]*content=")[^"]*(")""")

    private val OG_IMAGE_DIR = Regex("""'og-images/[^/'"]+/""")

    /** The `host + page` expression inside an SEO tag; group 2 excludes the closing quote. */
    private val PREFIX = Regex("""(\$\{config\.site_host\}\s*\+\s*)([^"]+)""")
}
