package bakery.seo

/**
 * BKY-OUT-1 (i18n_review S-263) — which `og-images/{lang}/` directory holds the
 * cards of a variant head.
 *
 * Measured on the published site: only the reference (`fr`) and `en` hold
 * complete card sets, the other languages hold none. A variant therefore reads
 * its **own** directory when it exists and the **reference** otherwise — a
 * partial set is worse than a reference card, because the card of an article
 * without an image in it would 404.
 *
 * Pure value object: the caller probes the disk once and passes the directory
 * names, the rule itself has no I/O.
 */
object OgImageLanguage {
    /**
     * The directory to read for [language]: [language] when it is one of the
     * [available] directories, [defaultLanguage] otherwise. The reference is
     * returned even when it is missing from [available]: the reference is the
     * only set the pipeline guarantees, so a variant never points to a directory
     * that does not exist.
     */
    fun resolve(
        language: String,
        defaultLanguage: String,
        available: Set<String>,
    ): String {
        require(language.isNotBlank()) { "language must not be blank" }
        require(defaultLanguage.isNotBlank()) { "defaultLanguage must not be blank" }
        return if (language in available) language else defaultLanguage
    }
}
