package bakery.i18n

import java.io.File

/**
 * BKY-OUT-1 — the single layout rule for every per-language Thymeleaf file.
 *
 * A site keeps its reference template in `templates/` and each translated copy
 * in `i18n/{lang}/templates/` (the layout produced by `materializeTemplates`
 * and by the site-side pipeline). A legacy `{lang}/templates/` tree is still
 * probed last so an older consumer keeps working.
 *
 * This exists because two producers read the same site two different ways:
 * `injectLangSwitch` already knew `i18n/{lang}/` while `injectSeo` only probed
 * `{lang}/templates/` and therefore never found a variant header on a site
 * using the current layout — the "measured twice, two answers" split-brain.
 *
 * Pure ordering: [relativeCandidates] never touches the disk, [resolve] does the
 * single probe the caller needs.
 */
object I18nTemplateLayout {

    /** Ordered relative paths to probe for `{fileName}` of [lang], most specific first. */
    fun relativeCandidates(
        fileName: String,
        lang: String,
        defaultLang: String,
    ): List<String> {
        require(fileName.isNotBlank()) { "fileName must not be blank" }
        require(lang.isNotBlank()) { "lang must not be blank" }
        require(defaultLang.isNotBlank()) { "defaultLang must not be blank" }
        if (lang == defaultLang) return listOf("templates/$fileName")
        return listOf("i18n/$lang/templates/$fileName", "$lang/templates/$fileName")
    }

    /** The existing file for [lang], or `null` when no known layout carries it. */
    fun resolve(
        siteRoot: File,
        fileName: String,
        lang: String,
        defaultLang: String,
    ): File? =
        relativeCandidates(fileName, lang, defaultLang)
            .asSequence()
            .map { siteRoot.resolve(it) }
            .firstOrNull { it.isFile }
}
