package bakery.i18n

import bakery.BakeryConstants
import bakery.i18n.path.ContentSourcePathResolver
import bakery.intention.ResolveIntention
import bakery.intention.ResolveIntentionError
import contracts.i18n.TranslationService
import document.translation.ContentTranslationService
import document.translation.delta.ArticleModification
import document.translation.delta.BlockChecksumEntry
import document.translation.delta.ContentChecksum
import document.translation.delta.I18nDelta
import document.translation.delta.I18nDeltaApplier
import document.translation.plantuml.PlantUmlTranslationAdapter
import document.translation.validation.PlantUmlValidationReport
import document.translation.validation.PlantUmlValidationResult
import document.translation.validation.TableValidationReport
import document.translation.validation.TableValidationResult
import document.translation.validation.ValidationMode
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "Migration contenu i18n — résultat non-déterministe (LLM), non-cacheable")
abstract class MigrateContentI18nTask : DefaultTask() {
    @get:Internal
    var translationService: TranslationService? = null

    @get:Internal
    var contentRootDir: File? = null

    /**
     * Segments du content root (`bake.srcPath`) relatifs au project dir, tels que
     * `jbake` ou `site/content`. Nécessaires au
     * [bakery.i18n.path.ContentSourcePathResolver] pour éviter de doubler le
     * préfixe quand l'utilisateur passe un `contentI18nSource` déjà préfixé.
     */
    @get:Internal
    var contentSrcPath: String? = null

    @get:Internal
    var dslIntention: ContentMigrationIntention? = null

