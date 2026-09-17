package bakery.i18n.js

import bakery.BakeryConstants

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-3.
 *
 * Domain object describing a client-side i18n dictionary translation run.
 *
 * The reference language is the floor: every key it owns (minus `.html`
 * variants) must exist in every target language. Targets must be supported by
 * the N0 [BakeryConstants.SUPPORTED_LANGS] contract and differ from the
 * reference.
 */
data class I18nClientMigrationIntention(
    val sourceDirs: List<String>,
    val referenceLanguage: String = "fr",
    val targetLanguages: List<String> = listOf("en"),
    val dryRun: Boolean = true,
) {
    init {
        require(sourceDirs.isNotEmpty()) {
            "Au moins un repertoire source (sourceDirs) est requis pour la traduction i18n client."
        }
        sourceDirs.forEach { dir ->
            require(dir.isNotBlank()) { "Un repertoire source ne peut pas etre vide." }
        }
        require(referenceLanguage in BakeryConstants.SUPPORTED_LANGS) {
            "Langue de reference '$referenceLanguage' non supportee. Utilisez : ${BakeryConstants.SUPPORTED_LANGS.joinToString()}."
        }
        require(targetLanguages.isNotEmpty()) { "Au moins une langue cible est requise." }
        targetLanguages.forEach { lang ->
            require(lang in BakeryConstants.SUPPORTED_LANGS) {
                "Langue cible '$lang' non supportee. Utilisez : ${BakeryConstants.SUPPORTED_LANGS.joinToString()}."
            }
        }
        require(referenceLanguage !in targetLanguages) {
            "La langue de reference '$referenceLanguage' ne peut pas etre une langue cible."
        }
    }
}
