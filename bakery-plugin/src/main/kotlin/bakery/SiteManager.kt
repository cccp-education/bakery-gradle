package bakery

import bakery.article.GenerateArticleTask
import bakery.llm.IaConfig
import bakery.llm.OllamaLlmService
import bakery.scaffold.GenerateSiteFromIntentionTask
import bakery.scaffold.ScaffoldIntentionDsl
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
                group = GENERATE_GROUP
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
            "create config file."
                .apply(::println)
                .let(logger::info)
            SiteConfiguration(
                bake = BakeConfiguration(resourcePathForType(siteType), "bake"),
                pushPage = GitPushConfiguration(from = resourcePathForType(siteType), to = "cvs"),
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

        // Resolve all configs through ConfigResolver 4-layer cascade:
        // CLI (-P) > gradle.properties > DSL (BakeryExtension) > site.yml (YAML) > defaults
        val ext = extension ?: BakeryExtension(project.objects)
        val resolvedFirebase = ConfigResolver.resolveFirebaseConfig(props, site.firebase)
        val resolvedGoogleForms = ConfigResolver.resolveGoogleFormsConfig(props, ext.googleForms, site.googleForms)
        val resolvedFirebaseAuth = ConfigResolver.resolveFirebaseAuthConfig(props, ext.firebaseAuth, site.firebaseAuth)
        val resolvedComments = ConfigResolver.resolveCommentsConfig(props, ext.commentsConfig, site.comments)
        val resolvedAnalytics = ConfigResolver.resolveAnalyticsConfig(props, ext.analytics, site.analytics)
        val resolvedNewsletter = ConfigResolver.resolveNewsletterConfig(props, ext.newsletter, site.newsletter)
        val resolvedTheme = ConfigResolver.resolveThemeConfig(props, ext.theme, site.theme)
        val resolvedLayout = ConfigResolver.resolveLayoutConfig(props, ext.layout, site.layout)

        injectResolvedConfigIntoJbakeProperties(targetDir, site) { key, value ->
            when (key) {
                "firebaseApiKey" -> resolvedFirebase.apiKey
                "firebaseProjectId" -> resolvedFirebase.projectId
                "googleFormsFormId" -> resolvedGoogleForms.formId
                "googleFormsWidth" -> resolvedGoogleForms.width
                "googleFormsHeight" -> resolvedGoogleForms.height
                "firebaseAuthApiKey" -> resolvedFirebaseAuth.apiKey
                "firebaseAuthDomain" -> resolvedFirebaseAuth.authDomain
                "firebaseAuthProjectId" -> resolvedFirebaseAuth.projectId
                "commentsEnabled" -> resolvedComments.enabled.toString()
                "commentsCollection" -> resolvedComments.collection
                "analyticsProvider" -> resolvedAnalytics.provider
                "analyticsDomain" -> resolvedAnalytics.domain
                "analyticsScriptSrc" -> resolvedAnalytics.scriptSrc
                "newsletterEnabled" -> resolvedNewsletter.enabled.toString()
                "newsletterProvider" -> resolvedNewsletter.provider
                "newsletterEndpoint" -> resolvedNewsletter.endpoint
                "themeMode" -> resolvedTheme.mode
                "themePrimaryColor" -> resolvedTheme.primaryColor
                "themeSecondaryColor" -> resolvedTheme.secondaryColor
                "themeFontFamily" -> resolvedTheme.fontFamily
                "themeLogoUrl" -> resolvedTheme.logoUrl
                "themeFaviconUrl" -> resolvedTheme.faviconUrl
                "layoutType" -> resolvedLayout.layoutType.name
                else -> value
            }
        }

        // BKY-LENS-3: Inject augmented context budget into jbake.properties
        if (ext.augmentedContext.enabled) {
            injectLensBudgetIntoJbakeProperties(targetDir, site, ext.augmentedContext)
        }

        logger.lifecycle("✓ Site scaffolded (type: ${siteType.alias}) from resource: $resourcePath")
    }

    /**
     * Generic injection of resolved config properties into jbake.properties.
     * Replaces the previous pattern of separate inject*IntoJbakeProperties methods.
     * All config resolution goes through ConfigResolver 4-layer cascade.
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

        // Firebase contact form
        val firebaseResolved = resolver("firebaseApiKey", "")
        if (firebaseResolved.isNotBlank()) {
            updateProperty(lines, "firebaseApiKey", firebaseResolved)
            updateProperty(lines, "firebaseProjectId", resolver("firebaseProjectId", ""))
        }

        // Google Forms
        val gfFormId = resolver("googleFormsFormId", "")
        if (gfFormId.isNotBlank()) {
            updateProperty(lines, "googleFormsFormId", gfFormId)
            updateProperty(lines, "googleFormsWidth", resolver("googleFormsWidth", "640"))
            updateProperty(lines, "googleFormsHeight", resolver("googleFormsHeight", "800"))
        }

        // Firebase Auth
        val fAuthKey = resolver("firebaseAuthApiKey", "")
        if (fAuthKey.isNotBlank()) {
            updateProperty(lines, "firebaseAuthApiKey", fAuthKey)
            updateProperty(lines, "firebaseAuthDomain", resolver("firebaseAuthDomain", ""))
            updateProperty(lines, "firebaseAuthProjectId", resolver("firebaseAuthProjectId", ""))
        }

        // Comments
        val commentsEnabled = resolver("commentsEnabled", "false")
        if (commentsEnabled != "false" || resolver("commentsCollection", "comments") != "comments") {
            updateProperty(lines, "commentsEnabled", commentsEnabled)
            updateProperty(lines, "commentsCollection", resolver("commentsCollection", "comments"))
        }

        // Analytics
        val analyticsProvider = resolver("analyticsProvider", "")
        if (analyticsProvider.isNotBlank()) {
            updateProperty(lines, "analyticsProvider", analyticsProvider)
            updateProperty(lines, "analyticsDomain", resolver("analyticsDomain", ""))
            updateProperty(lines, "analyticsScriptSrc", resolver("analyticsScriptSrc", ""))
        }

        // Newsletter
        val newsletterEnabled = resolver("newsletterEnabled", "false")
        if (newsletterEnabled != "false" || resolver("newsletterProvider", "").isNotBlank()) {
            updateProperty(lines, "newsletterEnabled", newsletterEnabled)
            updateProperty(lines, "newsletterProvider", resolver("newsletterProvider", ""))
            updateProperty(lines, "newsletterEndpoint", resolver("newsletterEndpoint", ""))
        }

        // Theme
        updateProperty(lines, "themeMode", resolver("themeMode", "auto"))
        updateProperty(lines, "themePrimaryColor", resolver("themePrimaryColor", "#0d6efd"))
        updateProperty(lines, "themeSecondaryColor", resolver("themeSecondaryColor", "#6c757d"))
        updateProperty(lines, "themeFontFamily", resolver("themeFontFamily", ""))
        updateProperty(lines, "themeLogoUrl", resolver("themeLogoUrl", ""))
        updateProperty(lines, "themeFaviconUrl", resolver("themeFaviconUrl", ""))

        // Layout
        updateProperty(lines, "layoutType", resolver("layoutType", "FULL_WIDTH"))

        jbakeProps.writeText(lines.joinToString("\n"), UTF_8)
        logger.lifecycle("✓ Injected resolved config into jbake.properties")
    }

    private fun updateProperty(lines: MutableList<String>, key: String, value: String) {
        val idx = lines.indexOfFirst { it.startsWith("$key=") }
        if (idx >= 0) {
            lines[idx] = "$key=$value"
        } else {
            lines.add("$key=$value")
        }
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
            val contextData = augmentedContextFile.readText(UTF_8)
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
            updateProperty(lines, "augmentedContextData", contextData)
        } else {
            logger.info("[BakeryPlugin] augmentedContextData: file not found at ${augmentedContextFile.absolutePath}, skipping injection")
            updateProperty(lines, "augmentedContextData", "")
        }

        jbakeProps.writeText(lines.joinToString("\n"), UTF_8)
        logger.lifecycle("✓ Injected LensBudget config into jbake.properties (maxArticlesPerPage=${augmentedContext.budget.maxArticlesPerPage}, minSimilarity=${augmentedContext.budget.minSimilarity})")
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

// ==================== Generate Article Task (BKY-JB-0) ====================

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
            task.group = GENERATE_GROUP
            task.description = "Génère un article de blog assisté IA via Ollama — injecte dans content/blog/YYYY/MM/"
            task.contentRootDir = contentRoot
            task.topic.set(project.providers.gradleProperty("topic").orElse(""))
            task.articleTon.set(project.providers.gradleProperty("articleTon").orElse(""))
            task.articleAudience.set(project.providers.gradleProperty("articleAudience").orElse(""))
            task.articleKeywords.set(project.providers.gradleProperty("articleKeywords").orElse(""))
            task.articleLang.set(project.providers.gradleProperty("articleLang").orElse("fr"))

            // Inject DSL intention if configured
            articleIntentionDsl?.let { dsl ->
                if (dsl.topic.isNotBlank()) {
                    task.dslIntention = try {
                        dsl.toIntention()
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                }
            }

            if (iaConfig.enabled) {
                task.llmService = OllamaLlmService.create(
                    baseUrl = iaConfig.baseUrl,
                    modelName = iaConfig.modelName,
                    timeout = iaConfig.timeout
                )
                project.logger.info(
                    "[BakeryPlugin] generateArticle IA activé : {} @ {}",
                    iaConfig.modelName, iaConfig.baseUrl
                )
            } else {
                project.logger.info("[BakeryPlugin] generateArticle IA désactivé (ia.enabled = false)")
            }
        }
    }

    // ==================== Generate Site From Intention Task (BKY-IA-1) ====================

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
            task.group = GENERATE_GROUP
            task.description = "Génère la structure d'un site assistée par IA — scaffold interactif"
            task.targetDir = targetDir
            task.scaffoldDescription.set(project.providers.gradleProperty("scaffoldDescription").orElse(""))
            task.siteType.set(project.providers.gradleProperty("siteType").orElse(""))
            task.scaffoldLang.set(project.providers.gradleProperty("scaffoldLang").orElse("fr"))
            task.projectName.set(project.providers.gradleProperty("projectName").orElse(""))

            // Inject DSL intention if configured
            scaffoldIntentionDsl?.let { dsl ->
                if (dsl.description.isNotBlank()) {
                    task.dslIntention = try {
                        dsl.toIntention()
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                }
            }

            if (iaConfig.enabled) {
                task.llmService = OllamaLlmService.create(
                    baseUrl = iaConfig.baseUrl,
                    modelName = iaConfig.modelName,
                    timeout = iaConfig.timeout
                )
                project.logger.info(
                    "[BakeryPlugin] generateSiteFromIntention IA activé : {} @ {}",
                    iaConfig.modelName, iaConfig.baseUrl
                )
            } else {
                project.logger.info("[BakeryPlugin] generateSiteFromIntention IA désactivé (ia.enabled = false)")
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

                    val siteYmlFile = file(configPath)
                    projectDir.saveConfiguration(site, siteYmlFile, username, repo, token)

                    logger.lifecycle("")
                    logger.lifecycle("✓ Configuration saved to ${siteYmlFile.absolutePath}")
                    logger.lifecycle("  You can now run: ./gradlew bake")
                }
            }
        }
    }

// ==================== Collect Site Context Task (BKY-N3-7) ====================

    internal fun Project.registerCollectSiteContextTask(
        site: SiteConfiguration,
        augmentedContext: bakery.lens.AugmentedContextDsl? = null
    ) {
        tasks.register("collectSiteContext") { task ->
            task.apply {
                group = COLLECT_GROUP
                description = "Collecte le contexte du site baké → build/bakery/metadata.json pour runner-gradle N3."
                dependsOn(BAKE_TASK)

                doLast {
                    val bakedDir = layout.buildDirectory.get().asFile.resolve(site.bake.destDirPath)
                    val outputDir = layout.buildDirectory.get().asFile.resolve("bakery")

                    logger.lifecycle("[collectSiteContext] Scanning baked dir: {}", bakedDir.absolutePath)

                    if (augmentedContext != null && augmentedContext.enabled) {
                        SiteContextCollector.collectWithAugmentedContext(bakedDir, outputDir, augmentedContext)
                        logger.lifecycle("[collectSiteContext] metadata.json with augmentedEntries written to: {}", outputDir.absolutePath)
                    } else {
                        SiteContextCollector.collect(bakedDir, outputDir)
                        logger.lifecycle("[collectSiteContext] metadata.json written to: {}", outputDir.absolutePath)
                    }
                }
            }
        }
    }

// ==================== Collect Augmented Context Task (BKY-LENS-3) ====================

    /**
     * Enregistre la tâche `collectAugmentedContext` — Pattern LENTILLE.
     *
     * Orchestre le pipeline LENS :
     * 1. Charge composite-context.json (AugmentedContextResolver)
     * 2. Extrait le sous-graphe (SubgraphExtractor)
     * 3. Score les nœuds (AugmentedArticlesService)
     * 4. Applique le budget (LensBudget)
     * 5. Écrit le JSON augmenté → build/bakery/augmented-context.json
     *
     * Remplace `registerCollectRelatedArticlesTask` (BKG legacy, supprimé en LENS-3.3).
     */
    internal fun Project.registerCollectAugmentedContextTask(
        site: SiteConfiguration,
        augmentedContextDsl: bakery.lens.AugmentedContextDsl? = null
    ) {
        tasks.register("collectAugmentedContext") { task ->
            task.apply {
                group = COLLECT_GROUP
                description = "Collecte le contexte augmenté LENS (ségrégation + enrichissement + budget) → build/bakery/augmented-context.json."
                dependsOn("collectSiteContext")

                doLast {
                    if (augmentedContextDsl == null || !augmentedContextDsl.enabled) {
                        logger.info("[collectAugmentedContext] AugmentedContext désactivé. Skip.")
                        return@doLast
                    }

                    val outputDir = layout.buildDirectory.get().asFile.resolve("bakery")
                    outputDir.mkdirs()

                    // 1. Charger le composite-context.json (si disponible)
                    val contextPath = augmentedContextDsl.contextPath
                    val contextFile = projectDir.resolve(contextPath)
                    val resolver = bakery.lens.AugmentedContextResolver()
                    val compositeContext = resolver.resolve(contextFile.absolutePath)
                    if (compositeContext == null && contextFile.exists()) {
                        logger.info("[collectAugmentedContext] composite-context.json trouvé mais invalide à $contextPath.")
                    } else if (!contextFile.exists()) {
                        logger.info("[collectAugmentedContext] composite-context.json non trouvé à $contextPath. RAG désactivé.")
                    }

                    // 2. Extraire le sous-graphe depuis graph.json
                    val graphFilePath = augmentedContextDsl.lens.graphFilePath
                    val graphFile = projectDir.resolve(graphFilePath)
                    val extractor = bakery.lens.SubgraphExtractor()
                    val subgraph = if (graphFile.exists()) {
                        val fullGraph = extractor.loadGraph(graphFile.absolutePath)
                        extractor.extract(fullGraph, augmentedContextDsl.lens)
                    } else {
                        logger.info("[collectAugmentedContext] graph.json non trouvé à $graphFilePath. Sous-graphe vide.")
                        bakery.lens.SiteSubgraph(emptyList(), emptyList(), emptyList())
                    }

                    // 3. RAG results — le canal RAG du composite-context fournit le contenu
                    // textuel, pas des scores structurés. Les scores RAG viendront de pgvector
                    // via codebase-gradle (connexion N3 en BKY-LENS-5). Pour l'instant, RAG=vide.
                    val ragResults: Map<String, Double> = emptyMap()
                    if (compositeContext != null) {
                        val channels = resolver.extractChannels(compositeContext)
                        val ragContent = channels[contracts.context.ChannelType.RAG]
                        if (ragContent != null) {
                            logger.info("[collectAugmentedContext] Canal RAG disponible ({} caractères). Scoring RAG via pgvector à venir (BKY-LENS-5).", ragContent.length)
                        }
                    }

                    // 4. Scoring hybride
                    val service = bakery.lens.AugmentedArticlesService()
                    val allScored = service.scoreAll(
                        subgraph = subgraph,
                        ragResults = ragResults,
                        lensRules = augmentedContextDsl.lens.rules
                    )

                    // 5. Filtrer par règles + budget
                    val filtered = service.applyRules(allScored, augmentedContextDsl.lens.rules)
                    val budgeted = augmentedContextDsl.budget.apply(filtered)

                    // 6. Écrire le résultat
                    val mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
                    val output = mapOf(
                        "version" to "1.0",
                        "pipeline" to "LENS",
                        "budget" to mapOf(
                            "maxArticlesPerPage" to augmentedContextDsl.budget.maxArticlesPerPage,
                            "minSimilarity" to augmentedContextDsl.budget.minSimilarity
                        ),
                        "scoredNodes" to budgeted,
                        "totalCandidates" to allScored.size,
                        "totalAfterRules" to filtered.size,
                        "totalAfterBudget" to budgeted.size
                    )
                    mapper.writerWithDefaultPrettyPrinter()
                        .writeValue(outputDir.resolve("augmented-context.json"), output)

                    logger.lifecycle(
                        "[collectAugmentedContext] {} nœuds scorés → {} après règles → {} après budget → {}",
                        allScored.size, filtered.size, budgeted.size,
                        outputDir.resolve("augmented-context.json").absolutePath
                    )
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