    @get:Input
    @get:Optional
    @get:Option(option = "contentI18nSource", description = "Répertoire source du contenu à traduire")
    abstract val contentI18nSource: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "contentI18nOutput", description = "Répertoire de sortie pour le contenu traduit")
    abstract val contentI18nOutput: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "contentI18nTargetLangs", description = "Langues cibles séparées par virgules (ex: en,zh,es)")
    abstract val contentI18nTargetLangs: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "contentI18nSourceLang", description = "Langue source du contenu (ex: fr)")
    abstract val contentI18nSourceLang: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "contentI18nDryRun", description = "Mode dry-run (true/false) — prévisualise sans écrire")
    abstract val contentI18nDryRun: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "contentI18nValidation", description = "Mode de validation plantuml/table (STRICT/LENIENT/OFF)")
    abstract val contentI18nValidation: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "contentI18nParallelism", description = "Traductions parallèles (1 à 25 providers Ollama, max = ports sains du pool)")
    abstract val contentI18nParallelism: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "contentI18nExcludePaths", description = "Chemins relatifs à exclure de la traduction (ex: draft,.well-known)")
    abstract val contentI18nExcludePaths: Property<String>

    init {
        group = BakeryConstants.TRANSFORM_GROUP
        description =
            "Migre le contenu AsciiDoc d'un site bakery vers l'i18n — copie le contenu source, traduit les fichiers .adoc, preserve les fichiers non-adoc"
        contentI18nSource.convention("")
        contentI18nOutput.convention("")
        contentI18nTargetLangs.convention("")
        contentI18nSourceLang.convention("")
        contentI18nDryRun.convention("")
        contentI18nValidation.convention("")
        contentI18nParallelism.convention("")
        contentI18nExcludePaths.convention("")
    }

    @TaskAction
    fun executeContentMigration() {
        val intention = resolveIntention()

        logger.lifecycle("[migrateContentI18n] Source : {}", intention.sourceDir)
        logger.lifecycle("[migrateContentI18n] Sortie : {}", intention.outputDir)
        logger.lifecycle("[migrateContentI18n] Langue source : {}", intention.sourceLanguage)
        logger.lifecycle("[migrateContentI18n] Langues cibles : {}", intention.targetLanguages.joinToString(", "))
        logger.lifecycle("[migrateContentI18n] Dry-run : {}", intention.dryRun)
        logger.lifecycle("[migrateContentI18n] Validation : {}", intention.validation)

        val validationMode = try {
            ValidationMode.valueOf(intention.validation)
        } catch (e: IllegalArgumentException) {
            logger.warn("[migrateContentI18n] Mode de validation '{}' invalide, fallback LENIENT.", intention.validation)
            ValidationMode.LENIENT
        }

        val sourceDir = resolveSourceDir(intention)
        if (!sourceDir.exists()) {
            logger.warn("[migrateContentI18n] Le répertoire source n'existe pas : {}", sourceDir.absolutePath)
            return
        }

        val outputBaseDir = resolveOutputDir(intention)
        // CHE-I18N-22 US-4 (defect 1) — `excludePaths` must narrow the *delta*,
        // not only the non-adoc copy loop: the 72 cheroliv.com drafts (39% of the
        // metered budget) were still scheduled for translation.
        val currentChecksums =
            ContentMigrationPlanner.checksumOf(sourceDir, intention.excludePaths.toSet())

        if (intention.dryRun) {
            logger.lifecycle("[migrateContentI18n] DRY-RUN — aucun fichier modifié.")
            return
        }

        val translationService = this.translationService

        val allTableValidationResults = mutableListOf<TableValidationResult.Invalid>()
        val allPlantUmlValidationResults = mutableListOf<PlantUmlValidationResult.Invalid>()

        for (targetLang in intention.targetLanguages) {
            val langDir = outputBaseDir.resolve(targetLang)
            val storedChecksums = loadStoredChecksums(langDir)
            val delta = computeDelta(storedChecksums, currentChecksums)

            val existingTargetFiles =
                if (langDir.exists()) {
                    langDir
                        .walkTopDown()
                        .filter { it.isFile && it.extension == "adoc" }
                        .map { it.relativeTo(langDir).path }
                        .toSet()
                } else {
                    emptySet()
                }

            val applier = I18nDeltaApplier(delta, existingTargetFiles)
            val applicationResult = applier.apply()

            val filesToTranslate = applicationResult.toTranslate.paths
            logger.lifecycle(
                "[migrateContentI18n] [{}] Delta : {} à traduire, {} préservés.",
                targetLang,
                filesToTranslate.size,
                applicationResult.toPreserve.paths.size,
            )

            copyNonAdocFiles(sourceDir, langDir, intention.excludePaths.toSet())

            if (translationService != null && filesToTranslate.isNotEmpty()) {
                val plantUmlAdapter = PlantUmlTranslationAdapter(translationService, plantUmlValidationMode = validationMode)
                val contentService =
                    ContentTranslationService(
                        translationService,
                        parallelism = intention.parallelism,
                        plantUmlAdapter = plantUmlAdapter,
                    )
                // CHE-I18N-22 US-4 (defect 2) — the previous file-by-file loop was
                // sequential: `parallelism` reached the service but nothing drove it
                // concurrently. The file loop now runs on a bounded pool (pilot
                // decision S-044: up to one worker per healthy port of the
                // 11437-11465 pool, 25 today). Each worker owns its
                // ContentTranslationService so the translator's mutable validation
                // lists are never shared across threads.
                val workers = intention.parallelism.coerceAtLeast(1)
                val executor = java.util.concurrent.Executors.newFixedThreadPool(workers)
                val tableResults = java.util.concurrent.ConcurrentLinkedQueue<TableValidationResult.Invalid>()
                val plantUmlResults = java.util.concurrent.ConcurrentLinkedQueue<PlantUmlValidationResult.Invalid>()
                var translatedCount = java.util.concurrent.atomic.AtomicInteger(0)
                var errorCount = java.util.concurrent.atomic.AtomicInteger(0)

                val futures =
                    filesToTranslate.map { relPath ->
                        executor.submit {
                            val sourceFile = sourceDir.resolve(relPath)
                            val targetFile = langDir.resolve(relPath)
                            targetFile.parentFile.mkdirs()
                            val workerService =
                                ContentTranslationService(
                                    translationService,
                                    parallelism = 1,
                                    plantUmlAdapter = PlantUmlTranslationAdapter(translationService, plantUmlValidationMode = validationMode),
                                )
                            try {
                                val previousBlockChecksums = loadBlockChecksums(langDir, relPath)
                                val newBlockChecksums = workerService.translateSingleFileWithBlockDelta(
                                    sourceFile = sourceFile,
                                    targetFile = targetFile,
                                    previousBlockChecksums = previousBlockChecksums,
                                    sourceLanguage = intention.sourceLanguage,
                                    targetLanguage = targetLang,
                                )
                                storeBlockChecksums(langDir, relPath, newBlockChecksums)
                                translatedCount.incrementAndGet()
                            } catch (e: Exception) {
                                errorCount.incrementAndGet()
                                logger.warn("[migrateContentI18n] [{}] ERREUR bloc {} : {}", targetLang, relPath, e.message)
                            }
                            tableResults.addAll(workerService.drainTableValidationResults())
                            plantUmlResults.addAll(workerService.drainPlantUmlValidationResults())
                        }
                    }
                futures.forEach { it.get() }
                executor.shutdown()
                allTableValidationResults.addAll(tableResults)
                allPlantUmlValidationResults.addAll(plantUmlResults)
                logger.lifecycle(
                    "[migrateContentI18n] [{}] Fichiers traduits : {}, erreurs : {} (parallelism={})",
                    targetLang,
                    translatedCount.get(),
                    errorCount.get(),
                    workers,
                )
            } else if (translationService == null) {
                for (relPath in filesToTranslate) {
                    val sourceFile = sourceDir.resolve(relPath)
                    val targetFile = langDir.resolve(relPath)
                    targetFile.parentFile.mkdirs()
                    sourceFile.copyTo(targetFile, overwrite = true)
                }
                logger.lifecycle(
                    "[migrateContentI18n] [{}] {} fichiers copiés sans traduction.",
                    targetLang,
                    filesToTranslate.size,
                )
            }

            storeChecksums(langDir, currentChecksums)
        }

        writeValidationReport(outputBaseDir, allTableValidationResults, allPlantUmlValidationResults)
    }

    private fun copyNonAdocFiles(
        sourceDir: File,
        langDir: File,
        excludeRelativePaths: Set<String>,
    ) {
        // CHE-I18N-UNIFY — `SourceTree` prunes excluded subtrees instead of
        // walking through them: with the output inside the source tree
        // (`source=jbake`, `output=jbake/i18n`), `walkTopDown` would recopy the
        // whole `i18n` tree into every variant (the S-045 7.3 GB recursion).
        SourceTree.walk(sourceDir, excludeRelativePaths).forEach { file ->
            val relPath = file.relativeTo(sourceDir).path
            if (file.isDirectory) {
                langDir.resolve(relPath).mkdirs()
                return@forEach
            }
            if (file.extension == "adoc") return@forEach
            val target = langDir.resolve(relPath)
            target.parentFile.mkdirs()
            file.copyTo(target, overwrite = true)
        }
    }

    private fun writeValidationReport(
        outputBaseDir: File,
        tableResults: List<TableValidationResult.Invalid>,
        plantUmlResults: List<PlantUmlValidationResult.Invalid>,
    ) {
        val tableReport = TableValidationReport.fromResults(tableResults)
        val plantUmlReport = PlantUmlValidationReport.fromResults(plantUmlResults)
        val consolidated = ValidationReport(
            table = tableReport.entries,
            plantUml = plantUmlReport.entries,
        )
        val reportFile = outputBaseDir.resolve("validation-report.json")
        reportFile.writeText(consolidated.toJson())
        logger.lifecycle(
            "[migrateContentI18n] Rapport de validation : {} (tables: {} invalides, plantuml: {} invalides)",
            reportFile.absolutePath,
            tableResults.size,
            plantUmlResults.size,
        )
    }

    private fun loadStoredChecksums(langDir: File): Map<String, String> {
        val checksumFile = langDir.resolve(".bakery-checksums.properties")
        if (!checksumFile.exists()) return emptyMap()
        return checksumFile
            .readLines()
            .filter { it.contains("=") }
            .associate { line ->
                val (path, hash) = line.split("=", limit = 2)
                path to hash
            }
    }

    private fun storeChecksums(
        langDir: File,
        checksums: Map<String, String>,
    ) {
        val checksumFile = langDir.resolve(".bakery-checksums.properties")
        checksumFile.writeText(
            checksums.entries.joinToString("\n") { "${it.key}=${it.value}" },
        )
    }

    private fun blockChecksumsFile(langDir: File, relPath: String): File =
        langDir.resolve(".bakery-block-checksums").resolve("$relPath.checksums")

    private fun loadBlockChecksums(langDir: File, relPath: String): Map<String, BlockChecksumEntry> {
        val file = blockChecksumsFile(langDir, relPath)
        if (!file.exists()) return emptyMap()
        return file
            .readLines()
            .filter { it.contains("=") }
            .associate { line ->
                val (idx, raw) = line.split("=", limit = 2)
                idx to BlockChecksumEntry.parse(raw)
            }
    }

    private fun storeBlockChecksums(
        langDir: File,
        relPath: String,
        checksums: Map<String, BlockChecksumEntry>,
    ) {
        val file = blockChecksumsFile(langDir, relPath)
        file.parentFile.mkdirs()
        file.writeText(
            checksums.entries.joinToString("\n") { "${it.key}=${it.value.serialize()}" },
        )
    }

    private fun computeDelta(
        beforeChecksums: Map<String, String>,
        afterChecksums: Map<String, String>,
    ): I18nDelta {
        val modified = mutableListOf<ArticleModification>()
        for ((path, afterHash) in afterChecksums) {
            val beforeHash = beforeChecksums[path]
            if (beforeHash == null || beforeHash != afterHash) {
                modified.add(ArticleModification(path, beforeHash, afterHash, 0))
            }
        }
        return I18nDelta(modified, emptyList(), afterChecksums)
    }

    internal fun resolveIntention(): ContentMigrationIntention {
        val resolvedSource =
            ResolveIntention
                .fromCliRequired(
                    contentI18nSource.orNull,
                    dslIntention?.sourceDir,
                    ResolveIntentionError.MissingRequiredField(
                        cliFlag = "--contentI18nSource",
                        dslPath = "bakery { contentI18nMigration { sourceDir = \"...\" } }",
                    ),
                ).fold(
                    ifLeft = { throw it.toException() },
                    ifRight = { it },
                )

        val resolvedOutput =
            ResolveIntention
                .fromCliRequired(
                    contentI18nOutput.orNull,
                    dslIntention?.outputDir,
                    ResolveIntentionError.MissingRequiredField(
                        cliFlag = "--contentI18nOutput",
                        dslPath = "bakery { contentI18nMigration { outputDir = \"...\" } }",
                    ),
                ).fold(
                    ifLeft = { throw it.toException() },
                    ifRight = { it },
                )

        val resolvedTargetLangs =
            ResolveIntention.fromCliList(
                contentI18nTargetLangs.orNull,
                dslIntention?.targetLanguages,
                listOf("en"),
            )

        val resolvedSourceLang =
            ResolveIntention.fromCli(
                contentI18nSourceLang.orNull,
                dslIntention?.sourceLanguage,
                "fr",
            )

        val resolvedDryRun =
            ResolveIntention.fromCliBoolean(
                contentI18nDryRun.orNull,
                dslIntention?.dryRun,
                true,
            )

        val resolvedExclude =
            ResolveIntention.fromCliList(
                contentI18nExcludePaths.orNull,
                dslIntention?.excludePaths,
                emptyList(),
            )

        val resolvedValidation =
            ResolveIntention.fromCli(
                contentI18nValidation.orNull,
                dslIntention?.validation,
                "LENIENT",
            )

        val resolvedParallelism =
            ResolveIntention
                .fromCli(contentI18nParallelism.orNull, dslIntention?.parallelism?.toString(), "1")
                .toIntOrNull()
                ?: 1

        return ContentMigrationIntention(
            sourceDir = resolvedSource,
            outputDir = resolvedOutput,
            sourceLanguage = resolvedSourceLang,
            targetLanguages = resolvedTargetLangs,
            dryRun = resolvedDryRun,
            excludePaths = resolvedExclude,
            parallelism = resolvedParallelism,
            validation = resolvedValidation,
        )
    }

    private fun resolveSourceDir(intention: ContentMigrationIntention): File {
        val root = contentRootDir ?: return File(intention.sourceDir)
        val srcPath = contentSrcPath
        // Backward-compat : contentRootDir configuré seul (sans contentSrcPath),
        // la résolution relative s'appuie sur le content root comme avant.
        if (srcPath.isNullOrBlank()) {
            val candidate = File(intention.sourceDir)
            return if (candidate.isAbsolute) candidate else root.resolve(intention.sourceDir)
        }
        return ContentSourcePathResolver.resolve(
            projectDir = project.projectDir,
            srcPath = srcPath,
            sourceDir = intention.sourceDir,
        )
    }

    private fun resolveOutputDir(intention: ContentMigrationIntention): File {
        val candidate = File(intention.outputDir)
        return if (candidate.isAbsolute) candidate else File(project.projectDir, intention.outputDir)
    }
}
