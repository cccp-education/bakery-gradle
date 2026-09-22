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
 * Ink economy: a copy that differs from the reference is preserved (never
 * re-translated); only a missing or still-French template is translated — the
 * eight partial cheroliv.com variants converge instead of being skipped whole.
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

    @get:Input
    @get:Optional
    @get:Option(
        option = "templateForceLangs",
        description = "Langues à régénérer intégralement (ex: es,ar) — variantes héritées restées françaises",
    )
    abstract val templateForceLangs: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "templateParallelism", description = "Traductions parallèles (1 à 25 ports sains du pool)")
    abstract val templateParallelism: Property<String>

    init {
        group = BakeryConstants.TRANSFORM_GROUP
        description =
            "Traduit les templates Thymeleaf du site dans chaque langue cible (stratégie swap de copies complètes, JBake sans MessageResolver)"
        templateTargetLangs.convention("")
        templateSourceLang.convention("")
        templateDryRun.convention("")
        templateForceLangs.convention("")
        templateParallelism.convention("")
    }

    @TaskAction
    fun execute() {
        val site =
            siteDir ?: run {
                logger.warn("[translateTemplates] siteDir non configuré — ignoré")
                return
            }
        // The reference tree is the bake source (`jbake/`), which holds both
        // `templates/` and `content/`. CHE-I18N-UNIFY D1: the i18n base is
        // co-located under the bake root — `jbake/i18n/{lang}/templates/` — so
        // every variant lives next to the reference it mirrors.
        val referenceDir = site.resolve("templates")
        if (!referenceDir.exists()) {
            logger.warn("[translateTemplates] Aucun répertoire templates dans {}", site.absolutePath)
            return
        }
        val i18nRoot = site.resolve("i18n")

        val targetLangs =
            ResolveIntention
                .fromCliList(templateTargetLangs.orNull, null, emptyList())
        val sourceLang = ResolveIntention.fromCli(templateSourceLang.orNull, null, "fr")
        val dryRun = ResolveIntention.fromCliBoolean(templateDryRun.orNull, null, true)
        val forceLangs = ResolveIntention.fromCliList(templateForceLangs.orNull, null, emptyList()).toSet()
        val parallelism =
            TemplateTranslationPlanner.requireParallelism(
                ResolveIntention.fromCli(templateParallelism.orNull, null, "1").toIntOrNull() ?: 1,
            )

        if (targetLangs.isEmpty()) {
            logger.warn("[translateTemplates] Aucune langue cible fournie.")
            return
        }

        logger.lifecycle("[translateTemplates] Site : {}", site.absolutePath)
        logger.lifecycle("[translateTemplates] Langue source : {}", sourceLang)
        logger.lifecycle("[translateTemplates] Langues cibles : {}", targetLangs.joinToString(", "))
        logger.lifecycle("[translateTemplates] Dry-run : {}", dryRun)
        logger.lifecycle("[translateTemplates] Parallélisme : {}", parallelism)
        if (forceLangs.isNotEmpty()) {
            logger.lifecycle("[translateTemplates] Langues forcées : {}", forceLangs.joinToString(", "))
        }

        val service = translationService
        if (service == null) {
            logger.warn("[translateTemplates] Aucun TranslationService — aucun template traduit.")
            return
        }

        val templateFiles = referenceDir.walkTopDown().filter { it.isFile && it.extension == "thyme" }.toList()
        val reference = templateFiles.associate { it.relativeTo(referenceDir).path to it.readText() }

        for (language in targetLangs) {
            if (language == sourceLang) continue
            val targetDir = i18nRoot.resolve("$language/templates")
            val target =
                templateFiles.associate {
                    val relative = it.relativeTo(referenceDir).path
                    relative to targetDir.resolve(relative)
                }.filterValues { it.exists() }
                    .mapValues { it.value.readText() }

            val toTranslate = TemplateTranslationPlanner.plan(reference, target, force = language in forceLangs)

            var translatedFiles = 0
            var failedSegments = 0
            var preservedFiles = 0

            if (dryRun) {
                for (relativePath in reference.keys) {
                    if (relativePath !in toTranslate) {
                        preservedFiles++
                        continue
                    }
                    logger.lifecycle("[translateTemplates] [{}] DRY-RUN {}", language, relativePath)
                }
            } else {
                // Bound the concurrency to the healthy ports of the pool (pilot
                // decision S-044): one worker per provider, never a shared
                // mutable translator across threads.
                val executor = java.util.concurrent.Executors.newFixedThreadPool(parallelism)
                val translated = java.util.concurrent.atomic.AtomicInteger(0)
                val failed = java.util.concurrent.atomic.AtomicInteger(0)
                val rejected = java.util.concurrent.atomic.AtomicInteger(0)
                val futures =
                    reference.keys
                        .filter { it in toTranslate }
                        .map { relativePath ->
                            executor.submit {
                                val referenceFile = referenceDir.resolve(relativePath)
                                val targetFile = targetDir.resolve(relativePath)
                                targetFile.parentFile.mkdirs()
                                val workerTranslator = TemplateTextTranslator(service)
                                try {
                                    val result =
                                        workerTranslator.translate(referenceFile.readText(), sourceLang, language)
                                    // A translation that breaks the tag skeleton is
                                    // never written (the model can eat a closing
                                    // `">`): the target file stays missing and the
                                    // delta schedules it again on the next run.
                                    if (!TemplateStructureGuard.isWellFormed(result.content)) {
                                        rejected.incrementAndGet()
                                        logger.warn(
                                            "[translateTemplates] [{}] RÉSultat structurellement invalide {} — non écrit (sera repris au prochain run)",
                                            language,
                                            relativePath,
                                        )
                                        return@submit
                                    }
                                    targetFile.writeText(result.content)
                                    translated.incrementAndGet()
                                    failed.addAndGet(result.failedSegments)
                                } catch (e: Exception) {
                                    logger.warn(
                                        "[translateTemplates] [{}] ERREUR {} : {}",
                                        language,
                                        relativePath,
                                        e.message,
                                    )
                                }
                            }
                        }
                futures.forEach { it.get() }
                executor.shutdown()
                translatedFiles = translated.get()
                failedSegments = failed.get()
                preservedFiles = reference.size - toTranslate.size
                if (rejected.get() > 0) {
                    logger.warn(
                        "[translateTemplates] [{}] {} templates rejetés (structure cassée) — repris au prochain run",
                        language,
                        rejected.get(),
                    )
                }
            }
            if (!dryRun) {
                // CHE-I18N-QUALITY — the preserved templates are not necessarily
                // healthy: a variant translated *before* attributes were
                // translated at all (the S-049 cheroliv.com corpus) differs from
                // the reference, so the planner preserves it forever while its
                // `placeholder="Nom"` stays French. Repair only the reference
                // values still verbatim in the target — scope-exact, no
                // re-translation of already-translated prose (Économie d'Encre).
                val repairedAttributes =
                    repairPreservedAttributes(
                        language,
                        referenceDir,
                        targetDir,
                        reference.keys - toTranslate.toSet(),
                        service,
                        sourceLang,
                    )

                // US-7d — realign `<html lang>` on *every* target template,
                // including the preserved ones: a free WCAG 3.1.1 repair that
                // needs no model call (Ink Economy Law).
                val aligned = alignHtmlLanguage(language, referenceDir, targetDir)
                logger.lifecycle(
                    "[translateTemplates] [{}] {} templates traduits, {} préservés, {} attributs réparés, {} segments en échec, {} alignés lang",
                    language,
                    translatedFiles,
                    preservedFiles,
                    repairedAttributes,
                    failedSegments,
                    aligned,
                )
            }
        }
    }

    /**
     * CHE-I18N-QUALITY — repairs the attributes still French in a variant the
     * planner preserved, and returns how many files changed.
     *
     * The scope is the value, never the file or the segment: a translated prose
     * is invisible here, so a converged variant is a strict no-op and a second
     * run changes nothing.
     */
    private fun repairPreservedAttributes(
        language: String,
        referenceDir: File,
        targetDir: File,
        preservedPaths: Set<String>,
        service: TranslationService,
        sourceLang: String,
    ): Int {
        var repaired = 0
        preservedPaths.forEach { relativePath ->
            val referenceFile = referenceDir.resolve(relativePath)
            val targetFile = targetDir.resolve(relativePath)
            if (!referenceFile.exists() || !targetFile.exists()) return@forEach
            val pending = TemplateAttributeRepair.pending(referenceFile.readText(), targetFile.readText())
            if (pending.isEmpty()) return@forEach
            val values = TemplateTextTranslator(service).translateValues(pending, sourceLang, language)
            if (values.replacements.isEmpty()) return@forEach
            val repairedContent = TemplateAttributeRepair.repair(targetFile.readText(), values.replacements)
            if (repairedContent != targetFile.readText()) {
                targetFile.writeText(repairedContent)
                repaired++
            }
        }
        return repaired
    }

    /**
     * Rewrites `<html lang>` of every target template to the language code, and
     * returns how many files changed. Idempotent.
     */
    private fun alignHtmlLanguage(
        language: String,
        referenceDir: File,
        targetDir: File,
    ): Int {
        if (!targetDir.exists()) return 0
        return targetDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "thyme" }
            .count { file ->
                val content = file.readText()
                val aligned = TemplateLanguageAttribute.ensureLanguage(content, language)
                if (aligned != content) {
                    file.writeText(aligned)
                    true
                } else {
                    false
                }
            }
    }
}
