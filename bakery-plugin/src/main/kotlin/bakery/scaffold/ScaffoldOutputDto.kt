package bakery.scaffold

/**
 * DTO intermediaire pour le parsing Jackson de la reponse LLM.
 *
 * Separe le contrat JSON (champs optionnels, noms exacts)
 * du domaine [ScaffoldOutput] (champs obligatoires, defaults).
 *
 * Utilise par [ScaffoldGenerator.parseJsonOutput] (CS-3 — Jackson remplace
 * le parsing regex maison).
 *
 * Champs optionnels pour supporter les reponses partielles du LLM.
 */
internal data class ScaffoldOutputDto(
    val siteType: String? = null,
    val projectName: String? = null,
    val description: String? = null,
    val templates: List<String>? = null,
    val metadata: ScaffoldMetadataDto? = null
)

internal data class ScaffoldMetadataDto(
    val title: String? = null,
    val description: String? = null,
    val tags: List<String>? = null,
    val layout: String? = null,
    val language: String? = null
)
