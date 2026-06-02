package bakery.article

/**
 * DSL Gradle pour configurer l'intention de génération d'article.
 *
 * Usage dans `build.gradle.kts` :
 * ```
 * bakery {
 *     articleIntention {
 *         topic = "Kotlin pour Gradle"
 *         ton = "technique"          // informatif | technique | pédagogique | convaincre
 *         audience = "développeur"   // general | développeur | formateur
 *         keywords = listOf("dsl", "plugin")
 *         lang = "fr"               // fr | en
 *     }
 * }
 * ```
 *
 * Pont entre la configuration Gradle (String-based DSL) et le
 * modèle domaine [ArticleIntention] (typed, validated).
 */
open class ArticleIntentionDsl {
    /** Sujet de l'article (obligatoire). */
    var topic: String = ""

    /** Registre stylistique : informatif | technique | pédagogique | convaincre. */
    var ton: String = "informatif"

    /** Public cible : general | développeur | formateur. */
    var audience: String = "general"

    /** Mots-clés SEO/RAG. */
    var keywords: List<String> = emptyList()

    /** Langue de l'article : fr | en. */
    var lang: String = "fr"

    /**
     * Convertit le DSL en modèle domaine validé.
     *
     * @throws IllegalArgumentException si le topic est vide, le ton ou l'audience invalide
     */
    fun toIntention(): ArticleIntention = ArticleIntention(
        topic = topic,
        ton = ArticleTon.entries.firstOrNull { it.name.lowercase() == ton.lowercase() }
            ?: throw IllegalArgumentException("Ton '$ton' inconnu. Valeurs : ${ArticleTon.entries.map { it.name.lowercase() }}"),
        audience = ArticleAudience.entries.firstOrNull { it.name.lowercase() == audience.lowercase() }
            ?: throw IllegalArgumentException("Audience '$audience' inconnue. Valeurs : ${ArticleAudience.entries.map { it.name.lowercase() }}"),
        rawKeywords = keywords,
        lang = lang
    )
}