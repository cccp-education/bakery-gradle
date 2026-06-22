package bakery.scenarios


import bakery.createConfigFile
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner.create
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import java.io.File
import java.io.File.createTempFile

class BakeryWorld {
    val log: Logger = getLogger(BakeryWorld::class.java)

    // Scope de coroutines pour le scénario
    val scope = CoroutineScope(Default + SupervisorJob())

    // État partagé entre les steps
    var projectDir: File? = null
    var buildResult: BuildResult? = null
    var exception: Throwable? = null
    // Simulated remote for end-to-end deployProfile tests
    var simulatedRemoteDir: File? = null
    var simulatedRemoteUri: String? = null
    var migrationSiteDir: String? = null
    var realSiteDir: File? = null

    // Jobs asynchrones en cours
    private val asyncJobs = mutableListOf<Deferred<BuildResult>>()

    /**
     * Exécute une tâche Gradle de manière asynchrone
     */
    fun executeGradleAsync(vararg tasks: String): Deferred<BuildResult> {
        require(projectDir != null) { "Project directory must be initialized" }
        log.info("Starting async Gradle execution: ${tasks.joinToString(" ")}")
        return scope.async {
            try {
                create()
                    .withProjectDir(projectDir!!)
                    .withArguments(tasks.toList() + "--stacktrace")
                    .withPluginClasspath()
                    .build()
            } catch (e: Exception) {
                log.error("Gradle build failed", e)
                exception = e
                throw e
            }
        }.also { asyncJobs.add(it) }
    }

    /**
     * Exécute une tâche Gradle de manière synchrone
     */
    suspend fun executeGradle(vararg tasks: String)
            : BuildResult = executeGradleAsync(*tasks)
        .await()
        .also { buildResult = it }

    /**
     * Exécute une tâche Gradle en capturant l'exception si elle échoue.
     * Utile pour les scénarios Cucumber qui testent les cas d'erreur.
     */
    fun executeGradleExpectingFailure(vararg tasks: String) {
        require(projectDir != null) { "Project directory must be initialized" }
        log.info("Starting Gradle execution (expecting failure): ${tasks.joinToString(" ")}")
        try {
            buildResult = create()
                .withProjectDir(projectDir!!)
                .withArguments(tasks.toList() + "--stacktrace")
                .withPluginClasspath()
                .build()
        } catch (e: Exception) {
            log.error("Gradle build failed as expected", e)
            exception = e
        }
    }

    /**
     * Exécute une action avec un timeout
     */
    suspend fun <T> withTimeout(seconds: Long, block: suspend () -> T)
            : T = withTimeout(seconds * 1000) { block() }

    /**
     * Attend la fin de toutes les opérations asynchrones
     */
    suspend fun awaitAll() {
        if (asyncJobs.isNotEmpty()) {
            log.info("Waiting for ${asyncJobs.size} async operations...")
            asyncJobs.awaitAll()
            log.info("All async operations completed")
        }
    }

    /**
     * Nettoyage des ressources
     */
    @Suppress("unused")
    fun cleanup() {
        scope.cancel()
        projectDir?.deleteRecursively()
        projectDir = null
        buildResult = null
        exception = null
        asyncJobs.clear()
    }

    /**
     * Crée un projet Gradle de test
     */
    fun createGradleProject(configFileName: String = "site.yml"): File {
        val pluginId = "education.cccp.bakery"
        val buildScriptContent = "bakery { configPath = file(\"$configFileName\").absolutePath }"
        createTempFile("gradle-test-", "").apply {
            delete()
            mkdirs()
        }.run {
            resolve("settings.gradle.kts")
                .apply { createNewFile() }
                .writeText(
                    "pluginManagement.repositories.gradlePluginPortal()\n" +
                            "rootProject.name = \"${name}\""
                )
            resolve("build.gradle.kts")
                .apply { createNewFile() }
                .writeText("plugins { id(\"$pluginId\") }\n$buildScriptContent")
            createConfigFile()
            projectDir = this
            return this
        }
    }

    /**
     * Crée un projet Gradle de test avec bloc IA par défaut.
     */
    fun createGradleProjectWithIA(): File {
        val pluginId = "education.cccp.bakery"
        val buildScriptContent = """
            bakery {
                configPath = file("site.yml").absolutePath
                ia {
                    baseUrl = "http://localhost:11438"
                    modelName = "gpt-oss:120b-cloud"
                }
            }
        """.trimIndent()
        createTempFile("gradle-test-", "").apply {
            delete()
            mkdirs()
        }.run {
            resolve("settings.gradle.kts")
                .apply { createNewFile() }
                .writeText(
                    "pluginManagement.repositories.gradlePluginPortal()\n" +
                            "rootProject.name = \"${name}\""
                )
            resolve("build.gradle.kts")
                .apply { createNewFile() }
                .writeText("plugins { id(\"$pluginId\") }\n$buildScriptContent")
            createConfigFile()
            projectDir = this
            return this
        }
    }

