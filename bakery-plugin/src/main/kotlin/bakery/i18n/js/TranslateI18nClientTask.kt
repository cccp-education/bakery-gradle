package bakery.i18n.js

import bakery.BakeryConstants
import bakery.intention.ResolveIntention
import bakery.intention.ResolveIntentionError
import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
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
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-3 (propagation — US-4).
 *
 * Translates the client-side i18n dictionaries of a JBake site
 * (`var DICT` chrome + `TALARIA.I18N.extend` patch) through the N0
 * [TranslationService] contract.
 *
 * Ink Economy Law: only the keys missing from a target language are sent to the
 * model; a complete dictionary is a strict no-op and never reaches the LLM. The
 * rewrite itself is delegated to the pure [I18nJsDictionary] writer.
 *
 * After the translation, the development source (`maquette/js/`) is propagated
 * byte-identically to the publication copy (`jbake/assets/js/`) — decision
 * S-039, pattern `propagate()` of the talaria harness. Only drifted copies are
 * rewritten, and the dry-run never writes.
 */
@DisableCachingByDefault(because = "Traduction i18n client — résultat non-déterministe (LLM), non-cacheable")
abstract class TranslateI18nClientTask : DefaultTask() {
    @get:Internal
    var translationService: TranslationService? = null

    @get:Internal
    var dslIntention: I18nClientMigrationIntention? = null

