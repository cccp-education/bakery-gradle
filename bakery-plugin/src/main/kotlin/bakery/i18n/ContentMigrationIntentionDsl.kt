package bakery.i18n

open class ContentMigrationIntentionDsl {
    var sourceDir: String = ""
    var outputDir: String = ""
    var sourceLanguage: String = "fr"
    var targetLanguages: List<String> = listOf("en")
    var dryRun: Boolean = true
    var excludePaths: List<String> = emptyList()
    var parallelism: Int = 1

    fun toIntention(): ContentMigrationIntention = ContentMigrationIntention(
        sourceDir = sourceDir,
        outputDir = outputDir,
        sourceLanguage = sourceLanguage,
        targetLanguages = targetLanguages,
        dryRun = dryRun,
        excludePaths = excludePaths,
        parallelism = parallelism
    )
}