    /**
     * Crée un projet Gradle de test avec site fully configured (bake.srcPath + maquette existants).
     * Nécessaire pour tester les tasks qui ne sont enregistrées que dans la branche "else" du plugin
     * (generateArticle, deploySite, etc.).
     */
    fun createGradleProjectWithSiteConfigured(iaEnabled: Boolean = true): File {
        val pluginId = "education.cccp.bakery"
        val iaBlock = if (iaEnabled) """
            ia {
                baseUrl = "http://localhost:11437"
                modelName = "gpt-oss:120b-cloud"
            }
        """.trimIndent() else ""

        val buildScriptContent = """
            bakery {
                configPath = file("site.yml").absolutePath
                $iaBlock
            }
        """.trimIndent()

        createTempFile("gradle-test-", "").apply {
            delete()
            mkdirs()
        }.run {
            resolve("settings.gradle.kts")
                .apply { createNewFile() }
                .writeText(
                    "pluginManagement.repositories.gradlePluginPortal()\n" +
                            "rootProject.name = \"${name}\""
                )
            resolve("build.gradle.kts")
                .apply { createNewFile() }
                .writeText("plugins { id(\"$pluginId\") }\n$buildScriptContent")
            // Écrire un site.yml minimal mais valide
            resolve("site.yml").writeText("""
                bake:
                  srcPath: "site"
                  destDirPath: "build/bake"
                pushPage:
                  from: "site"
                  to: "cvs"
                  repo:
                    name: "test-site"
                    repository: "https://github.com/user/repo.git"
                    credentials:
                      username: "user"
                      password: "token"
                  branch: "main"
                  message: "Deploy test"
                pushMaquette:
                  from: "maquette"
                  to: "cvs"
                  repo:
                    name: "test-maquette"
                    repository: "https://github.com/user/maquette.git"
                    credentials:
                      username: "user"
                      password: "token"
                  branch: "main"
                  message: "Deploy maquette"
            """.trimIndent())
            // Créer les répertoires pour que le plugin prenne la branche "else" du afterEvaluate
            resolve("site").mkdirs()
            resolve("maquette").mkdirs()
            projectDir = this
            return this
        }
    }

    /**
     * Crée un projet Gradle de test avec bloc articleIntention DSL.
     */
    fun createGradleProjectWithArticleIntention(
        topic: String,
        ton: String? = null,
        audience: String? = null,
        keywords: String? = null,
        lang: String? = null
    ): File {
        val pluginId = "education.cccp.bakery"
        val articleIntentionBlock = buildString {
            appendLine("    articleIntention {")
            appendLine("        topic = \"$topic\"")
            ton?.let { appendLine("        ton = \"$it\"") }
            audience?.let { appendLine("        audience = \"$it\"") }
            keywords?.let {
                val kwList = it.split(",").map { k -> "\"${k.trim()}\"" }.joinToString(", ")
                appendLine("        keywords = listOf($kwList)")
            }
            lang?.let { appendLine("        lang = \"$it\"") }
            appendLine("    }")
        }
        val buildScriptContent = """
            bakery {
                configPath = file("site.yml").absolutePath
$articleIntentionBlock
            }
        """.trimIndent()

        createTempFile("gradle-test-", "").apply {
            delete()
            mkdirs()
        }.run {
            resolve("settings.gradle.kts")
                .apply { createNewFile() }
                .writeText(
                    "pluginManagement.repositories.gradlePluginPortal()\n" +
                            "rootProject.name = \"${name}\""
                )
            resolve("build.gradle.kts")
                .apply { createNewFile() }
                .writeText("plugins { id(\"$pluginId\") }\n$buildScriptContent")
            // Écrire un site.yml minimal mais valide pour la branche "else"
            resolve("site.yml").writeText("""
                bake:
                  srcPath: "site"
                  destDirPath: "build/bake"
                pushPage:
                  from: "site"
                  to: "cvs"
                  repo:
                    name: "test-site"
                    repository: "https://github.com/user/repo.git"
                    credentials:
                      username: "user"
                      password: "token"
                  branch: "main"
                  message: "Deploy test"
                pushMaquette:
                  from: "maquette"
                  to: "cvs"
            """.trimIndent())
            resolve("site").mkdirs()
            resolve("maquette").mkdirs()
            projectDir = this
            return this
        }
    }

