package bakery.i18n

data class I18nMigrationResult(
    val keysExtracted: Int,
    val filesGenerated: Int,
    val templatesModified: Int = 0,
    val dryRun: Boolean
)
