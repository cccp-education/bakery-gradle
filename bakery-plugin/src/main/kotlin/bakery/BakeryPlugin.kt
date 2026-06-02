package bakery

import bakery.FileSystemManager.from
import bakery.FileSystemManager.yamlMapper
import bakery.SiteManager.BAKERY_GROUP
import bakery.SiteManager.configureBakeTask
import bakery.SiteManager.configureConfigPath
import bakery.SiteManager.configureJBakePlugin
import bakery.SiteManager.createJBakeRuntimeConfiguration
import bakery.SiteManager.registerCollectRelatedArticlesTask
import bakery.SiteManager.registerCollectSiteConfigTask
import bakery.SiteManager.registerCollectSiteContextTask
import bakery.SiteManager.registerDeployMaquetteTask
import bakery.SiteManager.registerDeployProfileTask
import bakery.SiteManager.registerDeploySiteTask
import bakery.SiteManager.registerGenerateArticleTask
import bakery.SiteManager.registerGenerateSiteTask
import bakery.SiteManager.registerInjectRelatedArticlesTask
import bakery.SiteManager.registerPagefindTask
import bakery.SiteManager.registerServeTask
import bakery.SiteManager.registerUtilityTasks
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
                project.registerGenerateSiteTask(targetDir, siteType)
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
                project.registerCollectSiteContextTask(site)
                project.registerCollectRelatedArticlesTask(site)
                project.registerInjectRelatedArticlesTask(site)
                project.registerGenerateArticleTask(site, bakeryExtension.ia, bakeryExtension.articleIntention)
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