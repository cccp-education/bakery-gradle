package bakery

import arrow.core.Either
import bakery.ConfigPrompts.getOrPrompt
import bakery.ConfigPrompts.saveConfiguration
import bakery.FileSystemManager.copyResourceDirectory
import bakery.FileSystemManager.from
import com.github.gradle.node.npm.task.NpxTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.JavaExec
import org.jbake.gradle.JBakeExtension
import org.jbake.gradle.JBakePlugin
import org.jbake.gradle.JBakeTask
import java.io.File

object SiteTaskRegistrar {

    fun Project.registerGenerateSiteTask(
        siteTargetDir: File = projectDir,
        siteType: SiteType = SiteType.BLOG,
        extension: BakeryExtension? = null
    ) {
        tasks.register("generateSite") { task ->
            task.apply {
                group = BakeryConstants.GENERATE_GROUP
                description = "Initialise site and maquette folders (type: ${siteType.alias})."

                doLast {
                    val targetDir = siteTargetDir.also { it.mkdirs() }
                    val resourcePath = bakery.site.GenerateSiteService.resourcePathForType(siteType)
                    val props = extension?.let { ConfigResolver.loadProperties(this@registerGenerateSiteTask) } ?: emptyMap()
                    val siteYmlFile = targetDir.resolve("site.yml")
                    if (!siteYmlFile.exists()) {
                        bakery.site.GenerateSiteService.createAndConfigureSiteYml(siteYmlFile, siteType)
                    }
                    bakery.site.GenerateSiteService.setupGitIgnore(targetDir)
                    bakery.site.GenerateSiteService.setupGitAttributes(targetDir)
                    copySiteResources(targetDir, siteYmlFile, resourcePath, siteType, extension, props)
                }
            }
        }
    }

    private fun Project.copySiteResources(
        targetDir: File,
        configFile: File,
        resourcePath: String = "site",
        siteType: SiteType = SiteType.BLOG,
        extension: BakeryExtension? = null,
        props: Map<String, String> = emptyMap()
    ) {
        val site = from(configFile.absolutePath)
            .fold(
                { logger.warn("Failed to read site config, using defaults: ${it.message}"); SiteConfiguration() },
                { it }
            )
        val result1 = copyResourceDirectory(resourcePath, targetDir, project)
        if (result1 is Either.Left) {
            logger.error("Failed to copy resource '$resourcePath': ${result1.value}")
            throw IllegalStateException(result1.value)
        }
        val result2 = copyResourceDirectory(site.pushMaquette.from, targetDir, project)
        if (result2 is Either.Left) {
            logger.error("Failed to copy maquette '${site.pushMaquette.from}': ${result2.value}")
            throw IllegalStateException(result2.value)
        }

        val ext = extension ?: BakeryExtension(project.objects)
        val (resolvedConfigs, _) = ConfigResolver.resolveAll(props, ext, site)

        bakery.site.GenerateSiteService.injectConfigIntoJbakeProperties(
            targetDir, site, resolvedConfigs, ext.augmentedContext.takeIf { it.enabled }
        )

        logger.lifecycle("✓ Site scaffolded (type: ${siteType.alias}) from resource: $resourcePath")
    }

    internal fun Project.configureJBakePlugin(site: SiteConfiguration) {
        plugins.apply(JBakePlugin::class.java)

        extensions.configure(JBakeExtension::class.java) {
            it.srcDirName = site.bake.srcPath
            it.destDirName = site.bake.destDirPath
            it.configuration[BakeryConstants.ASCIIDOCTOR_OPTION_REQUIRES] = BakeryConstants.ASCIIDOCTOR_DIAGRAM
            it.configuration[BakeryConstants.ASCIIDOC_ATTRIBUTES_PROP] = arrayOf(
                "${BakeryConstants.ASCIIDOC_SOURCE_DIR}=${project.projectDir.resolve(site.bake.srcPath)}",
                BakeryConstants.ASCIIDOC_DIAGRAMS_DIRECTORY,
            )
        }
    }

