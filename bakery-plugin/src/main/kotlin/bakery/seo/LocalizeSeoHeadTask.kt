package bakery.seo

import bakery.BakeryConstants
import bakery.i18n.DocumentDirection
import bakery.i18n.I18nTemplateLayout
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * BKY-OUT-1 / BKY-OUT-2 — localises every variant head: SEO fields and document
 * direction.
 *
 * Walks the variant `header.thyme` of each configured language (the layout is
 * [I18nTemplateLayout], the *same* rule `injectLangSwitch` uses) and applies
 * [LocalizedSeoHead] to it. The og-image directory is resolved from the disk
 * (`assets/og-images/{lang}/`): a language without its own cards falls back to
 * `en`, then to the reference language, so no variant ever emits a 404 card.
 *
 * Deterministic and idempotent: a second run rewrites nothing. No LLM, no
 * re-translation, no store eviction — the head literals are repaired in place
 * (Ink Economy).
 */
@DisableCachingByDefault(because = "Template rewrite — file I/O on variant heads, non-cacheable")
abstract class LocalizeSeoHeadTask : DefaultTask() {
    @get:Internal
    var siteDir: File? = null

    @get:Internal
    var defaultLanguage: String = "fr"

    @get:Internal
    var languages: List<String> = listOf("fr")

    /** Root-relative directory holding the per-language Open Graph cards. */
    @get:Internal
    var ogImagesRelativePath: String = "assets/og-images"

    init {
        group = BakeryConstants.TRANSFORM_GROUP
        description =
            "Localises every variant head: canonical/og:url/og:locale/og:image language and the document direction (dir=rtl for ar/fa/ur)"
    }

    @TaskAction
    fun executeLocalization() {
        val site =
            siteDir ?: run {
                logger.warn("[localizeSeoHead] siteDir not configured — skipping")
                return
            }
        if (!site.exists()) {
            logger.warn("[localizeSeoHead] site directory does not exist: ${site.absolutePath}")
            return
        }

        val availableOgLanguageDirs = availableOgLanguageDirs(site)
        var changedCount = 0

        for (language in languages) {
            val header = I18nTemplateLayout.resolve(site, HEADER_THYME, language, defaultLanguage)
            if (header == null) {
                logger.lifecycle("[localizeSeoHead] No $HEADER_THYME for '$language' — skipping")
                continue
            }

            val ogImageLanguage = resolveOgImageLanguage(language, availableOgLanguageDirs)
            val original = header.readText()
            val localised = LocalizedSeoHead.rewrite(original, language, defaultLanguage, ogImageLanguage)
            val updated = DocumentDirection.apply(localised, language)

            if (updated != original) {
                header.writeText(updated)
                changedCount++
                logger.lifecycle(
                    "[localizeSeoHead] Localised head '$language' (og:locale ${LocalizedSeoHead.localeOf(language)}, " +
                        "og-images/$ogImageLanguage/)",
                )
            } else {
                logger.lifecycle("[localizeSeoHead] No change for '$language' (already localised)")
            }
        }

        logger.lifecycle("[localizeSeoHead] Processed ${languages.size} language(s), localised $changedCount head(s)")
    }

    private fun availableOgLanguageDirs(site: File): Set<String> =
        site.resolve(ogImagesRelativePath)
            .listFiles()
            .orEmpty()
            .filter { it.isDirectory }
            .map { it.name }
            .toSet()

    /**
     * The directory that actually holds this language's cards: its own when it
     * exists, else the **reference** — the only complete set (measured: 110 fr
     * cards, 107 en, none for the 20 others). A partial set would 404 the card of
     * an article that has no image in it, which is worse than a reference card.
     */
    private fun resolveOgImageLanguage(
        language: String,
        available: Set<String>,
    ): String = if (language in available) language else defaultLanguage

    private companion object {
        const val HEADER_THYME = "header.thyme"
    }
}
