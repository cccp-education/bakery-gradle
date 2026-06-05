package bakery

import bakery.ConfigPrompts.getOrPrompt
import bakery.ConfigPrompts.saveConfiguration
import bakery.injection.configInjectors
import bakery.injection.updateProperty
import bakery.FileSystemManager.copyResourceDirectory
import bakery.FileSystemManager.from
import bakery.FileSystemManager.yamlMapper
import bakery.GitService.GIT_ATTRIBUTES_CONTENT
import com.github.gradle.node.npm.task.NpxTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.JavaExec
import org.jbake.gradle.JBakeExtension
import org.jbake.gradle.JBakePlugin
import org.jbake.gradle.JBakeTask
import java.io.File
import java.io.File.separator
import kotlin.text.Charsets.UTF_8

object SiteTaskRegistrar {

    /**
     * Résoud le chemin de ressource JBake en fonction du type de site.
     */
    private fun resourcePathForType(siteType: SiteType): String = when (siteType) {
        SiteType.BLOG -> "site"
        SiteType.BASIC -> "site-basic"
    }

    /**
     * Résoud la description par défaut pour site.yml selon le type.
     */
    private fun defaultSiteDescription(siteType: SiteType): String = when (siteType) {
        SiteType.BLOG -> "Blog JBake avec articles, tags et archives"
        SiteType.BASIC -> "Site statique minimal"
    }

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
                    val resourcePath = resourcePathForType(siteType)
                    val props = extension?.let { ConfigResolver.loadProperties(this@registerGenerateSiteTask) } ?: emptyMap()
                    targetDir
                        .resolve("site.yml")
                        .apply { if (!exists()) createAndConfigureSiteYml(targetDir, siteType) }
                        .run {
                            setupGitIgnore(targetDir)
                            setupGitAttributes(targetDir)
                            copySiteResources(targetDir, this, resourcePath, siteType, extension, props)
                        }
                }
            }
        }
    }

    private fun Project.createAndConfigureSiteYml(
        targetDir: File,
        siteType: SiteType = SiteType.BLOG
    ): File = targetDir
        .resolve("site.yml").apply {
            createNewFile()
            logger.lifecycle("create config file.")
            SiteConfiguration(
                bake = BakeConfiguration(resourcePathForType(siteType), "bake"),
                pushPage = GitPushConfiguration(from = resourcePathForType(siteType), to = "cvs"),
                pushMaquette = GitPushConfiguration(from = "maquette", to = "cvs")
            ).run(yamlMapper::writeValueAsString)
                .run(::writeText)
            logger.lifecycle("write config file.")
        }


    private fun Project.setupGitIgnore(targetDir: File) {
        targetDir.resolve(".gitignore").run {
            if (!exists()) {
                createNewFile()
                writeText(
                    ".gradle\nbuild\n.kotlin\nsite.yml\n.idea\n" +
                            "*.iml\n*.ipr\n*.iws\nlocal.properties\n"
                )
            } else if (!readText(UTF_8).contains("site.yml")) {
                appendText("\nsite.yml\n", UTF_8)
            }
        }
    }

    private fun Project.setupGitAttributes(targetDir: File) {
        targetDir.resolve(".gitattributes").run {
            if (!exists()) {
                createNewFile()
                writeText(GIT_ATTRIBUTES_CONTENT.trimIndent(), UTF_8)
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
        // Copier le répertoire de ressources spécifique au type de site
        copyResourceDirectory(resourcePath, targetDir, project)
        // Copier le répertoire maquette (partagé entre tous les types)
        copyResourceDirectory(site.pushMaquette.from, targetDir, project)

        // Resolve all 8 configs through ConfigResolver 4-layer cascade:
        // CLI (-P) > gradle.properties > DSL (BakeryExtension) > site.yml (YAML) > defaults
        val ext = extension ?: BakeryExtension(project.objects)
        val (resolvedConfigs, _) = ConfigResolver.resolveAll(props, ext, site)

        injectResolvedConfigIntoJbakeProperties(targetDir, site, resolvedConfigs.toResolver())

        // BKY-LENS-3: Inject augmented context budget into jbake.properties
        if (ext.augmentedContext.enabled) {
            injectLensBudgetIntoJbakeProperties(targetDir, site, ext.augmentedContext)
        }

        logger.lifecycle("✓ Site scaffolded (type: ${siteType.alias}) from resource: $resourcePath")
    }

    /**
     * Generic injection of resolved config properties into jbake.properties.
     * Uses the Strategy pattern: each domain has its own ConfigInjector.
     */
    private fun Project.injectResolvedConfigIntoJbakeProperties(
        targetDir: File,
        site: SiteConfiguration,
        resolver: (key: String, existingValue: String) -> String
    ) {
        val jbakeProps = targetDir.resolve(site.bake.srcPath)
            .resolve("jbake.properties")
        if (!jbakeProps.exists()) {
            logger.warn("jbake.properties not found at ${jbakeProps.absolutePath}")
            return
        }
        val lines = jbakeProps.readText(UTF_8).lines().toMutableList()

        configInjectors.values.forEach { it.inject(lines, resolver) }

        jbakeProps.writeText(lines.joinToString("\n"), UTF_8)
        logger.lifecycle("✓ Injected resolved config into jbake.properties")
    }

    /**
     * BKY-LENS-3: Inject LensBudget properties into jbake.properties.
     *
     * Injecte :
     * - augmentedContextEnabled=true
     * - lensBudgetMaxArticlesPerPage=4
     * - lensBudgetMinSimilarity=0.7
     * - augmentedContextData (JSON du fichier augmented-context.json, si présent)
     */
    private fun Project.injectLensBudgetIntoJbakeProperties(
        targetDir: File,
        site: SiteConfiguration,
        augmentedContext: bakery.lens.AugmentedContextDsl
    ) {
        val jbakeProps = targetDir.resolve(site.bake.srcPath)
            .resolve("jbake.properties")
        if (!jbakeProps.exists()) {
            logger.warn("jbake.properties not found at ${jbakeProps.absolutePath}")
            return
        }
        val lines = jbakeProps.readText(UTF_8).lines().toMutableList()

        updateProperty(lines, "augmentedContextEnabled", augmentedContext.enabled.toString())
        updateProperty(lines, "lensBudgetMaxArticlesPerPage", augmentedContext.budget.maxArticlesPerPage.toString())
        updateProperty(lines, "lensBudgetMinSimilarity", augmentedContext.budget.minSimilarity.toString())

        // Inject augmentedContextData from the augmented-context.json file (if baked)
        val augmentedContextFile = projectDir.resolve("build/bakery/augmented-context.json")
        if (augmentedContextFile.exists()) {
            val contextData = escapeJsonForJavaProperties(
                augmentedContextFile.readText(UTF_8)
            )
            updateProperty(lines, "augmentedContextData", contextData)
        } else {
            logger.info("[BakeryPlugin] augmentedContextData: file not found at ${augmentedContextFile.absolutePath}, skipping injection")
            updateProperty(lines, "augmentedContextData", "")
        }

        jbakeProps.writeText(lines.joinToString("\n"), UTF_8)
        logger.lifecycle("✓ Injected LensBudget config into jbake.properties (maxArticlesPerPage=${augmentedContext.budget.maxArticlesPerPage}, minSimilarity=${augmentedContext.budget.minSimilarity})")
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
            .getByName(BakeryConstants.BAKE_TASK).apply {
                group = BakeryConstants.GENERATE_GROUP
                input = file(site.bake.srcPath)
                output = layout.buildDirectory
                    .dir(site.bake.destDirPath)
                    .get()
                    .asFile
            }
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
                    logger.lifecycle("  Token: ${if (token.isNotEmpty()) "***configured***" else "not set"}")

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
                    "Serving $group at: https://localhost:8820/"
                        .apply(logger::info)
                        .run(::println)
                }
            }
        }
    }
}