    @get:Input
    @get:Optional
    @get:Option(option = "i18nClientSource", description = "Repertoires sources separes par virgules (ex: maquette/js)")
    abstract val i18nClientSource: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "i18nClientTargetLangs", description = "Langues cibles separees par virgules (ex: en,it,de)")
    abstract val i18nClientTargetLangs: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "i18nClientSourceLang", description = "Langue de reference (ex: fr)")
    abstract val i18nClientSourceLang: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "i18nClientDryRun", description = "Mode dry-run (true/false) — previsualise sans ecrire")
    abstract val i18nClientDryRun: Property<String>

    @get:Input
    @get:Optional
    @get:Option(
        option = "i18nClientPropagate",
        description = "Propage maquette/js -> jbake/assets/js byte-identique (true/false)",
    )
    abstract val i18nClientPropagate: Property<String>

    init {
        group = BakeryConstants.TRANSFORM_GROUP
        description =
            "Traduit les dictionnaires i18n JS client (DICT chrome + patch) — delta des cles manquantes, economie d'encre"
        i18nClientSource.convention("")
        i18nClientTargetLangs.convention("")
        i18nClientSourceLang.convention("")
        i18nClientDryRun.convention("")
        i18nClientPropagate.convention("")
    }

    @TaskAction
    fun executeI18nClientTranslation() {
        val intention = resolveIntention()

        logger.lifecycle("[translateI18nClient] Source : {}", intention.sourceDirs.joinToString(", "))
        logger.lifecycle("[translateI18nClient] Langue de reference : {}", intention.referenceLanguage)
        logger.lifecycle("[translateI18nClient] Langues cibles : {}", intention.targetLanguages.joinToString(", "))
        logger.lifecycle("[translateI18nClient] Dry-run : {}", intention.dryRun)
        logger.lifecycle("[translateI18nClient] Propagation : {}", intention.propagate)

        val files = resolveSourceFiles(intention)
        val catalogueFiles = files.filterValues { I18nJsFormat.of(it) == I18nJsFormat.CATALOG }
        val flatFiles = files.filterKeys { it !in catalogueFiles.keys }

        logCatalogueCoverage(cataloguePlan(catalogueFiles.values, intention))

        val invalidFiles =
            flatFiles.filterValues { source -> !I18nClientFormatGuard.verify(source).isValid }.keys
        if (invalidFiles.isNotEmpty()) {
            invalidFiles.forEach { file ->
                val report = I18nClientFormatGuard.verify(flatFiles.getValue(file))
                logger.warn(
                    "[translateI18nClient] {} ignore — format invalide : {}",
                    file,
                    report.violations.joinToString { "${it.kind}:${it.detail}" },
                )
            }
        }
        val validFiles = flatFiles.filterKeys { it !in invalidFiles }

        val plan = I18nClientDelta.plan(validFiles, intention.referenceLanguage, intention.targetLanguages)

        val total = plan.sumOf { it.keys.size }
        logger.lifecycle(
            "[translateI18nClient] Delta : {} cles manquantes sur {} fichier(s).",
            total,
            plan.map { it.file }.distinct().size,
        )

        if (total == 0) {
            logger.lifecycle("[translateI18nClient] Rien a traduire — economie d'encre, aucun appel LLM.")
            if (!intention.dryRun && intention.propagate) propagateToPublication(intention)
            return
        }

        if (intention.dryRun) {
            plan.forEach { entry ->
                logger.lifecycle("  [{}] {} -> {} cle(s)", entry.language, entry.file, entry.keys.size)
            }
            logger.lifecycle("[translateI18nClient] DRY-RUN — aucun fichier modifie.")
            return
        }

        val translationService = this.translationService
        if (translationService == null) {
            logger.warn("[translateI18nClient] Aucun TranslationService — les dictionnaires sources sont inchanges.")
            if (intention.propagate) propagateToPublication(intention)
            return
        }

        val sourceByFile = resolveFileMap(intention)
        val dictionaries = I18nJsDictionary.parse(*validFiles.values.toTypedArray())
        val updated = LinkedHashMap(validFiles)

        var translatedCount = 0
        var failureCount = 0
        for (entry in plan) {
            val updatedSource = updated.getValue(entry.file)
            val referenceTexts = dictionaries[intention.referenceLanguage].orEmpty()
            val translations = LinkedHashMap<String, String>()
            for (key in entry.keys) {
                val sourceText = referenceTexts[key] ?: continue
                when (
                    val result =
                        translationService.translate(
                            TranslationRequest(
                                sourceText = sourceText,
                                sourceLanguage = intention.referenceLanguage,
                                targetLanguage = entry.language,
                            ),
                        )
                ) {
                    is TranslationResult.Success -> {
                        translations[key] = result.translatedText
                        translatedCount++
                    }
                    is TranslationResult.Failure -> {
                        failureCount++
                        logger.warn(
                            "[translateI18nClient] [{}] {} ECHEC ({}) — cle conservee absente",
                            entry.language,
                            key,
                            result.reason,
                        )
                    }
                }
            }
            updated[entry.file] = I18nJsDictionary.insertTranslations(updatedSource, entry.language, translations)
        }

        for ((file, content) in updated) {
            if (content != validFiles.getValue(file)) {
                val report = I18nClientFormatGuard.verify(content)
                if (report.isValid) {
                    sourceByFile.getValue(file).writeText(content)
                } else {
                    logger.warn(
                        "[translateI18nClient] {} non ecrit — format invalide : {}",
                        file,
                        report.violations.joinToString { "${it.kind}:${it.detail}" },
                    )
                }
            }
        }

        logger.lifecycle(
            "[translateI18nClient] Fichiers ecrits : {}, cles traduites : {}, echecs : {}",
            updated.count { (file, content) -> content != validFiles[file] },
            translatedCount,
            failureCount,
        )

        if (intention.propagate) propagateToPublication(intention)
    }

    /**
     * Structured-catalogue coverage of [intention]: the languages, formations
     * and fields of the reference language the target languages are missing.
     *
     * The catalogue (`TALARIA.I18N.CATALOG`) is nested and unquoted, so it can
     * never go through the flat writer ([I18nJsDictionary]). This report is the
     * visibility the flat delta cannot give — the contract is coverage, not a
     * flat rewrite (cadrage talaria S-054). A tree without catalogue yields
     * [I18nCatalogPlan.EMPTY].
     */
    internal fun catalogueCoverage(intention: I18nClientMigrationIntention): I18nCatalogPlan =
        cataloguePlan(
            catalogueSources = resolveSourceFiles(intention).values.filter { I18nJsFormat.of(it).isCatalogue },
            intention = intention,
        )

    private fun cataloguePlan(
        catalogueSources: Collection<String>,
        intention: I18nClientMigrationIntention,
    ): I18nCatalogPlan {
        if (catalogueSources.isEmpty()) return I18nCatalogPlan.EMPTY

        val plans =
            catalogueSources.map {
                I18nCatalog.plan(it, intention.referenceLanguage, intention.targetLanguages)
            }
        return I18nCatalogPlan(
            languages = plans.flatMap { it.languages }.distinct(),
            missingLanguages = plans.flatMap { it.missingLanguages }.distinct(),
            missingFormations = mergeNested(plans.map { it.missingFormations }),
            missingFields = mergeDoubleNested(plans.map { it.missingFields }),
        )
    }

    private fun mergeNested(
        maps: List<Map<String, List<String>>>,
    ): Map<String, List<String>> {
        val merged = LinkedHashMap<String, List<String>>()
        for (map in maps) {
            for ((language, values) in map) {
                merged[language] = (merged[language].orEmpty() + values).distinct()
            }
        }
        return merged
    }

    private fun mergeDoubleNested(
        maps: List<Map<String, Map<String, List<String>>>>,
    ): Map<String, Map<String, List<String>>> {
        val merged = LinkedHashMap<String, MutableMap<String, List<String>>>()
        for (map in maps) {
            for ((language, formations) in map) {
                val target = merged.getOrPut(language) { LinkedHashMap() }
                for ((formation, fields) in formations) {
                    target[formation] = (target[formation].orEmpty() + fields).distinct()
                }
            }
        }
        return merged
    }

    private fun logCatalogueCoverage(plan: I18nCatalogPlan) {
        if (plan == I18nCatalogPlan.EMPTY) return
        if (!plan.hasGap) {
            logger.lifecycle("[translateI18nClient] Catalogue structurel : aucune lacune de couverture.")
            return
        }
        logger.warn(
            "[translateI18nClient] Catalogue structurel : {} langue(s), {} formation(s), {} champ(s) manquants.",
            plan.missingLanguageCount,
            plan.missingFormationCount,
            plan.missingFieldCount,
        )
        plan.missingLanguages.forEach { language ->
            logger.warn("[translateI18nClient]   catalogue [{}] langue entiere manquante", language)
        }
        plan.missingFormations.forEach { (language, formations) ->
            logger.warn("[translateI18nClient]   catalogue [{}] formations manquantes : {}", language, formations)
        }
        plan.missingFields.forEach { (language, formations) ->
            formations.forEach { (formation, fields) ->
                logger.warn(
                    "[translateI18nClient]   catalogue [{}] {} champs manquants : {}",
                    language,
                    formation,
                    fields,
                )
            }
        }
    }

    /**
     * Mirrors every development dictionary of [intention] to its
     * `jbake/assets/js/` publication copy, byte-identically. Only drifted or
     * missing copies are rewritten (Ink Economy Law) — a fully aligned
     * publication tree is a strict no-op.
     */
    private fun propagateToPublication(intention: I18nClientMigrationIntention): Int {
        val sources = LinkedHashMap<String, String>()
        for (sourceDir in intention.sourceDirs) {
            val dir = resolveDir(sourceDir)
            if (!dir.exists()) continue
            dir.walkTopDown()
                .filter { it.isFile && it.extension == "js" }
                .sortedBy { it.relativeTo(dir).path }
                .forEach { file ->
                    val sourcePath = "$sourceDir/${file.relativeTo(dir).path}"
                    sources.putIfAbsent(sourcePath, file.readText())
                }
        }
        if (sources.isEmpty()) return 0

        val targets =
            sources.keys
                .mapNotNull { sourcePath ->
                    val targetPath = I18nClientPropagation.targetFor(sourcePath) ?: return@mapNotNull null
                    val targetFile = project.projectDir.resolve(targetPath)
                    if (targetFile.isFile) targetPath to targetFile.readText() else null
                }.toMap()

        val drift = I18nClientPropagation.drift(sources, targets)
        for (sourcePath in drift) {
            val targetPath = I18nClientPropagation.targetFor(sourcePath) ?: continue
            val targetFile = project.projectDir.resolve(targetPath)
            targetFile.parentFile?.mkdirs()
            targetFile.writeText(sources.getValue(sourcePath))
            logger.lifecycle("[translateI18nClient] Propaged {} -> {}", sourcePath, targetPath)
        }

        logger.lifecycle(
            "[translateI18nClient] Propagation : {} fichier(s) aligne(s), {} deja byte-identique(s).",
            drift.size,
            sources.size - drift.size,
        )
        return drift.size
    }

    private fun resolveFileMap(intention: I18nClientMigrationIntention): Map<String, File> {
        val result = LinkedHashMap<String, File>()
        for (sourceDir in intention.sourceDirs) {
            val dir = resolveDir(sourceDir)
            if (!dir.exists()) {
                logger.warn("[translateI18nClient] Le repertoire source n'existe pas : {}", dir.absolutePath)
                continue
            }
            dir.walkTopDown()
                .filter { it.isFile && it.extension == "js" }
                .sortedBy { it.relativeTo(dir).path }
                .forEach { file ->
                    val key = file.relativeTo(dir).path
                    result.putIfAbsent(key, file)
                }
        }
        return result
    }

    private fun resolveSourceFiles(intention: I18nClientMigrationIntention): Map<String, String> =
        resolveFileMap(intention).mapValues { (_, file) -> file.readText() }

    private fun resolveDir(sourceDir: String): File {
        val candidate = File(sourceDir)
        return if (candidate.isAbsolute) candidate else project.projectDir.resolve(sourceDir)
    }

    internal fun resolveIntention(): I18nClientMigrationIntention {
        val resolvedSources =
            ResolveIntention.fromCliList(
                i18nClientSource.orNull,
                dslIntention?.sourceDirs,
                emptyList(),
            )
        if (resolvedSources.isEmpty()) {
            throw ResolveIntentionError
                .MissingRequiredField(
                    cliFlag = "--i18nClientSource",
                    dslPath = "bakery { i18nClient { sourceDirs = [...] } }",
                ).toException()
        }

        val resolvedTargetLangs =
            ResolveIntention.fromCliList(
                i18nClientTargetLangs.orNull,
                dslIntention?.targetLanguages,
                listOf("en"),
            )

        val resolvedReferenceLang =
            ResolveIntention.fromCli(
                i18nClientSourceLang.orNull,
                dslIntention?.referenceLanguage,
                "fr",
            )

        val resolvedDryRun =
            ResolveIntention.fromCliBoolean(
                i18nClientDryRun.orNull,
                dslIntention?.dryRun,
                true,
            )

        val resolvedPropagate =
            ResolveIntention.fromCliBoolean(
                i18nClientPropagate.orNull,
                dslIntention?.propagate,
                true,
            )

        return I18nClientMigrationIntention(
            sourceDirs = resolvedSources,
            referenceLanguage = resolvedReferenceLang,
            targetLanguages = resolvedTargetLangs,
            dryRun = resolvedDryRun,
            propagate = resolvedPropagate,
        )
    }
}
