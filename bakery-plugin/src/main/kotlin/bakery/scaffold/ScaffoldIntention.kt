package bakery.scaffold

import bakery.BakeryConstants

/**
 * Intention de scaffolding de site — modele domaine DDD.
 *
 * Encapsule la description en langage naturel d'un site a generer,
 * guidee par le LLM pour produire une [ScaffoldOutput] structuree.
 *
 * Pattern identique a [bakery.article.ArticleIntention] :
 * - Modele immuable, valide a la construction
 * - Enums typees pour les choix contraints
 * - Methode [toPromptContext] pour guider le LLM
 */
data class ScaffoldIntention(
    val description: String,
    val siteType: ScaffoldSiteType = ScaffoldSiteType.BLOG,
    val lang: String = "fr",
    val projectName: String = ""
) {
    init {
        require(description.isNotBlank()) { "La description est obligatoire pour le scaffolding assiste par IA." }
        require(lang in BakeryConstants.SUPPORTED_LANGS) { "Langue '$lang' non supportee. Utilisez : ${BakeryConstants.SUPPORTED_LANGS.joinToString()}." }
    }

    /**
     * Genere un contexte lisible par le LLM a partir de l'intention.
     */
    fun toPromptContext(): String = buildString {
        appendLine("Description du site : $description")
        appendLine("Type de site : ${siteType.label}")
        appendLine("Langue : $lang")
        if (projectName.isNotBlank()) appendLine("Nom du projet : $projectName")
    }
}

/**
 * Types de sites supportes par le scaffolding IA.
 *
 * Chaque type correspond a un ensemble de templates et de metadonnees JBake
 * que le LLM peut selectionner et configurer.
 */
enum class ScaffoldSiteType(val label: String) {
    BLOG("blog"),
    PORTFOLIO("portfolio"),
    DOC("documentation"),
    FORMATION("formation");

    companion object {
        fun fromStringOrDefault(value: String?): ScaffoldSiteType =
            if (value.isNullOrBlank()) BLOG
            else entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: BLOG
    }
}