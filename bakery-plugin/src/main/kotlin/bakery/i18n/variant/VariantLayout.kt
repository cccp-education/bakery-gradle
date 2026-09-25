package bakery.i18n.variant

import java.io.File

/**
 * BKY-LANG-NAV-8 — the single source of truth for the i18n variant layout.
 *
 * Variants are co-located under `{bakeRoot}/i18n/{lang}/`, next to the reference
 * tree:
 *
 * ```
 * jbake/
 *   content/                  reference (fr) articles
 *   templates/                reference Thymeleaf (fr)
 *   assets/                   shared, never duplicated per language
 *   jbake.properties          shared bake configuration
 *   i18n/{lang}/
 *     content/                translated articles
 *     templates/              translated / materialised Thymeleaf
 * ```
 *
 * The same layout is produced by `materializeTemplates` (frozen bundle) and by
 * `migrateContentI18n` (LLM translation), so a single rule drives the bake of
 * every variant — no split-brain with the site-side layout.
 *
 * Pure domain: relative path resolution only, no I/O, no Gradle.
 */
class VariantLayout(
    bakeRoot: File,
) {
    val bakeRoot: File = bakeRoot
    val referenceTemplates: File = bakeRoot.resolve(REFERENCE_TEMPLATES)
    val sharedAssets: File = bakeRoot.resolve(SHARED_ASSETS)
    val jbakeProperties: File = bakeRoot.resolve(JBAKE_PROPERTIES)
    val i18nRoot: File = bakeRoot.resolve(I18N_DIR)

    fun variant(language: String): File {
        require(language.isNotBlank()) { "A language code is required to resolve an i18n variant." }
        return i18nRoot.resolve(language)
    }

    fun templates(language: String): File = variant(language).resolve("templates")

    fun content(language: String): File = variant(language).resolve("content")

    /**
     * The language codes present as direct sub-directories of the i18n tree,
     * sorted, the reference excluded. Used when `site.yml` declares no i18n
     * configuration: the corpus itself is the source of truth (a site that
     * ships `jbake/i18n/{lang}/` wants those languages baked).
     */
    fun discoverLanguages(referenceLanguage: String): List<String> =
        i18nRoot
            .listFiles()
            .orEmpty()
            .filter { it.isDirectory }
            .map { it.name }
            .filterNot { it == referenceLanguage }
            .sorted()

    /**
     * The languages declared by the frozen `messages_{lang}.properties` bundles
     * co-located with the reference templates, sorted, the reference excluded.
     * A site that owns frozen bundles declares its materialisable languages
     * without any i18n section in its (often secret) config.
     */
    fun discoverBundledLanguages(referenceLanguage: String): List<String> =
        referenceTemplates
            .listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.startsWith(BUNDLE_PREFIX) && it.name.endsWith(BUNDLE_SUFFIX) }
            .map { it.name.removePrefix(BUNDLE_PREFIX).removeSuffix(BUNDLE_SUFFIX) }
            .filterNot { it == referenceLanguage }
            .sorted()

    companion object {
        const val REFERENCE_TEMPLATES = "templates"
        const val SHARED_ASSETS = "assets"
        const val JBAKE_PROPERTIES = "jbake.properties"
        const val I18N_DIR = "i18n"
        private const val BUNDLE_PREFIX = "messages_"
        private const val BUNDLE_SUFFIX = ".properties"
    }
}
