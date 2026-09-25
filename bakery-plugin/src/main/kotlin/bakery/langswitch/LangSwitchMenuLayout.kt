package bakery.langswitch

import java.io.File

/**
 * BKY-LANG-NAV-8 — where a language tree stores its `menu.thyme`.
 *
 * Two layouts coexist in the ecosystem:
 *   - the **materialised i18n layout** `i18n/{lang}/templates/` produced by
 *     `materializeTemplates` from a frozen bundle (the golden-master layout the
 *     cccp.education / magic-stick rollout owns, zero LLM);
 *   - the **direct variant layout** `{lang}/templates/` used by the tree sites
 *     (cheroliv.com, talaria.school) and the default language at the reference
 *     root `templates/`.
 *
 * `injectLangSwitch` historically only knew the direct layout, so it silently
 * skipped a materialised variant (constat S-237). A single ordered rule removes
 * that blind spot without breaking the historical sites: the i18n layout wins
 * when present, then the direct variant, then the reference root when the
 * request targets the default language.
 *
 * Domain-pure: relative candidate order and a filesystem probe only. No Gradle.
 */
object LangSwitchMenuLayout {
    const val MENU_THYME = "menu.thyme"

    /**
     * Ordered relative paths to probe for `{lang}`'s menu, most specific first.
     * Pure ordering, no disk access: the caller probes the actual file.
     */
    fun relativeCandidates(
        lang: String,
        defaultLang: String,
    ): List<String> {
        require(lang.isNotBlank()) { "lang must not be blank" }
        require(defaultLang.isNotBlank()) { "defaultLang must not be blank" }

        if (lang == defaultLang) return listOf("templates/$MENU_THYME")
        return listOf("i18n/$lang/templates/$MENU_THYME", "$lang/templates/$MENU_THYME")
    }

    /**
     * Resolves the existing menu file for `{lang}`, or `null` when the language
     * has no menu in any known layout. The caller decides whether a missing menu
     * is a skip (`injectLangSwitch`) or a violation.
     */
    fun resolve(
        siteRoot: File,
        lang: String,
        defaultLang: String,
    ): File? =
        relativeCandidates(lang, defaultLang)
            .asSequence()
            .map { siteRoot.resolve(it) }
            .firstOrNull { it.isFile }
}