    internal fun Project.configureBakeTask(site: SiteConfiguration) {
        tasks.withType(JBakeTask::class.java)
            .findByName(BakeryConstants.BAKE_TASK)?.apply {
                group = BakeryConstants.GENERATE_GROUP
                input = file(site.bake.srcPath)
                output = layout.buildDirectory
                    .dir(site.bake.destDirPath)
                    .get()
                    .asFile
                doFirst {
                    val tree = site.tree
                    if (tree != null) {
                        val srcDir = file(site.bake.srcPath)
                        bakery.tree.TreeBakeService.injectTreeConfig(tree, srcDir)
                    }
                }
            } ?: logger.warn("[bakery] bake task not found — JBake task configuration skipped. Is the JBake plugin applied?")
    }

    internal fun Project.registerPagefindTask(site: SiteConfiguration) {
        tasks.register("pagefind", NpxTask::class.java) { task ->
            task.apply {
                group = BakeryConstants.TRANSFORM_GROUP
                description = "Index the baked site with Pagefind for full-text search."
                dependsOn(BakeryConstants.BAKE_TASK)

                command.set("pagefind")
                args.addAll(
                    "--site", layout.buildDirectory.get().asFile.resolve(site.bake.destDirPath).absolutePath
                )

                doFirst {
                    val bakedDir = layout.buildDirectory.get().asFile.resolve(site.bake.destDirPath)
                    if (!bakedDir.exists() || bakedDir.listFiles().isNullOrEmpty()) {
                        throw IllegalStateException("Baked directory is empty or does not exist at ${bakedDir.absolutePath}")
                    }
                }
            }
        }
    }

    internal fun Project.registerCollectSiteConfigTask(
        site: SiteConfiguration,
        isGradlePropertiesEnabled: Boolean
    ) {
        tasks.register("collectSiteConfig") { task ->
            task.apply {
                group = BakeryConstants.COLLECT_GROUP
                description = "Initialize Bakery configuration."

                doLast {
                    val token = getOrPrompt(
                        propertyName = "GitHub Token",
                        cliProperty = "githubToken",
                        sensitive = true
                    )
                    val username = getOrPrompt(
                        propertyName = "GitHub Username",
                        cliProperty = "githubUsername",
                        sensitive = false
                    )
                    val repo = getOrPrompt(
                        propertyName = "GitHub Repository URL",
                        cliProperty = "githubRepo",
                        sensitive = false,
                        example = "https://github.com/username/repo.git"
                    )
                    val configPath = getOrPrompt(
                        propertyName = "Config Path",
                        cliProperty = "configPath",
                        sensitive = false,
                        example = "site.yml",
                        default = "site.yml"
                    )
                    logger.lifecycle("✓ Bakery configuration completed:")
                    logger.lifecycle("  Username: $username")
                    logger.lifecycle("  Repository: $repo")
                    logger.lifecycle("  Config Path: $configPath")
                    logger.lifecycle("  Token: ${maskSecret(SecretField.Token(token))}")

                    val siteYmlFile = file(configPath)
                    projectDir.saveConfiguration(site, siteYmlFile, username, repo, token)

                    logger.lifecycle("")
                    logger.lifecycle("✓ Configuration saved to ${siteYmlFile.absolutePath}")
                    logger.lifecycle("  You can now run: ./gradlew bake")
                }
            }
        }
    }

    internal fun Project.registerServeTask(
        site: SiteConfiguration,
        jbakeRuntime: Configuration
    ) {
        tasks.register("serve", JavaExec::class.java) { task ->
            task.apply {
                group = BakeryConstants.INFO_GROUP
                description = "Serves the baked site locally."
                mainClass.set("org.jbake.launcher.Main")
                classpath = jbakeRuntime
                environment("GEM_PATH", jbakeRuntime.asPath)
                jvmArgs(
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.base/java.util=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                    "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED"
                )
                args = listOf(
                    file(site.bake.srcPath).absolutePath,
                    layout.buildDirectory.get()
                        .asFile.resolve(site.bake.destDirPath)
                        .absolutePath,
                    "-s"
                )

                doFirst {
                    logger.lifecycle("Serving $group at: https://localhost:8820/")
                }
            }
        }
    }
}
