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

@DisableCachingByDefault(because = "Migration contenu i18n — résultat non-déterministe (LLM), non-cacheable")
abstract class MigrateContentI18nTask : DefaultTask() {

    @get:Internal
    var translationService: TranslationService? = null

    @get:Internal
    var contentRootDir: File? = null

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

    init {
        group = BakeryConstants.TRANSFORM_GROUP
        description = "Migre le contenu AsciiDoc d'un site bakery vers l'i18n — copie le contenu source, traduit les fichiers .adoc, preserve les fichiers non-adoc"
        contentI18nSource.convention("")
        contentI18nOutput.convention("")
        contentI18nTargetLangs.convention("")
        contentI18nSourceLang.convention("")
        contentI18nDryRun.convention("")
    }

    @TaskAction
    fun executeContentMigration() {
        val intention = resolveIntention()

        logger.lifecycle("[migrateContentI18n] Source : {}", intention.sourceDir)
        logger.lifecycle("[migrateContentI18n] Sortie : {}", intention.outputDir)
        logger.lifecycle("[migrateContentI18n] Langue source : {}", intention.sourceLanguage)
        logger.lifecycle("[migrateContentI18n] Langues cibles : {}", intention.targetLanguages.joinToString(", "))
        logger.lifecycle("[migrateContentI18n] Dry-run : {}", intention.dryRun)

        val sourceDir = resolveSourceDir(intention)
        if (!sourceDir.exists()) {
            logger.warn("[migrateContentI18n] Le répertoire source n'existe pas : {}", sourceDir.absolutePath)
            return
        }

        val outputBaseDir = resolveOutputDir(intention)

        val copyService = ContentCopyService()
        val copyResult = copyService.copy(
            sourceDir = sourceDir,
            outputBaseDir = outputBaseDir,
            targetLanguages = intention.targetLanguages,
            dryRun = intention.dryRun,
            excludeRelativePaths = intention.excludePaths.toSet()
        )
        logger.lifecycle("[migrateContentI18n] Fichiers copiés : {}", copyResult.filesCopied.size)

        if (intention.dryRun) {
            logger.lifecycle("[migrateContentI18n] DRY-RUN — aucun fichier modifié.")
            return
        }

        val translationService = this.translationService
        if (translationService == null) {
            logger.lifecycle("[migrateContentI18n] Aucun service de traduction — les fichiers sont copiés sans traduction.")
            return
        }

        for (targetLang in intention.targetLanguages) {
            val langDir = outputBaseDir.resolve(targetLang)
            if (!langDir.exists()) continue

            val contentService = ContentTranslationService(translationService, parallelism = intention.parallelism)
            val translationResult = contentService.translate(
                langDir = langDir,
                sourceLanguage = intention.sourceLanguage,
                targetLanguage = targetLang
            )
            logger.lifecycle("[migrateContentI18n] [{}] Fichiers traduits : {}, erreurs : {}",
                targetLang, translationResult.filesTranslated.size, translationResult.errors.size)
        }
    }

    internal fun resolveIntention(): ContentMigrationIntention {
        val resolvedSource = ResolveIntention.fromCliRequired(
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

        val resolvedOutput = ResolveIntention.fromCliRequired(
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

        val resolvedTargetLangs = ResolveIntention.fromCliList(
            contentI18nTargetLangs.orNull,
            dslIntention?.targetLanguages,
            listOf("en"),
        )

        val resolvedSourceLang = ResolveIntention.fromCli(
            contentI18nSourceLang.orNull,
            dslIntention?.sourceLanguage,
            "fr",
        )

        val resolvedDryRun = ResolveIntention.fromCliBoolean(
            contentI18nDryRun.orNull,
            dslIntention?.dryRun,
            true,
        )

        val resolvedExclude = dslIntention?.excludePaths ?: emptyList()

        return ContentMigrationIntention(
            sourceDir = resolvedSource,
            outputDir = resolvedOutput,
            sourceLanguage = resolvedSourceLang,
            targetLanguages = resolvedTargetLangs,
            dryRun = resolvedDryRun,
            excludePaths = resolvedExclude,
            parallelism = dslIntention?.parallelism ?: 1
        )
    }

    private fun resolveSourceDir(intention: ContentMigrationIntention): File {
        val root = contentRootDir ?: return File(intention.sourceDir)
        val candidate = File(intention.sourceDir)
        return if (candidate.isAbsolute) candidate else root.resolve(intention.sourceDir)
    }

    private fun resolveOutputDir(intention: ContentMigrationIntention): File {
        val candidate = File(intention.outputDir)
        return if (candidate.isAbsolute) candidate else File(project.projectDir, intention.outputDir)
    }
}
