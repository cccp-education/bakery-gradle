package bakery

import bakery.article.GenerateArticleTask
import bakery.llm.IaConfig
import bakery.llm.LlmService
import bakery.llm.OllamaLlmService
import bakery.scaffold.GenerateSiteFromIntentionTask
import bakery.scaffold.ScaffoldIntentionDsl
import bakery.theme.GenerateThemeTask
import bakery.theme.ThemeIntentionDsl
import bakery.firebase.ValidateFirebaseConfigTask
import bakery.i18n.I18nMigrationIntentionDsl
import bakery.i18n.LlmServiceTranslationAdapter
import bakery.i18n.MigrateToI18nTask
import org.gradle.api.Project
import java.io.File

object ContentTaskRegistrar {

    /**
     * Enregistre la tâche `generateArticle` pour un site configuré.
     *
     * @param site Configuration du site (utilise bake.srcPath pour le content root)
     * @param iaConfig Configuration IA (Ollama baseUrl, modelName) depuis `bakery { ia { ... } }`
     * @param articleIntentionDsl Configuration intention article depuis `bakery { articleIntention { ... } }`
     */
    internal fun Project.registerGenerateArticleTask(
        site: SiteConfiguration,
        iaConfig: IaConfig = IaConfig(),
        articleIntentionDsl: bakery.article.ArticleIntentionDsl? = null
    ) {
        val contentRoot = project.projectDir.resolve(site.bake.srcPath)
        tasks.register("generateArticle", GenerateArticleTask::class.java) { task ->
            task.group = BakeryConstants.GENERATE_GROUP
            task.description = "Génère un article de blog assisté IA via Ollama — injecte dans content/blog/YYYY/MM/"
            task.contentRootDir = contentRoot
            task.topic.set(project.providers.gradleProperty("topic").orElse(""))
            task.articleTon.set(project.providers.gradleProperty("articleTon").orElse(""))
            task.articleAudience.set(project.providers.gradleProperty("articleAudience").orElse(""))
            task.articleKeywords.set(project.providers.gradleProperty("articleKeywords").orElse(""))
            task.articleLang.set(project.providers.gradleProperty("articleLang").orElse("fr"))

            resolveIntention(
                dsl = articleIntentionDsl,
                isConfigured = { it.topic.isNotBlank() },
                toIntention = { it.toIntention() },
                taskLabel = "article",
                setIntention = { task.dslIntention = it }
            )

            createLlmServiceIfEnabled(iaConfig) { task.llmService = it }
            project.logger.info("[BakeryPlugin] generateArticle IA ${if (iaConfig.enabled) "activé" else "désactivé (ia.enabled = false)"}")
        }
    }

    /**
     * Enregistre la tâche `generateSiteFromIntention` pour le scaffolding IA.
     *
     * @param targetDir Répertoire cible du site
     * @param iaConfig Configuration IA (Ollama baseUrl, modelName) depuis `bakery { ia { ... } }`
     * @param scaffoldIntentionDsl Configuration intention scaffold depuis `bakery { scaffoldIntention { ... } }`
     */
    internal fun Project.registerGenerateSiteFromIntentionTask(
        targetDir: File,
        iaConfig: IaConfig = IaConfig(),
        scaffoldIntentionDsl: ScaffoldIntentionDsl? = null
    ) {
        tasks.register("generateSiteFromIntention", GenerateSiteFromIntentionTask::class.java) { task ->
            task.group = BakeryConstants.GENERATE_GROUP
            task.description = "Génère la structure d'un site assistée par IA — scaffold interactif"
            task.targetDir = targetDir
            task.scaffoldDescription.set(project.providers.gradleProperty("scaffoldDescription").orElse(""))
            task.siteType.set(project.providers.gradleProperty("siteType").orElse(""))
            task.scaffoldLang.set(project.providers.gradleProperty("scaffoldLang").orElse("fr"))
            task.projectName.set(project.providers.gradleProperty("projectName").orElse(""))

            resolveIntention(
                dsl = scaffoldIntentionDsl,
                isConfigured = { it.description.isNotBlank() },
                toIntention = { it.toIntention() },
                taskLabel = "scaffold",
                setIntention = { task.dslIntention = it }
            )

            createLlmServiceIfEnabled(iaConfig) { task.llmService = it }
            project.logger.info("[BakeryPlugin] generateSiteFromIntention IA ${if (iaConfig.enabled) "activé" else "désactivé (ia.enabled = false)"}")
        }
    }

    /**
     * Enregistre la tâche `generateTheme` pour la sélection de variante de thème.
     *
     * @param site Configuration du site (utilise bake.srcPath pour le répertoire cible)
     * @param themeIntentionDsl Configuration intention thème depuis `bakery { themeIntention { ... } }`
     */
    internal fun Project.registerGenerateThemeTask(
        site: SiteConfiguration,
        themeIntentionDsl: bakery.theme.ThemeIntentionDsl? = null
    ) {
        val contentRoot = project.projectDir.resolve(site.bake.srcPath)
        tasks.register("generateTheme", GenerateThemeTask::class.java) { task ->
            task.group = BakeryConstants.GENERATE_GROUP
            task.description = "Génère un thème à partir du catalogue — résolution variante + surcharges"
            task.targetDir = contentRoot

            task.themeVariant.set(project.providers.gradleProperty("themeVariant").orElse(""))
            task.themeDescription.set(project.providers.gradleProperty("themeDescription").orElse(""))
            task.themePrimaryColor.set(project.providers.gradleProperty("themePrimaryColor").orElse(""))
            task.themeSecondaryColor.set(project.providers.gradleProperty("themeSecondaryColor").orElse(""))
            task.themeAccentColor.set(project.providers.gradleProperty("themeAccentColor").orElse(""))
            task.themeBackgroundColor.set(project.providers.gradleProperty("themeBackgroundColor").orElse(""))
            task.themeTextColor.set(project.providers.gradleProperty("themeTextColor").orElse(""))
            task.themeFontFamily.set(project.providers.gradleProperty("themeFontFamily").orElse(""))
            task.themeHeadingFont.set(project.providers.gradleProperty("themeHeadingFont").orElse(""))

            resolveIntention(
                dsl = themeIntentionDsl,
                isConfigured = { it.description.isNotBlank() },
                toIntention = { it.toIntention() },
                taskLabel = "theme",
                setIntention = { task.dslIntention = it }
            )

            project.logger.info("[BakeryPlugin] generateTheme tâche enregistrée (variante=${themeIntentionDsl?.variant ?: "par défaut"})")
        }
    }

