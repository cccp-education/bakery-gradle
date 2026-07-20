package bakery.i18n.rtl

import bakery.BakeryConstants
import document.translation.AsciiDocParser
import document.translation.ArticleRenderer
import document.translation.AsciiDocRenderer
import document.translation.JbakeNativeRenderer
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * EPIC BKY-I18N-DEPLOY — US DEPLOY-4.
 *
 * Walks `content-i18n/{lang}/**/*.adoc` (the persistent, source-controlled
 * output of `migrateContentI18n` non-dry-run — NOT `build/` which is wiped by
 * `clean`, per the Ink Economy Law encoded in AGENT.adoc) and applies
 * [RtlDirectionInjector] to each article frontmatter:
 * - RTL languages (ar, ur) get `:jbake-lang: {lang}` + `:lang: rtl`
 * - LTR languages get `:jbake-lang: {lang}` and any pre-existing `:lang: rtl`
 *   is dropped so a source RTL article translated to a LTR language drops RTL.
 *
 * Idempotent: re-running yields the same output (the injector overwrites
 * `:jbake-lang:` and resets `:lang:` deterministically). Executed after
 * `migrateContentI18n`, before `bake`. Integrated into
 * [bakery.ContentTaskRegistrar] after `registerMigrateContentI18nTask`.
 */
@DisableCachingByDefault(because = "RTL injection — file I/O on translated content, non-cacheable")
abstract class RtlDirectionInjectionTask : DefaultTask() {

    @get:Input
    @get:Optional
    @get:Option(option = "contentI18nOutput", description = "Répertoire de sortie i18n (ex: content-i18n) — persistant, hors build/")
    abstract val contentI18nOutput: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "contentI18nTargetLangs", description = "Langues cibles séparées par virgules (ex: en,ar,ur)")
    abstract val contentI18nTargetLangs: Property<String>

    private val injector = RtlDirectionInjector()
    private val parser = AsciiDocParser()
    private val renderer: ArticleRenderer = JbakeNativeRenderer()
    private val fallbackRenderer: ArticleRenderer = AsciiDocRenderer()

    init {
        group = BakeryConstants.TRANSFORM_GROUP
        description = "Injects :jbake-lang: and :lang: rtl directives into translated articles frontmatter (ar/ur RTL, others LTR)"
        contentI18nOutput.convention("")
        contentI18nTargetLangs.convention("")
    }

    @TaskAction
    fun executeRtlInjection() {
        val outputDir = resolveOutputDir()
        if (!outputDir.exists()) {
            logger.warn("[injectRtlDirection] Le répertoire de sortie n'existe pas : {}", outputDir.absolutePath)
            return
        }

        val targetLangs = resolveTargetLangs()
        if (targetLangs.isEmpty()) {
            logger.lifecycle("[injectRtlDirection] Aucune langue cible — rien à faire.")
            return
        }
        logger.lifecycle("[injectRtlDirection] Sortie : {}", outputDir.absolutePath)
        logger.lifecycle("[injectRtlDirection] Langues cibles : {}", targetLangs.joinToString(", "))

        var injectedCount = 0
        for (lang in targetLangs) {
            val langDir = outputDir.resolve(lang)
            if (!langDir.exists()) {
                logger.lifecycle("[injectRtlDirection] [{}] Répertoire absent — ignoré.", lang)
                continue
            }
            val adocFiles = langDir.walkTopDown()
                .filter { it.isFile && it.extension == "adoc" }
                .toList()
            logger.lifecycle("[injectRtlDirection] [{}] {} fichiers .adoc trouvés.", lang, adocFiles.size)
            for (file in adocFiles) {
                val original = file.readText()
                val article = parser.parse(original)
                val injected = injector.inject(article.frontmatter, lang)
                if (injected == article.frontmatter) continue
                val updatedArticle = article.copy(frontmatter = injected)
                val outputRenderer = if (article.frontmatter.isJbakeNative) renderer else fallbackRenderer
                val rendered = outputRenderer.render(updatedArticle)
                if (rendered != original) {
                    file.writeText(rendered)
                    injectedCount++
                }
            }
        }
        logger.lifecycle("[injectRtlDirection] Terminé — {} fichiers mis à jour.", injectedCount)
    }

    private fun resolveOutputDir(): File {
        val raw = contentI18nOutput.get().ifBlank { "content-i18n" }
        val candidate = File(raw)
        return if (candidate.isAbsolute) candidate else File(project.projectDir, raw)
    }

    private fun resolveTargetLangs(): List<String> =
        contentI18nTargetLangs.get().split(",").map { it.trim() }.filter { it.isNotBlank() }
}