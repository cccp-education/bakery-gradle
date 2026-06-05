package bakery

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.BufferedReader
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
    ): String = resolveConfigValue(
        ConfigPromptEnvironment.defaultFor(logger),
        this,
        propertyName,
        cliProperty,
        sensitive,
        example,
        default
    )

    fun resolveConfigValue(
        env: ConfigPromptEnvironment,
        project: Project,
        propertyName: String,
        cliProperty: String,
        sensitive: Boolean = false,
        example: String? = null,
        default: String? = null
    ): String {
        if (project.hasProperty(cliProperty)) {
            val value = (project.property(cliProperty) as? String)
            if (!value.isNullOrBlank()) return value
        }

        val envVar = cliProperty
            .replace(Regex(REGEX_ALPHA), REGEX_REPLACEMENT)
            .uppercase()

        getenv(envVar)?.takeIf { it.isNotBlank() }?.let { return it }

        default?.let { return it }

        if (sensitive) return promptSensitiveValue(env, propertyName)
        return promptNormalValue(env, propertyName, example)
    }

    private fun promptSensitiveValue(
        env: ConfigPromptEnvironment,
        propertyName: String
    ): String {
        var input: CharArray?
        do {
            env.writeOutput("Enter $propertyName (hidden): ")
            input = env.readPassword()
            if (input == null || input.isEmpty())
                env.logger.warn("$propertyName cannot be empty. Please try again.")
        } while (input == null || input.isEmpty())
        return String(input).also { input.fill('0') }
    }

    private fun promptNormalValue(
        env: ConfigPromptEnvironment,
        propertyName: String,
        example: String?
    ): String {
        val exampleText = example?.let { " (e.g., $it)" } ?: ""
        var input: String?
        do {
            env.writeOutput("Enter $propertyName$exampleText: ")
            input = env.readInput()
            if (input.isNullOrBlank())
                env.logger.warn("$propertyName cannot be empty. Please try again.")
        } while (input.isNullOrBlank())
        return input
    }

    data class ConfigPromptEnvironment(
        val readInput: () -> String?,
        val readPassword: () -> CharArray?,
        val writeOutput: (String) -> Unit,
        val logger: Logger
    ) {
        companion object {
            fun defaultFor(logger: Logger): ConfigPromptEnvironment {
                val console = console()
                val fallbackReader = BufferedReader(InputStreamReader(System.`in`))
                return if (console != null) {
                    ConfigPromptEnvironment(
                        readInput = { console.readLine() },
                        readPassword = { console.readPassword() },
                        writeOutput = { print(it) },
                        logger = logger
                    )
                } else {
                    ConfigPromptEnvironment(
                        readInput = { fallbackReader.readLine() },
                        readPassword = { null },
                        writeOutput = { print(it); logger.lifecycle("Console not available. Using standard input.") },
                        logger = logger
                    )
                }
            }
        }
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