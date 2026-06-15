package bakery

import bakery.FileSystemManager.from
import bakery.FileSystemManager.yamlMapper
import bakery.BakeryConstants.BAKERY_GROUP
import bakery.SiteManager.configureConfigPath
import bakery.SiteScaffolder.resolveAndValidateSiteTarget
import bakery.SiteManager.createJBakeRuntimeConfiguration
import bakery.SiteManager.registerUtilityTasks
import bakery.SiteTaskRegistrar.configureBakeTask
import bakery.SiteTaskRegistrar.configureJBakePlugin
import bakery.SiteTaskRegistrar.registerCollectSiteConfigTask
import bakery.SiteTaskRegistrar.registerGenerateSiteTask
import bakery.SiteTaskRegistrar.registerPagefindTask
import bakery.SiteTaskRegistrar.registerServeTask
import bakery.DeployTaskRegistrar.registerDeployMaquetteTask
import bakery.DeployTaskRegistrar.registerDeployProfileTask
import bakery.DeployTaskRegistrar.registerDeploySiteTask
import bakery.LensTaskRegistrar.registerCollectAugmentedContextTask
import bakery.LensTaskRegistrar.registerCollectSiteContextTask
import bakery.ContentTaskRegistrar.registerGenerateArticleTask
import bakery.ContentTaskRegistrar.registerGenerateSiteFromIntentionTask
import bakery.ContentTaskRegistrar.registerGenerateThemeTask
import bakery.ContentTaskRegistrar.registerValidateFirebaseConfigTask
import bakery.llm.OllamaLlmService
import com.fasterxml.jackson.module.kotlin.readValue
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import arrow.core.Either
import java.io.File

internal fun Project.registerVerifyConfigurationMappingTask(configPath: String) {
    tasks.register("verifyConfigurationMapping", VerifyConfigurationMappingTask::class.java) { task ->
        task.group = "verification"
        task.description = "Valide le mapping YAML site.yml → SiteConfiguration et masque les secrets"
        task.configPath.set(configPath)
    }
}


class BakeryPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.apply("com.github.node-gradle.node")
        val jbakeRuntime = project.createJBakeRuntimeConfiguration()
        val bakeryExtension = project.extensions.create(
            BAKERY_GROUP,
            BakeryExtension::class.java
        )

        project.afterEvaluate {
            // CS-FIN-1 (CS-16) — `isGradlePropertiesEnabled` doit être évalué ICI,
            // après que le DSL utilisateur ait été appliqué, sinon il reste
            // toujours `false` et `configureConfigPath` peut écraser silencieusement
            // la config DSL via gradle.properties.
            val isGradlePropertiesEnabled = bakeryExtension.configPath.isPresent

            project.configureConfigPath(bakeryExtension, isGradlePropertiesEnabled)
                .fold(
                    ifLeft = { project.logger.info("[bakery] $it") },
                    ifRight = { /* configPath loaded from gradle.properties — logged in SiteManager */ }
                )

            // US-61a — verifyConfigurationMapping est enregistrée AVANT le
            // branchement scaffold/fullPipeline pour être disponible dans les
            // deux modes. Si configPath est absent, on n'enregistre pas la tâche
            // (elle n'a pas de fichier à vérifier).
            if (bakeryExtension.configPath.isPresent) {
                project.registerVerifyConfigurationMappingTask(
                    bakeryExtension.configPath.get()
                )
            }

            // CS-FIN-1 (CS-21) — Guard `isPresent` avant `get()`.
            // Si l'utilisateur n'a défini `configPath` NI dans le DSL, NI dans
            // `gradle.properties`, NI via `-P`, on ne peut pas exécuter le
            // pipeline JBake. On bascule en mode "scaffold only" (tâches de
            // génération de site) et on prévient l'utilisateur.
            if (!bakeryExtension.configPath.isPresent) {
                project.logger.warn(
                    "[bakery] configPath is not set — only scaffold tasks will be available. " +
                        "Define it via DSL (`bakery { configPath = \"site.yml\" }`), " +
                        "`gradle.properties` (`bakery.config.path=site.yml`), or `-Pbakery.config.path=...`."
                )
                registerScaffoldOnlyTasks(project, bakeryExtension, jbakeRuntime)
                return@afterEvaluate
            }

            val configFile = project.layout
                .projectDirectory.asFile
                .toPath()
                .resolve(bakeryExtension.configPath.get())
                .toFile()
            val configDir = configFile.parentFile

            if (!configFile.exists()) {
                project.logger.lifecycle("Config file does not exist at $configFile — switching to scaffold only.")
                registerScaffoldOnlyTasks(project, bakeryExtension, jbakeRuntime)
            } else {
                val parseResult = Either.catch {
                    yamlMapper.readValue<SiteConfiguration>(configFile)
                }
                parseResult.fold(
                    ifLeft = { error ->
                        project.logger.warn(
                            "Failed to parse configuration file '${configFile.absolutePath}': ${error.message}. " +
                                "Falling back to scaffold only."
                        )
                        registerScaffoldOnlyTasks(project, bakeryExtension, jbakeRuntime)
                    },
                    ifRight = { site ->
                        if (!configDir.resolve(site.bake.srcPath).exists() &&
                            !configDir.resolve(site.pushMaquette.from).exists()
                        ) {
                            project.logger.lifecycle(
                                "Site and maquette directories do not exist — switching to scaffold only."
                            )
                            registerScaffoldOnlyTasks(project, bakeryExtension, jbakeRuntime)
                        } else {
                            registerFullPipelineTasks(project, bakeryExtension, jbakeRuntime, isGradlePropertiesEnabled)
                        }
                    }
                )
            }
        }
    }

    /**
     * CS-FIN-1 — Enregistre uniquement les tâches de scaffold (generateSite,
     * generateSiteFromIntention). Utilisé quand :
     * . configPath est absent (mode dégradé — l'utilisateur doit configurer)
     * . configFile pointe vers un fichier inexistant (projet neuf à scaffolder)
     */
    private fun registerScaffoldOnlyTasks(
        project: Project,
        bakeryExtension: BakeryExtension,
        @Suppress("UNUSED_PARAMETER") jbakeRuntime: Configuration
    ) {
        val targetDir = project.resolveAndValidateSiteTarget()
        val siteType = SiteScaffolder.resolveSiteType(bakeryExtension)
        project.registerGenerateSiteTask(targetDir, siteType, bakeryExtension)
        project.registerGenerateSiteFromIntentionTask(
            targetDir, bakeryExtension.ia, bakeryExtension.scaffoldIntention
        )
    }

    /**
     * Pipeline complet : JBake + déploiement + utilities. Utilisé quand un
     * fichier de configuration valide pointe vers un site existant.
     *
     * Tâches indépendantes (parallélisables par Gradle, pas de `dependsOn`) :
     * - collectSiteContext ∥ generateTheme ∥ generateArticle
     * - deployMaquette ∥ deployProfile
     * - pagefind ∥ validateFirebaseConfig ∥ serve
     *
     * Chaîne séquentielle déjà configurée via `dependsOn` :
     * bake → pagefind → deploySite → deploySite (via registerDeploySiteTask)
     */
    private fun registerFullPipelineTasks(
        project: Project,
        bakeryExtension: BakeryExtension,
        jbakeRuntime: Configuration,
        isGradlePropertiesEnabled: Boolean
    ) {
        val configDir = project.layout
            .projectDirectory.asFile
            .toPath()
            .resolve(bakeryExtension.configPath.get())
            .toFile()
            .parentFile
        val rawSite = project.from(bakeryExtension.configPath.get())
            .fold(
                { project.logger.warn("Failed to read site config, using defaults: ${it.message}"); SiteConfiguration() },
                { it }
            )
        val site = rawSite.resolvePaths(configDir)
        project.configureJBakePlugin(site)
        project.configureBakeTask(site)
        project.registerDeploySiteTask(site)
        project.registerDeployMaquetteTask(site)
        project.registerPagefindTask(site)
        if (site.pushProfile != null) {
            project.registerDeployProfileTask(site)
        }
        project.registerServeTask(site, jbakeRuntime)
        project.registerUtilityTasks()
        project.registerCollectSiteConfigTask(site, isGradlePropertiesEnabled)
        project.registerCollectSiteContextTask(site, bakeryExtension.augmentedContext)
        project.registerCollectAugmentedContextTask(site, bakeryExtension.augmentedContext)
        project.registerGenerateArticleTask(site, bakeryExtension.ia, bakeryExtension.articleIntention)
        project.registerGenerateThemeTask(site, bakeryExtension.themeIntention)
        project.registerValidateFirebaseConfigTask(site, bakeryExtension.ia, bakeryExtension.firebaseAuth)
    }
}

internal fun SiteConfiguration.resolvePaths(base: File): SiteConfiguration = copy(
    bake = bake.copy(srcPath = resolvePath(base, bake.srcPath)),
    pushMaquette = pushMaquette.copy(from = resolvePath(base, pushMaquette.from)),
    pushPage = pushPage.copy(from = resolvePath(base, pushPage.from)),
    pushProfile = pushProfile?.copy(from = resolvePath(base, pushProfile.from)),
)

internal fun resolvePath(base: File, path: String): String =
    if (path.isBlank() || File(path).isAbsolute) path else base.resolve(path).absolutePath