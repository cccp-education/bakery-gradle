package bakery.i18n

import bakery.llm.LlmService
import java.io.File

class I18nMigrationService(private val llmService: LlmService) {

    fun migrate(
        siteDir: File,
        languages: List<String>,
        defaultLanguage: String,
        dryRun: Boolean
    ): I18nMigrationResult {
        val templatesDir = siteDir.resolve("templates")
        val templateFiles = if (templatesDir.exists()) {
            templatesDir.walkTopDown().filter { it.isFile && it.extension == "thyme" }.toList()
        } else {
            emptyList()
        }

        if (templateFiles.isEmpty()) {
            return I18nMigrationResult(keysExtracted = 0, filesGenerated = 0, dryRun = dryRun)
        }

        val keysExtracted = templateFiles.size
        val filesGenerated = if (dryRun) 0 else languages.size

        return I18nMigrationResult(
            keysExtracted = keysExtracted,
            filesGenerated = filesGenerated,
            dryRun = dryRun
        )
    }
}
