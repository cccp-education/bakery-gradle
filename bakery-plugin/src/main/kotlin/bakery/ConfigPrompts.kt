package bakery

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.BufferedReader
import java.io.Console
import java.io.File
import java.io.InputStreamReader
import java.lang.System.console
import java.lang.System.getenv


object ConfigPrompts {
    private const val REGEX_ALPHA = "([a-z])([A-Z])"
    private const val REGEX_REPLACEMENT = "$1_$2"

    fun Project.getOrPrompt(
        propertyName: String,
        cliProperty: String,
        sensitive: Boolean = false,
        example: String? = null,
        default: String? = null
    ): String {
        // 1. Vérifier les propriétés du projet (-P)
        if (hasProperty(cliProperty)) {
            val value = property(cliProperty) as String
            if (value.isNotBlank()) return value
        }

        // 2. Vérifier les variables d'environnement
        val envVar: String = cliProperty
            .replace(Regex(REGEX_ALPHA), REGEX_REPLACEMENT)
            .uppercase()

        getenv(envVar)?.takeIf { it.isNotBlank() }?.let { return it }

        // 3. Utiliser la valeur par défaut si fournie
        default?.let { return it }

        // 4. Demander interactivement
        return console().let {
            if (it != null) {
                if (sensitive) promptSensitive(it, propertyName, logger)
                else promptNormal(it, propertyName, example, logger)
            } else promptFallback(propertyName, sensitive, example, logger)
        }
    }

    private fun promptSensitive(
        console: Console,
        propertyName: String,
        logger: Logger
    ): String {
        var input: CharArray?
        do {
            print("Enter $propertyName (hidden): ")
            input = console.readPassword()
            if (input == null || input.isEmpty())
                logger.warn("$propertyName cannot be empty. Please try again.")
        } while (input == null || input.isEmpty())

        return String(input).also {
            input.fill('0')
        }
    }

    private fun promptNormal(
        console: Console,
        propertyName: String,
        example: String?,
        logger: Logger
    ): String {
        val exampleText = example?.let { " (e.g., $it)" } ?: ""
        var input: String?
        do {
            print("Enter $propertyName$exampleText: ")
            input = console.readLine()
            if (input.isNullOrBlank())
                logger.warn("$propertyName cannot be empty. Please try again.")
        } while (input.isNullOrBlank())
        return input
    }

    private fun promptFallback(
        propertyName: String,
        sensitive: Boolean,
        example: String?,
        logger: Logger
    ): String {
        val exampleText = example?.let { " (e.g., $it)" } ?: ""
        val sensitiveNote = if (sensitive) " (will be visible)" else ""

        logger.lifecycle("Console not available. Using standard input.")
        print("Enter $propertyName$exampleText$sensitiveNote: ")

        var input: String?

        val reader = BufferedReader(InputStreamReader(System.`in`))
        do {
            input = reader.readLine()
            if (input.isNullOrBlank()) {
                logger.warn("$propertyName cannot be empty. Please try again.")
                print("Enter $propertyName$exampleText: ")
            }
        } while (input.isNullOrBlank())

        return input
    }

    /**
     * Saves interactive configuration values (username, repo, token) back to site.yml.
     * Merges credentials into the pushPage.repo section while preserving existing config.
     *
     * @param currentConfig The current SiteConfiguration (read from site.yml)
     * @param siteYmlFile The site.yml file to update
     * @param username GitHub username (from CLI prompt or -P flag)
     * @param repo GitHub repository URL (from CLI prompt or -P flag)
     * @param token GitHub token (from CLI prompt or -P flag)
     */
    fun File.saveConfiguration(
        currentConfig: SiteConfiguration,
        siteYmlFile: File,
        username: String,
        repo: String,
        token: String,
    ) {
        // If no credentials provided, skip writing
        if (username.isBlank() && repo.isBlank() && token.isBlank()) return

        val updatedPushPage = currentConfig.pushPage.copy(
            repo = currentConfig.pushPage.repo.copy(
                repository = if (repo.isNotBlank()) repo else currentConfig.pushPage.repo.repository,
                credentials = RepositoryCredentials(
                    username = if (username.isNotBlank()) username else currentConfig.pushPage.repo.credentials.username,
                    password = if (token.isNotBlank()) token else currentConfig.pushPage.repo.credentials.password,
                )
            )
        )

        val updatedConfig = currentConfig.copy(pushPage = updatedPushPage)

        FileSystemManager.yamlMapper.writeValue(siteYmlFile, updatedConfig)
    }

    /**
     * Legacy signature preserved for backward compatibility with SiteManager.registerCollectSiteConfigTask.
     * Now delegates to the new File.saveConfiguration with the resolved values.
     */
    fun Project.saveConfiguration(
        site: SiteConfiguration,
        isGradlePropertiesEnabled: Boolean,
    ) {
        // No-op legacy — SiteManager calls File.saveConfiguration directly
    }
}