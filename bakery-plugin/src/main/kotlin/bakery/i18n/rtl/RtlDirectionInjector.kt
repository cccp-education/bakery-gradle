package bakery.i18n.rtl

import document.translation.PivotFrontmatter
import contracts.i18n.LanguageCatalog

/**
 * EPIC BKY-I18N-DEPLOY — US DEPLOY-3.
 *
 * Injects JBake language directive into a translated article frontmatter.
 *
 * - `:jbake-lang: {lang}` is always written (the target language code), overwriting
 *   any previous value so re-injecting for the same language is idempotent.
 * - `:lang: rtl` is written only when the target language is RTL (Arabic, Urdu),
 *   otherwise any existing `:lang: rtl` is removed so a source RTL article
 *   translated to a LTR language drops RTL layout.
 *
 * Direction is delegated to `LanguageCatalog.findByCode(lang)?.rtl` (i18n-contracts,
 * MEM-3). Pure domain: no I/O, no file writing. Inspired by the
 * `DeckTranslator.ensureRtlDirection` pattern (MIAMI EPIC SLD-5).
 */
data class RtlDirective(val lang: String, val rtl: Boolean)

class RtlDirectionInjector {

    fun inject(frontmatter: PivotFrontmatter, lang: String): PivotFrontmatter {
        val supported = LanguageCatalog.findByCode(lang) ?: return frontmatter
        val directive = RtlDirective(lang, supported.rtl)
        val updatedJbake = frontmatter.jbakeAttributes.toMutableMap()
        updatedJbake["lang"] = directive.lang
        val updatedAsciidoc = frontmatter.asciidocAttributes.toMutableMap()
        if (directive.rtl) {
            updatedAsciidoc["lang"] = "rtl"
        } else {
            updatedAsciidoc.remove("lang")
        }
        return frontmatter.copy(
            jbakeAttributes = updatedJbake,
            asciidocAttributes = updatedAsciidoc
        )
    }
}