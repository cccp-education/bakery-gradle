package bakery.i18n

/**
 * CHE-I18N-22 US-7d — keeps the `<html lang>` attribute honest in a translated
 * template.
 *
 * `TemplateTextTranslator` only rewrites visible text by design, so a variant
 * template kept the source `<html lang="fr">`: a German or Arabic page
 * announced itself as French (WCAG 3.1.1 "Language of Page"). This object
 * rewrites exactly that attribute and nothing else — `hreflang` alternates and
 * the language-switcher options are preserved verbatim.
 *
 * Pure domain: no I/O, no LLM, no Gradle. Applying the source language is a
 * strict no-op (Ink Economy Law).
 */
object TemplateLanguageAttribute {

    private val HTML_LANG = Regex("""(<html\b[^>]*?\blang\s*=\s*")[^"]*(")""")

    fun ensureLanguage(
        template: String,
        targetLanguage: String,
    ): String {
        if (targetLanguage == SOURCE_LANGUAGE) return template
        return HTML_LANG.replace(template) { match ->
            "${match.groupValues[1]}$targetLanguage${match.groupValues[2]}"
        }
    }

    const val SOURCE_LANGUAGE = "fr"
}
