package bakery.i18n

import bakery.BakeryConstants

data class ContentMigrationIntention(
    val sourceDir: String,
    val outputDir: String,
    val sourceLanguage: String = "fr",
    val targetLanguages: List<String> = listOf("en"),
    val dryRun: Boolean = true,
    val excludePaths: List<String> = emptyList(),
    val parallelism: Int = 1
) {
    init {
        require(sourceDir.isNotBlank()) { "Le repertoire source (sourceDir) est obligatoire pour la migration de contenu." }
        require(outputDir.isNotBlank()) { "Le repertoire de sortie (outputDir) est obligatoire pour la migration de contenu." }
        require(targetLanguages.isNotEmpty()) { "Au moins une langue cible est requise." }
        targetLanguages.forEach { lang ->
            require(lang in BakeryConstants.SUPPORTED_LANGS) {
                "Langue cible '$lang' non supportee. Utilisez : ${BakeryConstants.SUPPORTED_LANGS.joinToString()}."
            }
        }
        require(sourceLanguage in BakeryConstants.SUPPORTED_LANGS) {
            "Langue source '$sourceLanguage' non supportee. Utilisez : ${BakeryConstants.SUPPORTED_LANGS.joinToString()}."
        }
        require(sourceLanguage !in targetLanguages) {
            "La langue source '$sourceLanguage' ne peut pas etre une langue cible."
        }
    }
}
