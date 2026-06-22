package bakery.i18n

import bakery.BakeryConstants
import bakery.intention.ResolveIntention
import bakery.intention.ResolveIntentionError
import bakery.llm.LlmService
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

@DisableCachingByDefault(because = "Migration i18n — résultat non-déterministe (LLM), non-cacheable")
abstract class MigrateToI18nTask : DefaultTask() {

    @get:Internal
    var llmService: LlmService? = null

    @get:Internal
    var translationService: TranslationService? = null

    @get:Internal
    var contentRootDir: File? = null

    @get:Internal
    var dslIntention: I18nMigrationIntention? = null

    @get:Input
    @get:Optional
    @get:Option(option = "i18nSite", description = "Répertoire du site à migrer vers i18n")
    abstract val i18nSite: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "i18nLangs", description = "Langues cibles séparées par virgules (ex: en,ar,zh)")
    abstract val i18nLangs: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "i18nDefaultLang", description = "Langue par défaut (ex: fr)")
    abstract val i18nDefaultLang: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "i18nDryRun", description = "Mode dry-run (true/false) — prévisualise sans écrire")
    abstract val i18nDryRun: Property<String>

    init {
        group = BakeryConstants.TRANSFORM_GROUP
        description = "Migre un site bakery existant vers l'i18n — scanne les templates, extrait le texte hardcodé, génère messages_{lang}.properties"
        i18nSite.convention("")
        i18nLangs.convention("")
        i18nDefaultLang.convention("")
        i18nDryRun.convention("")
    }

    @TaskAction
    fun executeMigration() {
        val intention = resolveIntention()

        logger.lifecycle("[migrateToI18n] Site : {}", intention.siteDir)
        logger.lifecycle("[migrateToI18n] Langues : {}", intention.languages.joinToString(", "))
        logger.lifecycle("[migrateToI18n] Langue par défaut : {}", intention.defaultLanguage)
        logger.lifecycle("[migrateToI18n] Dry-run : {}", intention.dryRun)

        val siteDir = resolveSiteDir(intention)
        if (!siteDir.exists()) {
            logger.warn("[migrateToI18n] Le répertoire du site n'existe pas : {}", siteDir.absolutePath)
            return
        }

        val templatesDir = siteDir.resolve("templates")
        if (!templatesDir.exists()) {
            logger.warn("[migrateToI18n] Aucun répertoire templates trouvé dans {}", siteDir.absolutePath)
            return
        }

        val migrationService = I18nMigrationService(translationService)
        val result = migrationService.migrate(
            siteDir = siteDir,
            languages = intention.languages,
            defaultLanguage = intention.defaultLanguage,
            dryRun = intention.dryRun
        )

        logger.lifecycle("[migrateToI18n] Clés extraites : {}", result.keysExtracted)
        logger.lifecycle("[migrateToI18n] Fichiers générés : {}", result.filesGenerated)
        if (intention.dryRun) {
            logger.lifecycle("[migrateToI18n] DRY-RUN — aucun fichier modifié.")
        }
    }

    internal fun resolveIntention(): I18nMigrationIntention {
        val resolvedSiteDir = ResolveIntention.fromCliRequired(
            i18nSite.orNull,
            dslIntention?.siteDir,
            ResolveIntentionError.MissingRequiredField(
                cliFlag = "--i18nSite",
                dslPath = "bakery { i18nMigration { siteDir = \"...\" } }",
            ),
        ).fold(
            ifLeft = { throw it.toException() },
            ifRight = { it },
        )

        val resolvedLangs = ResolveIntention.fromCliList(
            i18nLangs.orNull,
            dslIntention?.languages,
            listOf("en"),
        )

        val resolvedDefaultLang = ResolveIntention.fromCli(
            i18nDefaultLang.orNull,
            dslIntention?.defaultLanguage,
            "fr",
        )

        val resolvedDryRun = ResolveIntention.fromCliBoolean(
            i18nDryRun.orNull,
            dslIntention?.dryRun,
            true,
        )

        return I18nMigrationIntention(
            siteDir = resolvedSiteDir,
            languages = resolvedLangs,
            defaultLanguage = resolvedDefaultLang,
            dryRun = resolvedDryRun
        )
    }

    private fun resolveSiteDir(intention: I18nMigrationIntention): File {
        val root = contentRootDir
            ?: return File(intention.siteDir)

        val candidate = File(intention.siteDir)
        return if (candidate.isAbsolute) candidate else root.resolve(intention.siteDir)
    }
}
