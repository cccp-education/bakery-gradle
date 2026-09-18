package bakery.i18n

import bakery.BakeryConstants

data class ContentMigrationIntention(
    val sourceDir: String,
    val outputDir: String,
    val sourceLanguage: String = "fr",
    val targetLanguages: List<String> = listOf("en"),
    val dryRun: Boolean = true,
    val excludePaths: List<String> = emptyList(),
    val parallelism: Int = 1,
    val validation: String = "LENIENT",
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
        require(validation in setOf("STRICT", "LENIENT", "OFF")) {
            "Mode de validation '$validation' invalide. Utilisez : STRICT, LENIENT, OFF."
        }
        require(parallelism in 1..MAX_PARALLELISM) {
            "Parallélisme '$parallelism' invalide. Utilisez une valeur entre 1 et $MAX_PARALLELISM (provider Ollama au plus)."
        }
    }

    companion object {
        /**
         * CHE-I18N-22 US-8 — pilot decision S-044: the translation may run on up
         * to **twenty-five** Ollama providers at once, the number of ports the
         * pool (11437-11465) can actually serve at once: 29 ports minus three
         * without a container (11438, 11449, 11450) and one whose account hit
         * its monthly quota (11437) at S-044.
         *
         * A single article is never translated by several providers at once
         * (per-article work stays sequential); the concurrency is across
         * articles. Default remains 1 for an unconfigured consumer.
         */
        const val MAX_PARALLELISM = 25
    }
}