    /**
     * Crée un projet Gradle de test avec bloc scaffoldIntention DSL.
     */
    fun createGradleProjectWithScaffoldIntention(
        description: String? = null,
        siteType: String? = null,
        lang: String? = null,
        projectName: String? = null
    ): File {
        val pluginId = "education.cccp.bakery"
        val scaffoldIntentionBlock = buildString {
            if (description != null) {
                appendLine("    scaffoldIntention {")
                appendLine("        description = \"$description\"")
                siteType?.let { appendLine("        siteType = \"$it\"") }
                lang?.let { appendLine("        lang = \"$it\"") }
                projectName?.let { appendLine("        projectName = \"$it\"") }
                appendLine("    }")
            }
        }
        val buildScriptContent = """
            bakery {
                configPath = file("site.yml").absolutePath
$scaffoldIntentionBlock
            }
        """.trimIndent()

        createTempFile("gradle-test-", "").apply {
            delete()
            mkdirs()
        }.run {
            resolve("settings.gradle.kts")
                .apply { createNewFile() }
                .writeText(
                    "pluginManagement.repositories.gradlePluginPortal()\n" +
                            "rootProject.name = \"${name}\""
                )
            resolve("build.gradle.kts")
                .apply { createNewFile() }
                .writeText("plugins { id(\"$pluginId\") }\n$buildScriptContent")
            createConfigFile()
            projectDir = this
            return this
        }
    }

    /**
     * Crée un vrai répertoire de site avec templates pour les tests Cucumber migrateToI18n.
     * @param name nom du sous-répertoire
     * @param withTemplates si true, crée templates/ avec .thyme hardcodé FR
     * @param alreadyI18n si true, les templates utilisent déjà th:text
     * @param emptyDivs si true, les templates n'ont que des divs vides
     */
    fun createRealSiteDirectory(
        name: String,
        withTemplates: Boolean = true,
        alreadyI18n: Boolean = false,
        emptyDivs: Boolean = false
    ): File {
        val siteDir = projectDir!!.resolve(name)
        siteDir.mkdirs()
        siteDir.resolve("jbake.properties").writeText("site.host=http://example.com\n")
        if (withTemplates) {
            val templatesDir = siteDir.resolve("templates")
            templatesDir.mkdirs()
            if (alreadyI18n) {
                templatesDir.resolve("header.thyme").writeText(
                    """<header><h1 th:text="#{header.title}">Mon Site</h1><nav><a href="/" th:text="#{nav.home}">Accueil</a></nav></header>"""
                )
            } else if (emptyDivs) {
                templatesDir.resolve("header.thyme").writeText("<div></div>")
                templatesDir.resolve("footer.thyme").writeText("<div></div>")
            } else {
                templatesDir.resolve("header.thyme").writeText(
                    """<header><h1>Mon Site</h1><nav><a href="/">Accueil</a> <a href="/contact">Contact</a></nav></header>"""
                )
                templatesDir.resolve("footer.thyme").writeText(
                    """<footer><p>© 2024 Mon Site. Tous droits réservés.</p></footer>"""
                )
            }
        }
        realSiteDir = siteDir
        return siteDir
    }

    /**
     * Copie un répertoire de site depuis les resources de test vers le projet Gradle temporaire.
     * Utilisé pour les tests Cucumber sur la fixture magic-stick (BKY-I18N-PROD-MIG).
     */
    fun copyRealSiteFromTestResources(name: String, resourcePath: String): File {
        val resourceUrl = this::class.java.classLoader.getResource(resourcePath)
            ?: throw IllegalStateException("Resource not found: $resourcePath")
        val sourceDir = File(resourceUrl.toURI())
        val targetDir = projectDir!!.resolve(name)
        sourceDir.copyRecursively(targetDir, overwrite = true)
        realSiteDir = targetDir
        return targetDir
    }

    /**
     * Crée un projet Gradle de test avec bloc i18nMigration DSL.
     */
    fun createGradleProjectWithI18nMigrationIntention(
        siteDir: String,
        languages: List<String>,
        defaultLanguage: String,
        dryRun: Boolean
    ): File {
        val pluginId = "education.cccp.bakery"
        val langListStr = languages.joinToString(", ") { "\"$it\"" }
        val i18nMigrationBlock = """
            i18nMigration {
                siteDir = "$siteDir"
                languages = listOf($langListStr)
                defaultLanguage = "$defaultLanguage"
                dryRun = $dryRun
            }
        """.trimIndent()
        val buildScriptContent = """
            bakery {
                configPath = file("site.yml").absolutePath
                $i18nMigrationBlock
            }
        """.trimIndent()

        createTempFile("gradle-test-", "").apply {
            delete()
            mkdirs()
        }.run {
            resolve("settings.gradle.kts")
                .apply { createNewFile() }
                .writeText(
                    "pluginManagement.repositories.gradlePluginPortal()\n" +
                            "rootProject.name = \"${name}\""
                )
            resolve("build.gradle.kts")
                .apply { createNewFile() }
                .writeText("plugins { id(\"$pluginId\") }\n$buildScriptContent")
            createConfigFile()
            projectDir = this
            return this
        }
    }
}
