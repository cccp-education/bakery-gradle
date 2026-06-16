package bakery.i18n

import bakery.BakeryConstants

data class I18nMigrationIntention(
    val siteDir: String,
    val languages: List<String> = listOf("en"),
    val defaultLanguage: String = "fr",
    val dryRun: Boolean = true
) {
    init {
        require(siteDir.isNotBlank()) { "Le repertoire du site (siteDir) est obligatoire pour la migration i18n." }
        require(languages.isNotEmpty()) { "Au moins une langue cible est requise." }
        languages.forEach { lang ->
            require(lang in BakeryConstants.SUPPORTED_LANGS) {
                "Langue '$lang' non supportee. Utilisez : ${BakeryConstants.SUPPORTED_LANGS.joinToString()}."
            }
        }
        require(defaultLanguage in BakeryConstants.SUPPORTED_LANGS) {
            "Langue par defaut '$defaultLanguage' non supportee. Utilisez : ${BakeryConstants.SUPPORTED_LANGS.joinToString()}."
        }
    }

    fun toPromptContext(): String = buildString {
        appendLine("Repertoire du site : $siteDir")
        appendLine("Langues cibles : ${languages.joinToString(", ")}")
        appendLine("Langue par defaut : $defaultLanguage")
        appendLine("Mode dry-run : $dryRun")
    }
}
