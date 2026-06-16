package bakery.i18n

open class I18nMigrationIntentionDsl {
    var siteDir: String = ""
    var languages: List<String> = listOf("en")
    var defaultLanguage: String = "fr"
    var dryRun: Boolean = true

    fun toIntention(): I18nMigrationIntention = I18nMigrationIntention(
        siteDir = siteDir,
        languages = languages,
        defaultLanguage = defaultLanguage,
        dryRun = dryRun
    )
}
