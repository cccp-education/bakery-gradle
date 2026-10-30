package bakery.i18n

/**
 * BKY-OUT-2 (i18n_review S-263) — the document direction of a variant page.
 *
 * The review measured `lang="ar"` with **no** `dir="rtl"` and no logical CSS:
 * three languages (Arabic, Persian, Urdu) were published in a left-to-right
 * layout with right-to-left glyphs. The existing `RtlDirectionInjector` only
 * writes AsciiDoc front matter for *slides* — it never touches a site page.
 *
 * The direction is a property of the language, not of the page: it is applied on
 * the `<html>` opening tag (the single switch the browser needs to mirror the
 * layout) and it is idempotent — an existing `dir` is replaced, never duplicated.
 *
 * Pure value object: no I/O, no Gradle.
 */
object DocumentDirection {

    /** The right-to-left languages of the workspace catalog. */
    val RTL_LANGUAGES: Set<String> = setOf("ar", "fa", "ur")

    fun isRtl(language: String): Boolean {
        require(language.isNotBlank()) { "language must not be blank" }
        return language in RTL_LANGUAGES
    }

    /**
     * The `<html>` tag of [header] carrying the direction of [language]: `dir="rtl"`
     * for Arabic/Persian/Urdu, and **no `dir`** for a left-to-right language (a
     * stale `rtl` inherited from a translated copy is dropped).
     *
     * A head without an `<html>` tag is returned unchanged (the template is not a
     * document root — nothing to declare).
     */
    fun apply(
        header: String,
        language: String,
    ): String {
        require(language.isNotBlank()) { "language must not be blank" }
        val match = HTML_TAG.find(header) ?: return header
        val tag = match.value
        val withoutDirection = DIRECTION.replace(tag, "")
        val normalised = withoutDirection.replace(Regex("""\s{2,}"""), " ")
        val directed =
            if (!isRtl(language)) {
                normalised
            } else if (normalised.endsWith("/>")) {
                normalised.dropLast(2) + " dir=\"rtl\"/>"
            } else {
                normalised.dropLast(1) + " dir=\"rtl\">"
            }
        return header.replace(tag, directed)
    }

    private val HTML_TAG = Regex("""<html\b[^>]*>""", RegexOption.IGNORE_CASE)

    private val DIRECTION = Regex("""\s+dir\s*=\s*("[^"]*"|'[^']*')""", RegexOption.IGNORE_CASE)
}
