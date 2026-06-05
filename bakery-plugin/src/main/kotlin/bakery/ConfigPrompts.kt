package bakery

import org.gradle.api.Project
import java.io.File

object ConfigPrompts {

    fun Project.getOrPrompt(
        propertyName: String,
        cliProperty: String,
        sensitive: Boolean = false,
        example: String? = null,
        default: String? = null
    ): String = ConfigPromptM.fromCliOrPrompt(
        propertyName, cliProperty, sensitive, example, default
    ).run(ConfigPromptEnvironment.defaultFor(logger, this))

    fun File.saveConfiguration(
        currentConfig: SiteConfiguration,
        siteYmlFile: File,
        username: String,
        repo: String,
        token: String,
    ) {
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

    @Deprecated("Legacy signature — no-op. Use File.saveConfiguration directly.")
    fun Project.saveConfiguration(
        site: SiteConfiguration,
        isGradlePropertiesEnabled: Boolean,
    ) {
        // No-op legacy
    }
}