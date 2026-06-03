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
    // Simulated remote for end-to-end publishProfile tests
    var simulatedRemoteDir: File? = null
    var simulatedRemoteUri: String? = null

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
     * Crée un projet Gradle de test avec bloc IA pool multi-port.
     */
    fun createGradleProjectWithIAPool(): File {
        val pluginId = "education.cccp.bakery"
        val buildScriptContent = """
            bakery {
                configPath = file("site.yml").absolutePath
                ia {
                    poolPorts = "11437,11438,11439"
                    poolModel = "gpt-oss:120b-cloud"
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
}
