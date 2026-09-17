package bakery.i18n.js

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-3.
 *
 * DSL `bakery { i18nClient { ... } }` mirroring the intention. Defaults are
 * `dryRun = true` so an accidental invocation never calls the metered LLM
 * (Ink Economy Law).
 */
open class I18nClientMigrationIntentionDsl {
    var sourceDirs: List<String> = emptyList()
    var referenceLanguage: String = "fr"
    var targetLanguages: List<String> = listOf("en")
    var dryRun: Boolean = true

    fun toIntention(): I18nClientMigrationIntention =
        I18nClientMigrationIntention(
            sourceDirs = sourceDirs,
            referenceLanguage = referenceLanguage,
            targetLanguages = targetLanguages,
            dryRun = dryRun,
        )
}
