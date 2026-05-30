package bakery

import bakery.ConfigPrompts.getOrPrompt
import bakery.ConfigPrompts.saveConfiguration
import bakery.FileSystemManager.copyResourceDirectory
import bakery.FileSystemManager.createCnameFile
import bakery.FileSystemManager.from
import bakery.FileSystemManager.isYmlUri
import bakery.FileSystemManager.yamlMapper
import bakery.GitService.GIT_ATTRIBUTES_CONTENT
import bakery.GitService.pushPages
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import com.github.gradle.node.npm.task.NpxTask
import org.gradle.api.tasks.JavaExec
import org.jbake.gradle.JBakeExtension
import org.jbake.gradle.JBakePlugin
import org.jbake.gradle.JBakeTask
import java.io.File
import java.io.File.separator
import kotlin.text.Charsets.UTF_8

object SiteManager {

    const val BAKERY_GROUP = "bakery"
    const val GENERATE_GROUP = "generate"
    const val DEPLOY_GROUP = "deploy"
    const val TRANSFORM_GROUP = "transform"
    const val INFO_GROUP = "info"
    const val COLLECT_GROUP = "collect"
    const val BAKE_TASK = "bake"
    const val ASCIIDOCTOR_OPTION_REQUIRES = "asciidoctor.option.requires"
    const val ASCIIDOCTOR_DIAGRAM = "asciidoctor-diagram"
    const val ASCIIDOC_ATTRIBUTES_PROP = "asciidoctor.attributes"
    const val ASCIIDOC_DIAGRAMS_DIRECTORY = "imagesDir=diagrams"
    const val ASCIIDOC_SOURCE_DIR = "sourceDir"
    const val BAKERY_CONFIG_PATH_KEY = "bakery.config.path"
    const val CNAME = "CNAME"


    fun Project.createJBakeRuntimeConfiguration()
            : Configuration = configurations.create("jbakeRuntime").apply {
        description = "Classpath for running Jbake core directly"
        listOf(
            "org.jbake:jbake-core:2.7.0",
            "commons-configuration:commons-configuration:1.10",
            "org.asciidoctor:asciidoctorj-diagram:3.0.1",
            "org.asciidoctor:asciidoctorj-diagram-plantuml:1.2025.3"
        ).forEach { this@createJBakeRuntimeConfiguration.dependencies.add(name, it) }
    }

    fun Project.configureConfigPath(
        bakeryExtension: BakeryExtension,
        isGradlePropertiesEnabled: Boolean
    ) = if (isGradlePropertiesEnabled) Unit
    else {
        val gradlePropertiesFile = layout.projectDirectory.asFile.resolve("gradle.properties")
        if (gradlePropertiesFile.exists())
            properties.run {
                val configPath = get(BAKERY_CONFIG_PATH_KEY)?.toString()
                if (keys.contains(BAKERY_CONFIG_PATH_KEY) &&
                    !configPath.isNullOrBlank() &&
                    configPath.isYmlUri
                ) bakeryExtension.configPath.set(configPath)
            } else logger.info(
            "Nor dsl configuration like 'bakery { configPath = file(\"site.yml\").absolutePath }\n' " +
                    "or gradle properties file found"
        )
    }


// ==================== Generate Site Task ====================

    fun Project.registerGenerateSiteTask(siteTargetDir: File = projectDir) {
        tasks.register("generateSite") { task ->
            task.apply {
                group = GENERATE_GROUP
                description = "Initialise site and maquette folders."

                doLast {
                    val targetDir = siteTargetDir.also { it.mkdirs() }
                    targetDir
                        .resolve("site.yml")
                        .apply { if (!exists()) createAndConfigureSiteYml(targetDir) }
                        .run {
                            setupGitIgnore(targetDir)
                            setupGitAttributes(targetDir)
                            copySiteResources(targetDir, this)
                        }
                }
            }
        }
    }

