package bakery.langswitch

/**
 * BKY-LANG-NAV-4 — the ex-nihilo model guard (decision D7).
 *
 * The injected shared menu (NAV-2) is page-aware by construction, but a site
 * scaffolded from the model (`site-basic/`, `site/`) receives the templates
 * *verbatim*: if the model selector is page-blind, every brand-new site is born
 * buggy (constat S-228). This object checks the model menu text itself.
 *
 * Presence of a `lang-option` is not enough — the option must be *page-aware*
 * (read `content.uri`) and carry `data-lang` (the JS contract, D4). A page-blind
 * absolute href (`'/' + lang.code + '/'`) is exactly the bug we are closing.
 *
 * Domain-pure: template text in, violations out. No I/O, no Gradle.
 */
object LangSwitchModelGuard {
    const val MISSING_SELECTOR = "missing-selector"
    const val PAGE_BLIND_HREF = "page-blind-href"
    const val MISSING_DATA_LANG = "missing-data-lang"
    const val MISSING_PAGE_AWARENESS = "missing-page-awareness"

    data class Violation(
        val kind: String,
        val detail: String,
    )

    data class Report(
        val violations: List<Violation>,
    ) {
        val isSuccess: Boolean get() = violations.isEmpty()
    }

    fun verify(menuThyme: String): Report {
        if (!menuThyme.contains("language-switcher-container")) {
            return Report(listOf(Violation(MISSING_SELECTOR, "language-switcher-container")))
        }

        val violations = mutableListOf<Violation>()
        val selectorBlock =
            menuThyme.substringAfter("language-switcher-container")

        if (!selectorBlock.contains("lang-option")) {
            violations += Violation(MISSING_SELECTOR, "lang-option")
        }

        if (!selectorBlock.contains("data-lang")) {
            violations += Violation(MISSING_DATA_LANG, "data-lang")
        }

        if (PAGE_BLIND_HREF_PATTERN.containsMatchIn(selectorBlock)) {
            violations += Violation(PAGE_BLIND_HREF, "'/' + lang.code + '/'")
        }

        if (!selectorBlock.contains("content.uri")) {
            violations += Violation(MISSING_PAGE_AWARENESS, "content.uri")
        }

        return Report(violations)
    }

    /** The page-blind absolute language-tree link the S-228 constat identified. */
    private val PAGE_BLIND_HREF_PATTERN = Regex("""'/' *\+ *\$\{lang""")
}
