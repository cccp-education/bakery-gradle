package bakery.i18n.deploy

import bakery.BakeryConstants
import bakery.i18n.plantuml.PlantUmlStrategy
import contracts.i18n.LanguageCatalog

/**
 * EPIC BKY-I18N-DEPLOY — US DEPLOY-7.
 *
 * Plan de traduction d'un site vers plusieurs langues cibles.
 *
 * Domaine pur — pas de logique I/O, pas d'appel LLM. Pilote la matrice
 * 8 sites x 10 langues du dogfooding DEPLOY-7. Les méthodes [rtlTargets],
 * [ltrTargets], [missingLanguages], [isComplete] calculent le delta ciblé
 * (Loi de l'Économie d'Encre) à partir des langues déjà présentes sur le site.
 *
 * La stratégie PlantUml par défaut est [PlantUmlStrategy.PreserveTechnical]
 * (sécurité : on préserve par défaut, le classifier affinera au cas par cas).
 */
data class SiteTranslationPlan(
    val siteName: String,
    val sourceLanguage: String,
    val targetLanguages: Set<String>,
    val defaultPlantUmlStrategy: PlantUmlStrategy = PlantUmlStrategy.PreserveTechnical
) {
    init {
        require(siteName.isNotBlank()) {
            "Site name (siteName) is required for the translation plan."
        }
        require(sourceLanguage.isNotBlank()) {
            "Source language (sourceLanguage) is required."
        }
        require(targetLanguages.isNotEmpty()) {
            "At least one target language is required."
        }
        require(sourceLanguage in BakeryConstants.SUPPORTED_LANGS) {
            "Source language '$sourceLanguage' not supported. Use: ${BakeryConstants.SUPPORTED_LANGS.joinToString()}."
        }
        targetLanguages.forEach { lang ->
            require(lang in BakeryConstants.SUPPORTED_LANGS) {
                "Target language '$lang' not supported. Use: ${BakeryConstants.SUPPORTED_LANGS.joinToString()}."
            }
        }
        require(sourceLanguage !in targetLanguages) {
            "Source language '$sourceLanguage' cannot be a target language."
        }
    }

    fun rtlTargets(): Set<String> =
        targetLanguages.filter { LanguageCatalog.findByCode(it)?.rtl == true }.toSet()

    fun ltrTargets(): Set<String> = targetLanguages - rtlTargets()

    fun missingLanguages(existing: Set<String>): Set<String> = targetLanguages - existing

    fun isComplete(existing: Set<String>): Boolean = missingLanguages(existing).isEmpty()

    fun withTargetLanguages(newTargets: Set<String>): SiteTranslationPlan =
        copy(targetLanguages = newTargets)
}