package bakery.i18n

import bakery.BakeryConstants
import bakery.intention.ResolveIntention
import bakery.intention.ResolveIntentionError
import contracts.i18n.TranslationService
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
 * CHE-I18N-22 US-7 — translates the site's Thymeleaf templates for every target
 * language, using the full-template swap strategy.
 *
 * `migrateToI18n` extracts hardcoded text into `messages_{lang}.properties` and
 * rewrites the templates with `#{key}`. That works for a Spring-backed site, but
 * `jbake-core:2.7.0` installs **no MessageResolver**: the bake would emit the raw
 * `#{key}`. cheroliv.com therefore ships one integrally translated copy of each
 * template under `i18n/{lang}/templates/`, which the CI swaps in place of
 * `jbake/` before baking.
 *
 * This task produces exactly that: for each target language it copies the
 * reference `templates/` tree into `i18n/{lang}/templates/` (only for a language
 * that has no template yet) and translates every visible text segment through
 * the N0 [TranslationService] (the codebase-backed pool).
 *
 * Ink economy: a language whose templates already exist is skipped; the
 * translation is delta-free by nature (a template is small and finite).
 */
@DisableCachingByDefault(because = "Traduction de templates — résultat non-déterministe (LLM), non-cacheable")
abstract class TranslateTemplatesTask : DefaultTask() {
    @get:Internal
    var translationService: TranslationService? = null

    @get:Internal
    var siteDir: File? = null

    @get:Input
    @get:Optional
    @get:Option(option = "templateTargetLangs", description = "Langues cibles séparées par virgules (ex: en,de,it)")
    abstract val templateTargetLangs: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "templateSourceLang", description = "Langue source (ex: fr)")
    abstract val templateSourceLang: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "templateDryRun", description = "Mode dry-run (true/false)")
    abstract val templateDryRun: Property<String>

    init {
        group = BakeryConstants.TRANSFORM_GROUP
        description =
            "Traduit les templates Thymeleaf du site dans chaque langue cible (stratégie swap de copies complètes, JBake sans MessageResolver)"
        templateTargetLangs.convention("")
        templateSourceLang.convention("")
        templateDryRun.convention("")
    }

    @TaskAction
    fun execute() {
        val site =
            siteDir ?: run {
                logger.warn("[translateTemplates] siteDir non configuré — ignoré")
                return
            }
        // The reference tree is the bake source (`jbake/`), which holds both
        // `templates/` and `content/`. Variants live in `i18n/{lang}/`.
        val referenceDir = site.resolve("templates")
        if (!referenceDir.exists()) {
            logger.warn("[translateTemplates] Aucun répertoire templates dans {}", site.absolutePath)
            return
        }
        val siteRoot = site.parentFile

        val targetLangs =
            ResolveIntention
                .fromCliList(templateTargetLangs.orNull, null, emptyList())
        val sourceLang = ResolveIntention.fromCli(templateSourceLang.orNull, null, "fr")
        val dryRun = ResolveIntention.fromCliBoolean(templateDryRun.orNull, null, true)

        if (targetLangs.isEmpty()) {
            logger.warn("[translateTemplates] Aucune langue cible fournie.")
            return
        }

        logger.lifecycle("[translateTemplates] Site : {}", site.absolutePath)
        logger.lifecycle("[translateTemplates] Langue source : {}", sourceLang)
        logger.lifecycle("[translateTemplates] Langues cibles : {}", targetLangs.joinToString(", "))
        logger.lifecycle("[translateTemplates] Dry-run : {}", dryRun)

        val translator = translationService?.let { TemplateTextTranslator(it) }
        if (translator == null) {
            logger.warn("[translateTemplates] Aucun TranslationService — aucun template traduit.")
            return
        }

        val templateFiles = referenceDir.walkTopDown().filter { it.isFile && it.extension == "thyme" }.toList()

        for (language in targetLangs) {
            if (language == sourceLang) continue
            val targetDir = siteRoot.resolve("i18n/$language/templates")
            val alreadyPresent = targetDir.exists() && targetDir.listFiles().orEmpty().isNotEmpty()

            var translatedFiles = 0
            var failedSegments = 0
            for (reference in templateFiles) {
                val relative = reference.relativeTo(referenceDir)
                val target = targetDir.resolve(relative)

                // A variant already translated is preserved (never re-translated):
                // the `en` templates were authored long ago and must survive.
                if (target.exists() && alreadyPresent) continue

                if (dryRun) {
                    logger.lifecycle("[translateTemplates] [{}] DRY-RUN {}", language, relative)
                    continue
                }

                target.parentFile.mkdirs()
                val result = translator.translate(reference.readText(), sourceLang, language)
                target.writeText(result.content)
                translatedFiles++
                failedSegments += result.failedSegments
            }
            if (!dryRun) {
                logger.lifecycle(
                    "[translateTemplates] [{}] {} templates traduits, {} segments en échec",
                    language,
                    translatedFiles,
                    failedSegments,
                )
            }
        }
    }
}
