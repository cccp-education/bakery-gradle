package bakery.scaffold

/**
 * DSL bridge pour [ScaffoldIntention] — passage Gradle → domaine.
 *
 * Pattern identique a [bakery.article.ArticleIntentionDsl] :
 * - Open class avec var mutables pour le DSL Gradle
 * - Methode [toIntention()] pour convertir en modele domaine valide
 * - Validateurs dans le modele, pas dans la DSL
 */
open class ScaffoldIntentionDsl {
    var description: String = ""
    var siteType: String = "blog"
    var lang: String = "fr"
    var projectName: String = ""

    /**
     * Convertit la DSL en modele domaine valide.
     *
     * @throws IllegalArgumentException si la description est vide
     * (la validation est deleguee au constructeur de [ScaffoldIntention])
     */
    fun toIntention(): ScaffoldIntention = ScaffoldIntention(
        description = description,
        siteType = ScaffoldSiteType.fromStringOrDefault(siteType),
        lang = lang,
        projectName = projectName
    )
}