    /**
     * Enregistre la tâche `validateFirebaseConfig` pour la validation de la configuration Firebase.
     *
     * @param site Configuration du site (résolution ConfigResolver pour les deux configs)
     * @param iaConfig Configuration IA (injecte LLM si ia.enabled = true via DSL ou CLI)
     * @param props Properties issues de loadProperties (CLI + gradle.properties)
     */
    internal fun Project.registerValidateFirebaseConfigTask(
        site: SiteConfiguration,
        iaConfig: IaConfig = IaConfig(),
        firebaseAuthDsl: FirebaseAuthDsl = FirebaseAuthDsl(),
        props: Map<String, String> = emptyMap()
    ) {
        tasks.register("validateFirebaseConfig", ValidateFirebaseConfigTask::class.java) { task ->
            task.group = BakeryConstants.VALIDATE_GROUP
            task.description = "Valide la cohérence de la configuration Firebase (mécanique + IA optionnelle)"

            // Résoudre les configs via ConfigResolver 4-layer cascade
            val resolvedFirebaseAuth = ConfigResolver.resolveFirebaseAuthConfig(
                props, firebaseAuthDsl, site.firebaseAuth
            )
            val resolvedFirebaseContact = site.firebase

            task.resolvedAuthConfig = resolvedFirebaseAuth
            task.resolvedContactConfig = resolvedFirebaseContact

            createLlmServiceIfEnabled(iaConfig) { task.llmService = it }
            project.logger.info("[BakeryPlugin] validateFirebaseConfig IA ${if (iaConfig.enabled) "activé" else "désactivé (ia.enabled = false)"}")
        }
    }

    /**
     * Enregistre la tâche `migrateToI18n` pour la migration i18n d'un site existant.
     *
     * @param site Configuration du site (utilise bake.srcPath pour le content root)
     * @param iaConfig Configuration IA (Ollama baseUrl, modelName) depuis `bakery { ia { ... } }`
     * @param i18nMigrationDsl Configuration intention migration depuis `bakery { i18nMigration { ... } }`
     */
    internal fun Project.registerMigrateToI18nTask(
        site: SiteConfiguration,
        iaConfig: IaConfig = IaConfig(),
        i18nMigrationDsl: I18nMigrationIntentionDsl? = null
    ) {
        val contentRoot = project.projectDir.resolve(site.bake.srcPath)
        tasks.register("migrateToI18n", MigrateToI18nTask::class.java) { task ->
            task.group = BakeryConstants.TRANSFORM_GROUP
            task.description = "Migre un site bakery existant vers l'i18n — scanne les templates, extrait le texte hardcodé, génère messages_{lang}.properties"
            task.contentRootDir = contentRoot
            task.i18nSite.set(project.providers.gradleProperty("i18nSite").orElse(""))
            task.i18nLangs.set(project.providers.gradleProperty("i18nLangs").orElse(""))
            task.i18nDefaultLang.set(project.providers.gradleProperty("i18nDefaultLang").orElse(""))
            task.i18nDryRun.set(project.providers.gradleProperty("i18nDryRun").orElse(""))

            resolveIntention(
                dsl = i18nMigrationDsl,
                isConfigured = { it.siteDir.isNotBlank() },
                toIntention = { it.toIntention() },
                taskLabel = "i18nMigration",
                setIntention = { task.dslIntention = it }
            )

            createLlmServiceIfEnabled(iaConfig) { task.llmService = it }
            task.translationService = task.llmService?.let(::LlmServiceTranslationAdapter)
            project.logger.info("[BakeryPlugin] migrateToI18n IA ${if (iaConfig.enabled) "activé" else "désactivé (ia.enabled = false)"}")
        }
    }

    /** Construit un [LlmService] si l'IA est activée et l'injecte via [applyService]. */
    private fun createLlmServiceIfEnabled(
        iaConfig: IaConfig,
        applyService: (LlmService) -> Unit
    ) {
        if (iaConfig.enabled) {
            val service = OllamaLlmService.create(
                baseUrl = iaConfig.baseUrl,
                modelName = iaConfig.modelName,
                timeout = iaConfig.timeout
            )
            applyService(service)
        }
    }

    /** Résout une intention DSL avec le pattern CLI>DSL>defaults et l'injecte dans la tâche. */
    private inline fun <D, T> Project.resolveIntention(
        dsl: D?,
        isConfigured: (D) -> Boolean,
        toIntention: (D) -> T,
        taskLabel: String,
        setIntention: (T?) -> Unit
    ) {
        dsl?.let { d ->
            if (isConfigured(d)) {
                setIntention(try {
                    toIntention(d)
                } catch (e: IllegalArgumentException) {
                    logger.warn("[BakeryPlugin] Failed to parse $taskLabel intention from DSL: ${e.message}")
                    null
                })
            }
        }
    }
}