    private fun Project.createAndConfigureSiteYml(targetDir: File): File = targetDir
        .resolve("site.yml").apply {
            createNewFile()
            "create config file."
                .apply(::println)
                .let(logger::info)
            SiteConfiguration(
                bake = BakeConfiguration("site", "bake"),
                pushPage = GitPushConfiguration(from = "site", to = "cvs"),
                pushMaquette = GitPushConfiguration(from = "maquette", to = "cvs")
            ).run(yamlMapper::writeValueAsString)
                .run(::writeText)
            "write config file."
                .apply(::println)
                .let(project.logger::info)
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

    private fun Project.copySiteResources(targetDir: File, configFile: File) {
        val site = from(configFile.absolutePath)
        copyResourceDirectory(site.bake.srcPath, targetDir, project)
        copyResourceDirectory(site.pushMaquette.from, targetDir, project)
        injectFirebaseConfigIntoJbakeProperties(targetDir, site)
    }

    private fun Project.injectFirebaseConfigIntoJbakeProperties(targetDir: File, site: SiteConfiguration) {
        val jbakeProps = targetDir.resolve(site.bake.srcPath)
            .resolve("jbake.properties")
        if (!jbakeProps.exists()) {
            logger.warn("jbake.properties not found at ${jbakeProps.absolutePath}")
            return
        }
        val firebaseConfig = site.firebase ?: return
        val lines = jbakeProps.readText(UTF_8).lines().toMutableList()
        fun updateProperty(key: String, value: String) {
            val idx = lines.indexOfFirst { it.startsWith("$key=") }
            if (idx >= 0) {
                lines[idx] = "$key=$value"
            } else {
                lines.add("$key=$value")
            }
        }
        updateProperty("firebaseApiKey", firebaseConfig.project.apiKey)
        updateProperty("firebaseProjectId", firebaseConfig.project.projectId)
        jbakeProps.writeText(lines.joinToString("\n"), UTF_8)
        logger.lifecycle("✓ Injected Firebase config into jbake.properties")
    }

// ==================== Bakery Tasks Configuration ====================

    internal fun Project.configureJBakePlugin(site: SiteConfiguration) {
        plugins.apply(JBakePlugin::class.java)

        extensions.configure(JBakeExtension::class.java) {
            it.srcDirName = site.bake.srcPath
            it.destDirName = site.bake.destDirPath
            it.configuration[ASCIIDOCTOR_OPTION_REQUIRES] = ASCIIDOCTOR_DIAGRAM
            it.configuration[ASCIIDOC_ATTRIBUTES_PROP] = arrayOf(
                "$ASCIIDOC_SOURCE_DIR=${project.projectDir.resolve(site.bake.srcPath)}",
                ASCIIDOC_DIAGRAMS_DIRECTORY,
            )
        }
    }

    internal fun Project.configureBakeTask(site: SiteConfiguration) {
        tasks.withType(JBakeTask::class.java)
            .getByName(BAKE_TASK).apply {
                group = GENERATE_GROUP
                input = file(site.bake.srcPath)
                output = layout.buildDirectory
                    .dir(site.bake.destDirPath)
                    .get()
                    .asFile
            }
    }

// ==================== Git Push Task Factory ====================

    internal fun Project.registerGitPushTask(
        taskName: String,
        taskDescription: String,
        taskGroup: String = DEPLOY_GROUP,
        dependsOnTask: String? = null,
        doFirstAction: (org.gradle.api.Task).() -> Unit = {},
        fromPath: () -> String,
        toPath: () -> String,
        gitConfig: GitPushConfiguration
    ) {
        tasks.register(taskName) { task ->
            task.apply {
                group = taskGroup
                description = taskDescription
                dependsOnTask?.let { dependsOn(it) }
                doFirst { doFirstAction(this) }
                doLast {
                    pushPages(fromPath, toPath, gitConfig, logger)
                }
            }
        }
    }

// ==================== Pagefind Task ====================

    internal fun Project.registerPagefindTask(site: SiteConfiguration) {
        tasks.register("pagefind", NpxTask::class.java) { task ->
            task.apply {
                group = TRANSFORM_GROUP
                description = "Index the baked site with Pagefind for full-text search."
                dependsOn(BAKE_TASK)

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

// ==================== Deploy Site Task ====================

    internal fun Project.registerDeploySiteTask(site: SiteConfiguration) {
        val buildDir = layout.buildDirectory.get().asFile.absolutePath
        val destDirPath = site.bake.destDirPath
        val pushPage = site.pushPage

        registerGitPushTask(
            taskName = "deploySite",
            taskDescription = "Deploy site online.",
            dependsOnTask = "pagefind",
            doFirstAction = { site.createCnameFile(project) },
            fromPath = { "$buildDir$separator$destDirPath" },
            toPath = { "$buildDir$separator${pushPage.to}" },
            gitConfig = pushPage
        )
    }

// ==================== Deploy Maquette Task ====================

    internal fun Project.registerDeployMaquetteTask(site: SiteConfiguration) {
        registerGitPushTask(
            taskName = "deployMaquette",
            taskDescription = "Deploy maquette online.",
            doFirstAction = { prepareAndCopyMaquette(site) },
            fromPath = {
                val buildDir = layout.buildDirectory.asFile.get()
                buildDir.resolve(site.pushMaquette.from).absolutePath
            },
            toPath = {
                val buildDir = layout.buildDirectory.get().asFile
                buildDir.resolve(site.pushMaquette.to).absolutePath
            },
            gitConfig = site.pushMaquette
        )
    }

    private fun Project.prepareAndCopyMaquette(site: SiteConfiguration) {
        val uiDir = layout.projectDirectory.asFile
            .resolve(site.pushMaquette.from)
        val uiBuildDir = layout.buildDirectory.asFile.get()
            .resolve(site.pushMaquette.from)

        // Validation
        if (!uiDir.exists()) throw IllegalStateException("$uiDir does not exist")
        if (!uiDir.isDirectory) throw IllegalStateException("$uiDir should be a directory")

        // Préparation du répertoire de build
        if (uiBuildDir.exists()) uiBuildDir.deleteRecursively()
        uiBuildDir.mkdirs()

        if (!uiBuildDir.isDirectory) throw IllegalStateException("$uiBuildDir should be directory")

        // Logging et copie
        uiDir.absolutePath
            .apply(logger::info)
            .run(::println)

        uiBuildDir.path
            .apply(logger::info)
            .run(::println)

        uiDir.copyRecursively(uiBuildDir, overwrite = true)
    }

// ==================== Deploy Profile Task ====================

    internal fun Project.registerDeployProfileTask(site: SiteConfiguration) {
        tasks.register("deployProfile", PublishProfileTask::class.java) { task ->
            task.apply {
                group = DEPLOY_GROUP
                description = "Push profile files (e.g. README.md) to GitHub repository"
            }
        }
    }

// ==================== Serve Task ====================

    internal fun Project.registerServeTask(
        site: SiteConfiguration,
        jbakeRuntime: Configuration
    ) {
        tasks.register("serve", JavaExec::class.java) { task ->
            task.apply {
                group = INFO_GROUP
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

// ==================== Collect Site Config Task ====================

    internal fun Project.registerCollectSiteConfigTask(
        site: SiteConfiguration,
        isGradlePropertiesEnabled: Boolean
    ) {
        tasks.register("collectSiteConfig") { task ->
            task.apply {
                group = COLLECT_GROUP
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

                    project.saveConfiguration(site, isGradlePropertiesEnabled)

                    logger.lifecycle("")
                    logger.lifecycle("✓ Configuration saved successfully!")
                    logger.lifecycle("  You can now run: ./gradlew bake")
                }
            }
        }
    }

// ==================== Collect Site Context Task (BKY-N3-7) ====================

    internal fun Project.registerCollectSiteContextTask(site: SiteConfiguration) {
        tasks.register("collectSiteContext") { task ->
            task.apply {
                group = COLLECT_GROUP
                description = "Collecte le contexte du site baké → build/bakery/metadata.json pour runner-gradle N3."
                dependsOn(BAKE_TASK)

                doLast {
                    val bakedDir = layout.buildDirectory.get().asFile.resolve(site.bake.destDirPath)
                    val outputDir = layout.buildDirectory.get().asFile.resolve("bakery")

                    logger.lifecycle("[collectSiteContext] Scanning baked dir: {}", bakedDir.absolutePath)
                    SiteContextCollector.collect(bakedDir, outputDir)
                    logger.lifecycle("[collectSiteContext] metadata.json written to: {}", outputDir.absolutePath)
                }
            }
        }
    }

// ==================== Utility Tasks ====================

    // TODO: Implémenter la création/initialisation du repository GitHub Pages
    // via l'API GitHub (gh cli ou GraphQL). Nécessite un token avec les scopes
    // appropriés. Sera invoqué manuellement ou en CI.
    internal fun Project.registerUtilityTasks() {
        tasks.register("createPagesRepository") { task ->
            task.apply {
                group = DEPLOY_GROUP
                description = "TODO: Create the GitHub Pages repository via API."
                doLast {
                    throw NotImplementedError(
                        "createPagesRepository is planned but not yet implemented. " +
                        "Future integration: GitHub API with Personal Access Token."
                    )
                }
            }
        }

        tasks.register("updatePagesSecret") { task ->
            task.apply {
                group = DEPLOY_GROUP
                description = "TODO: Update the GitHub Pages publishing secret."
                doLast {
                    throw NotImplementedError(
                        "updatePagesSecret is planned but not yet implemented. " +
                        "Future integration: GitHub API for repository secrets."
                    )
                }
            }
        }
    }


}