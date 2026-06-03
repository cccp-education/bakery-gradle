package bakery

import bakery.FileSystemManager.from
import bakery.FileSystemManager.yamlMapper
import bakery.BakeryConstants.BAKERY_GROUP
import bakery.SiteManager.configureConfigPath
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
import java.io.File


class BakeryPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.apply("com.github.node-gradle.node")
        val jbakeRuntime = project.createJBakeRuntimeConfiguration()
        val bakeryExtension = project.extensions.create(
            BAKERY_GROUP,
            BakeryExtension::class.java
        )
        val isGradlePropertiesEnabled = bakeryExtension.configPath.isPresent

        project.afterEvaluate {
            project.configureConfigPath(bakeryExtension, isGradlePropertiesEnabled)
            val configFile = project.layout
                .projectDirectory.asFile
                .toPath()
                .resolve(bakeryExtension.configPath.get())
                .toFile()
            val configDir = configFile.parentFile

            if (!configFile.exists() || (configFile.exists() &&
                        yamlMapper.readValue<SiteConfiguration>(configFile).run {
                            !configDir.resolve(bake.srcPath).exists() &&
                                    !configDir.resolve(pushMaquette.from).exists()
                        })
            ) {
                "config file does not exists or site and maquette directories do not exist."
                    .apply(::println)
                    .let(project.logger::info)
                val targetDir = SiteScaffolder.resolveSiteTargetDir(
                    bakeryExtension,
                    project.projectDir
                )
                if (targetDir != project.projectDir) {
                    SiteScaffolder.validateSiteTargetDoesNotExist(targetDir)
                }
                val siteType = SiteScaffolder.resolveSiteType(bakeryExtension)
                project.registerGenerateSiteTask(targetDir, siteType, bakeryExtension)
                project.registerGenerateSiteFromIntentionTask(
                    targetDir, bakeryExtension.ia, bakeryExtension.scaffoldIntention
                )
            } else {
                val rawSite = project.from(bakeryExtension.configPath.get())
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
    }
}

private fun SiteConfiguration.resolvePaths(base: File): SiteConfiguration = copy(
    bake = bake.copy(srcPath = resolvePath(base, bake.srcPath)),
    pushMaquette = pushMaquette.copy(from = resolvePath(base, pushMaquette.from)),
    pushPage = pushPage.copy(from = resolvePath(base, pushPage.from)),
    pushProfile = pushProfile?.copy(from = resolvePath(base, pushProfile.from)),
)

private fun resolvePath(base: File, path: String): String =
    if (path.isBlank() || File(path).isAbsolute) path else base.resolve(path).absolutePath