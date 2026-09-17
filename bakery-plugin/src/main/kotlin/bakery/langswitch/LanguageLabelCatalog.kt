package bakery.langswitch

import contracts.i18n.LanguageCatalog

/**
 * CHE-I18N-22 US-2 — the display labels of the language switcher.
 *
 * Derived from the N0 contract [LanguageCatalog] so the switcher can never
 * lag behind the supported languages again (the S-217 anti split-brain rule:
 * `BakeryConstants.SUPPORTED_LANGS` already derives from the same source).
 *
 * The label is the language's own name ([SupportedLanguage.nativeName]) — a
 * speaker must recognise their language written in their script, never as a
 * raw code.
 */
object LanguageLabelCatalog {
    fun labels(): Map<String, String> = LanguageCatalog.ALL.associate { it.code to it.nativeName }
}
