package bakery.i18n

data class I18nMigrationResult(
    val keysExtracted: Int,
    val filesGenerated: Int,
    val dryRun: Boolean
)
